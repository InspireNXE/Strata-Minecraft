package org.inspirenxe.strata.minecraft;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;

/**
 * Turns Strata spans into chat components and measures them with Minecraft's font.
 */
public final class MinecraftText implements TextMetrics {
    private final Font font;

    public MinecraftText(final Font font) {
        this.font = font;
    }

    public Font font() {
        return this.font;
    }

    public Component component(final TextSpan span) {
        return Component.literal(span.text()).withStyle(style(span.style()));
    }

    @Override
    public float width(final TextSpan span) {
        return this.font.width(this.component(span));
    }

    @Override
    public float lineHeight(final TextStyle style) {
        return this.font.lineHeight;
    }

    private static Style style(final TextStyle style) {
        // Color and shadow are passed when drawing, so only the decorations live on the style
        return Style.EMPTY
                .withBold(style.bold())
                .withItalic(style.italic())
                .withUnderlined(style.underline())
                .withStrikethrough(style.strikethrough());
    }
}
