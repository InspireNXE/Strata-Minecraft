package org.inspirenxe.strata.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Builds Strata {@link Tooltip}s from vanilla text, such as the tooltip vanilla shows for an item in a slot.
 *
 * <p>Vanilla chat components become {@link Tooltip#lines styled lines}. Each run of text keeps its color, bold,
 * italic, underline and strikethrough, and draws with a shadow as vanilla tooltip text does. Text without a color
 * is white. Neighboring runs in the same style merge into one span. Obfuscated text draws as plain text, and fonts
 * and shadow colors other than the default are dropped.</p>
 *
 * <p>The tooltips hold text only. Item tooltip images, such as a bundle's contents, are not drawn, and vanilla's
 * extra gap below an item's name is left out. A tooltip is built once, so build a new one when the stack
 * changes.</p>
 */
public final class MinecraftTooltips {
    /**
     * The color of tooltip text that sets none: white, as vanilla draws it.
     */
    public static final int DEFAULT_COLOR = 0xFFFFFFFF;

    private MinecraftTooltips() {
    }

    /**
     * Returns the tooltip vanilla shows for an item stack in a slot, from the running game.
     *
     * @param stack the stack
     * @return the tooltip; it has no lines when the stack hides its tooltip
     * @see #item(Minecraft, ItemStack)
     */
    public static Tooltip item(final ItemStack stack) {
        return item(Minecraft.getInstance(), stack);
    }

    /**
     * Returns the tooltip vanilla shows for an item stack in a slot. The lines come from vanilla's
     * {@code Screen.getTooltipFromItem}, so they follow the player's advanced tooltips setting (F3+H) at the time of
     * the call. Vanilla builds them for the current player and level, when there are any.
     *
     * @param minecraft the game
     * @param stack the stack
     * @return the tooltip; it has no lines when the stack hides its tooltip
     */
    public static Tooltip item(final Minecraft minecraft, final ItemStack stack) {
        return lines(Screen.getTooltipFromItem(minecraft, stack));
    }

    /**
     * Returns a tooltip of vanilla text, one line per element.
     *
     * @param lines the lines, top to bottom
     * @return the tooltip
     */
    public static Tooltip lines(final List<? extends FormattedText> lines) {
        final List<List<TextSpan>> converted = new ArrayList<>(lines.size());
        for (final FormattedText line : lines) {
            converted.add(spans(line));
        }
        return Tooltip.lines(converted);
    }

    /**
     * Converts one line of vanilla text to spans, in the tooltip styling this class describes.
     *
     * @param text the text, usually a chat component
     * @return the spans, left to right; empty for empty text
     */
    public static List<TextSpan> spans(final FormattedText text) {
        final List<TextSpan> spans = new ArrayList<>();
        text.visit((style, content) -> {
            if (!content.isEmpty()) {
                final TextStyle converted = textStyle(style);
                final int last = spans.size() - 1;
                if (last >= 0 && spans.get(last).style().equals(converted)) {
                    spans.set(last, spans.get(last).withText(spans.get(last).text() + content));
                } else {
                    spans.add(new TextSpan(content, converted));
                }
            }
            return Optional.empty();
        }, Style.EMPTY);
        return List.copyOf(spans);
    }

    /**
     * Converts a vanilla style to the Strata style of tooltip text.
     *
     * @param style the vanilla style
     * @return the Strata style, with a shadow
     */
    static TextStyle textStyle(final Style style) {
        final TextColor color = style.getColor();
        return new TextStyle(color == null ? DEFAULT_COLOR : 0xFF000000 | color.getValue(), style.isBold(),
                style.isItalic(), style.isUnderlined(), style.isStrikethrough(), true);
    }
}
