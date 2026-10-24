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

import com.mojang.blaze3d.font.GlyphProvider;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;

import java.util.Optional;

/**
 * The Minecraft {@link TextMetrics}: turns Strata spans into chat components and measures them with a vanilla
 * {@link Font}.
 *
 * <p>Widths are whole GUI pixels, as vanilla's font reports them, and include vanilla's extra pixel for bold. The
 * line height is the font's {@code lineHeight}, 9. The ascent is vanilla's {@link GlyphProvider#BASELINE}, 7: every
 * font, a resource pack's included, places its glyphs on a baseline that far below the top of the line, each glyph
 * by its own ascent. Color and shadow do not change widths.</p>
 *
 * <p>A style's {@linkplain TextStyle#font() font} names the game's font of the same id, defined by a resource pack in
 * {@code assets/<namespace>/font/<path>.json}; an id no pack defines measures and draws in the default font. A style's
 * {@linkplain TextStyle#size() size} scales vanilla's 9-pixel text, so the {@linkplain #DEFAULT_SIZE default size} is
 * 9 and size 18 is twice as large. {@link #version()} changes on every resource reload.</p>
 */
public final class MinecraftTextMetrics implements TextMetrics {
    /** The size text of size 0 draws at, in GUI pixels: vanilla's line height. */
    public static final float DEFAULT_SIZE = 9f;

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

    /**
     * Returns how much larger than vanilla's 9-pixel text a style draws.
     *
     * @param style the style
     * @return the factor, positive
     */
    public static float scale(final TextStyle style) {
        return style.size() > 0f ? style.size() / DEFAULT_SIZE : 1f;
    }

    @Override
    public float width(final TextSpan span) {
        return this.font.width(this.component(span)) * scale(span.style());
    }

    @Override
    public float lineHeight(final TextStyle style) {
        return this.font.lineHeight * scale(style);
    }

    @Override
    public float ascent(final TextStyle style) {
        return Math.min(GlyphProvider.BASELINE, this.font.lineHeight) * scale(style);
    }

    @Override
    public long version() {
        return MinecraftFonts.version();
    }

    private static Style style(final TextStyle style) {
        Style result = Style.EMPTY
                .withBold(style.bold())
                .withItalic(style.italic())
                .withUnderlined(style.underline())
                .withStrikethrough(style.strikethrough());
        if (style.font() != null) {
            final Optional<FontDescription> font = MinecraftFonts.description(style.font());
            if (font.isPresent()) {
                result = result.withFont(font.get());
            }
        }
        return result;
    }
}
