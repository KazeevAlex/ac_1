package org.example.scheduler;

import org.example.worker.CallbackSchedulerWorker;
import org.example.model.ScheduledCallback;

import java.time.Instant;

public class BaseCallbackSchedulerImpl implements CallbackScheduler {

    private final CallbackSchedulerWorker worker;

    public BaseCallbackSchedulerImpl(CallbackSchedulerWorker worker) {
        this.worker = worker;
    }

    private volatile boolean active = true;

    @Override
    public void schedule(Runnable callback, Instant when) {
        if (!isActive()) {
            throw new IllegalStateException();
        }
        validateArguments(callback, when);
        worker.submit(new ScheduledCallback(callback, when));
    }

    private void validateArguments(Runnable callback, Instant when) {
        if (callback == null || when == null) {
            throw new NullPointerException();
        }
        if (when.isBefore(Instant.now())) {
            throw new IllegalArgumentException();
        }
    }

    private boolean isActive() {
        return active || worker.isActive();
    }

    @Override
    public void close() {
        worker.stop();
        active = false;
    }
}
