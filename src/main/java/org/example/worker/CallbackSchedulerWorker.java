package org.example.worker;

import org.example.model.ScheduledCallback;

public interface CallbackSchedulerWorker {

    void submit(ScheduledCallback scheduledCallback);

    boolean isTerminated();

    void awaitTermination();

    void terminateForcibly();
}
