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

import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.button.RadioButton;
import org.inspirenxe.strata.api.control.button.ToggleSwitch;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.collection.GridView;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.display.BusyIndicator;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.display.ProgressBar;
import org.inspirenxe.strata.api.control.display.Separator;
import org.inspirenxe.strata.api.control.field.ColorPicker;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.control.pane.ScrollBar;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.overlay.Overlays;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeId;

/**
 * Themes built from vanilla Minecraft's own GUI sprites, so Strata controls look like vanilla widgets.
 */
public final class MinecraftThemes {
    // Borders as declared in each sprite's .png.mcmeta in 26.3; Minecraft reads the .mcmeta, other engines these
    private static final Insets BUTTON_BORDER = Insets.of(3f);
    /**
     * Vanilla has no pressed button sprite; the highlighted one darkened by this tint stands in for it.
     */
    private static final int PRESSED_TINT = 0xFFB0B0B0;
    private static final Insets BUTTON_DISABLED_BORDER = Insets.of(1f);
    private static final Insets FIELD_BORDER = Insets.of(1f);
    private static final Insets SLIDER_BORDER = Insets.of(1f);
    private static final Insets SLIDER_HANDLE_BORDER = new Insets(2f, 2f, 2f, 3f);
    private static final Insets SCROLLER_BORDER = Insets.of(1f);
    private static final Insets TAB_BORDER = new Insets(2f, 2f, 2f, 0f);
    private static final Insets POPUP_BORDER = Insets.of(6f);
    private static final Insets TOAST_BORDER = Insets.of(4f);

    /**
     * The id {@link #vanilla()} is registered under. Pass {@code ThemeRef.of(MinecraftThemes.VANILLA)} to a
     * {@link StrataScreen} to look like the game and follow resource packs, which restyle it with
     * {@code assets/minecraft/strata/themes/vanilla.json}.
     */
    public static final ThemeId VANILLA = ThemeId.of("minecraft", "vanilla");

    private MinecraftThemes() {
    }

    /**
     * Returns a theme matching vanilla's widgets. It defines every key of every built-in control:
     * <ul>
     *     <li>buttons use {@code widget/button}, {@code widget/button_highlighted} (hovered, focused and pressed) and
     *     {@code widget/button_disabled}, with white shadowed captions, gray when disabled;</li>
     *     <li>text boxes use {@code widget/text_field} and {@code widget/text_field_highlighted}, with vanilla's
     *     {@code 0xE0E0E0} text and {@code 0x707070} when disabled;</li>
     *     <li>checkboxes use {@code widget/checkbox}, {@code widget/checkbox_highlighted} (hovered and focused),
     *     {@code widget/checkbox_selected} and {@code widget/checkbox_selected_highlighted}, which have the check mark
     *     drawn in, so no mark paint is set. Vanilla has no disabled checkbox sprites, so a disabled checkbox shows
     *     the normal ones with a gray caption;</li>
     *     <li>sliders use {@code widget/slider} for the track, {@code widget/slider_highlighted} while focused,
     *     {@code widget/slider_handle} for the thumb and {@code widget/slider_handle_highlighted} while hovered,
     *     focused or pressed, with a white shadowed label, gray when disabled;</li>
     *     <li>labels are white with a shadow;</li>
     *     <li>scroll bars use {@code widget/scroller} for the thumb and {@code widget/scroller_background} for the
     *     track;</li>
     *     <li>tab bars use {@code widget/tab}, {@code widget/tab_highlighted} and {@code widget/tab_selected}.
     *     {@code widget/tab} leaves its top 4 pixels empty and edges the next 2, so an unselected tab's text sits two
     *     units lower, as in vanilla;</li>
     *     <li>windows use {@code popup/background}, the frame of vanilla's popup screens, with no title bar paint;</li>
     *     <li>combo boxes use the button sprites, over a near-black list;</li>
     *     <li>radio buttons use the checkbox sprites, so a selected one shows vanilla's check mark;</li>
     *     <li>toggle switches use the slider sprites: {@code widget/slider} for the track when off,
     *     {@code widget/slider_highlighted} when on, and the handle sprites for the knob;</li>
     *     <li>expanders use the button sprites for their header;</li>
     *     <li>slot grids use {@code container/slot}, with vanilla's white hover highlight over the item;</li>
     *     <li>the slots of a {@link StrataContainerScreen} use {@code container/slot}, and in Strata mode
     *     {@code container/slot_highlight_back} and {@code container/slot_highlight_front}, as vanilla's container
     *     screens do;</li>
     *     <li>menus are near-black with a white highlight, and table headers translucent black;</li>
     *     <li>spinners, text areas, trees and split panes add no keys: they use the button, text box, list and
     *     separator keys;</li>
     *     <li>list rows, progress bars, separators and the dialog backdrop use plain colors, since vanilla has no
     *     sprites for them. Tooltips use Strata's fallback, vanilla's classic dark tooltip colors;</li>
     *     <li>toasts use {@code toast/now_playing}, the frame of vanilla's advancement and music toasts, with white
     *     text and no shadow, as vanilla's toasts draw it. {@code toast/advancement} looks the same but has no
     *     {@code .mcmeta}, so the game would stretch it over toasts of other sizes.</li>
     * </ul>
     *
     * @return the vanilla theme
     */
    public static Theme vanilla() {
        final TextStyle text = TextStyle.DEFAULT.withColor(0xFFFFFFFF).withShadow(true);
        return Theme.builder(VANILLA.toString())
                .paint(Button.STYLE_NORMAL, Paint.nineSlice(sprite("widget/button"), BUTTON_BORDER))
                .paint(Button.STYLE_HOVERED, Paint.nineSlice(sprite("widget/button_highlighted"), BUTTON_BORDER))
                // Vanilla has no pressed sprite; the highlighted one darkened
                .paint(Button.STYLE_PRESSED, new Paint.NineSlice(sprite("widget/button_highlighted"), BUTTON_BORDER,
                        PRESSED_TINT))
                .paint(Button.STYLE_DISABLED, Paint.nineSlice(sprite("widget/button_disabled"), BUTTON_DISABLED_BORDER))
                .textStyle(Button.STYLE_TEXT, text)
                .textStyle(Button.STYLE_TEXT_DISABLED, text.withColor(0xFFA0A0A0))
                .textStyle(Label.STYLE_TEXT, text)
                // Vanilla's checkbox sprites have no .mcmeta, so they stretch over the box
                .paint(CheckBox.STYLE_NORMAL, Paint.sprite(sprite("widget/checkbox")))
                .paint(CheckBox.STYLE_HOVERED, Paint.sprite(sprite("widget/checkbox_highlighted")))
                .paint(CheckBox.STYLE_CHECKED, Paint.sprite(sprite("widget/checkbox_selected")))
                .paint(CheckBox.STYLE_CHECKED_HOVERED, Paint.sprite(sprite("widget/checkbox_selected_highlighted")))
                // Vanilla has no mixed sprite; a square in the text color over the empty box
                .paint(CheckBox.STYLE_MARK_MIXED, Paint.solid(0xFFE0E0E0))
                .paint(CheckBox.STYLE_MARK_MIXED_DISABLED, Paint.solid(0xFFA0A0A0))
                .textStyle(CheckBox.STYLE_TEXT, text)
                .textStyle(CheckBox.STYLE_TEXT_DISABLED, text.withColor(0xFFA0A0A0))
                .paint(Slider.STYLE_TRACK, Paint.nineSlice(sprite("widget/slider"), SLIDER_BORDER))
                .paint(Slider.STYLE_TRACK_FOCUSED, Paint.nineSlice(sprite("widget/slider_highlighted"), SLIDER_BORDER))
                .paint(Slider.STYLE_THUMB, Paint.nineSlice(sprite("widget/slider_handle"), SLIDER_HANDLE_BORDER))
                .paint(Slider.STYLE_THUMB_HOVERED, Paint.nineSlice(sprite("widget/slider_handle_highlighted"), SLIDER_HANDLE_BORDER))
                .textStyle(Slider.STYLE_TEXT, text)
                .textStyle(Slider.STYLE_TEXT_DISABLED, text.withColor(0xFFA0A0A0))
                // Vanilla's EditBox has no disabled sprite; its uneditable text is 0x707070
                .paint(TextBox.STYLE_NORMAL, Paint.nineSlice(sprite("widget/text_field"), FIELD_BORDER))
                .paint(TextBox.STYLE_FOCUSED, Paint.nineSlice(sprite("widget/text_field_highlighted"), FIELD_BORDER))
                .paint(TextBox.STYLE_DISABLED, Paint.nineSlice(sprite("widget/text_field"), FIELD_BORDER))
                .paint(TextBox.STYLE_SELECTION, Paint.solid(0x800000FF))
                .paint(TextBox.STYLE_CARET, Paint.solid(0xFFD0D0D0))
                .textStyle(TextBox.STYLE_TEXT, text.withColor(0xFFE0E0E0))
                .textStyle(TextBox.STYLE_TEXT_DISABLED, text.withColor(0xFF707070))
                .textStyle(TextBox.STYLE_TEXT_READ_ONLY, text.withColor(0xFFA0A0A0))
                .paint(ScrollPanel.STYLE_THUMB, Paint.nineSlice(sprite("widget/scroller"), SCROLLER_BORDER))
                .paint(ScrollPanel.STYLE_TRACK, Paint.nineSlice(sprite("widget/scroller_background"), SCROLLER_BORDER))
                .paint(ListView.STYLE_ROW_HOVERED, Paint.solid(0x30FFFFFF))
                .paint(ListView.STYLE_ROW_SELECTED, Paint.solid(0x60FFFFFF))
                .textStyle(ListView.STYLE_PLACEHOLDER, text.withColor(0xFFA0A0A0))
                .paint(ComboBox.STYLE_NORMAL, Paint.nineSlice(sprite("widget/button"), BUTTON_BORDER))
                .paint(ComboBox.STYLE_HOVERED, Paint.nineSlice(sprite("widget/button_highlighted"), BUTTON_BORDER))
                .paint(ComboBox.STYLE_DISABLED, Paint.nineSlice(sprite("widget/button_disabled"), BUTTON_DISABLED_BORDER))
                .paint(ComboBox.STYLE_POPUP, Paint.solid(0xF0101010))
                .textStyle(ComboBox.STYLE_TEXT, text)
                .textStyle(ComboBox.STYLE_TEXT_DISABLED, text.withColor(0xFFA0A0A0))
                .paint(Window.STYLE_BACKGROUND, Paint.nineSlice(sprite("popup/background"), POPUP_BORDER))
                .textStyle(Window.STYLE_TITLE_TEXT, text)
                .textStyle(Window.STYLE_CLOSE, text.withColor(0xFFA0A0A0))
                .textStyle(Window.STYLE_CLOSE_HOVERED, text)
                .paint(TabBar.STYLE_NORMAL, Paint.nineSlice(sprite("widget/tab"), TAB_BORDER))
                .paint(TabBar.STYLE_HOVERED, Paint.nineSlice(sprite("widget/tab_highlighted"), TAB_BORDER))
                .paint(TabBar.STYLE_SELECTED, Paint.nineSlice(sprite("widget/tab_selected"), TAB_BORDER))
                .textStyle(TabBar.STYLE_TEXT, text.withColor(0xFFA0A0A0))
                .textStyle(TabBar.STYLE_TEXT_SELECTED, text)
                .textStyle(TabBar.STYLE_TEXT_DISABLED, text.withColor(0xFF606060))
                // widget/tab is empty in its top 4 pixels and edged in the next 2, so its inside starts 6 down;
                // both paddings are 10 tall, so tabs are a line plus 10, nearer vanilla's 24
                .insets(TabBar.STYLE_PADDING, new Insets(8f, 7f, 8f, 3f))
                .insets(TabBar.STYLE_PADDING_SELECTED, new Insets(8f, 5f, 8f, 5f))
                .paint(ProgressBar.STYLE_TRACK, Paint.solid(0xFF000000))
                .paint(ProgressBar.STYLE_FILL, Paint.solid(0xFF008200))
                .textStyle(ProgressBar.STYLE_TEXT, text)
                .paint(Separator.STYLE_LINE, Paint.solid(0xB9FFFFFF))
                .paint(RadioButton.STYLE_NORMAL, Paint.sprite(sprite("widget/checkbox")))
                .paint(RadioButton.STYLE_HOVERED, Paint.sprite(sprite("widget/checkbox_highlighted")))
                .paint(RadioButton.STYLE_SELECTED, Paint.sprite(sprite("widget/checkbox_selected")))
                .paint(RadioButton.STYLE_SELECTED_HOVERED, Paint.sprite(sprite("widget/checkbox_selected_highlighted")))
                .textStyle(RadioButton.STYLE_TEXT, text)
                .textStyle(RadioButton.STYLE_TEXT_DISABLED, text.withColor(0xFFA0A0A0))
                .paint(ToggleSwitch.STYLE_TRACK, Paint.nineSlice(sprite("widget/slider"), SLIDER_BORDER))
                .paint(ToggleSwitch.STYLE_TRACK_ON, Paint.nineSlice(sprite("widget/slider_highlighted"), SLIDER_BORDER))
                .paint(ToggleSwitch.STYLE_KNOB, Paint.nineSlice(sprite("widget/slider_handle"), SLIDER_HANDLE_BORDER))
                .paint(ToggleSwitch.STYLE_KNOB_HOVERED, Paint.nineSlice(sprite("widget/slider_handle_highlighted"), SLIDER_HANDLE_BORDER))
                .textStyle(ToggleSwitch.STYLE_TEXT, text)
                .textStyle(ToggleSwitch.STYLE_TEXT_DISABLED, text.withColor(0xFFA0A0A0))
                .paint(TableView.STYLE_HEADER, Paint.solid(0x80000000))
                .paint(TableView.STYLE_HEADER_HOVERED, Paint.solid(0x30FFFFFF))
                .textStyle(TableView.STYLE_HEADER_TEXT, text)
                .paint(TableView.STYLE_GRID, Paint.solid(0x40FFFFFF))
                .paint(Menu.STYLE_BACKGROUND, Paint.solid(0xF0101010))
                .paint(Menu.STYLE_ITEM_HOVERED, Paint.solid(0x60FFFFFF))
                .textStyle(Menu.STYLE_TEXT, text)
                .textStyle(Menu.STYLE_TEXT_DISABLED, text.withColor(0xFFA0A0A0))
                .paint(Expander.STYLE_HEADER, Paint.nineSlice(sprite("widget/button"), BUTTON_BORDER))
                .paint(Expander.STYLE_HEADER_HOVERED, Paint.nineSlice(sprite("widget/button_highlighted"), BUTTON_BORDER))
                .textStyle(Expander.STYLE_TEXT, text)
                .textStyle(Expander.STYLE_TEXT_DISABLED, text.withColor(0xFFA0A0A0))
                .paint(GridView.STYLE_CELL, Paint.sprite(sprite("container/slot")))
                .paint(GridView.STYLE_CELL_HOVERED, Paint.solid(0x80FFFFFF))
                .textStyle(Button.STYLE_LINK_TEXT, text)
                .textStyle(Button.STYLE_LINK_TEXT_HOVERED, text.withColor(0xFFFFFF55))
                .textStyle(Button.STYLE_LINK_TEXT_DISABLED, text.withColor(0xFFA0A0A0))
                .paint(ScrollBar.STYLE_SLIM_TRACK, Paint.NONE)
                .paint(ScrollBar.STYLE_SLIM_THUMB, Paint.solid(0xFF3333AA))
                .paint(ScrollBar.STYLE_SLIM_THUMB_HOVERED, Paint.solid(0xFFCCCCCC))
                .paint(ScrollBar.STYLE_SLIM_THUMB_DISABLED, Paint.solid(0xFF555555))
                .paint(BusyIndicator.STYLE_DOT, Paint.solid(0xFF404040))
                .paint(BusyIndicator.STYLE_DOT_ACTIVE, Paint.solid(0xFF808080))
                .textStyle(BusyIndicator.STYLE_TEXT, text)
                .paint(TextBox.STYLE_SUGGESTIONS, Paint.solid(0xD0000000))
                .textStyle(TextBox.STYLE_SUGGESTION_TEXT, text.withColor(0xFFAAAAAA))
                .paint(ColorPicker.STYLE_FRAME, Paint.solid(0xFFA0A0A0))
                .paint(ColorPicker.STYLE_MARKER, Paint.solid(0xFFFFFFFF))
                .paint(ColorPicker.STYLE_MARKER_OUTLINE, Paint.solid(0xFF000000))
                .paint(ColorPicker.STYLE_DISABLED, Paint.solid(0xA0000000))
                .paint(Window.STYLE_GRIP, Paint.solid(0xFFA0A0A0))
                .paint(Window.STYLE_GRIP_HOVERED, Paint.solid(0xFFFFFFFF))
                .paint(Overlays.STYLE_BACKDROP, Paint.solid(0x60000000))
                // The advancement frame as a nine-slice; toast/advancement itself has no .mcmeta and would stretch
                .paint(Overlays.STYLE_TOAST, Paint.nineSlice(sprite("toast/now_playing"), TOAST_BORDER))
                .textStyle(Overlays.STYLE_TOAST_TEXT, TextStyle.DEFAULT.withColor(0xFFFFFFFF))
                .paint(MenuPanel.STYLE_SLOT, Paint.sprite(sprite("container/slot")))
                .paint(MenuPanel.STYLE_HIGHLIGHT_BACK, Paint.sprite(sprite("container/slot_highlight_back")))
                .paint(MenuPanel.STYLE_HIGHLIGHT_FRONT, Paint.sprite(sprite("container/slot_highlight_front")))
                .build();
    }

    private static SpriteId sprite(final String path) {
        return SpriteId.of("minecraft", path);
    }
}
