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
import org.inspirenxe.strata.spi.UiHost;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * A vanilla screen that hosts one Strata screen. Open it like any other: {@code minecraft.setScreen(new
 * StrataScreen(title, new MyScreen(), theme))}.
 *
 * <p>Input Strata does not consume falls through to vanilla, so Escape still closes the screen. A Strata screen
 * that asks to close gets vanilla's {@link #onClose()}.</p>
 */
public class StrataScreen extends Screen {
    private final org.inspirenxe.strata.api.screen.Screen content;
    private final Theme theme;
    private final Set<Key> keysDown;
    private MinecraftCanvas canvas;
    private UiHost host;

    public StrataScreen(final Component title, final org.inspirenxe.strata.api.screen.Screen content, final Theme theme) {
        super(title);
        this.content = Objects.requireNonNull(content, "content");
        this.theme = Objects.requireNonNull(theme, "theme");
        this.keysDown = EnumSet.noneOf(Key.class);
    }

    public org.inspirenxe.strata.api.screen.Screen content() {
        return this.content;
    }

    @Override
    protected void init() {
        if (this.host == null) {
            this.canvas = new MinecraftCanvas(this.font);
            final MinecraftPlatform platform = new MinecraftPlatform(this.minecraft, this.canvas.text(), this);
            this.host = StrataCore.createHost(platform, this.theme, this::onClose);
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
        this.keysDown.clear();
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
        // Strata counts clicks itself, so vanilla's double-click flag is not needed
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
        return this.host.scroll((float) x, (float) y, (float) scrollX, (float) scrollY, ScrollKind.WHEEL, Modifiers.NONE)
                || super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(final KeyEvent event) {
        final Key key = MinecraftInput.key(event.key());
        // Minecraft sends presses and repeats through the same method
        final KeyAction action = this.keysDown.add(key) ? KeyAction.PRESS : KeyAction.REPEAT;
        return this.host.key(key, action, MinecraftInput.modifiers(event)) || super.keyPressed(event);
    }

    @Override
    public boolean keyReleased(final KeyEvent event) {
        final Key key = MinecraftInput.key(event.key());
        this.keysDown.remove(key);
        return this.host.key(key, KeyAction.RELEASE, MinecraftInput.modifiers(event)) || super.keyReleased(event);
    }

    @Override
    public boolean charTyped(final CharacterEvent event) {
        return this.host.charTyped(event.codepoint(), Modifiers.NONE) || super.charTyped(event);
    }
}
