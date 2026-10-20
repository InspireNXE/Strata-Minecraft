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
import net.minecraft.server.packs.resources.ResourceManager;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.button.RadioButton;
import org.inspirenxe.strata.api.control.button.ToggleSwitch;
import org.inspirenxe.strata.api.control.collection.GridView;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.control.display.BusyIndicator;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.theme.ThemeRegistryImpl;
import org.inspirenxe.strata.core.theme.ThemeResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The vanilla theme resolved from its base file alone, with no resource pack and nothing added in code.
 */
class VanillaThemeTest {
    private Theme vanilla;

    @BeforeEach
    void setUp() {
        final Minecraft minecraft = mock(Minecraft.class);
        final ResourceManager resources = mock(ResourceManager.class);
        when(minecraft.getResourceManager()).thenReturn(resources);
        when(resources.getResourceStack(any())).thenReturn(List.of());
        this.vanilla = new ThemeResolver(new MinecraftThemeSource(minecraft), new ThemeRegistryImpl())
                .resolve(ThemeRef.of(MinecraftThemeSource.VANILLA));
    }

    @Test
    void everyKeyOfEveryBuiltInControlComesFromTheVanillaFile() {
        // The flat theme sets every key of every built-in control, so none of them may be left to it
        final Set<String> keys = new LinkedHashSet<>(Themes.flat().paints().keySet());
        keys.addAll(Themes.flat().textStyles().keySet());
        keys.addAll(Themes.flat().insets().keySet());
        final TreeMap<String, String> notVanilla = new TreeMap<>();
        for (final String key : keys) {
            final String layer = this.vanilla.layer(key).orElse("none");
            if (!MinecraftThemeSource.VANILLA_LAYER.equals(layer)) {
                notVanilla.put(key, layer);
            }
        }
        assertEquals(new TreeMap<String, String>(), notVanilla);
        assertEquals("minecraft:vanilla", this.vanilla.name());
    }

    @Test
    void vanillaDefinesEveryRequiredKey() {
        final List<String> required = List.of(Button.STYLE_NORMAL, Button.STYLE_HOVERED, Button.STYLE_PRESSED,
                Button.STYLE_DISABLED, Label.STYLE_TEXT, CheckBox.STYLE_NORMAL, CheckBox.STYLE_HOVERED,
                CheckBox.STYLE_CHECKED, CheckBox.STYLE_CHECKED_HOVERED, Slider.STYLE_TRACK, Slider.STYLE_THUMB,
                Slider.STYLE_THUMB_HOVERED, TextBox.STYLE_NORMAL, TextBox.STYLE_FOCUSED, TextBox.STYLE_DISABLED,
                RadioButton.STYLE_NORMAL, RadioButton.STYLE_HOVERED, RadioButton.STYLE_SELECTED,
                RadioButton.STYLE_SELECTED_HOVERED, ToggleSwitch.STYLE_TRACK, ToggleSwitch.STYLE_TRACK_ON,
                ToggleSwitch.STYLE_KNOB, TableView.STYLE_HEADER, Menu.STYLE_BACKGROUND, Menu.STYLE_ITEM_HOVERED,
                GridView.STYLE_CELL, BusyIndicator.STYLE_DOT, BusyIndicator.STYLE_DOT_ACTIVE);
        assertEquals(List.of(), List.copyOf(this.vanilla.missing(required)));
    }

    @Test
    void vanillaDrawsSlotsWithVanillasSprites() {
        final List<String> slots = List.of(MenuPanel.STYLE_SLOT, MenuPanel.STYLE_HIGHLIGHT_BACK,
                MenuPanel.STYLE_HIGHLIGHT_FRONT);
        assertEquals(List.of(), List.copyOf(this.vanilla.missing(slots)));
        assertEquals(Paint.sprite(SpriteId.of("minecraft", "container/slot")),
                this.vanilla.paintOr(MenuPanel.STYLE_SLOT, null));
    }

    @Test
    void thePressedButtonIsTheHighlightedOneDarkened() {
        assertEquals(new Paint.NineSlice(SpriteId.of("minecraft", "widget/button_highlighted"), Insets.of(3f),
                0xFFB0B0B0), this.vanilla.paint(Button.STYLE_PRESSED).orElseThrow());
        assertEquals(new Insets(8f, 6f, 8f, 5f), this.vanilla.insets(Button.STYLE_PADDING).orElseThrow());
    }
}
