package org.example.scheduler;

import org.example.model.ScheduledCallback;
import org.example.worker.CallbackSchedulerWorker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BaseCallbackSchedulerImplTest {

    private static final int WAIT_AFTER_CLOSE_MILLIS = 10;

    @Mock
    CallbackSchedulerWorker worker;

    @InjectMocks
    BaseCallbackSchedulerImpl scheduler;

    @Test
    void testSuccessfulSchedule() {
        scheduler.schedule(()->{}, Instant.now().plusMillis(10));
        verify(worker).submit(any(ScheduledCallback.class));
    }

    @ParameterizedTest
    @MethodSource("invalidArguments")
    void testInvalidArguments(Runnable callback, Instant when, Class<? extends Throwable> exceptionClass) {
        assertThrows(exceptionClass, () -> scheduler.schedule(callback, when));
    }

    private static Stream<Arguments> invalidArguments() {
        Runnable emptyCallback = () -> {};
        return Stream.of(
                Arguments.of(null, Instant.now(), NullPointerException.class),
                Arguments.of(emptyCallback, null, NullPointerException.class),
                Arguments.of(emptyCallback, Instant.now().minusMillis(10), IllegalArgumentException.class)
        );
    }

    @Test
    void testScheduleAfterClose() throws InterruptedException {
        new Thread(() -> scheduler.close()).start();
        TimeUnit.MILLISECONDS.sleep(WAIT_AFTER_CLOSE_MILLIS);

        assertThrows(
                IllegalStateException.class,
                () -> scheduler.schedule(() -> {}, Instant.now())
        );
    }

    @Test
    void testClose() throws InterruptedException, IllegalAccessException, NoSuchFieldException {
        new Thread(() -> scheduler.close()).start();
        TimeUnit.MILLISECONDS.sleep(WAIT_AFTER_CLOSE_MILLIS);

        Field runningField = scheduler.getClass().getDeclaredField("running");
        runningField.setAccessible(true);
        Boolean runningValue = (Boolean) runningField.get(scheduler);

        assertFalse(runningValue);
        verify(worker).awaitTermination();
    }
}