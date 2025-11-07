package org.example.scheduler;

import org.example.model.ScheduledCallback;
import org.example.worker.CallbackSchedulerWorker;

import java.time.Instant;
import java.util.Objects;

public class BaseCallbackSchedulerImpl implements CallbackScheduler {

    private final CallbackSchedulerWorker worker;
    private final SchedulerValidator schedulerValidator = new SchedulerValidator();

    private volatile boolean running = true;

    public BaseCallbackSchedulerImpl(CallbackSchedulerWorker worker) {
        Objects.requireNonNull(worker, "Worker must not be null");
        this.worker = worker;
    }

    @Override
    public void schedule(Runnable callback, Instant when) {
        if (!isRunning()) {
            throw new IllegalStateException("Scheduler is shut down");
        }
        schedulerValidator.validateArguments(callback, when);
        worker.submit(new ScheduledCallback(callback, when));
    }

    private boolean isRunning() {
        return running;
    }

    @Override
    public void close() {
        running = false;
        worker.awaitTermination();

        if (!worker.isTerminated()) {
            worker.terminateForcibly();
        }
    }
}
