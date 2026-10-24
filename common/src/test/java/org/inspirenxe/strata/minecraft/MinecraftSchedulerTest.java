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
import org.inspirenxe.strata.api.text.TextStyle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Screens' repeating tasks run on the client tick where Sponge's scheduler is not there, and text sizes scale vanilla's
 * 9-pixel text.
 */
class MinecraftSchedulerTest {
    private final MinecraftScheduler scheduler = new MinecraftScheduler();

    private void ticks(final int count) {
        for (int i = 0; i < count; i++) {
            this.scheduler.tick();
        }
    }

    @Test
    void anIntervalIsRoundedUpToWholeTicks() {
        final int[] runs = new int[2];
        this.scheduler.every(100L, () -> runs[0]++);
        this.scheduler.every(60L, () -> runs[1]++);
        this.ticks(1);
        assertEquals(0, runs[0]);
        this.ticks(1);
        assertEquals(1, runs[0], "100 ms is two ticks");
        assertEquals(1, runs[1], "60 ms rounds up to two ticks");
        this.ticks(4);
        assertEquals(3, runs[0]);
        assertEquals(3, runs[1]);
    }

    @Test
    void anIntervalShorterThanATickRunsEveryTick() {
        final int[] runs = new int[1];
        this.scheduler.every(1L, () -> runs[0]++);
        this.ticks(3);
        assertEquals(3, runs[0]);
    }

    @Test
    void aClosedTaskStops() {
        final int[] runs = new int[1];
        final Subscription subscription = this.scheduler.every(50L, () -> runs[0]++);
        this.ticks(1);
        subscription.close();
        subscription.close();
        this.ticks(5);
        assertEquals(1, runs[0]);
    }

    @Test
    void anIntervalOfZeroIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> this.scheduler.every(0L, () -> { }));
    }

    @Test
    void theDefaultSchedulerIsTheTickOneUntilAnotherIsInstalled() {
        assertEquals(MinecraftScheduler.class, StrataMinecraft.scheduler().getClass());
    }

    @Test
    void aSizeScalesVanillasText() {
        assertEquals(1f, MinecraftTextMetrics.scale(TextStyle.DEFAULT));
        assertEquals(1f, MinecraftTextMetrics.scale(TextStyle.DEFAULT.withSize(9f)));
        assertEquals(2f, MinecraftTextMetrics.scale(TextStyle.DEFAULT.withSize(18f)));
    }
}
