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
import net.minecraft.client.input.InputQuirks;
import org.inspirenxe.strata.api.event.cause.CauseStack;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.spi.Capabilities;
import org.inspirenxe.strata.spi.Clipboard;
import org.inspirenxe.strata.spi.Clock;
import org.inspirenxe.strata.spi.Conventions;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.TextInputSink;
import org.inspirenxe.strata.spi.ThemeSource;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

/**
 * The Minecraft {@link Platform}, one per hosting vanilla screen.
 *
 * <ul>
 *     <li>Text is measured with the screen's font through {@link MinecraftTextMetrics}.</li>
 *     <li>The clipboard is vanilla's keyboard handler. An empty clipboard reads as empty.</li>
 *     <li>The clock is {@link Clock#SYSTEM}.</li>
 *     <li>Text input: since 26.3 Minecraft sends no typed characters until a text field reports focus, so starting
 *     and stopping text input calls {@code Minecraft.onTextInputFocusChange} for the hosting screen. A secret field
 *     starts text input the same way: the game's text input takes no input type, so it has no mode for secrets.</li>
 *     <li>Minecraft reports held keys as repeated presses, which {@link StrataScreen} turns into repeats, so
 *     {@link Capabilities#sendsKeyRepeat()} is true.</li>
 *     <li>Vanilla slices every gui sprite by its {@code .png.mcmeta} whenever it draws one, so
 *     {@link Capabilities#slicesSprites()} is true and sized and tiled paints reach the canvas whole.</li>
 *     <li>Vanilla's buttons, checkboxes, cycle buttons and tabs act as the mouse button goes down, so
 *     {@link Conventions#activateOnPress()} is true. The shortcut modifier is the one vanilla's own edit shortcuts
 *     use: Command ({@link Modifier#SUPER}) on macOS and Control elsewhere. Mnemonics are on: vanilla binds nothing
 *     to Alt, so Alt with a letter and Alt alone reach Strata's menus and buttons.</li>
 *     <li>Themes named by id come from the game's {@link MinecraftThemeSource}, which reads resource packs.</li>
 *     <li>The cause stack is the one {@link StrataMinecraft#install(CauseStack)} was given, such as Sponge's, or
 *     Strata's own when it was started without one.</li>
 *     <li>The user is the local player's profile, vanilla's {@code Minecraft.getGameProfile()}, so every
 *     {@code Origin.User} carries a {@code GameProfile}.</li>
 *     <li>The UI thread is the client thread, the game's render thread. Tasks handed to it are queued on the game's
 *     own task queue, which the client runs between frames, even when they are handed over from the client thread,
 *     and {@code Minecraft.isSameThread()} tells whether a caller is on it.</li>
 * </ul>
 */
public final class MinecraftPlatform implements Platform {
    private static final Capabilities CAPABILITIES = new Capabilities(true, false, true);
    // Vanilla's buttons, checkboxes, cycle buttons and tabs act as the mouse button goes down, and its edit
    // shortcuts take Command on macOS
    private static final Conventions CONVENTIONS = new Conventions(true,
            InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY ? Modifier.SUPER : Modifier.CONTROL, true);

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
    public Conventions conventions() {
        return CONVENTIONS;
    }

    @Override
    public ThemeSource themes() {
        return MinecraftThemeSource.get(this.minecraft);
    }

    @Override
    public Optional<CauseStack> causes() {
        return StrataMinecraft.causes();
    }

    @Override
    public Optional<Object> user() {
        return Optional.ofNullable(this.minecraft.getGameProfile());
    }

    @Override
    public Optional<Executor> uiExecutor() {
        return Optional.of(MinecraftPlatform.uiExecutor(this.minecraft));
    }

    @Override
    public Optional<BooleanSupplier> uiThreadCheck() {
        return Optional.of(this.minecraft::isSameThread);
    }

    /**
     * Returns an executor that queues tasks on the game's task queue, which the client thread runs between frames.
     * Vanilla's own {@code execute} runs a task at once when called on the client thread, and Strata needs it run
     * after the current call returns, so this schedules it in every case.
     *
     * @param minecraft the game
     * @return the executor
     */
    static Executor uiExecutor(final Minecraft minecraft) {
        return minecraft::schedule;
    }

    /**
     * Returns how many device pixels one UI unit covers: the GUI scale, the framebuffer pixels per GUI pixel. The
     * game asks SDL for a framebuffer of high pixel density, so on a display that scales its content, such as a
     * Retina display, the framebuffer already holds the display's own pixels, and the window's content scale is
     * left out rather than counted twice.
     *
     * @param minecraft the game
     * @return the density, at least 1
     */
    static float pixelsPerUnit(final Minecraft minecraft) {
        return Math.max(1, minecraft.getWindow().getGuiScale());
    }
}
