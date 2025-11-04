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

    public static final int WAITING_SLEEP_TIME_MILLIS = 100;

    private final Thread worker;
    private final Queue<ScheduledCallback> workQueue = new PriorityQueue<>();
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition condition = lock.newCondition();

    public BaseCallbackSchedulerWorkerImpl() {
        worker = new Thread(getSchedulerTask());
        worker.start();
    }

    @Override
    public void submit(ScheduledCallback scheduledCallback) {
        lock.lock();
        try {
            if (!isActive()) {
                throw new IllegalStateException("Worker is not active.");
            }
            workQueue.offer(scheduledCallback);
            condition.signal(); // для реализации ожидания через condition
        } finally {
            lock.unlock();
        }
    }

    private Runnable getSchedulerTask() {
        return () -> {
            while (isActive()) {
                lock.lock();
                ScheduledCallback callback = null;
                try {
//                    sleepWaitingImplementation();
                    conditionWaitingImplementation();
                    callback = workQueue.remove();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
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
            condition.await();
        }
        while (isNotCallbackTime()) {
            var callbackDeadline = workQueue.peek().when();
            condition.awaitUntil(Date.from(callbackDeadline));
        }
    }

    private boolean isNotCallbackTime() {
        var callback = workQueue.peek();
        return callback.when().isAfter(Instant.now());
    }

    @Override
    public boolean isActive() {
        return !worker.isInterrupted();
    }

    @Override
    public void stop() {
        worker.interrupt();
    }
}
