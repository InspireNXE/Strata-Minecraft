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
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.spi.ThemeDocument;
import org.inspirenxe.strata.spi.ThemeSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Themes from the client's resource packs. One source serves the whole game; {@link MinecraftPlatform} hands it
 * to every host.
 *
 * <ul>
 *     <li>{@link MinecraftThemes#VANILLA} is registered as a base theme. Mods can register their own with
 *     {@link #register}.</li>
 *     <li>Theme files are read from {@code assets/<namespace>/strata/themes/<path>.json} in every resource pack
 *     that has one, mods' own resources and a server's pack included, lowest priority first as vanilla stacks
 *     them. Files are read once per id and cached until resources reload.</li>
 *     <li>The source registers itself as a reload listener on the client resource manager the first time
 *     {@link #get} is called. Each reload drops the cache and changes {@link #version()}, so every open
 *     {@code StrataScreen} with a theme ref resolves it again.</li>
 *     <li>An id that is not a valid Minecraft identifier, or a file that cannot be read, gives no documents.</li>
 * </ul>
 */
public final class MinecraftThemeSource implements ThemeSource, ResourceManagerReloadListener {
    private static MinecraftThemeSource instance;

    private final Minecraft minecraft;
    private final Map<ThemeId, Theme> bases;
    private final Map<ThemeId, List<ThemeDocument>> cache;
    private final AtomicLong version;

    MinecraftThemeSource(final Minecraft minecraft) {
        this.minecraft = minecraft;
        this.bases = new HashMap<>();
        this.cache = new HashMap<>();
        this.version = new AtomicLong();
        this.bases.put(MinecraftThemes.VANILLA, MinecraftThemes.vanilla());
    }

    /**
     * Returns the game's theme source, creating it and registering its reload listener on first use.
     *
     * @param minecraft the game
     * @return the source
     */
    public static synchronized MinecraftThemeSource get(final Minecraft minecraft) {
        if (instance == null) {
            instance = new MinecraftThemeSource(Objects.requireNonNull(minecraft, "minecraft"));
            if (minecraft.getResourceManager() instanceof final ReloadableResourceManager manager) {
                manager.registerReloadListener(instance);
            }
        }
        return instance;
    }

    /**
     * Registers a theme built in code under an id, replacing any earlier one, and changes {@link #version()} so
     * open screens pick it up. Theme files for the id still apply on top of it.
     *
     * @param id the id theme refs and theme files use
     * @param theme the theme
     */
    public synchronized void register(final ThemeId id, final Theme theme) {
        this.bases.put(Objects.requireNonNull(id, "id"), Objects.requireNonNull(theme, "theme"));
        this.version.incrementAndGet();
    }

    @Override
    public synchronized Optional<Theme> base(final ThemeId id) {
        return Optional.ofNullable(this.bases.get(id));
    }

    @Override
    public synchronized List<ThemeDocument> documents(final ThemeId id) {
        return this.cache.computeIfAbsent(id, this::read);
    }

    @Override
    public long version() {
        return this.version.get();
    }

    @Override
    public synchronized void onResourceManagerReload(final ResourceManager resourceManager) {
        this.cache.clear();
        this.version.incrementAndGet();
    }

    @Override
    public String getName() {
        return "Strata themes";
    }

    private List<ThemeDocument> read(final ThemeId id) {
        final Identifier location = Identifier.tryBuild(id.namespace(), "strata/themes/" + id.path() + ".json");
        if (location == null) {
            return List.of();
        }
        final List<ThemeDocument> documents = new ArrayList<>();
        for (final Resource resource : this.minecraft.getResourceManager().getResourceStack(location)) {
            try {
                documents.add(new ThemeDocument(resource.sourcePackId() + "/" + location, resource.readAllAsString()));
            } catch (final IOException ignored) {
                // An unreadable file is left out, as the ThemeSource contract asks
            }
        }
        return List.copyOf(documents);
    }
}
