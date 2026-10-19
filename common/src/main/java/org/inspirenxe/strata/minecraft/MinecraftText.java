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

import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns vanilla's formatted text into Strata {@link TextSpan}s, so a {@link org.inspirenxe.strata.api.control.display.Label}
 * can show chat components and text with {@code §} formatting codes:
 *
 * <pre>{@code
 * label.setSpans(MinecraftText.spans(Component.translatable("container.inventory")));
 * label.setSpans(MinecraftText.spans("§6Gold §rand §lbold"));
 * }</pre>
 *
 * <p>Each run of text is drawn in a base style with the vanilla style laid over it, the way a child component's
 * style is laid over its parent's: a color, bold, italic, underline or strikethrough the text sets replaces the
 * base's, and whatever it leaves unset comes from the base. The color keeps the base's alpha, since vanilla colors
 * have none, and the shadow is always the base's. {@code §} codes inside the text apply as vanilla applies them when
 * it draws: a color code clears bold, italic, underline and strikethrough, and {@code §r} goes back to the style of
 * the component the code is in. Neighboring runs in the same style merge into one span, and the codes themselves
 * are dropped.</p>
 *
 * <p>Obfuscated text becomes plain text, and fonts, shadow colors, click and hover events and insertions are
 * dropped. A {@code \n} in the text stays in the span, where a label starts a new line. Broken surrogate pairs
 * become {@code U+FFFD}, as vanilla draws them. Translations resolve in the game's language at the time of the
 * call, so convert again after the language changes.</p>
 */
public final class MinecraftText {
    /**
     * Vanilla's usual GUI text: opaque white with a shadow, as the {@link MinecraftThemes#vanilla() vanilla theme}
     * draws labels and as vanilla draws tooltips.
     */
    public static final TextStyle DEFAULT_STYLE = TextStyle.DEFAULT.withColor(0xFFFFFFFF).withShadow(true);

    private MinecraftText() {
    }

    /**
     * Converts vanilla text to spans over {@link #DEFAULT_STYLE}.
     *
     * @param text the text, usually a chat component
     * @return the spans, in reading order; empty for empty text
     */
    public static List<TextSpan> spans(final FormattedText text) {
        return spans(text, DEFAULT_STYLE);
    }

    /**
     * Converts vanilla text to spans over a base style.
     *
     * @param text the text, usually a chat component
     * @param base the style of text that sets no style of its own, and the shadow of all of it
     * @return the spans, in reading order; empty for empty text
     */
    public static List<TextSpan> spans(final FormattedText text, final TextStyle base) {
        final SpanSink sink = new SpanSink(base);
        StringDecomposer.iterateFormatted(text, style(base), sink);
        return sink.spans();
    }

    /**
     * Converts a string with {@code §} formatting codes to spans over {@link #DEFAULT_STYLE}.
     *
     * @param text the text, such as {@code "§aGreen §rplain"}
     * @return the spans, in reading order; empty for empty text
     */
    public static List<TextSpan> spans(final String text) {
        return spans(text, DEFAULT_STYLE);
    }

    /**
     * Converts a string with {@code §} formatting codes to spans over a base style. {@code §r} goes back to the
     * base.
     *
     * @param text the text, such as {@code "§aGreen §rplain"}
     * @param base the style of text before any code, and the shadow of all of it
     * @return the spans, in reading order; empty for empty text
     */
    public static List<TextSpan> spans(final String text, final TextStyle base) {
        final SpanSink sink = new SpanSink(base);
        StringDecomposer.iterateFormatted(text, style(base), sink);
        return sink.spans();
    }

    /**
     * Returns the vanilla style matching a Strata style's color, bold, italic, underline and strikethrough, each set
     * explicitly so that vanilla's own styles inherit them.
     *
     * @param style the Strata style
     * @return the vanilla style
     */
    static Style style(final TextStyle style) {
        return Style.EMPTY
                .withColor(TextColor.fromRgb(style.color() & 0xFFFFFF))
                .withBold(style.bold())
                .withItalic(style.italic())
                .withUnderlined(style.underline())
                .withStrikethrough(style.strikethrough());
    }

    /**
     * Converts a vanilla style, already laid over the base, to a Strata style.
     *
     * @param style the vanilla style
     * @param base the base, for the alpha, the shadow and a missing color
     * @return the Strata style
     */
    static TextStyle textStyle(final Style style, final TextStyle base) {
        final TextColor color = style.getColor();
        final int argb = color == null ? base.color() : (base.color() & 0xFF000000) | (color.getValue() & 0xFFFFFF);
        return new TextStyle(argb, style.isBold(), style.isItalic(), style.isUnderlined(), style.isStrikethrough(),
                base.shadow());
    }

    /**
     * Collects code points into spans, starting a new span whenever the converted style changes.
     */
    private static final class SpanSink implements FormattedCharSink {
        private final TextStyle base;
        private final List<TextSpan> spans;
        private final StringBuilder run;
        private Style lastStyle;
        private TextStyle runStyle;

        SpanSink(final TextStyle base) {
            this.base = base;
            this.spans = new ArrayList<>();
            this.run = new StringBuilder();
        }

        @Override
        public boolean accept(final int position, final Style style, final int codePoint) {
            if (style != this.lastStyle) {
                this.lastStyle = style;
                final TextStyle converted = textStyle(style, this.base);
                if (!converted.equals(this.runStyle)) {
                    this.flush();
                    this.runStyle = converted;
                }
            }
            this.run.appendCodePoint(codePoint);
            return true;
        }

        private void flush() {
            if (!this.run.isEmpty()) {
                this.spans.add(new TextSpan(this.run.toString(), this.runStyle));
                this.run.setLength(0);
            }
        }

        List<TextSpan> spans() {
            this.flush();
            return List.copyOf(this.spans);
        }
    }
}
