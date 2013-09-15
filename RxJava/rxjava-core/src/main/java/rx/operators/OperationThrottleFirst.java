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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.concurrency.Schedulers;
import rx.concurrency.TestScheduler;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func1;

/**
 * Throttle by windowing a stream and returning the first value in each window.
 */
public final class OperationThrottleFirst {

    /**
     * Throttles to first value in each window.
     * 
     * @param items
     *            The {@link Observable} which is publishing events.
     * @param windowDuration
     *            Duration of windows within with the first value will be chosen.
     * @param unit
     *            The unit of time for the specified timeout.
     * @return A {@link Func1} which performs the throttle operation.
     */
    public static <T> OnSubscribeFunc<T> throttleFirst(Observable<T> items, long windowDuration, TimeUnit unit) {
        return throttleFirst(items, windowDuration, unit, Schedulers.threadPoolForComputation());
    }

    /**
     * Throttles to first value in each window.
     * 
     * @param items
     *            The {@link Observable} which is publishing events.
     * @param windowDuration
     *            Duration of windows within with the first value will be chosen.
     * @param unit
     *            The unit of time for the specified timeout.
     * @param scheduler
     *            The {@link Scheduler} to use internally to manage the timers which handle timeout for each event.
     * @return A {@link Func1} which performs the throttle operation.
     */
    public static <T> OnSubscribeFunc<T> throttleFirst(final Observable<T> items, final long windowDuration, final TimeUnit unit, final Scheduler scheduler) {
        return new OnSubscribeFunc<T>() {
            @Override
            public Subscription onSubscribe(Observer<? super T> observer) {

                final AtomicLong lastOnNext = new AtomicLong(0);
                final long timeInMilliseconds = unit.toMillis(windowDuration);

                return items.filter(new Func1<T, Boolean>() {

                    @Override
                    public Boolean call(T value) {
                        long now = scheduler.now();
                        if (lastOnNext.get() == 0 || now - lastOnNext.get() >= timeInMilliseconds) {
                            lastOnNext.set(now);
                            return Boolean.TRUE;
                        } else {
                            return Boolean.FALSE;
                        }
                    }

                }).subscribe(observer);
            }
        };
    }

    public static class UnitTest {

        private TestScheduler scheduler;
        private Observer<String> observer;

        @Before
        @SuppressWarnings("unchecked")
        public void before() {
            scheduler = new TestScheduler();
            observer = mock(Observer.class);
        }

        @Test
        public void testThrottlingWithCompleted() {
            Observable<String> source = Observable.create(new OnSubscribeFunc<String>() {
                @Override
                public Subscription onSubscribe(Observer<? super String> observer) {
                    publishNext(observer, 100, "one");    // publish as it's first
                    publishNext(observer, 300, "two");    // skip as it's last within the first 400
                    publishNext(observer, 900, "three");   // publish
                    publishNext(observer, 905, "four");   // skip
                    publishCompleted(observer, 1000);     // Should be published as soon as the timeout expires.

                    return Subscriptions.empty();
                }
            });

            Observable<String> sampled = Observable.create(OperationThrottleFirst.throttleFirst(source, 400, TimeUnit.MILLISECONDS, scheduler));
            sampled.subscribe(observer);

            InOrder inOrder = inOrder(observer);

            scheduler.advanceTimeTo(1000, TimeUnit.MILLISECONDS);
            inOrder.verify(observer, times(1)).onNext("one");
            inOrder.verify(observer, times(0)).onNext("two");
            inOrder.verify(observer, times(1)).onNext("three");
            inOrder.verify(observer, times(0)).onNext("four");
            inOrder.verify(observer, times(1)).onCompleted();
            inOrder.verifyNoMoreInteractions();
        }

        @Test
        public void testThrottlingWithError() {
            Observable<String> source = Observable.create(new OnSubscribeFunc<String>() {
                @Override
                public Subscription onSubscribe(Observer<? super String> observer) {
                    Exception error = new TestException();
                    publishNext(observer, 100, "one");    // Should be published since it is first
                    publishNext(observer, 200, "two");    // Should be skipped since onError will arrive before the timeout expires
                    publishError(observer, 300, error);   // Should be published as soon as the timeout expires.

                    return Subscriptions.empty();
                }
            });

            Observable<String> sampled = Observable.create(OperationThrottleFirst.throttleFirst(source, 400, TimeUnit.MILLISECONDS, scheduler));
            sampled.subscribe(observer);

            InOrder inOrder = inOrder(observer);

            scheduler.advanceTimeTo(400, TimeUnit.MILLISECONDS);
            inOrder.verify(observer).onNext("one");
            inOrder.verify(observer).onError(any(TestException.class));
            inOrder.verifyNoMoreInteractions();
        }

        private <T> void publishCompleted(final Observer<T> observer, long delay) {
            scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    observer.onCompleted();
                }
            }, delay, TimeUnit.MILLISECONDS);
        }

        private <T> void publishError(final Observer<T> observer, long delay, final Exception error) {
            scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    observer.onError(error);
                }
            }, delay, TimeUnit.MILLISECONDS);
        }

        private <T> void publishNext(final Observer<T> observer, long delay, final T value) {
            scheduler.schedule(new Action0() {
                @Override
                public void call() {
                    observer.onNext(value);
                }
            }, delay, TimeUnit.MILLISECONDS);
        }

        @SuppressWarnings("serial")
        private class TestException extends Exception {
        }

    }

}
