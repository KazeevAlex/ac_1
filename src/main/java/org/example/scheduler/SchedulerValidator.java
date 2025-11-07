package org.example.scheduler;

import java.time.Instant;
import java.util.Objects;

public class SchedulerValidator {

    public void validateArguments(Runnable callback, Instant when) {
        Objects.requireNonNull(callback, "Callback must not be null");
        Objects.requireNonNull(when, "Execution time 'when' must not be null");

        if (when.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Execution time must be in the future");
        }
    }
}