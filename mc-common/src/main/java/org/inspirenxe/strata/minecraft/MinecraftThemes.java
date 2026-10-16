package org.inspirenxe.strata.minecraft;

import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.Label;
import org.inspirenxe.strata.api.widget.TextBox;

/**
 * Themes built from vanilla Minecraft's own GUI sprites, so Strata widgets look like vanilla ones.
 */
public final class MinecraftThemes {
    // Borders as declared in each sprite's .png.mcmeta in 26.3; Minecraft reads the .mcmeta, other engines these
    private static final Insets BUTTON_BORDER = Insets.of(3f);
    private static final Insets BUTTON_DISABLED_BORDER = Insets.of(1f);
    private static final Insets FIELD_BORDER = Insets.of(1f);

    private MinecraftThemes() {
    }

    /**
     * Returns a theme matching vanilla's widgets. It defines every key of every built-in widget:
     * <ul>
     *     <li>buttons use {@code widget/button}, {@code widget/button_highlighted} (hovered, focused and pressed) and
     *     {@code widget/button_disabled}, with white shadowed captions, gray when disabled;</li>
     *     <li>text boxes use {@code widget/text_field} and {@code widget/text_field_highlighted}, with vanilla's
     *     {@code 0xE0E0E0} text and {@code 0x707070} when disabled;</li>
     *     <li>labels are white with a shadow.</li>
     * </ul>
     *
     * @return the vanilla theme
     */
    public static Theme vanilla() {
        final TextStyle text = TextStyle.DEFAULT.withColor(0xFFFFFFFF).withShadow(true);
        return Theme.builder("minecraft")
                .paint(Button.STYLE_NORMAL, Paint.nineSlice(sprite("widget/button"), BUTTON_BORDER))
                .paint(Button.STYLE_HOVERED, Paint.nineSlice(sprite("widget/button_highlighted"), BUTTON_BORDER))
                .paint(Button.STYLE_PRESSED, Paint.nineSlice(sprite("widget/button_highlighted"), BUTTON_BORDER))
                .paint(Button.STYLE_DISABLED, Paint.nineSlice(sprite("widget/button_disabled"), BUTTON_DISABLED_BORDER))
                .textStyle(Button.STYLE_TEXT, text)
                .textStyle(Button.STYLE_TEXT_DISABLED, text.withColor(0xFFA0A0A0))
                .textStyle(Label.STYLE_TEXT, text)
                // Vanilla's EditBox has no disabled sprite; its uneditable text is 0x707070
                .paint(TextBox.STYLE_NORMAL, Paint.nineSlice(sprite("widget/text_field"), FIELD_BORDER))
                .paint(TextBox.STYLE_FOCUSED, Paint.nineSlice(sprite("widget/text_field_highlighted"), FIELD_BORDER))
                .paint(TextBox.STYLE_DISABLED, Paint.nineSlice(sprite("widget/text_field"), FIELD_BORDER))
                .paint(TextBox.STYLE_SELECTION, Paint.solid(0x800000FF))
                .paint(TextBox.STYLE_CARET, Paint.solid(0xFFD0D0D0))
                .textStyle(TextBox.STYLE_TEXT, text.withColor(0xFFE0E0E0))
                .textStyle(TextBox.STYLE_TEXT_DISABLED, text.withColor(0xFF707070))
                .build();
    }

    private static SpriteId sprite(final String path) {
        return SpriteId.of("minecraft", path);
    }
}
