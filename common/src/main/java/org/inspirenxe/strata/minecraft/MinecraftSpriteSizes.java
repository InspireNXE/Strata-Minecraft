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
import net.minecraft.client.gui.GuiSpriteManager;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.ResourceLocation;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.spi.SpriteSizes;

import java.util.Optional;

/**
 * The sizes of the sprites in the {@code minecraft:gui} atlas, which images showing a sprite take until a size is set.
 * A nine-sliced or tiled sprite is as big as its {@code .png.mcmeta} says, so a resource pack with larger textures does
 * not change it; any other sprite is as big as its texture, one pixel to a GUI unit. A sprite the atlas does not have
 * is unknown. Each call reads the atlas as it is, so sizes follow a resource reload once the screen lays out again.
 */
final class MinecraftSpriteSizes implements SpriteSizes {
    private final Minecraft minecraft;

    /**
     * Creates the sizes of a client's GUI atlas.
     *
     * @param minecraft the client
     */
    MinecraftSpriteSizes(final Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public Optional<Size> size(final SpriteId sprite) {
        final GuiSpriteManager sprites = this.minecraft.getGuiSprites();
        final TextureAtlasSprite found = sprites.getSprite(ResourceLocation.fromNamespaceAndPath(sprite.namespace(),
                sprite.path()));
        if (found.contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
            return Optional.empty();
        }
        final GuiSpriteScaling scaling = sprites.getSpriteScaling(found);
        return Optional.of(switch (scaling) {
            case final GuiSpriteScaling.NineSlice nineSlice -> new Size(nineSlice.width(), nineSlice.height());
            case final GuiSpriteScaling.Tile tile -> new Size(tile.width(), tile.height());
            default -> new Size(found.contents().width(), found.contents().height());
        });
    }
}
