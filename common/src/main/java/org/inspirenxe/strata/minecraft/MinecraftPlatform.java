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
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.spi.Capabilities;
import org.inspirenxe.strata.spi.Clipboard;
import org.inspirenxe.strata.spi.Clock;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.TextInputSink;
import org.inspirenxe.strata.spi.ThemeSource;

import java.util.Optional;

/**
 * The Minecraft {@link Platform}, one per hosting vanilla screen.
 *
 * <ul>
 *     <li>Text is measured with the screen's font through {@link MinecraftTextMetrics}.</li>
 *     <li>The clipboard is vanilla's keyboard handler. An empty clipboard reads as empty.</li>
 *     <li>The clock is {@link Clock#SYSTEM}.</li>
 *     <li>Text input: since 26.3 Minecraft sends no typed characters until a text field reports focus, so starting
 *     and stopping text input calls {@code Minecraft.onTextInputFocusChange} for the hosting screen.</li>
 *     <li>Minecraft reports held keys as repeated presses, which {@link StrataScreen} turns into repeats, so
 *     {@link Capabilities#sendsKeyRepeat()} is true.</li>
 *     <li>Vanilla slices every gui sprite by its {@code .png.mcmeta} whenever it draws one, so
 *     {@link Capabilities#slicesSprites()} is true and sized and tiled paints reach the canvas whole.</li>
 *     <li>Themes named by id come from the game's {@link MinecraftThemeSource}, which reads resource packs.</li>
 * </ul>
 */
public final class MinecraftPlatform implements Platform {
    private static final Capabilities CAPABILITIES = new Capabilities(true, false, true);

    private final Minecraft minecraft;
    private final MinecraftTextMetrics textMetrics;
    private final Clipboard clipboard;
    private final TextInputSink textInput;

    /**
     * Creates the platform for one vanilla screen.
     *
     * @param minecraft the game
     * @param textMetrics the metrics matching the screen's canvas
     * @param listener the vanilla screen hosting Strata; Minecraft tracks text focus per listener
     */
    public MinecraftPlatform(final Minecraft minecraft, final MinecraftTextMetrics textMetrics, final GuiEventListener listener) {
        MinecraftTranslator.install();
        MinecraftAppearances.install();
        this.minecraft = minecraft;
        this.textMetrics = textMetrics;
        this.clipboard = new Clipboard() {
            @Override
            public Optional<String> get() {
                final String value = minecraft.keyboardHandler.getClipboard();
                return value == null || value.isEmpty() ? Optional.empty() : Optional.of(value);
            }

            @Override
            public void set(final String value) {
                minecraft.keyboardHandler.setClipboard(value);
            }
        };
        this.textInput = new TextInputSink() {
            @Override
            public void start(final Rect caretArea) {
                minecraft.onTextInputFocusChange(listener, true);
            }

            @Override
            public void stop() {
                minecraft.onTextInputFocusChange(listener, false);
            }
        };
    }

    /**
     * Returns the game.
     *
     * @return the Minecraft instance given to the constructor
     */
    public Minecraft minecraft() {
        return this.minecraft;
    }

    @Override
    public MinecraftTextMetrics textMetrics() {
        return this.textMetrics;
    }

    @Override
    public Clipboard clipboard() {
        return this.clipboard;
    }

    @Override
    public Clock clock() {
        return Clock.SYSTEM;
    }

    @Override
    public TextInputSink textInput() {
        return this.textInput;
    }

    @Override
    public Capabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public ThemeSource themes() {
        return MinecraftThemeSource.get(this.minecraft);
    }
}
