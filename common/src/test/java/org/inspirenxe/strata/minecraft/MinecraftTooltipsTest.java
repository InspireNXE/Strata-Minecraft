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

        assertEquals(Tooltip.lines(List.of(
                List.of(new TextSpan("Name", WHITE)),
                List.of(),
                List.of(new TextSpan("Lore", WHITE.withColor(0xFFAAAAAA)))
        )), tooltip);
    }
}
