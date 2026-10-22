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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The platform a vanilla screen gives Strata, over a mocked game.
 */
class MinecraftPlatformTest {
    private Minecraft minecraft;
    private MinecraftPlatform platform;

    @BeforeAll
    static void bootstrap() {
        VanillaBootstrap.run();
    }

    @BeforeEach
    void setUp() {
        this.minecraft = mock(Minecraft.class);
        this.platform = new MinecraftPlatform(this.minecraft, new MinecraftTextMetrics(mock(Font.class)),
                mock(GuiEventListener.class));
    }

    @Test
    void tasksAreQueuedOnTheGamesTaskQueue() {
        final Runnable task = () -> { };

        this.platform.uiExecutor().orElseThrow().execute(task);

        // Vanilla's execute would run the task at once on the client thread
        verify(this.minecraft).schedule(task);
        verify(this.minecraft, never()).execute(any());
    }

    @Test
    void theUiThreadIsTheClientThread() {
        final BooleanSupplier check = this.platform.uiThreadCheck().orElseThrow();

        when(this.minecraft.isSameThread()).thenReturn(true);
        assertTrue(check.getAsBoolean());

        when(this.minecraft.isSameThread()).thenReturn(false);
        assertFalse(check.getAsBoolean());
    }
}
