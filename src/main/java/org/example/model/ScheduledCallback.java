package org.example.model;

import java.time.Instant;

public record ScheduledCallback(Runnable callback, Instant when) implements Comparable<ScheduledCallback> {
    @Override
    public int compareTo(ScheduledCallback o) {
        return this.when.compareTo(o.when);
    }
}
