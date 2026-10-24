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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Without Sponge's client, tasks go to the scheduler Strata-Minecraft uses without Sponge.
 */
class SpongeSchedulerTest {

    @Test
    void tasksGoToTheFallbackWhileSpongesClientIsNotThere() {
        final List<Long> asked = new ArrayList<>();
        final Subscription given = () -> { };
        final SpongeScheduler scheduler = new SpongeScheduler((millis, task) -> {
            asked.add(millis);
            return given;
        }, Optional::empty);
        assertSame(given, scheduler.every(100L, () -> { }));
        assertEquals(List.of(100L), asked);
    }

    @Test
    void anIntervalOfZeroIsRefused() {
        final SpongeScheduler scheduler = new SpongeScheduler((millis, task) -> () -> { });
        assertThrows(IllegalArgumentException.class, () -> scheduler.every(0L, () -> { }));
    }
}
