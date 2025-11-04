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
    void tearDown() throws Exception {
        worker.stop();
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
            resultQueue.offer(earlierCallbackRecord);
            latch.countDown();
        };

        Runnable laterCallback = () -> {
            resultQueue.offer(laterCallbackRecord);
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
    void testScheduleAfterStop() throws Exception {
        worker.stop();
        assertThrows(
                IllegalStateException.class,
                () -> worker.submit(new ScheduledCallback(()->{}, Instant.now()))
        );
    }
}