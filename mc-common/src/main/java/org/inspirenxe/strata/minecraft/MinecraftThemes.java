package org.inspirenxe.strata.minecraft;

import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.Label;

/**
 * Themes built from vanilla's own GUI sprites, so Strata widgets look like vanilla ones.
 */
public final class MinecraftThemes {
    private static final Insets BUTTON_BORDER = Insets.of(3f);

    private MinecraftThemes() {
    }

    public static Theme vanilla() {
        final TextStyle text = TextStyle.DEFAULT.withColor(0xFFFFFFFF).withShadow(true);
        return Theme.builder("minecraft")
                .paint(Button.STYLE_NORMAL, Paint.nineSlice(sprite("widget/button"), BUTTON_BORDER))
                .paint(Button.STYLE_HOVERED, Paint.nineSlice(sprite("widget/button_highlighted"), BUTTON_BORDER))
                .paint(Button.STYLE_PRESSED, Paint.nineSlice(sprite("widget/button_highlighted"), BUTTON_BORDER))
                .paint(Button.STYLE_DISABLED, Paint.nineSlice(sprite("widget/button_disabled"), BUTTON_BORDER))
                .textStyle(Button.STYLE_TEXT, text)
                .textStyle(Button.STYLE_TEXT_DISABLED, text.withColor(0xFFA0A0A0))
                .textStyle(Label.STYLE_TEXT, text)
                .build();
    }

    private static SpriteId sprite(final String path) {
        return SpriteId.of("minecraft", path);
    }
}
