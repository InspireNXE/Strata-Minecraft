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
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.inspirenxe.strata.api.text.FontId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Maps a Strata {@link FontId} to the game's font of the same id, the one a resource pack defines in
 * {@code assets/<namespace>/font/<path>.json}. Vanilla draws an id it has no font for as boxes, so an id without a
 * definition maps to nothing, and the text draws in the default font. Whether each id has one is read once and kept
 * until resources reload; each reload changes {@link #version()}, so screens lay out again with the fonts the new packs
 * define.
 */
final class MinecraftFonts implements ResourceManagerReloadListener {
    private static MinecraftFonts instance;

    private final Map<FontId, Optional<FontDescription>> known = new HashMap<>();
    private final AtomicLong version = new AtomicLong();

    private MinecraftFonts() {
    }

    /**
     * Returns the game's font of an id.
     *
     * @param id the id
     * @return the font, or empty if no resource pack defines one, or the game has not started
     */
    static Optional<FontDescription> description(final FontId id) {
        final MinecraftFonts fonts = MinecraftFonts.get();
        return fonts == null ? Optional.empty() : fonts.find(id);
    }

    /**
     * Returns a number that changes on every resource reload.
     *
     * @return the version
     */
    static long version() {
        final MinecraftFonts fonts = MinecraftFonts.get();
        return fonts == null ? 0L : fonts.version.get();
    }

    private static synchronized MinecraftFonts get() {
        if (instance == null) {
            final Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null) {
                return null;
            }
            instance = new MinecraftFonts();
            if (minecraft.getResourceManager() instanceof final ReloadableResourceManager manager) {
                manager.registerReloadListener(instance);
            }
        }
        return instance;
    }

    private synchronized Optional<FontDescription> find(final FontId id) {
        return this.known.computeIfAbsent(id, key -> {
            final Identifier definition = Identifier.fromNamespaceAndPath(key.namespace(), "font/" + key.path() + ".json");
            return Minecraft.getInstance().getResourceManager().getResource(definition).isPresent()
                    ? Optional.of(new FontDescription.Resource(Identifier.fromNamespaceAndPath(key.namespace(), key.path())))
                    : Optional.empty();
        });
    }

    @Override
    public synchronized void onResourceManagerReload(final ResourceManager resourceManager) {
        this.known.clear();
        this.version.incrementAndGet();
    }

    @Override
    public String getName() {
        return "Strata fonts";
    }
}
