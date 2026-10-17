package org.inspirenxe.strata.minecraft;

import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.CheckBox;
import org.inspirenxe.strata.api.widget.Label;
import org.inspirenxe.strata.api.widget.Slider;
import org.inspirenxe.strata.api.widget.TextBox;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The vanilla theme must cover every required key of every built-in widget.
 */
class MinecraftThemesTest {

    @Test
    void vanillaDefinesEveryRequiredKey() {
        final List<String> required = List.of(Button.STYLE_NORMAL, Button.STYLE_HOVERED, Button.STYLE_PRESSED,
                Button.STYLE_DISABLED, Label.STYLE_TEXT, CheckBox.STYLE_NORMAL, CheckBox.STYLE_HOVERED,
                CheckBox.STYLE_CHECKED, CheckBox.STYLE_CHECKED_HOVERED, Slider.STYLE_TRACK, Slider.STYLE_THUMB,
                Slider.STYLE_THUMB_HOVERED, TextBox.STYLE_NORMAL, TextBox.STYLE_FOCUSED, TextBox.STYLE_DISABLED);
        assertEquals(List.of(), List.copyOf(MinecraftThemes.vanilla().missing(required)));
    }
}
