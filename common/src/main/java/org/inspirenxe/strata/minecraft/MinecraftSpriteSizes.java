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
import net.minecraft.server.packs.resources.Resource;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.spi.SpriteSizes;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * The sizes of the sprites a theme names, which images showing a sprite take until a size is set. 1.20.1 has no GUI
 * sprite atlas, so a sprite {@link VanillaSprites} maps to a region of a texture sheet is as big as the region, one
 * sheet pixel to a GUI unit, and a sprite it draws with fills has no size of its own. Any other sprite is as big as
 * its texture, {@code textures/gui/sprites/<path>.png}, one pixel to a GUI unit, read from the image's header. A
 * sprite with no texture is unknown. Each call reads the resources as they are, so sizes follow a resource reload
 * once the screen lays out again.
 */
final class MinecraftSpriteSizes implements SpriteSizes {
    private final Minecraft minecraft;

    /**
     * Creates the sizes of a client's GUI sprites.
     *
     * @param minecraft the client
     */
    MinecraftSpriteSizes(final Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public Optional<Size> size(final SpriteId sprite) {
        final VanillaSprites.Sprite kept = VanillaSprites.of(sprite).orElse(null);
        if (kept instanceof final VanillaSprites.Region region) {
            return Optional.of(new Size(region.width(), region.height()));
        }
        if (kept != null) {
            // Fills stretch to whatever size they are given
            return Optional.empty();
        }
        final Optional<Resource> resource = this.minecraft.getResourceManager()
                .getResource(VanillaSprites.texture(sprite));
        if (resource.isEmpty()) {
            return Optional.empty();
        }
        try (InputStream in = resource.get().open()) {
            return MinecraftCanvas.pngSize(in).map(size -> new Size(size[0], size[1]));
        } catch (final IOException unreadable) {
            return Optional.empty();
        }
    }
}
