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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds Strata {@link Tooltip}s from vanilla text, such as the tooltip vanilla shows for an item in a slot.
 *
 * <p>Vanilla chat components become {@link Tooltip#lines styled lines} through {@link MinecraftText}, over white
 * text with a shadow as vanilla draws tooltip text. Each run of text keeps its color, bold, italic, underline and
 * strikethrough, {@code §} codes apply as vanilla applies them, and neighboring runs in the same style merge into
 * one span. Obfuscated text draws as plain text, and fonts and shadow colors other than the default are
 * dropped.</p>
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

    private static final TextStyle TEXT_STYLE = TextStyle.DEFAULT.withColor(DEFAULT_COLOR).withShadow(true);

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
        final List<Text> converted = new ArrayList<>(lines.size());
        for (final FormattedText line : lines) {
            converted.add(MinecraftText.of(line));
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
        return MinecraftText.spans(text, TEXT_STYLE);
    }
}
