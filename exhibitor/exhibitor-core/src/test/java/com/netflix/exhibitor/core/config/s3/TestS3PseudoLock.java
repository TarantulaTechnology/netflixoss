/*
 * Copyright 2012 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.exhibitor.core.config.s3;

import com.google.common.collect.Lists;
import com.netflix.exhibitor.core.activity.ActivityLog;
import com.netflix.exhibitor.core.backup.s3.MockS3Client;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TestS3PseudoLock
{
    @Test
    public void  testGCOldLockFiles() throws Exception
    {
        final BlockingQueue<String>   queue = new ArrayBlockingQueue<String>(1);
        ActivityLog                   mockLog = Mockito.mock(ActivityLog.class);
        MockS3Client                  client = new MockS3Client(null, null)
        {
            @Override
            public void deleteObject(String bucket, String key) throws Exception
            {
                queue.put(key);
            }
        };
        S3PseudoLock        lock = new S3PseudoLock(client, "foo", "bar", 10, 10, 0);
        lock.lock(mockLog, 1, TimeUnit.DAYS);
        Thread.sleep(20);
        S3PseudoLock        lock2 = new S3PseudoLock(client, "foo", "bar", 10, 10, 0);
        lock2.lock(mockLog, 1, TimeUnit.DAYS);   // should clean the previous lock

        String              cleaned = queue.poll(5, TimeUnit.SECONDS);
        Assert.assertNotNull(cleaned);
    }

    @Test
    public void         testBlocking() throws Exception
    {
        final int       QTY = 5;
        final int       POLLING_MS = 1;

        final AtomicBoolean             isLocked = new AtomicBoolean(false);
        final AtomicInteger             lockCount = new AtomicInteger(0);

        final MockS3Client              client = new MockS3Client(null, null);
        final ActivityLog               mockLog = Mockito.mock(ActivityLog.class);

        ExecutorCompletionService<Void> completionService = new ExecutorCompletionService<Void>(Executors.newFixedThreadPool(QTY));
        for ( int i = 0; i < QTY; ++i )
        {
            completionService.submit
            (
                new Callable<Void>()
                {
                    @Override
                    public Void call() throws Exception
                    {
                        S3PseudoLock lock = new S3PseudoLock(client, "foo", "bar", Integer.MAX_VALUE, POLLING_MS, 0);
                        try
                        {
                            Assert.assertTrue(lock.lock(mockLog, 10, TimeUnit.SECONDS));
                            Assert.assertTrue(isLocked.compareAndSet(false, true));
                            lockCount.incrementAndGet();
                            Thread.sleep(POLLING_MS);
                        }
                        finally
                        {
                            if ( isLocked.compareAndSet(true, false) )
                            {
                                lock.unlock();
                            }
                        }
                        return null;
                    }
                }
            );
            Thread.sleep(1);
        }

        for ( int i = 0; i < QTY; ++i )
        {
            completionService.take().get();
        }

        Assert.assertEquals(lockCount.get(), QTY);
    }

    @Test
    public void         testMulti() throws Exception
    {
        final int       QTY = 5;
        final int       POLLING_MS = 1;

        ActivityLog     mockLog = Mockito.mock(ActivityLog.class);

        List<S3PseudoLock>  locks = Lists.newArrayList();
        for ( int i = 0; i < QTY; ++i )
        {
            MockS3Client        client = new MockS3Client();
            S3PseudoLock        lock = new S3PseudoLock(client, "foo", "-prefix-", 10000, POLLING_MS, 0);
            locks.add(lock);
        }

        for ( S3PseudoLock lock : locks )
        {
            Assert.assertTrue(lock.lock(mockLog, 5, TimeUnit.SECONDS));
            try
            {
                //noinspection PointlessArithmeticExpression
                Thread.sleep(POLLING_MS * 2);
            }
            finally
            {
                lock.unlock();
            }
        }
    }

    @Test
    public void         testSingle() throws Exception
    {
        MockS3Client        client = new MockS3Client();
        ActivityLog         mockLog = Mockito.mock(ActivityLog.class);

        S3PseudoLock        lock = new S3PseudoLock(client, "foo", "bar", 10000, 1, 0);
        Assert.assertTrue(lock.lock(mockLog, 5, TimeUnit.SECONDS));
        lock.unlock();
    }
}
