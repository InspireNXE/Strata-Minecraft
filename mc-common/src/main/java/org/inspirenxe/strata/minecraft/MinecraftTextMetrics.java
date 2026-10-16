package org.inspirenxe.strata.minecraft;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;

/**
 * The Minecraft {@link TextMetrics}: turns Strata spans into chat components and measures them with a vanilla
 * {@link Font}.
 *
 * <p>Widths are whole GUI pixels, as vanilla's font reports them, and include vanilla's extra pixel for bold. The
 * line height is the font's {@code lineHeight}, 9 for the default font. Color and shadow do not change widths.</p>
 */
public final class MinecraftTextMetrics implements TextMetrics {
    private final Font font;

    /**
     * Creates metrics for a font.
     *
     * @param font the vanilla font
     */
    public MinecraftTextMetrics(final Font font) {
        this.font = font;
    }

    /**
     * Returns the font text is measured and drawn with.
     *
     * @return the vanilla font
     */
    public Font font() {
        return this.font;
    }

    /**
     * Converts a span to a vanilla chat component carrying its bold, italic, underline and strikethrough. Color and
     * shadow are passed separately when drawing.
     *
     * @param span the span
     * @return the component
     */
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
        return Style.EMPTY
                .withBold(style.bold())
                .withItalic(style.italic())
                .withUnderlined(style.underline())
                .withStrikethrough(style.strikethrough());
    }
}
