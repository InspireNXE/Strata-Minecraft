package org.inspirenxe.strata.minecraft;

import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.CheckBox;
import org.inspirenxe.strata.api.widget.Label;
import org.inspirenxe.strata.api.widget.Menu;
import org.inspirenxe.strata.api.widget.RadioButton;
import org.inspirenxe.strata.api.widget.Slider;
import org.inspirenxe.strata.api.widget.SlotGrid;
import org.inspirenxe.strata.api.widget.TableView;
import org.inspirenxe.strata.api.widget.TextBox;
import org.inspirenxe.strata.api.widget.ToggleSwitch;
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
                Slider.STYLE_THUMB_HOVERED, TextBox.STYLE_NORMAL, TextBox.STYLE_FOCUSED, TextBox.STYLE_DISABLED,
                RadioButton.STYLE_NORMAL, RadioButton.STYLE_HOVERED, RadioButton.STYLE_SELECTED,
                RadioButton.STYLE_SELECTED_HOVERED, ToggleSwitch.STYLE_TRACK, ToggleSwitch.STYLE_TRACK_ON,
                ToggleSwitch.STYLE_KNOB, TableView.STYLE_HEADER, Menu.STYLE_BACKGROUND, Menu.STYLE_ITEM_HOVERED,
                SlotGrid.STYLE_SLOT);
        assertEquals(List.of(), List.copyOf(MinecraftThemes.vanilla().missing(required)));
    }
}
