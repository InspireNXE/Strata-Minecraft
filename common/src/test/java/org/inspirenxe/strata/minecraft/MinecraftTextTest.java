package org.inspirenxe.strata.minecraft;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Vanilla text and {@code §} codes must become spans that look the way vanilla draws them.
 */
class MinecraftTextTest {
    private static final TextStyle WHITE = MinecraftText.DEFAULT_STYLE;

    @Test
    void legacyCodesStyleTheTextAfterThem() {
        assertEquals(List.of(
                new TextSpan("Gold ", WHITE.withColor(0xFFFFAA00)),
                new TextSpan("bold", WHITE.withColor(0xFFFFAA00).withBold(true)),
                new TextSpan(" plain", WHITE)
        ), MinecraftText.spans("§6Gold §lbold§r plain"));
    }

    @Test
    void aColorCodeClearsTheFormattingBeforeIt() {
        assertEquals(List.of(
                new TextSpan("a", WHITE.withBold(true).withItalic(true)),
                new TextSpan("b", WHITE.withColor(0xFFFF5555))
        ), MinecraftText.spans("§l§oa§cb"));
    }

    @Test
    void unknownAndTrailingCodesAreDropped() {
        assertEquals(List.of(new TextSpan("ab", WHITE)), MinecraftText.spans("a§zb§"));
    }

    @Test
    void vanillaStylesLayOverTheBase() {
        final TextStyle base = TextStyle.DEFAULT.withColor(0x80404040).withItalic(true);
        final Component text = Component.literal("x")
                .append(Component.literal("y").withStyle(ChatFormatting.GREEN))
                .append(Component.literal("z").withStyle(style -> style.withItalic(false).withUnderlined(true)));

        assertEquals(List.of(
                new TextSpan("x", base),
                new TextSpan("y", base.withColor(0x8055FF55)),
                new TextSpan("z", base.withItalic(false).withUnderline(true))
        ), MinecraftText.spans(text, base), "the base's alpha and shadow stay, everything else can be replaced");
    }

    @Test
    void resetInsideAComponentGoesBackToThatComponentsStyle() {
        final Component text = Component.literal("§cred§r back").withStyle(ChatFormatting.BLUE);

        assertEquals(List.of(
                new TextSpan("red", WHITE.withColor(0xFFFF5555)),
                new TextSpan(" back", WHITE.withColor(0xFF5555FF))
        ), MinecraftText.spans(text));
    }

    @Test
    void lineBreaksStayAndEmptyTextHasNoSpans() {
        assertEquals(List.of(new TextSpan("one\ntwo", WHITE)), MinecraftText.spans(Component.literal("one\ntwo")));
        assertEquals(List.of(), MinecraftText.spans(Component.empty()));
        assertEquals(List.of(), MinecraftText.spans("§a"));
    }

    @Test
    void codePointsOutsideTheBasicPlaneStayWhole() {
        assertEquals(List.of(new TextSpan("😀!", WHITE)), MinecraftText.spans("😀!"));
    }
}
