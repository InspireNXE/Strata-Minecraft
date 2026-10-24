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

import org.inspirenxe.strata.api.Strata;
import org.inspirenxe.strata.api.event.cause.CauseStack;
import org.inspirenxe.strata.spi.Scheduler;
import org.inspirenxe.strata.core.StrataCore;

import java.util.Objects;
import java.util.Optional;

/**
 * Starts Strata-Minecraft. The loaders' entry points call {@link #install()}, or
 * {@link #install(CauseStack, Scheduler)} with Sponge's cause stack and scheduler where Sponge is present, when the mod
 * or plugin loads, so mods never need to.
 */
public final class StrataMinecraft {
    private static final System.Logger LOGGER = System.getLogger(StrataMinecraft.class.getName());

    private static boolean installed;
    private static volatile CauseStack causes;
    private static volatile Scheduler scheduler = MinecraftScheduler.INSTANCE;

    private StrataMinecraft() {
    }

    /**
     * Hands Strata-Minecraft's screen controller to Strata, so {@code Strata.screens()} opens screens as
     * {@link StrataScreen}s and {@code Strata.uiThread()} is the client thread, and checks that Strata's core is found the way mods reach it. Only the first call does
     * anything. A core that cannot be found is logged, not thrown, so the game still starts and the error shows
     * where a mod first asks for Strata.
     */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        StrataMinecraft.start();
    }

    /**
     * Starts Strata-Minecraft as {@link #install()} does, with the engine's own cause stack and scheduler, which every
     * {@link MinecraftPlatform} then hands Strata: hosts push their input frames on the stack and events read their
     * causes from it, and screens' repeating tasks run from the scheduler. The Sponge entry points call this with
     * Sponge's. Only the first call to either method does anything.
     *
     * @param causes the cause stack
     * @param scheduler the scheduler; it must run tasks on the client thread
     */
    public static synchronized void install(final CauseStack causes, final Scheduler scheduler) {
        if (installed) {
            return;
        }
        installed = true;
        StrataMinecraft.causes = Objects.requireNonNull(causes, "causes");
        StrataMinecraft.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        StrataMinecraft.start();
    }

    /**
     * Returns the cause stack Strata-Minecraft was started with.
     *
     * @return the stack given to {@link #install(CauseStack, Scheduler)}, or empty when Strata's own is used
     */
    public static Optional<CauseStack> causes() {
        return Optional.ofNullable(StrataMinecraft.causes);
    }

    /**
     * Returns the scheduler screens' repeating tasks run from.
     *
     * @return the scheduler given to {@link #install(CauseStack, Scheduler)}, or the {@link MinecraftScheduler} on the
     *     client tick
     */
    public static Scheduler scheduler() {
        return StrataMinecraft.scheduler;
    }

    /**
     * Counts a client tick for the {@link MinecraftScheduler}. The Strata screen the game shows calls it on each of
     * its ticks.
     */
    static void tick() {
        MinecraftScheduler.INSTANCE.tick();
    }

    private static void start() {
        StrataCore.setScreenController(MinecraftScreens.INSTANCE, MinecraftScreens.PLATFORM);
        try {
            if (Strata.screens() != MinecraftScreens.INSTANCE) {
                LOGGER.log(System.Logger.Level.ERROR, "Strata's core answered with another screen controller;"
                        + " is a second copy of strata-core loaded?");
            }
        } catch (final IllegalStateException e) {
            LOGGER.log(System.Logger.Level.ERROR, "Strata's core could not be found: " + e.getMessage());
        }
    }
}
