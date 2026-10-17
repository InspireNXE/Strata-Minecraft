package org.inspirenxe.strata.minecraft;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.StrataCore;
import org.inspirenxe.strata.spi.HeldKeys;
import org.inspirenxe.strata.spi.ScreenHost;

import java.util.Objects;

/**
 * A vanilla {@link Screen} that shows one Strata screen. Open it like any other screen:
 *
 * <pre>{@code
 * Minecraft.getInstance().gui.setScreen(
 *         new StrataScreen(Component.literal("Settings"), new SettingsScreen(), MinecraftThemes.vanilla()));
 * }</pre>
 *
 * <p>How Minecraft maps onto Strata:</p>
 * <ul>
 *     <li>One UI unit is one GUI pixel, so layouts follow the player's GUI scale. The host is resized whenever
 *     vanilla re-initializes the screen, such as on a window resize.</li>
 *     <li>The Strata screen is built when the vanilla screen first opens, and closed when vanilla removes it. A
 *     Strata screen that asks to close gets vanilla's {@link #onClose()}, which by default returns to the game. Override
 *     it to return to a parent screen instead.</li>
 *     <li>Input Strata does not consume goes on to vanilla, so Escape still closes the screen.</li>
 *     <li>Minecraft reports held keys as repeated presses; this screen turns them into repeats.</li>
 *     <li>Strata counts clicks itself and ignores vanilla's double-click flag.</li>
 *     <li>Scroll amounts already include the player's wheel sensitivity. Fractional amounts count as
 *     {@link ScrollKind#PRECISE}.</li>
 *     <li>Minecraft gives no modifiers with pointer moves, scrolls or typed characters, so those report
 *     {@link Modifiers#NONE}.</li>
 *     <li>Whether the game pauses behind the screen follows vanilla's {@link #isPauseScreen()}; override it to
 *     change that.</li>
 * </ul>
 */
public class StrataScreen extends Screen {
    private final org.inspirenxe.strata.api.screen.Screen content;
    private final Theme theme;
    private final HeldKeys heldKeys;
    private MinecraftCanvas canvas;
    private ScreenHost host;

    /**
     * Creates the vanilla screen. Nothing is built until Minecraft opens it.
     *
     * @param title the vanilla title, used for narration
     * @param content the Strata screen to show
     * @param theme the theme for its components, for example {@link MinecraftThemes#vanilla()}
     */
    public StrataScreen(final Component title, final org.inspirenxe.strata.api.screen.Screen content, final Theme theme) {
        super(title);
        this.content = Objects.requireNonNull(content, "content");
        this.theme = Objects.requireNonNull(theme, "theme");
        this.heldKeys = new HeldKeys();
    }

    /**
     * Returns the Strata screen this vanilla screen shows.
     *
     * @return the Strata screen
     */
    public org.inspirenxe.strata.api.screen.Screen content() {
        return this.content;
    }

    @Override
    protected void init() {
        if (this.host == null) {
            this.canvas = new MinecraftCanvas(this.font);
            final MinecraftPlatform platform = new MinecraftPlatform(this.minecraft, this.canvas.textMetrics(), this);
            this.host = StrataCore.createScreenHost(platform, this.theme, this::onClose);
            this.host.resize(this.width, this.height);
            this.host.open(this.content);
        } else {
            this.host.resize(this.width, this.height);
        }
    }

    @Override
    public void removed() {
        if (this.host != null) {
            this.host.close();
            this.host = null;
        }
        this.heldKeys.clear();
        super.removed();
    }

    @Override
    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        this.canvas.begin(graphics);
        this.host.render(this.canvas);
    }

    @Override
    public void mouseMoved(final double x, final double y) {
        this.host.pointerMoved((float) x, (float) y, Modifiers.NONE);
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
                MinecraftInput.scrollKind(scrollX, scrollY), Modifiers.NONE)
                || super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        final Key key = MinecraftInput.key(event.key());
        // Minecraft sends presses and repeats through the same method
        final KeyAction action = this.heldKeys.press(key);
        return this.host.key(key, action, MinecraftInput.modifiers(event)) || super.keyPressed(event);
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
}
