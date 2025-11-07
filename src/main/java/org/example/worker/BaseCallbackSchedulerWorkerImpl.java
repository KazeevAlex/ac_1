package org.example.worker;

import org.example.model.ScheduledCallback;

import java.time.Instant;
import java.util.Date;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BaseCallbackSchedulerWorkerImpl implements CallbackSchedulerWorker {

    private static final int AWAIT_TERMINATION_MILLIS = 200;
    private static final int WAITING_SLEEP_TIME_MILLIS = 100;

    private final Thread worker;
    private final Queue<ScheduledCallback> workQueue = new PriorityQueue<>();
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition workCondition = lock.newCondition();
    private final Condition terminationCondition = lock.newCondition();

    private volatile boolean terminated = false;

    public BaseCallbackSchedulerWorkerImpl() {
        worker = new Thread(getSchedulerTask());
        worker.setUncaughtExceptionHandler((thread, exception) ->
                // In a real application, use a proper logging framework.
                System.err.println("Worker execution failed: " + exception.getMessage()));

        worker.start();
    }

    @Override
    public void submit(ScheduledCallback scheduledCallback) {
        lock.lock();
        try {
            if (isTerminated()) {
                throw new IllegalStateException("Worker is terminated.");
            }
            workQueue.offer(scheduledCallback);
            workCondition.signal(); // для реализации ожидания через condition
        } finally {
            lock.unlock();
        }
    }

    private Runnable getSchedulerTask() {
        return () -> {
            while (!isTerminated()) {
                lock.lock();
                ScheduledCallback callback = null;
                try {
//                    sleepWaitingImplementation();
                    conditionWaitingImplementation();
                    callback = workQueue.remove();
                } catch (InterruptedException e) {
                    // In a real application, use a proper logging framework.
                    System.err.println("Worker was interrupted: " + e.getMessage());
                } finally {
                    lock.unlock();
                }
                try {
                    if (callback == null) {
                        continue;
                    }
                    callback.callback().run();
                } catch (Exception e) {
                    // In a real application, use a proper logging framework.
                    System.err.println("Callback execution failed: " + e.getMessage());
                }
            }
        };
    }

    private void sleepWaitingImplementation() throws InterruptedException {
        while (workQueue.isEmpty() || isNotCallbackTime()) {
            TimeUnit.MILLISECONDS.sleep(WAITING_SLEEP_TIME_MILLIS);
        }
    }

    private void conditionWaitingImplementation() throws InterruptedException {
        while (workQueue.isEmpty()) {
            workCondition.await();
        }
        while (isNotCallbackTime()) {
            var callbackDeadline = workQueue.peek().when();
            workCondition.awaitUntil(Date.from(callbackDeadline));
        }
    }

    private boolean isNotCallbackTime() {
        var callback = workQueue.peek();
        return callback.when().isAfter(Instant.now());
    }

    @Override
    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public void awaitTermination() {
        submit(getScheduledTermination());
        waitTermination();
    }

    private ScheduledCallback getScheduledTermination() {
        Runnable terminationTask = () -> {
            terminateForcibly();
            terminationCondition.signal();
        };
        var when = Instant.now().plusMillis(AWAIT_TERMINATION_MILLIS);
        return new ScheduledCallback(terminationTask, when);
    }

    private void waitTermination() {
        var when = Instant.now().plusMillis(AWAIT_TERMINATION_MILLIS);
        while (Instant.now().isBefore(when)) {
            try {
                terminationCondition.awaitUntil(Date.from(when));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void terminateForcibly() {
        terminated = true;
    }
}
