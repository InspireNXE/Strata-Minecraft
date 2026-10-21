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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.core.StrataCore;
import org.inspirenxe.strata.spi.HeldKeys;
import org.inspirenxe.strata.spi.ScreenHost;

import java.util.Objects;

/**
 * A vanilla {@link Screen} that shows one Strata screen. Mods open Strata screens with {@code Strata.screens()},
 * which makes one of these:
 *
 * <pre>{@code
 * Strata.screens().open(new SettingsScreen());
 * }</pre>
 *
 * <p>Construct one yourself only to subclass it, for example to override {@link #isPauseScreen()}, and open it like
 * any other vanilla screen:</p>
 *
 * <pre>{@code
 * Minecraft.getInstance().setScreen(
 *         new StrataScreen(Component.literal("Settings"), new SettingsScreen(), ThemeRef.platform()));
 * }</pre>
 *
 * <p>Given a {@link ThemeRef}, the screen follows resource packs: it resolves the ref when it opens and again
 * after every resource reload, keeping the ref's overrides. Given a {@link Theme}, it keeps that theme.</p>
 *
 * <p>How Minecraft maps onto Strata:</p>
 * <ul>
 *     <li>One UI unit is one GUI pixel, so layouts follow the player's GUI scale. The host is resized whenever
 *     vanilla re-initializes the screen, such as on a window resize.</li>
 *     <li>The Strata screen is built when the vanilla screen first opens, and closed when vanilla removes it. A
 *     Strata screen that asks to close gets vanilla's {@link #onClose()}.</li>
 *     <li>A screen opened from the Strata screen's handle, or with {@code Strata.screens()} while this screen shows,
 *     opens above it in a new {@code StrataScreen}. This screen keeps its tree, focus and state while it waits, and
 *     {@link #onClose()} on the one above shows this one again. If vanilla replaces the screen above with anything
 *     else, this one closes too. Closing this screen through its handle while it waits closes the screens above it
 *     as well. A screen opened from a handle while another screen covers this one opens on top, as with
 *     {@code Strata.screens()}.</li>
 *     <li>{@link #onClose()} returns to the screen this one was opened above, or else to the game, as vanilla's
 *     does. Override it to return somewhere else.</li>
 *     <li>Input Strata does not consume goes on to vanilla, so Escape still closes the screen.</li>
 *     <li>Minecraft reports held keys as repeated presses; this screen turns them into repeats.</li>
 *     <li>Strata counts clicks itself and ignores vanilla's double-click flag.</li>
 *     <li>Scroll amounts already include the player's wheel sensitivity. Fractional amounts count as
 *     {@link ScrollKind#PRECISE}.</li>
 *     <li>Minecraft gives no modifiers with pointer presses, so the screen reads the held modifier keys from the
 *     window for presses and releases. It gives none with pointer moves, scrolls or typed characters either, and
 *     those report {@link Modifiers#NONE}.</li>
 *     <li>Minecraft passes a character outside the Basic Multilingual Plane as two {@code char}s; the screen joins
 *     them into one code point.</li>
 *     <li>Whether the game pauses behind the screen follows vanilla's {@link #isPauseScreen()}; override it to
 *     change that.</li>
 * </ul>
 */
public class StrataScreen extends Screen {
    /**
     * Switches vanilla's screen. Tests replace it, since a mocked game has no gui.
     */
    static Switcher switcher = Switcher.VANILLA;

    private final org.inspirenxe.strata.api.screen.Screen content;
    private final Theme theme;
    private final ThemeRef themeRef;
    private final HeldKeys heldKeys;
    private final ScreenHandle handle;
    private MinecraftCanvas canvas;
    private ScreenHost host;
    /**
     * The Strata screen this one returns to when it closes, if it opened above one.
     */
    private StrataScreen parent;
    /**
     * The Strata screen above this one while this one waits beneath it.
     */
    private StrataScreen child;
    /**
     * Set while this screen closes back to its parent, so that removing it keeps the parent open.
     */
    private boolean returning;
    /**
     * The first half of a character typed as two {@code char}s, until the second arrives.
     */
    private char highSurrogate;

    /**
     * Creates the vanilla screen. Nothing is built until Minecraft opens it.
     *
     * @param title the vanilla title, used for narration
     * @param content the Strata screen to show
     * @param theme the theme for its components, which resource packs never change; to look like the game and
     *              follow packs, pass {@code ThemeRef.platform()} instead
     */
    public StrataScreen(final Component title, final org.inspirenxe.strata.api.screen.Screen content, final Theme theme) {
        super(title);
        this.content = Objects.requireNonNull(content, "content");
        this.theme = Objects.requireNonNull(theme, "theme");
        this.themeRef = null;
        this.heldKeys = new HeldKeys();
        this.handle = new Handle();
    }

    /**
     * Creates the vanilla screen with a theme that follows resource packs. Nothing is built or resolved until
     * Minecraft opens it.
     *
     * @param title the vanilla title, used for narration
     * @param content the Strata screen to show
     * @param theme the theme to resolve, for example {@code ThemeRef.platform()}, the vanilla theme
     */
    public StrataScreen(final Component title, final org.inspirenxe.strata.api.screen.Screen content,
                        final ThemeRef theme) {
        super(title);
        this.content = Objects.requireNonNull(content, "content");
        this.theme = null;
        this.themeRef = Objects.requireNonNull(theme, "theme");
        this.heldKeys = new HeldKeys();
        this.handle = new Handle();
    }

    /**
     * Creates the vanilla screen for a given game and font, as the public constructors do for the running game.
     */
    StrataScreen(final Minecraft minecraft, final Font font, final Component title,
                 final org.inspirenxe.strata.api.screen.Screen content, final ThemeRef theme) {
        super(title);
        this.minecraft = minecraft;
        this.font = font;
        this.content = Objects.requireNonNull(content, "content");
        this.theme = null;
        this.themeRef = Objects.requireNonNull(theme, "theme");
        this.heldKeys = new HeldKeys();
        this.handle = new Handle();
    }

    /**
     * Makes the vanilla screen for a Strata screen, titled with its {@link org.inspirenxe.strata.api.screen.Screen#title()}.
     *
     * @param minecraft the game
     * @param font the font to measure and draw with
     * @param content the Strata screen
     * @param theme its theme
     * @return the vanilla screen, not yet shown
     */
    static StrataScreen of(final Minecraft minecraft, final Font font,
                           final org.inspirenxe.strata.api.screen.Screen content, final ThemeRef theme) {
        return new StrataScreen(minecraft, font, MinecraftText.component(content.title()), content, theme);
    }

    /**
     * Returns the Strata screen this vanilla screen shows.
     *
     * @return the Strata screen
     */
    public org.inspirenxe.strata.api.screen.Screen content() {
        return this.content;
    }

    /**
     * Returns the handle that works on this vanilla screen, whether or not the Strata screen has opened yet.
     *
     * @return the handle
     */
    ScreenHandle handle() {
        return this.handle;
    }

    /**
     * Returns the Strata screen this one returns to when it closes.
     *
     * @return the parent, or {@code null}
     */
    StrataScreen parent() {
        return this.parent;
    }

    /**
     * Returns the Strata screen open above this one.
     *
     * @return the screen above, or {@code null} unless this one waits beneath it
     */
    StrataScreen child() {
        return this.child;
    }

    /**
     * Returns the host, creating it the first time and again after the screen closed.
     */
    ScreenHost host() {
        if (this.host == null) {
            this.canvas = new MinecraftCanvas(this.font);
            final MinecraftPlatform platform = new MinecraftPlatform(this.minecraft, this.canvas.textMetrics(), this);
            this.host = this.themeRef != null
                    ? StrataCore.createScreenHost(platform, this.themeRef, this::requestClose)
                    : StrataCore.createScreenHost(platform, this.theme, this::requestClose);
            this.host.setOpener(this::openAbove);
        }
        return this.host;
    }

    /**
     * Returns whether the Strata screen is open, showing or waiting beneath another.
     *
     * @return true while it is open
     */
    boolean isOpen() {
        return this.host != null && this.host.screen().isPresent();
    }

    @Override
    protected void init() {
        // Shown again, so nothing waits above this screen any more
        this.child = null;
        final ScreenHost current = this.host();
        current.resize(this.width, this.height);
        if (current.screen().isEmpty()) {
            current.open(this.content);
        }
    }

    @Override
    public void removed() {
        this.heldKeys.clear();
        this.highSurrogate = 0;
        if (this.canvas != null) {
            this.canvas.resetCursor();
        }
        if (this.child == null) {
            this.closeHost();
            if (this.parent != null) {
                if (this.returning) {
                    this.parent.child = null;
                } else if (this.parent.child == this) {
                    // Vanilla moved on from this screen without going back, so the screens beneath close too
                    this.parent.discard();
                }
            }
        }
        this.returning = false;
        super.removed();
    }

    /**
     * Shows the screen this one was opened above, or else returns to the game as vanilla does.
     */
    @Override
    public void onClose() {
        if (this.parent != null) {
            this.returning = true;
            switcher.set(this.minecraft, this.parent);
        } else {
            super.onClose();
        }
    }

    /**
     * Shows a new Strata screen above this one, which shows this one again when it closes.
     *
     * @param opened the new screen
     */
    void stack(final StrataScreen opened) {
        opened.parent = this;
        this.child = opened;
        switcher.set(this.minecraft, opened);
    }

    /**
     * What the Strata screen's handle opens screens with.
     */
    private ScreenHandle openAbove(final org.inspirenxe.strata.api.screen.Screen screen, final ThemeRef theme) {
        final StrataScreen opened = StrataScreen.of(this.minecraft, this.font, screen, theme);
        if (switcher.current(this.minecraft) == this) {
            this.stack(opened);
        } else {
            MinecraftScreens.show(this.minecraft, opened);
        }
        return opened.handle;
    }

    /**
     * Closes the Strata screen, however it is shown: through {@link #onClose()} when it shows, together with the
     * screens above it when it waits beneath them, and quietly otherwise.
     */
    private void requestClose() {
        final Screen shown = switcher.current(this.minecraft);
        if (shown == this) {
            this.onClose();
            return;
        }
        if (this.child == null) {
            this.closeHost();
            return;
        }
        StrataScreen top = this.child;
        while (top.child != null) {
            top = top.child;
        }
        for (StrataScreen screen = this; screen != top; ) {
            final StrataScreen next = screen.child;
            screen.child = null;
            screen.closeHost();
            screen = next;
        }
        if (this.parent != null) {
            this.parent.child = top;
        }
        top.parent = this.parent;
        if (shown == top) {
            top.onClose();
        } else {
            top.closeHost();
            if (top.parent != null) {
                top.parent.discard();
            }
        }
    }

    /**
     * Closes this screen and every Strata screen it waits above, once vanilla has moved on without returning.
     */
    private void discard() {
        this.child = null;
        this.closeHost();
        if (this.parent != null && this.parent.child == this) {
            this.parent.discard();
        }
    }

    private void closeHost() {
        if (this.host != null) {
            final ScreenHost closing = this.host;
            this.host = null;
            closing.close();
        }
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float a) {
        super.render(graphics, mouseX, mouseY, a);
        this.canvas.begin(graphics);
        this.host.render(this.canvas);
        this.canvas.end();
    }

    @Override
    public void mouseMoved(final double x, final double y) {
        this.host.pointerMoved((float) x, (float) y, Modifiers.NONE);
    }

    @Override
    public boolean mouseClicked(final double x, final double y, final int button) {
        return this.host.pointerButton((float) x, (float) y, MinecraftInput.button(button), true,
                MinecraftInput.heldModifiers()) || super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(final double x, final double y, final int button) {
        return this.host.pointerButton((float) x, (float) y, MinecraftInput.button(button), false,
                MinecraftInput.heldModifiers()) || super.mouseReleased(x, y, button);
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        return this.host.scroll((float) x, (float) y, MinecraftInput.scrollX(scrollX), (float) scrollY,
                MinecraftInput.scrollKind(scrollX, scrollY), Modifiers.NONE)
                || super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        final Key key = MinecraftInput.key(keyCode);
        // Minecraft sends presses and repeats through the same method
        final KeyAction action = this.heldKeys.press(key);
        return this.host.key(key, action, MinecraftInput.modifiers(modifiers)) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(final int keyCode, final int scanCode, final int modifiers) {
        final Key key = MinecraftInput.key(keyCode);
        this.heldKeys.release(key);
        return this.host.key(key, KeyAction.RELEASE, MinecraftInput.modifiers(modifiers))
                || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char character, final int modifiers) {
        if (Character.isHighSurrogate(character)) {
            this.highSurrogate = character;
            return true;
        }
        final int codepoint = Character.isLowSurrogate(character) && this.highSurrogate != 0
                ? Character.toCodePoint(this.highSurrogate, character) : character;
        this.highSurrogate = 0;
        return this.host.charTyped(codepoint, Modifiers.NONE) || super.charTyped(character, modifiers);
    }

    /**
     * Reads and sets the screen vanilla shows.
     */
    interface Switcher {
        /**
         * Vanilla's own: {@code minecraft.screen} and {@code minecraft.setScreen}.
         */
        Switcher VANILLA = new Switcher() {
            @Override
            public Screen current(final Minecraft minecraft) {
                return minecraft.screen;
            }

            @Override
            public void set(final Minecraft minecraft, final Screen screen) {
                minecraft.setScreen(screen);
            }
        };

        /**
         * Returns the screen vanilla shows.
         *
         * @param minecraft the game
         * @return the screen, or {@code null} in game
         */
        Screen current(Minecraft minecraft);

        /**
         * Shows a screen, removing the one shown before.
         *
         * @param minecraft the game
         * @param screen the screen to show
         */
        void set(Minecraft minecraft, Screen screen);
    }

    /**
     * The handle {@code Strata.screens()} returns for this vanilla screen. It works before the Strata screen has
     * opened and goes to the Strata screen's own handle once it has.
     */
    private final class Handle implements ScreenHandle {
        @Override
        public void invalidate() {
            if (StrataScreen.this.host != null) {
                StrataScreen.this.host.handle().ifPresent(ScreenHandle::invalidate);
            }
        }

        @Override
        public void close() {
            StrataScreen.this.requestClose();
        }

        @Override
        public ComponentFactory factory() {
            return StrataScreen.this.host().factory();
        }

        @Override
        public ScreenHandle open(final org.inspirenxe.strata.api.screen.Screen screen, final ThemeRef theme) {
            Objects.requireNonNull(screen, "screen");
            Objects.requireNonNull(theme, "theme");
            return StrataScreen.this.openAbove(screen, theme);
        }
    }
}
