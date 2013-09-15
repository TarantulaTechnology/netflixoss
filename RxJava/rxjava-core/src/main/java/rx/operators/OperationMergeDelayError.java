/**
 * Copyright 2013 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Subscription;
import rx.util.CompositeException;

/**
 * This behaves like {@link OperationMerge} except that if any of the merged Observables notify of
 * an error via <code>onError</code>, mergeDelayError will refrain from propagating that error
 * notification until all of the merged Observables have finished emitting items.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/mergeDelayError.png">
 * <p>
 * Even if multiple merged Observables send <code>onError</code> notifications, mergeDelayError will
 * only invoke the <code>onError</code> method of its Observers once.
 * <p>
 * This operation allows an Observer to receive all successfully emitted items from all of the
 * source Observables without being interrupted by an error notification from one of them.
 * <p>
 * NOTE: If this is used on an Observable that never completes, it will never call
 * <code>onError</code> and will effectively swallow errors.
 */
public final class OperationMergeDelayError {

    /**
     * Flattens the observable sequences from the list of Observables into one observable sequence
     * without any transformation and delays any onError calls until after all sequences have called
     * onError or onComplete so as to allow all successful onNext calls to be received.
     * 
     * @param sequences
     *            An observable sequence of elements to project.
     * @return An observable sequence whose elements are the result of flattening the output from the list of Observables.
     * @see <a href="http://msdn.microsoft.com/en-us/library/hh229099(v=vs.103).aspx">Observable.Merge(TSource) Method (IObservable(TSource)[])</a>
     */
    public static <T> OnSubscribeFunc<T> mergeDelayError(final Observable<? extends Observable<? extends T>> sequences) {
        // wrap in a Func so that if a chain is built up, then asynchronously subscribed to twice we will have 2 instances of Take<T> rather than 1 handing both, which is not thread-safe.
        return new OnSubscribeFunc<T>() {

            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {
                return new MergeDelayErrorObservable<T>(sequences).onSubscribe(observer);
            }
        };
    }

    public static <T> OnSubscribeFunc<T> mergeDelayError(final Observable<? extends T>... sequences) {
        return mergeDelayError(Observable.create(new OnSubscribeFunc<Observable<? extends T>>() {
            private volatile boolean unsubscribed = false;

            @Override
            public Subscription onSubscribe(Observer<? super Observable<? extends T>> observer) {
                for (Observable<? extends T> o : sequences) {
                    if (!unsubscribed) {
                        observer.onNext(o);
                    } else {
                        // break out of the loop if we are unsubscribed
                        break;
                    }
                }
                if (!unsubscribed) {
                    observer.onCompleted();
                }
                return new Subscription() {

                    @Override
                    public void unsubscribe() {
                        unsubscribed = true;
                    }

                };
            }
        }));
    }

    public static <T> OnSubscribeFunc<T> mergeDelayError(final List<? extends Observable<? extends T>> sequences) {
        return mergeDelayError(Observable.create(new OnSubscribeFunc<Observable<? extends T>>() {

            private volatile boolean unsubscribed = false;

            @Override
            public Subscription onSubscribe(Observer<? super Observable<? extends T>> observer) {
                for (Observable<? extends T> o : sequences) {
                    if (!unsubscribed) {
                        observer.onNext(o);
                    } else {
                        // break out of the loop if we are unsubscribed
                        break;
                    }
                }
                if (!unsubscribed) {
                    observer.onCompleted();
                }

                return new Subscription() {

                    @Override
                    public void unsubscribe() {
                        unsubscribed = true;
                    }

                };
            }
        }));
    }

    /**
     * This class is NOT thread-safe if invoked and referenced multiple times. In other words, don't subscribe to it multiple times from different threads.
     * <p>
     * It IS thread-safe from within it while receiving onNext events from multiple threads.
     * <p>
     * This should all be fine as long as it's kept as a private class and a new instance created from static factory method above.
     * <p>
     * Note how the take() factory method above protects us from a single instance being exposed with the Observable wrapper handling the subscribe flow.
     * 
     * @param <T>
     */
    private static final class MergeDelayErrorObservable<T> implements OnSubscribeFunc<T> {
        private final Observable<? extends Observable<? extends T>> sequences;
        private final MergeSubscription ourSubscription = new MergeSubscription();
        private AtomicBoolean stopped = new AtomicBoolean(false);
        private volatile boolean parentCompleted = false;
        private final ConcurrentHashMap<ChildObserver, ChildObserver> childObservers = new ConcurrentHashMap<ChildObserver, ChildObserver>();
        private final ConcurrentHashMap<ChildObserver, Subscription> childSubscriptions = new ConcurrentHashMap<ChildObserver, Subscription>();
        // onErrors we received that will be delayed until everything is completed and then sent
        private ConcurrentLinkedQueue<Throwable> onErrorReceived = new ConcurrentLinkedQueue<Throwable>();

        private MergeDelayErrorObservable(Observable<? extends Observable<? extends T>> sequences) {
            this.sequences = sequences;
        }

        public Subscription onSubscribe(Observer<? super T> actualObserver) {
            /**
             * Subscribe to the parent Observable to get to the children Observables
             */
            sequences.subscribe(new ParentObserver(actualObserver));

            /* return our subscription to allow unsubscribing */
            return ourSubscription;
        }

        /**
         * Manage the internal subscription with a thread-safe means of stopping/unsubscribing so we don't unsubscribe twice.
         * <p>
         * Also has the stop() method returning a boolean so callers know if their thread "won" and should perform further actions.
         */
        private class MergeSubscription implements Subscription {

            @Override
            public void unsubscribe() {
                stop();
            }

            public boolean stop() {
                // try setting to false unless another thread beat us
                boolean didSet = stopped.compareAndSet(false, true);
                if (didSet) {
                    // this thread won the race to stop, so unsubscribe from the actualSubscription
                    for (Subscription _s : childSubscriptions.values()) {
                        _s.unsubscribe();
                    }
                    return true;
                } else {
                    // another thread beat us
                    return false;
                }
            }
        }

        /**
         * Subscribe to the top level Observable to receive the sequence of Observable<T> children.
         * 
         * @param <T>
         */
        private class ParentObserver implements Observer<Observable<? extends T>> {
            private final Observer<? super T> actualObserver;

            public ParentObserver(Observer<? super T> actualObserver) {
                this.actualObserver = actualObserver;
            }

            @Override
            public void onCompleted() {
                parentCompleted = true;
                // this *can* occur before the children are done, so if it does we won't send onCompleted
                // but will let the child worry about it
                // if however this completes and there are no children processing, then we will send onCompleted

                if (childObservers.size() == 0) {
                    if (!stopped.get()) {
                        if (ourSubscription.stop()) {
                            if (onErrorReceived.size() == 1) {
                                // an onError was received from 1 ChildObserver so we now send it as a delayed error
                                actualObserver.onError(onErrorReceived.peek());
                            } else if (onErrorReceived.size() > 1) {
                                // an onError was received from more than 1 ChildObserver so we now send it as a delayed error
                                actualObserver.onError(new CompositeException(onErrorReceived));
                            } else {
                                // no delayed error so send onCompleted
                                actualObserver.onCompleted();
                            }
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable e) {
                actualObserver.onError(e);
            }

            @Override
            public void onNext(Observable<? extends T> childObservable) {
                if (stopped.get()) {
                    // we won't act on any further items
                    return;
                }

                if (childObservable == null) {
                    throw new IllegalArgumentException("Observable<T> can not be null.");
                }

                /**
                 * For each child Observable we receive we'll subscribe with a separate Observer
                 * that will each then forward their sequences to the actualObserver.
                 * <p>
                 * We use separate child Observers for each sequence to simplify the onComplete/onError handling so each sequence has its own lifecycle.
                 */
                ChildObserver _w = new ChildObserver(actualObserver);
                childObservers.put(_w, _w);
                Subscription _subscription = childObservable.subscribe(_w);
                // remember this Observer and the subscription from it
                childSubscriptions.put(_w, _subscription);
            }
        }

        /**
         * Subscribe to each child Observable<T> and forward their sequence of data to the actualObserver
         * 
         */
        private class ChildObserver implements Observer<T> {

            private final Observer<? super T> actualObserver;
            private volatile boolean finished = false;

            public ChildObserver(Observer<? super T> actualObserver) {
                this.actualObserver = actualObserver;
            }

            @Override
            public void onCompleted() {
                // remove self from map of Observers
                childObservers.remove(this);
                // if there are now 0 Observers left, so if the parent is also completed we send the onComplete to the actualObserver
                // if the parent is not complete that means there is another sequence (and child Observer) to come
                if (!stopped.get()) {
                    finishObserver();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (!stopped.get()) {
                    onErrorReceived.add(e);
                    // mark this ChildObserver as done
                    childObservers.remove(this);
                    // but do NOT forward to actualObserver as we want other ChildObservers to continue until completion
                    // and we'll delay the sending of onError until all others are done

                    // we mark finished==true as a safety to ensure that if further calls to onNext occur we ignore them
                    finished = true;

                    // check for whether the parent is completed and if so then perform the 'finishing' actions
                    finishObserver();
                }
            }

            /**
             * onComplete and onError when called need to check for the parent being complete and if so send the onCompleted or onError to the actualObserver.
             * <p>
             * This does NOT get invoked if synchronous execution occurs, but will when asynchronously executing.
             * <p>
             * TestCase testErrorDelayed4WithThreading specifically tests this use case.
             */
            private void finishObserver() {
                if (childObservers.size() == 0 && parentCompleted) {
                    if (ourSubscription.stop()) {
                        // this thread 'won' the race to unsubscribe/stop so let's send onError or onCompleted
                        if (onErrorReceived.size() == 1) {
                            // an onError was received from 1 ChildObserver so we now send it as a delayed error
                            actualObserver.onError(onErrorReceived.peek());
                        } else if (onErrorReceived.size() > 1) {
                            // an onError was received from more than 1 ChildObserver so we now send it as a delayed error
                            actualObserver.onError(new CompositeException(onErrorReceived));
                        } else {
                            // no delayed error so send onCompleted
                            actualObserver.onCompleted();
                        }
                    }
                }
            }

            @Override
            public void onNext(T args) {
                // in case the Observable is poorly behaved and doesn't listen to the unsubscribe request
                // we'll ignore anything that comes in after we've unsubscribed or an onError has been received and delayed
                if (!stopped.get() && !finished) {
                    actualObserver.onNext(args);
                }
            }

        }
    }

    public static class UnitTest {
        @Mock
        Observer<String> stringObserver;

        @Before
        public void before() {
            MockitoAnnotations.initMocks(this);
        }

        @Test
        public void testErrorDelayed1() {
            final Observable<String> o1 = Observable.create(new TestErrorObservable("four", null, "six")); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
            final Observable<String> o2 = Observable.create(new TestErrorObservable("one", "two", "three"));

            @SuppressWarnings("unchecked")
            Observable<String> m = Observable.create(mergeDelayError(o1, o2));
            m.subscribe(stringObserver);

            verify(stringObserver, times(1)).onError(any(NullPointerException.class));
            verify(stringObserver, never()).onCompleted();
            verify(stringObserver, times(1)).onNext("one");
            verify(stringObserver, times(1)).onNext("two");
            verify(stringObserver, times(1)).onNext("three");
            verify(stringObserver, times(1)).onNext("four");
            verify(stringObserver, times(0)).onNext("five");
            verify(stringObserver, times(0)).onNext("six");
        }

        @Test
        public void testErrorDelayed2() {
            final Observable<String> o1 = Observable.create(new TestErrorObservable("one", "two", "three"));
            final Observable<String> o2 = Observable.create(new TestErrorObservable("four", null, "six")); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
            final Observable<String> o3 = Observable.create(new TestErrorObservable("seven", "eight", null));
            final Observable<String> o4 = Observable.create(new TestErrorObservable("nine"));

            @SuppressWarnings("unchecked")
            Observable<String> m = Observable.create(mergeDelayError(o1, o2, o3, o4));
            m.subscribe(stringObserver);

            verify(stringObserver, times(1)).onError(any(NullPointerException.class));
            verify(stringObserver, never()).onCompleted();
            verify(stringObserver, times(1)).onNext("one");
            verify(stringObserver, times(1)).onNext("two");
            verify(stringObserver, times(1)).onNext("three");
            verify(stringObserver, times(1)).onNext("four");
            verify(stringObserver, times(0)).onNext("five");
            verify(stringObserver, times(0)).onNext("six");
            verify(stringObserver, times(1)).onNext("seven");
            verify(stringObserver, times(1)).onNext("eight");
            verify(stringObserver, times(1)).onNext("nine");
        }

        @Test
        public void testErrorDelayed3() {
            final Observable<String> o1 = Observable.create(new TestErrorObservable("one", "two", "three"));
            final Observable<String> o2 = Observable.create(new TestErrorObservable("four", "five", "six"));
            final Observable<String> o3 = Observable.create(new TestErrorObservable("seven", "eight", null));
            final Observable<String> o4 = Observable.create(new TestErrorObservable("nine"));

            @SuppressWarnings("unchecked")
            Observable<String> m = Observable.create(mergeDelayError(o1, o2, o3, o4));
            m.subscribe(stringObserver);

            verify(stringObserver, times(1)).onError(any(NullPointerException.class));
            verify(stringObserver, never()).onCompleted();
            verify(stringObserver, times(1)).onNext("one");
            verify(stringObserver, times(1)).onNext("two");
            verify(stringObserver, times(1)).onNext("three");
            verify(stringObserver, times(1)).onNext("four");
            verify(stringObserver, times(1)).onNext("five");
            verify(stringObserver, times(1)).onNext("six");
            verify(stringObserver, times(1)).onNext("seven");
            verify(stringObserver, times(1)).onNext("eight");
            verify(stringObserver, times(1)).onNext("nine");
        }

        @Test
        public void testErrorDelayed4() {
            final Observable<String> o1 = Observable.create(new TestErrorObservable("one", "two", "three"));
            final Observable<String> o2 = Observable.create(new TestErrorObservable("four", "five", "six"));
            final Observable<String> o3 = Observable.create(new TestErrorObservable("seven", "eight"));
            final Observable<String> o4 = Observable.create(new TestErrorObservable("nine", null));

            @SuppressWarnings("unchecked")
            Observable<String> m = Observable.create(mergeDelayError(o1, o2, o3, o4));
            m.subscribe(stringObserver);

            verify(stringObserver, times(1)).onError(any(NullPointerException.class));
            verify(stringObserver, never()).onCompleted();
            verify(stringObserver, times(1)).onNext("one");
            verify(stringObserver, times(1)).onNext("two");
            verify(stringObserver, times(1)).onNext("three");
            verify(stringObserver, times(1)).onNext("four");
            verify(stringObserver, times(1)).onNext("five");
            verify(stringObserver, times(1)).onNext("six");
            verify(stringObserver, times(1)).onNext("seven");
            verify(stringObserver, times(1)).onNext("eight");
            verify(stringObserver, times(1)).onNext("nine");
        }

        @Test
        public void testErrorDelayed4WithThreading() {
            final TestAsyncErrorObservable o1 = new TestAsyncErrorObservable("one", "two", "three");
            final TestAsyncErrorObservable o2 = new TestAsyncErrorObservable("four", "five", "six");
            final TestAsyncErrorObservable o3 = new TestAsyncErrorObservable("seven", "eight");
            // throw the error at the very end so no onComplete will be called after it
            final TestAsyncErrorObservable o4 = new TestAsyncErrorObservable("nine", null);

            @SuppressWarnings("unchecked")
            Observable<String> m = Observable.create(mergeDelayError(Observable.create(o1), Observable.create(o2), Observable.create(o3), Observable.create(o4)));
            m.subscribe(stringObserver);

            try {
                o1.t.join();
                o2.t.join();
                o3.t.join();
                o4.t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            verify(stringObserver, times(1)).onError(any(NullPointerException.class));
            verify(stringObserver, never()).onCompleted();
            verify(stringObserver, times(1)).onNext("one");
            verify(stringObserver, times(1)).onNext("two");
            verify(stringObserver, times(1)).onNext("three");
            verify(stringObserver, times(1)).onNext("four");
            verify(stringObserver, times(1)).onNext("five");
            verify(stringObserver, times(1)).onNext("six");
            verify(stringObserver, times(1)).onNext("seven");
            verify(stringObserver, times(1)).onNext("eight");
            verify(stringObserver, times(1)).onNext("nine");
        }

        @Test
        public void testCompositeErrorDelayed1() {
            final Observable<String> o1 = Observable.create(new TestErrorObservable("four", null, "six")); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
            final Observable<String> o2 = Observable.create(new TestErrorObservable("one", "two", null));

            @SuppressWarnings("unchecked")
            Observable<String> m = Observable.create(mergeDelayError(o1, o2));
            m.subscribe(stringObserver);

            verify(stringObserver, times(1)).onError(any(CompositeException.class));
            verify(stringObserver, never()).onCompleted();
            verify(stringObserver, times(1)).onNext("one");
            verify(stringObserver, times(1)).onNext("two");
            verify(stringObserver, times(0)).onNext("three");
            verify(stringObserver, times(1)).onNext("four");
            verify(stringObserver, times(0)).onNext("five");
            verify(stringObserver, times(0)).onNext("six");
        }

        @Test
        public void testCompositeErrorDelayed2() {
            final Observable<String> o1 = Observable.create(new TestErrorObservable("four", null, "six")); // we expect to lose "six" from the source (and it should never be sent by the source since onError was called
            final Observable<String> o2 = Observable.create(new TestErrorObservable("one", "two", null));

            @SuppressWarnings("unchecked")
            Observable<String> m = Observable.create(mergeDelayError(o1, o2));
            CaptureObserver w = new CaptureObserver();
            m.subscribe(w);

            assertNotNull(w.e);
            if (w.e instanceof CompositeException) {
                assertEquals(2, ((CompositeException) w.e).getExceptions().size());
                w.e.printStackTrace();
            } else {
                fail("Expecting CompositeException");
            }

        }

        /**
         * The unit tests below are from OperationMerge and should ensure the normal merge functionality is correct.
         */

        @Test
        public void testMergeObservableOfObservables() {
            final Observable<String> o1 = Observable.create(new TestSynchronousObservable());
            final Observable<String> o2 = Observable.create(new TestSynchronousObservable());

            Observable<Observable<String>> observableOfObservables = Observable.create(new OnSubscribeFunc<Observable<String>>() {

                @Override
                public Subscription onSubscribe(Observer<? super Observable<String>> observer) {
                    // simulate what would happen in an observable
                    observer.onNext(o1);
                    observer.onNext(o2);
                    observer.onCompleted();

                    return new Subscription() {

                        @Override
                        public void unsubscribe() {
                            // unregister ... will never be called here since we are executing synchronously
                        }

                    };
                }

            });
            Observable<String> m = Observable.create(mergeDelayError(observableOfObservables));
            m.subscribe(stringObserver);

            verify(stringObserver, never()).onError(any(Throwable.class));
            verify(stringObserver, times(1)).onCompleted();
            verify(stringObserver, times(2)).onNext("hello");
        }

        @Test
        public void testMergeArray() {
            final Observable<String> o1 = Observable.create(new TestSynchronousObservable());
            final Observable<String> o2 = Observable.create(new TestSynchronousObservable());

            @SuppressWarnings("unchecked")
            Observable<String> m = Observable.create(mergeDelayError(o1, o2));
            m.subscribe(stringObserver);

            verify(stringObserver, never()).onError(any(Throwable.class));
            verify(stringObserver, times(2)).onNext("hello");
            verify(stringObserver, times(1)).onCompleted();
        }

        @Test
        public void testMergeList() {
            final Observable<String> o1 = Observable.create(new TestSynchronousObservable());
            final Observable<String> o2 = Observable.create(new TestSynchronousObservable());
            List<Observable<String>> listOfObservables = new ArrayList<Observable<String>>();
            listOfObservables.add(o1);
            listOfObservables.add(o2);

            Observable<String> m = Observable.create(mergeDelayError(listOfObservables));
            m.subscribe(stringObserver);

            verify(stringObserver, never()).onError(any(Throwable.class));
            verify(stringObserver, times(1)).onCompleted();
            verify(stringObserver, times(2)).onNext("hello");
        }

        @Test
        public void testUnSubscribe() {
            TestObservable tA = new TestObservable();
            TestObservable tB = new TestObservable();

            @SuppressWarnings("unchecked")
            Observable<String> m = Observable.create(mergeDelayError(Observable.create(tA), Observable.create(tB)));
            Subscription s = m.subscribe(stringObserver);

            tA.sendOnNext("Aone");
            tB.sendOnNext("Bone");
            s.unsubscribe();
            tA.sendOnNext("Atwo");
            tB.sendOnNext("Btwo");
            tA.sendOnCompleted();
            tB.sendOnCompleted();

            verify(stringObserver, never()).onError(any(Throwable.class));
            verify(stringObserver, times(1)).onNext("Aone");
            verify(stringObserver, times(1)).onNext("Bone");
            assertTrue(tA.unsubscribed);
            assertTrue(tB.unsubscribed);
            verify(stringObserver, never()).onNext("Atwo");
            verify(stringObserver, never()).onNext("Btwo");
            verify(stringObserver, never()).onCompleted();
        }

        @Test
        public void testMergeArrayWithThreading() {
            final TestASynchronousObservable o1 = new TestASynchronousObservable();
            final TestASynchronousObservable o2 = new TestASynchronousObservable();

            @SuppressWarnings("unchecked")
            Observable<String> m = Observable.create(mergeDelayError(Observable.create(o1), Observable.create(o2)));
            m.subscribe(stringObserver);

            try {
                o1.t.join();
                o2.t.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            verify(stringObserver, never()).onError(any(Throwable.class));
            verify(stringObserver, times(2)).onNext("hello");
            verify(stringObserver, times(1)).onCompleted();
        }

        private static class TestSynchronousObservable implements OnSubscribeFunc<String> {

            @Override
            public Subscription onSubscribe(Observer<? super String> observer) {

                observer.onNext("hello");
                observer.onCompleted();

                return new Subscription() {

                    @Override
                    public void unsubscribe() {
                        // unregister ... will never be called here since we are executing synchronously
                    }

                };
            }
        }

        private static class TestASynchronousObservable implements OnSubscribeFunc<String> {
            Thread t;

            @Override
            public Subscription onSubscribe(final Observer<? super String> observer) {
                t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        observer.onNext("hello");
                        observer.onCompleted();
                    }

                });
                t.start();

                return new Subscription() {

                    @Override
                    public void unsubscribe() {

                    }

                };
            }
        }

        /**
         * A Observable that doesn't do the right thing on UnSubscribe/Error/etc in that it will keep sending events down the pipe regardless of what happens.
         */
        private static class TestObservable implements OnSubscribeFunc<String> {

            Observer<? super String> observer = null;
            volatile boolean unsubscribed = false;
            Subscription s = new Subscription() {

                @Override
                public void unsubscribe() {
                    unsubscribed = true;

                }

            };

            /* used to simulate subscription */
            public void sendOnCompleted() {
                observer.onCompleted();
            }

            /* used to simulate subscription */
            public void sendOnNext(String value) {
                observer.onNext(value);
            }

            /* used to simulate subscription */
            @SuppressWarnings("unused")
            public void sendOnError(Throwable e) {
                observer.onError(e);
            }

            @Override
            public Subscription onSubscribe(final Observer<? super String> observer) {
                this.observer = observer;
                return s;
            }
        }

        private static class TestErrorObservable implements OnSubscribeFunc<String> {

            String[] valuesToReturn;

            TestErrorObservable(String... values) {
                valuesToReturn = values;
            }

            @Override
            public Subscription onSubscribe(Observer<? super String> observer) {
                boolean errorThrown = false;
                for (String s : valuesToReturn) {
                    if (s == null) {
                        System.out.println("throwing exception");
                        observer.onError(new NullPointerException());
                        errorThrown = true;
                        // purposefully not returning here so it will continue calling onNext
                        // so that we also test that we handle bad sequences like this
                    } else {
                        observer.onNext(s);
                    }
                }
                if (!errorThrown) {
                    observer.onCompleted();
                }

                return new Subscription() {

                    @Override
                    public void unsubscribe() {
                        // unregister ... will never be called here since we are executing synchronously
                    }

                };
            }
        }

        private static class TestAsyncErrorObservable implements OnSubscribeFunc<String> {

            String[] valuesToReturn;

            TestAsyncErrorObservable(String... values) {
                valuesToReturn = values;
            }

            Thread t;

            @Override
            public Subscription onSubscribe(final Observer<? super String> observer) {
                t = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        for (String s : valuesToReturn) {
                            if (s == null) {
                                System.out.println("throwing exception");
                                try {
                                    Thread.sleep(100);
                                } catch (Throwable e) {

                                }
                                observer.onError(new NullPointerException());
                                return;
                            } else {
                                observer.onNext(s);
                            }
                        }
                        System.out.println("subscription complete");
                        observer.onCompleted();
                    }

                });
                t.start();

                return new Subscription() {

                    @Override
                    public void unsubscribe() {

                    }

                };
            }
        }

        private static class CaptureObserver implements Observer<String> {
            volatile Throwable e;

            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {
                this.e = e;
            }

            @Override
            public void onNext(String args) {

            }

        }
    }
}
