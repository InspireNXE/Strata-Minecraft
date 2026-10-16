package org.inspirenxe.strata.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.spi.Capabilities;
import org.inspirenxe.strata.spi.Clipboard;
import org.inspirenxe.strata.spi.Clock;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.TextInputSink;

import java.util.Optional;

/**
 * The Minecraft side of the SPI for one hosting screen. Minecraft sends key repeats itself.
 */
public final class MinecraftPlatform implements Platform {
    private static final Capabilities CAPABILITIES = new Capabilities(true);

    private final Minecraft minecraft;
    private final MinecraftText text;
    private final Clipboard clipboard;
    private final TextInputSink textInput;

    /**
     * @param listener the screen that hosts Strata; Minecraft tracks text focus per listener
     */
    public MinecraftPlatform(final Minecraft minecraft, final MinecraftText text, final GuiEventListener listener) {
        this.minecraft = minecraft;
        this.text = text;
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
        // Since 26.3 Minecraft delivers no characters until a text field says it has focus
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

    public Minecraft minecraft() {
        return this.minecraft;
    }

    @Override
    public TextMetrics textMetrics() {
        return this.text;
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
}
