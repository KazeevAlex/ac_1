package org.example.worker;

import org.example.model.ScheduledCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;

class BaseCallbackSchedulerWorkerImplTest {

    BaseCallbackSchedulerWorkerImpl worker;

    @BeforeEach
    void setUp() {
        worker = new BaseCallbackSchedulerWorkerImpl();
    }

    @AfterEach
    void tearDown() {
        worker.terminateForcibly();
    }

    @Test
    void testSuccessfulSchedule() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        BlockingQueue<String> resultQueue = new LinkedBlockingQueue<>();

        long earlierPlusMillis = 10;
        long laterPlusMillis = 30;

        String earlierCallbackRecord = "earlier callback record";
        String laterCallbackRecord = "later callback record";

        Runnable earlierCallback = () -> {
            resultQueue.add(earlierCallbackRecord);
            latch.countDown();
        };

        Runnable laterCallback = () -> {
            resultQueue.add(laterCallbackRecord);
            latch.countDown();
        };

        worker.submit(new ScheduledCallback(laterCallback, Instant.now().plusMillis(laterPlusMillis)));
        worker.submit(new ScheduledCallback(earlierCallback, Instant.now().plusMillis(earlierPlusMillis)));

        latch.await();

        assertEquals(2, resultQueue.size());
        assertEquals(earlierCallbackRecord, resultQueue.poll());
        assertEquals(laterCallbackRecord, resultQueue.poll());
    }

    @Test
    void testScheduleAfterTerminateForcibly() {
        worker.terminateForcibly();
        assertThrows(
                IllegalStateException.class,
                () -> worker.submit(new ScheduledCallback(()->{}, Instant.now()))
        );
    }

    @Test
    void testCallbackThrowsException() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        BlockingQueue<String> resultQueue = new LinkedBlockingQueue<>();

        String successfulCallbackRecord = "successful callback record";

        Runnable throwingCallback = () -> {
            latch.countDown();
            throw new RuntimeException("Test exception from callback");
        };

        Runnable successfulCallback = () -> {
            resultQueue.add(successfulCallbackRecord);
            latch.countDown();
        };

        worker.submit(new ScheduledCallback(throwingCallback, Instant.now().plusMillis(10)));
        worker.submit(new ScheduledCallback(successfulCallback, Instant.now().plusMillis(20)));

        latch.await();

        assertEquals(1, resultQueue.size());
        assertEquals(successfulCallbackRecord, resultQueue.poll());
    }
}