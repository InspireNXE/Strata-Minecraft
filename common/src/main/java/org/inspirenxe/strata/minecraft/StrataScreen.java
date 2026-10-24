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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.api.transfer.Transfer;
import org.inspirenxe.strata.core.StrataCore;
import org.inspirenxe.strata.spi.HeldKeys;
import org.inspirenxe.strata.spi.ScreenHost;

import java.nio.file.Path;
import java.util.List;
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
 * Minecraft.getInstance().gui.setScreen(
 *         new StrataScreen(Component.literal("Settings"), new SettingsScreen(), ThemeRef.platform()));
 * }</pre>
 *
 * <p>Given a {@link ThemeRef}, the screen follows resource packs: it resolves the ref when it opens and again
 * after every resource reload, keeping the ref's overrides. Given a {@link Theme}, it keeps that theme.</p>
 *
 * <p>How Minecraft maps onto Strata:</p>
 * <ul>
 *     <li>One UI unit is one GUI pixel, so layouts follow the player's GUI scale. The host is resized whenever
 *     vanilla re-initializes the screen, such as on a window resize or a change of GUI scale, and told the GUI
 *     scale as its density in device pixels per unit.</li>
 *     <li>The Strata screen is built when the vanilla screen first opens, and closed when vanilla removes it. A
 *     Strata screen that closes through its handle gets vanilla's {@link #onClose()}.</li>
 *     <li>Escape that Strata does not use, while {@link #shouldCloseOnEsc()} is true, and any other call to
 *     {@link #onClose()} first ask the Strata screen through its {@code closeRequested}, and close nothing when it
 *     refuses. A subclass that overrides {@link #onClose()} keeps that for Escape; when other code calls it, the
 *     asking happens where the override calls {@code super.onClose()}. Closes the game makes for its own reasons,
 *     such as another screen replacing this one or a disconnect, never ask.</li>
 *     <li>The title vanilla shows and narrates is the Strata screen's: {@link #getTitle()} follows
 *     {@code ScreenHandle.setTitle} and the game's language. A screen constructed with a vanilla title shows that
 *     title until the Strata screen sets another.</li>
 *     <li>A screen opened from the Strata screen's handle, or with {@code Strata.screens()} while this screen shows,
 *     opens above it in a new {@code StrataScreen}. This screen keeps its tree, focus and state while it waits, and
 *     {@link #onClose()} on the one above shows this one again. If vanilla replaces the screen above with anything
 *     else, this one closes too. Closing this screen through its handle while it waits closes the screens above it
 *     as well. A screen opened from a handle while another screen covers this one opens on top, as with
 *     {@code Strata.screens()}.</li>
 *     <li>{@link #onClose()} returns to the screen this one was opened above, or else to the game, as vanilla's
 *     does. Override it to return somewhere else.</li>
 *     <li>Input Strata does not consume goes on to vanilla.</li>
 *     <li>Minecraft reports held keys as repeated presses; this screen turns them into repeats.</li>
 *     <li>Strata counts clicks itself and ignores vanilla's double-click flag.</li>
 *     <li>Scroll amounts already include the player's wheel sensitivity. Fractional amounts count as
 *     {@link ScrollKind#PRECISE}.</li>
 *     <li>Minecraft gives no modifiers with pointer moves, scrolls or typed characters. Moves and scrolls report
 *     the Shift, Control and Alt keys held at the time, read from the game, so a Shift+wheel scrolls sideways.
 *     Typed characters report {@link Modifiers#NONE}, since some layouts type characters with Control and Alt
 *     held.</li>
 *     <li>Files dropped on the game window reach the component under the pointer as a drop of
 *     {@code Transfer.files}, through {@link #onFilesDrop(List)}. The game reports no drag before the drop, so
 *     components hear {@code DRAG_OVER} and {@code DROP} only when the files land, at the pointer's last position.</li>
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
    private HostTitle hostTitle;
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
     * Set while a close the Strata screen agreed to, or made through its handle, runs {@link #onClose()}, which then
     * does not ask again.
     */
    private boolean closing;
    // Where the pointer was last seen, in GUI pixels, for files dropped on the window
    private float pointerX;
    private float pointerY;

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
        this.hostTitle = new HostTitle(false);
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
        this.hostTitle = new HostTitle(false);
    }

    /**
     * Creates the vanilla screen for a given game and font, as the public constructors do for the running game.
     */
    StrataScreen(final Minecraft minecraft, final Font font, final Component title,
                 final org.inspirenxe.strata.api.screen.Screen content, final ThemeRef theme) {
        super(minecraft, font, title);
        this.content = Objects.requireNonNull(content, "content");
        this.theme = null;
        this.themeRef = Objects.requireNonNull(theme, "theme");
        this.heldKeys = new HeldKeys();
        this.handle = new Handle();
        this.hostTitle = new HostTitle(false);
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
        final StrataScreen screen = new StrataScreen(minecraft, font, MinecraftText.component(content.title()), content,
                theme);
        // The vanilla title was made from the Strata screen's, so the host's title stands for it from the start
        screen.hostTitle = new HostTitle(true);
        return screen;
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
                    ? StrataCore.createScreenHost(platform, this.themeRef, this::closeFromHandle)
                    : StrataCore.createScreenHost(platform, this.theme, this::closeFromHandle);
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
        current.setPixelsPerUnit(MinecraftPlatform.pixelsPerUnit(this.minecraft));
        current.resize(this.width, this.height);
        if (current.screen().isEmpty()) {
            current.open(this.content);
            this.hostTitle.opened(current);
        }
    }

    /**
     * Returns the title vanilla shows and narrates: the Strata screen's title as it is now, after any
     * {@code ScreenHandle.setTitle}, in the game's current language. A screen constructed with a vanilla title returns
     * that title until the Strata screen sets another, and every screen returns its vanilla title while the Strata
     * screen is not open.
     *
     * @return the title
     */
    @Override
    public Component getTitle() {
        return this.hostTitle.get(this.host, super.getTitle());
    }

    /**
     * Counts the client tick for the {@link MinecraftScheduler}, which runs screens' repeating tasks where Sponge's
     * scheduler does not.
     */
    @Override
    public void tick() {
        super.tick();
        StrataMinecraft.tick();
    }

    @Override
    public void removed() {
        this.heldKeys.clear();
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
     * Shows the screen this one was opened above, or else returns to the game as vanilla does. Unless the Strata
     * screen already agreed or closed itself through its handle, it is asked first through its
     * {@code closeRequested}, and nothing closes when it refuses.
     */
    @Override
    public void onClose() {
        if (!this.closing) {
            this.askToClose();
            return;
        }
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
     * Asks the Strata screen whether it may close, and closes this screen through {@link #onClose()} when it agrees.
     * The host closes the Strata screen itself when it agrees.
     */
    private void askToClose() {
        if (this.host == null || this.host.requestClose()) {
            this.closeAgreed();
        }
    }

    /**
     * Runs {@link #onClose()} for a close that needs no asking.
     */
    private void closeAgreed() {
        this.closing = true;
        try {
            this.onClose();
        } finally {
            this.closing = false;
        }
    }

    /**
     * Closes the Strata screen without asking, for its handle's {@code close()} and a {@code requestClose()} it
     * agreed to, however it is shown: through {@link #onClose()} when it shows, together with the screens above it
     * when it waits beneath them, and quietly otherwise.
     */
    private void closeFromHandle() {
        final Screen shown = switcher.current(this.minecraft);
        if (shown == this) {
            this.closeAgreed();
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
            top.closeAgreed();
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
            this.canvas.releaseTextures();
        }
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        this.pointerX = mouseX;
        this.pointerY = mouseY;
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.canvas.begin(graphics, MinecraftPlatform.pixelsPerUnit(this.minecraft));
        this.host.render(this.canvas);
    }

    @Override
    public void mouseMoved(final double x, final double y) {
        this.pointerX = (float) x;
        this.pointerY = (float) y;
        this.host.pointerMoved((float) x, (float) y, MinecraftInput.held(this.minecraft));
    }

    /**
     * Drops files from the desktop on the component under the pointer, as {@code Transfer.files}.
     *
     * @param files the dropped files; nothing happens for none
     */
    @Override
    public void onFilesDrop(final List<Path> files) {
        if (!files.isEmpty() && this.host != null) {
            this.host.dropped(this.pointerX, this.pointerY, Transfer.files(files), MinecraftInput.held(this.minecraft));
        }
    }

    @Override
    public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
        return this.host.pointerButton((float) event.x(), (float) event.y(), MinecraftInput.button(event.button()),
                true, MinecraftInput.modifiers(event)) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(final MouseButtonEvent event) {
        return this.host.pointerButton((float) event.x(), (float) event.y(), MinecraftInput.button(event.button()),
                false, MinecraftInput.modifiers(event)) || super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
        return this.host.scroll((float) x, (float) y, MinecraftInput.scrollX(scrollX), (float) scrollY,
                MinecraftInput.scrollKind(scrollX, scrollY), MinecraftInput.held(this.minecraft))
                || super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        final Key key = MinecraftInput.key(event.key());
        // Minecraft sends presses and repeats through the same method
        final KeyAction action = this.heldKeys.press(key);
        if (this.host.key(key, action, MinecraftInput.modifiers(event))) {
            return true;
        }
        if (event.isEscape() && this.shouldCloseOnEsc()) {
            // Asked here rather than in onClose, so a subclass's own onClose still waits for the answer
            this.askToClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(final KeyEvent event) {
        final Key key = MinecraftInput.key(event.key());
        this.heldKeys.release(key);
        return this.host.key(key, KeyAction.RELEASE, MinecraftInput.modifiers(event)) || super.keyReleased(event);
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        return this.host.charTyped(event.codepoint(), Modifiers.NONE) || super.charTyped(event);
    }

    /**
     * Reads and sets the screen vanilla shows.
     */
    interface Switcher {
        /**
         * Vanilla's own: {@code minecraft.gui}.
         */
        Switcher VANILLA = new Switcher() {
            @Override
            public Screen current(final Minecraft minecraft) {
                return minecraft.gui.screen();
            }

            @Override
            public void set(final Minecraft minecraft, final Screen screen) {
                minecraft.gui.setScreen(screen);
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
            StrataScreen.this.closeFromHandle();
        }

        @Override
        public boolean requestClose() {
            return StrataScreen.this.host != null
                    && StrataScreen.this.host.handle().map(ScreenHandle::requestClose).orElse(false);
        }

        @Override
        public boolean isOpen() {
            return StrataScreen.this.host != null && StrataScreen.this.host.handle().isPresent();
        }

        @Override
        public ScreenHandle setTitle(final Text title) {
            Objects.requireNonNull(title, "title");
            if (StrataScreen.this.host != null) {
                StrataScreen.this.host.handle().ifPresent(handle -> handle.setTitle(title));
            }
            return this;
        }

        @Override
        public Subscription every(final long millis, final Runnable task) {
            Objects.requireNonNull(task, "task");
            if (millis <= 0L) {
                throw new IllegalArgumentException("A task's interval must be above 0 ms: " + millis);
            }
            if (StrataScreen.this.host == null) {
                return () -> { };
            }
            return StrataScreen.this.host.handle().map(handle -> handle.every(millis, task)).orElse(() -> { });
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
