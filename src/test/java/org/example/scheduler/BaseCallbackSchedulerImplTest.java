package org.example.scheduler;

import org.example.model.ScheduledCallback;
import org.example.worker.CallbackSchedulerWorker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BaseCallbackSchedulerImplTest {

    @Mock
    CallbackSchedulerWorker worker;

    @InjectMocks
    BaseCallbackSchedulerImpl scheduler;

    @Test
    void testSuccessfulSchedule() {
        scheduler.schedule(()->{}, Instant.now());
        verify(worker).submit(any(ScheduledCallback.class));
    }

    @Test
    void testNullCallback() {
        assertThrows(
                NullPointerException.class,
                () -> scheduler.schedule(null, Instant.now())
        );
    }

    @Test
    void testNullWhen() {
        assertThrows(
                NullPointerException.class,
                () -> scheduler.schedule(() -> {}, null)
        );
    }

    @Test
    void testIllegalWhen() {
        assertThrows(
                IllegalArgumentException.class,
                () -> scheduler.schedule(() -> {}, Instant.now().minusMillis(10))
        );
    }

    @Test
    void testScheduleAfterClose() {
        scheduler.close();
        assertThrows(
                IllegalStateException.class,
                () -> scheduler.schedule(() -> {}, Instant.now())
        );
    }
}