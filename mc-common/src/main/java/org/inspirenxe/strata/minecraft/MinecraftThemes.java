package org.inspirenxe.strata.minecraft;

import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.overlay.Overlays;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.api.widget.CheckBox;
import org.inspirenxe.strata.api.widget.ComboBox;
import org.inspirenxe.strata.api.widget.Label;
import org.inspirenxe.strata.api.widget.ListView;
import org.inspirenxe.strata.api.widget.ProgressBar;
import org.inspirenxe.strata.api.widget.Separator;
import org.inspirenxe.strata.api.widget.Slider;
import org.inspirenxe.strata.api.widget.TabBar;
import org.inspirenxe.strata.api.widget.TextBox;
import org.inspirenxe.strata.api.widget.Window;

/**
 * Themes built from vanilla Minecraft's own GUI sprites, so Strata widgets look like vanilla ones.
 */
public final class MinecraftThemes {
    // Borders as declared in each sprite's .png.mcmeta in 26.3; Minecraft reads the .mcmeta, other engines these
    private static final Insets BUTTON_BORDER = Insets.of(3f);
    private static final Insets BUTTON_DISABLED_BORDER = Insets.of(1f);
    private static final Insets FIELD_BORDER = Insets.of(1f);
    private static final Insets SLIDER_BORDER = Insets.of(1f);
    private static final Insets SLIDER_HANDLE_BORDER = new Insets(2f, 2f, 2f, 3f);
    private static final Insets SCROLLER_BORDER = Insets.of(1f);
    private static final Insets TAB_BORDER = new Insets(2f, 2f, 2f, 0f);
    private static final Insets POPUP_BORDER = Insets.of(6f);

    private MinecraftThemes() {
    }

    /**
     * Returns a theme matching vanilla's widgets. It defines every key of every built-in widget:
     * <ul>
     *     <li>buttons use {@code widget/button}, {@code widget/button_highlighted} (hovered, focused and pressed) and
     *     {@code widget/button_disabled}, with white shadowed captions, gray when disabled;</li>
     *     <li>text boxes use {@code widget/text_field} and {@code widget/text_field_highlighted}, with vanilla's
     *     {@code 0xE0E0E0} text and {@code 0x707070} when disabled;</li>
     *     <li>check boxes use {@code widget/checkbox}, {@code widget/checkbox_highlighted} (hovered and focused),
     *     {@code widget/checkbox_selected} and {@code widget/checkbox_selected_highlighted}, which have the check mark
     *     drawn in, so no mark paint is set. Vanilla has no disabled check box sprites, so a disabled check box shows
     *     the normal ones with a gray caption;</li>
     *     <li>sliders use {@code widget/slider} for the track, {@code widget/slider_highlighted} while focused,
     *     {@code widget/slider_handle} for the thumb and {@code widget/slider_handle_highlighted} while hovered,
     *     focused or pressed, with a white shadowed label, gray when disabled;</li>
     *     <li>labels are white with a shadow;</li>
     *     <li>scroll bars use {@code widget/scroller} for the thumb and {@code widget/scroller_background} for the
     *     track;</li>
     *     <li>tab bars use {@code widget/tab}, {@code widget/tab_highlighted} and {@code widget/tab_selected};</li>
     *     <li>windows use {@code popup/background}, the frame of vanilla's popup screens, with no title bar paint;</li>
     *     <li>combo boxes use the button sprites, over a near-black list;</li>
     *     <li>list rows, progress bars, separators and the dialog backdrop use plain colors, since vanilla has no
     *     sprites for them. Tooltips use Strata's fallback, vanilla's classic dark tooltip colors.</li>
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
                // Vanilla's check box sprites have no .mcmeta, so they stretch over the box
                .paint(CheckBox.STYLE_NORMAL, Paint.sprite(sprite("widget/checkbox")))
                .paint(CheckBox.STYLE_HOVERED, Paint.sprite(sprite("widget/checkbox_highlighted")))
                .paint(CheckBox.STYLE_CHECKED, Paint.sprite(sprite("widget/checkbox_selected")))
                .paint(CheckBox.STYLE_CHECKED_HOVERED, Paint.sprite(sprite("widget/checkbox_selected_highlighted")))
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
                .paint(ProgressBar.STYLE_TRACK, Paint.solid(0xFF000000))
                .paint(ProgressBar.STYLE_FILL, Paint.solid(0xFF008200))
                .textStyle(ProgressBar.STYLE_TEXT, text)
                .paint(Separator.STYLE_LINE, Paint.solid(0xB9FFFFFF))
                .paint(Overlays.STYLE_BACKDROP, Paint.solid(0x60000000))
                .build();
    }

    private static SpriteId sprite(final String path) {
        return SpriteId.of("minecraft", path);
    }
}
