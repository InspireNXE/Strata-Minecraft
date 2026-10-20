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

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Vanilla text must become tooltip spans that keep vanilla's styling.
 */
class MinecraftTooltipsTest {
    private static final TextStyle WHITE = TextStyle.DEFAULT.withShadow(true);

    @Test
    void stylesCarryOverWithAShadow() {
        final Component line = Component.literal("Sword").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                .append(Component.literal(" of ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x123456)).withItalic(true)))
                .append(Component.literal("Doom").withStyle(ChatFormatting.UNDERLINE, ChatFormatting.STRIKETHROUGH));

        final TextStyle aquaBold = WHITE.withColor(0xFF55FFFF).withBold(true);
        assertEquals(List.of(
                new TextSpan("Sword", aquaBold),
                new TextSpan(" of ", aquaBold.withColor(0xFF123456).withItalic(true)),
                new TextSpan("Doom", aquaBold.withUnderline(true).withStrikethrough(true))
        ), MinecraftTooltips.spans(line));
    }

    @Test
    void uncoloredTextIsWhiteAndEqualRunsMerge() {
        final Component line = Component.literal("a").append("b").append(Component.literal(""));

        assertEquals(List.of(new TextSpan("ab", WHITE)), MinecraftTooltips.spans(line));
    }

    @Test
    void eachComponentIsALine() {
        final Tooltip tooltip = MinecraftTooltips.lines(List.of(Component.literal("Name"), Component.empty(),
                Component.literal("Lore").withStyle(ChatFormatting.GRAY)));

        final List<List<TextSpan>> resolved = ((Tooltip.Lines) tooltip).lines().stream()
                .map(text -> text.resolve(WHITE))
                .toList();
        assertEquals(List.of(
                List.of(new TextSpan("Name", WHITE)),
                List.of(),
                List.of(new TextSpan("Lore", WHITE.withColor(0xFFAAAAAA)))
        ), resolved);
    }
}
