package org.example.scheduler;

import org.example.model.ScheduledCallback;
import org.example.worker.CallbackSchedulerWorker;

import java.time.Instant;
import java.util.Objects;

public class BaseCallbackSchedulerImpl implements CallbackScheduler {

    private final CallbackSchedulerWorker worker;

    public BaseCallbackSchedulerImpl(CallbackSchedulerWorker worker) {
        Objects.requireNonNull(worker, "Worker must not be null");
        this.worker = worker;
    }

    @Override
    public void schedule(Runnable callback, Instant when) {
        if (!isActive()) {
            throw new IllegalStateException("Scheduler is shut down");
        }
        validateArguments(callback, when);
        worker.submit(new ScheduledCallback(callback, when));
    }

    private void validateArguments(Runnable callback, Instant when) {
        Objects.requireNonNull(callback, "Callback must not be null");
        Objects.requireNonNull(when, "Execution time 'when' must not be null");

        if (when.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Execution time must be in the future");
        }
    }

    private boolean isActive() {
        return worker.isActive();
    }

    @Override
    public void close() {
        worker.stop();
    }
}
