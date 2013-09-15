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
package rx.concurrency;

import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Scheduler;
import rx.Subscription;
import rx.util.functions.Action0;
import rx.util.functions.Func2;

/**
 * Executes work immediately on the current thread.
 */
public final class ImmediateScheduler extends Scheduler {
    private static final ImmediateScheduler INSTANCE = new ImmediateScheduler();

    public static ImmediateScheduler getInstance() {
        return INSTANCE;
    }

    private ImmediateScheduler() {
    }

    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action) {
        return action.call(this, state);
    }

    @Override
    public <T> Subscription schedule(T state, Func2<? super Scheduler, ? super T, ? extends Subscription> action, long dueTime, TimeUnit unit) {
        // since we are executing immediately on this thread we must cause this thread to sleep
        long execTime = now() + unit.toMillis(dueTime);

        return schedule(state, new SleepingAction<T>(action, this, execTime));
    }

    public static class UnitTest {

        @Test
        public void testNestedActions() {
            final ImmediateScheduler scheduler = new ImmediateScheduler();

            final Action0 firstStepStart = mock(Action0.class);
            final Action0 firstStepEnd = mock(Action0.class);

            final Action0 secondStepStart = mock(Action0.class);
            final Action0 secondStepEnd = mock(Action0.class);

            final Action0 thirdStepStart = mock(Action0.class);
            final Action0 thirdStepEnd = mock(Action0.class);

            final Action0 firstAction = new Action0() {
                @Override
                public void call() {
                    firstStepStart.call();
                    firstStepEnd.call();
                }
            };
            final Action0 secondAction = new Action0() {
                @Override
                public void call() {
                    secondStepStart.call();
                    scheduler.schedule(firstAction);
                    secondStepEnd.call();

                }
            };
            final Action0 thirdAction = new Action0() {
                @Override
                public void call() {
                    thirdStepStart.call();
                    scheduler.schedule(secondAction);
                    thirdStepEnd.call();
                }
            };

            InOrder inOrder = inOrder(firstStepStart, firstStepEnd, secondStepStart, secondStepEnd, thirdStepStart, thirdStepEnd);

            scheduler.schedule(thirdAction);

            inOrder.verify(thirdStepStart, times(1)).call();
            inOrder.verify(secondStepStart, times(1)).call();
            inOrder.verify(firstStepStart, times(1)).call();
            inOrder.verify(firstStepEnd, times(1)).call();
            inOrder.verify(secondStepEnd, times(1)).call();
            inOrder.verify(thirdStepEnd, times(1)).call();
        }

    }

}
