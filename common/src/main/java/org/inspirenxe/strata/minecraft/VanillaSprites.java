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

import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.theme.SpriteId;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Where Minecraft 1.20.1 keeps what newer versions draw from their gui sprite atlas, so a theme names the same
 * {@link SpriteId}s on every version. 1.20.1 has no atlas: its widgets are regions of texture sheets in
 * {@code textures/gui}, which resource packs still replace, and its text fields and scroll bars are drawn with fills.
 *
 * <p>The table holds the sprites the vanilla theme and the adapter use:</p>
 * <ul>
 *     <li>{@code widget/button}, {@code widget/button_highlighted} and {@code widget/button_disabled}: rows of
 *     {@code widgets.png}, sliced 20 by 4 as vanilla's buttons are.</li>
 *     <li>{@code widget/slider}, {@code widget/slider_highlighted}, {@code widget/slider_handle} and
 *     {@code widget/slider_handle_highlighted}: rows of {@code slider.png}, sliced 20 by 4.</li>
 *     <li>{@code widget/checkbox}, {@code widget/checkbox_highlighted}, {@code widget/checkbox_selected} and
 *     {@code widget/checkbox_selected_highlighted}: the cells of {@code checkbox.png}.</li>
 *     <li>{@code widget/tab}, {@code widget/tab_highlighted}, {@code widget/tab_selected} and
 *     {@code widget/tab_selected_highlighted}: rows of {@code tab_button.png}, sliced 2 on the sides and top.</li>
 *     <li>{@code container/slot}: the first slot of the player's inventory in {@code container/inventory.png}.</li>
 *     <li>{@code toast/system}: the system toast in {@code toasts.png}, which a nine-slice paint cuts at its own
 *     border.</li>
 *     <li>{@code widget/text_field} and {@code widget/text_field_highlighted}: black with a one-pixel gray or white
 *     edge, as vanilla's edit box fills them.</li>
 *     <li>{@code widget/scroller} and {@code widget/scroller_background}: the gray thumb with its lighter top and
 *     left, and the black track, as vanilla's lists fill them.</li>
 * </ul>
 *
 * <p>Any other sprite is the plain texture {@code assets/<namespace>/textures/gui/sprites/<path>.png}, or vanilla's
 * missing texture when Minecraft cannot name it.</p>
 */
final class VanillaSprites {
    private static final ResourceLocation WIDGETS = vanillaTexture("textures/gui/widgets.png");
    private static final ResourceLocation SLIDER = vanillaTexture("textures/gui/slider.png");
    private static final ResourceLocation CHECKBOX = vanillaTexture("textures/gui/checkbox.png");
    private static final ResourceLocation TABS = vanillaTexture("textures/gui/tab_button.png");
    private static final ResourceLocation INVENTORY = vanillaTexture("textures/gui/container/inventory.png");
    private static final ResourceLocation TOASTS = vanillaTexture("textures/gui/toasts.png");
    private static final Insets BUTTON_SLICES = new Insets(20f, 4f, 20f, 4f);
    private static final Insets TAB_SLICES = new Insets(2f, 2f, 2f, 0f);

    private static final Map<SpriteId, Sprite> TABLE = Map.ofEntries(
            Map.entry(vanilla("widget/button"), new Region(WIDGETS, 256, 256, 0, 66, 200, 20, BUTTON_SLICES)),
            Map.entry(vanilla("widget/button_highlighted"), new Region(WIDGETS, 256, 256, 0, 86, 200, 20, BUTTON_SLICES)),
            Map.entry(vanilla("widget/button_disabled"), new Region(WIDGETS, 256, 256, 0, 46, 200, 20, BUTTON_SLICES)),
            Map.entry(vanilla("widget/slider"), new Region(SLIDER, 256, 256, 0, 0, 200, 20, BUTTON_SLICES)),
            Map.entry(vanilla("widget/slider_highlighted"), new Region(SLIDER, 256, 256, 0, 20, 200, 20, BUTTON_SLICES)),
            Map.entry(vanilla("widget/slider_handle"), new Region(SLIDER, 256, 256, 0, 40, 200, 20, BUTTON_SLICES)),
            Map.entry(vanilla("widget/slider_handle_highlighted"), new Region(SLIDER, 256, 256, 0, 60, 200, 20, BUTTON_SLICES)),
            Map.entry(vanilla("widget/checkbox"), new Region(CHECKBOX, 64, 64, 0, 0, 20, 20, null)),
            Map.entry(vanilla("widget/checkbox_highlighted"), new Region(CHECKBOX, 64, 64, 20, 0, 20, 20, null)),
            Map.entry(vanilla("widget/checkbox_selected"), new Region(CHECKBOX, 64, 64, 0, 20, 20, 20, null)),
            Map.entry(vanilla("widget/checkbox_selected_highlighted"), new Region(CHECKBOX, 64, 64, 20, 20, 20, 20, null)),
            Map.entry(vanilla("widget/tab"), new Region(TABS, 256, 256, 0, 48, 130, 24, TAB_SLICES)),
            Map.entry(vanilla("widget/tab_highlighted"), new Region(TABS, 256, 256, 0, 72, 130, 24, TAB_SLICES)),
            Map.entry(vanilla("widget/tab_selected"), new Region(TABS, 256, 256, 0, 0, 130, 24, TAB_SLICES)),
            Map.entry(vanilla("widget/tab_selected_highlighted"), new Region(TABS, 256, 256, 0, 24, 130, 24, TAB_SLICES)),
            Map.entry(vanilla("container/slot"), new Region(INVENTORY, 256, 256, 7, 83, 18, 18, null)),
            Map.entry(vanilla("toast/system"), new Region(TOASTS, 256, 256, 0, 64, 160, 32, null)),
            Map.entry(vanilla("widget/text_field"), new Fills(List.of(
                    new Fill(Insets.NONE, 0xFFA0A0A0), new Fill(Insets.of(1f), 0xFF000000)))),
            Map.entry(vanilla("widget/text_field_highlighted"), new Fills(List.of(
                    new Fill(Insets.NONE, 0xFFFFFFFF), new Fill(Insets.of(1f), 0xFF000000)))),
            Map.entry(vanilla("widget/scroller"), new Fills(List.of(
                    new Fill(Insets.NONE, 0xFF808080), new Fill(new Insets(0f, 0f, 1f, 1f), 0xFFC0C0C0)))),
            Map.entry(vanilla("widget/scroller_background"), new Fills(List.of(new Fill(Insets.NONE, 0xFF000000))))
    );

    private VanillaSprites() {
    }

    /**
     * Returns where 1.20.1 keeps a sprite.
     *
     * @param sprite the sprite
     * @return its region or fills, or empty for a plain texture
     */
    static Optional<Sprite> of(final SpriteId sprite) {
        return Optional.ofNullable(TABLE.get(sprite));
    }

    /**
     * Returns the plain texture a sprite outside the table is read from.
     *
     * @param sprite the sprite
     * @return {@code <namespace>:textures/gui/sprites/<path>.png}
     */
    static ResourceLocation texture(final SpriteId sprite) {
        final ResourceLocation texture = ResourceLocation.tryBuild(sprite.namespace(),
                "textures/gui/sprites/" + sprite.path() + ".png");
        // A sprite id Minecraft cannot name draws vanilla's missing-texture pattern
        return texture != null ? texture : MissingTextureAtlasSprite.getLocation();
    }

    private static ResourceLocation vanillaTexture(final String path) {
        // tryBuild, since Forge deprecates the constructors; these paths are always valid
        return Objects.requireNonNull(ResourceLocation.tryBuild("minecraft", path));
    }

    private static SpriteId vanilla(final String path) {
        return SpriteId.of("minecraft", path);
    }

    /**
     * Where a sprite is kept.
     */
    sealed interface Sprite permits Region, Fills {
    }

    /**
     * A region of a texture sheet, in the sheet's pixels.
     *
     * @param texture the sheet
     * @param textureWidth the sheet's width
     * @param textureHeight the sheet's height
     * @param u the region's left edge
     * @param v the region's top edge
     * @param width the region's width
     * @param height the region's height
     * @param slices the borders vanilla slices the region at, with the edges and center tiled, or {@code null} to
     *               stretch it, or cut it at a nine-slice paint's own border
     */
    record Region(ResourceLocation texture, int textureWidth, int textureHeight, int u, int v, int width, int height,
                  Insets slices) implements Sprite {
        Region {
            Objects.requireNonNull(texture, "texture");
        }
    }

    /**
     * Rectangles of color, drawn in order over the sprite's area.
     *
     * @param fills the rectangles
     */
    record Fills(List<Fill> fills) implements Sprite {
        Fills {
            fills = List.copyOf(fills);
        }
    }

    /**
     * One rectangle of a {@link Fills}.
     *
     * @param inset how far inside the area the rectangle sits, on each side
     * @param argb its color
     */
    record Fill(Insets inset, int argb) {
        Fill {
            Objects.requireNonNull(inset, "inset");
        }
    }
}
