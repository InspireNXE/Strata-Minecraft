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
package org.inspirenxe.strata.sponge;

import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.spi.Scheduler;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.scheduler.ScheduledTask;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.plugin.PluginContainer;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Runs Strata screens' repeating tasks on Sponge's client scheduler, as tasks of the {@value #PLUGIN_ID} plugin, so
 * they show in Sponge's task list and run on the client thread with every other plugin's. Sponge rounds an interval to
 * whole ticks. Until Sponge's client is available, or where Sponge does not list Strata as a plugin, tasks run from
 * the scheduler Strata-Minecraft uses without Sponge.
 *
 * <p>Use it on the client thread only, as Sponge requires.</p>
 */
public final class SpongeScheduler implements Scheduler {
    /** The id Strata has as a Sponge plugin, and as a mod on the loaders Sponge runs on. */
    public static final String PLUGIN_ID = "strata";

    private final Scheduler fallback;
    private final Supplier<Optional<PluginContainer>> plugin;

    /**
     * Creates a scheduler that falls back to another.
     *
     * @param fallback where tasks run while Sponge cannot take them
     */
    public SpongeScheduler(final Scheduler fallback) {
        this(fallback, () -> Sponge.isClientAvailable() ? Sponge.pluginManager().plugin(PLUGIN_ID) : Optional.empty());
    }

    /**
     * Creates a scheduler that asks a supplier for Strata's plugin, for tests.
     *
     * @param fallback where tasks run while Sponge cannot take them
     * @param plugin gives Strata's plugin while Sponge's client can take tasks, or empty
     */
    SpongeScheduler(final Scheduler fallback, final Supplier<Optional<PluginContainer>> plugin) {
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public Subscription every(final long millis, final Runnable task) {
        Objects.requireNonNull(task, "task");
        if (millis <= 0L) {
            throw new IllegalArgumentException("A task's interval must be above 0 ms: " + millis);
        }
        final Optional<PluginContainer> plugin = this.plugin.get();
        if (plugin.isEmpty()) {
            return this.fallback.every(millis, task);
        }
        final SpongeTask scheduled = new SpongeTask(task);
        final Duration interval = Duration.ofMillis(millis);
        scheduled.scheduled = Sponge.client().scheduler().submit(Task.builder()
                .plugin(plugin.get())
                .delay(interval)
                .interval(interval)
                .execute(scheduled::run)
                .build());
        return scheduled;
    }

    private static final class SpongeTask implements Subscription {
        private final Runnable task;
        private ScheduledTask scheduled;
        private boolean closed;

        SpongeTask(final Runnable task) {
            this.task = task;
        }

        void run() {
            if (!this.closed) {
                this.task.run();
            }
        }

        @Override
        public void unsubscribe() {
            if (this.closed) {
                return;
            }
            this.closed = true;
            this.scheduled.cancel();
        }
    }
}
