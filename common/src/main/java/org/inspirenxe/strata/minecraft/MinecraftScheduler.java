/*
 * Copyright (c) InspireNXE <https://github.com/InspireNXE>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.inspirenxe.strata.minecraft;

import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.spi.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Runs the repeating tasks of Strata screens on the client tick, where Sponge's scheduler is not there to do it. The
 * Strata screen the game shows ticks it, twenty times a second, on the client thread, so an interval is rounded up to
 * whole ticks of 50 ms and tasks wait while a vanilla screen covers the Strata one. Every loader runs it the same way.
 *
 * <p>Use it on the client thread only.</p>
 */
public final class MinecraftScheduler implements Scheduler {
    /** The length of a client tick, in milliseconds. */
    public static final long TICK_MILLIS = 50L;

    static final MinecraftScheduler INSTANCE = new MinecraftScheduler();

    private final List<Task> tasks = new ArrayList<>();
    private long ticks;

    MinecraftScheduler() {
    }

    @Override
    public Subscription every(final long millis, final Runnable task) {
        Objects.requireNonNull(task, "task");
        if (millis <= 0L) {
            throw new IllegalArgumentException("A task's interval must be above 0 ms: " + millis);
        }
        final long interval = Math.max(1L, (millis + TICK_MILLIS - 1L) / TICK_MILLIS);
        final Task scheduled = new Task(interval, this.ticks + interval, task);
        this.tasks.add(scheduled);
        return scheduled;
    }

    /**
     * Counts a client tick and runs the tasks due on it, once each, in the order they were scheduled.
     */
    void tick() {
        this.ticks++;
        if (this.tasks.isEmpty()) {
            return;
        }
        // A task may close itself or another, or schedule one, while it runs
        for (final Task task : List.copyOf(this.tasks)) {
            if (!task.closed && task.due <= this.ticks) {
                task.due = this.ticks + task.interval;
                task.task.run();
            }
        }
    }

    private final class Task implements Subscription {
        private final long interval;
        private final Runnable task;
        private long due;
        private boolean closed;

        Task(final long interval, final long due, final Runnable task) {
            this.interval = interval;
            this.due = due;
            this.task = task;
        }

        @Override
        public void unsubscribe() {
            this.closed = true;
            MinecraftScheduler.this.tasks.remove(this);
        }
    }
}
