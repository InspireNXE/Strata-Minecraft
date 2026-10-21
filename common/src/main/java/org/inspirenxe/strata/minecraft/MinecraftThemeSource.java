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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.spi.ThemeDocument;
import org.inspirenxe.strata.spi.ThemeSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Themes from the client's resource packs. One source serves the whole game; {@link MinecraftPlatform} hands it
 * to every host.
 *
 * <ul>
 *     <li>{@link #VANILLA} is vanilla's look as a theme. Its base is a theme file Strata-Minecraft ships on the class
 *     path at {@code strata/minecraft/themes/vanilla.json}, outside {@code assets/}, so the resource manager never
 *     reads it as a pack file. {@link #bases} returns it, with the layer {@code vanilla}, the vanilla pack's id. The
 *     file names {@code strata:flat} as its parent and sets every key the flat theme sets, so no key of a built-in
 *     control comes from the flat theme.</li>
 *     <li>Mods can register themes built in code with {@link #register}. A theme defined from a parent and
 *     overrides with {@link org.inspirenxe.strata.api.Strata#themes()} works on every engine.</li>
 *     <li>Theme files are read from {@code assets/<namespace>/strata/themes/<path>.json} in every resource pack that
 *     has one. {@link #documents} returns the player's packs first, a server's or world's pack among them, in the
 *     order the game stacks them, and then the packs of mods' own resources, in the order the game stacks those, so
 *     a mod's theme files win over the player's packs. Each document's layer is its pack's id. Files are read once
 *     per id and cached until resources reload.</li>
 *     <li>The order changes only which theme file wins a key. The game's pack stack is left as it is, so sprites
 *     and every other asset keep the game's order.</li>
 *     <li>The source registers itself as a reload listener on the client resource manager the first time
 *     {@link #get} is called. Each reload drops the cache and changes {@link #version()}, so every open
 *     {@code StrataScreen} with a theme ref resolves it again.</li>
 *     <li>An id that is not a valid Minecraft identifier, or a file that cannot be read, gives no documents.</li>
 * </ul>
 *
 * <p>Minecraft 1.20.1 has no known pack info, so a pack comes from a mod when it is built in and is not one of
 * vanilla's own: {@code vanilla}, {@code programmer_art} and {@code high_contrast}. Packs from the resource pack
 * folder, a server or a world are not built in. Which of Forge's and Fabric API's mod packs report themselves as
 * built in has not been checked in a running game.</p>
 */
public final class MinecraftThemeSource implements ThemeSource, ResourceManagerReloadListener {
    /**
     * The id of vanilla's look as a theme. Pass {@code ThemeRef.of(MinecraftThemeSource.VANILLA)} to a
     * {@link StrataScreen} to look like the game and follow resource packs, which restyle it with
     * {@code assets/minecraft/strata/themes/vanilla.json}.
     */
    public static final ThemeId VANILLA = ThemeId.of("minecraft", "vanilla");

    /**
     * The layer the keys of the vanilla theme's base file report: the vanilla pack's id.
     */
    public static final String VANILLA_LAYER = "vanilla";

    /**
     * Where the vanilla theme's base file is on the class path.
     */
    static final String VANILLA_FILE = "/strata/minecraft/themes/vanilla.json";

    private static MinecraftThemeSource instance;

    private final Minecraft minecraft;
    private final List<ThemeDocument> vanilla;
    private final Map<ThemeId, Theme> bases;
    private final Map<ThemeId, List<ThemeDocument>> cache;
    private final AtomicLong version;

    MinecraftThemeSource(final Minecraft minecraft) {
        this.minecraft = minecraft;
        this.vanilla = List.of(MinecraftThemeSource.vanillaFile());
        this.bases = new HashMap<>();
        this.cache = new HashMap<>();
        this.version = new AtomicLong();
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
     * @throws IllegalArgumentException if the id is {@link #VANILLA}, whose base is a file; change its keys with
     *     {@code Strata.themes().override} instead
     */
    public synchronized void register(final ThemeId id, final Theme theme) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(theme, "theme");
        if (id.equals(VANILLA)) {
            throw new IllegalArgumentException(VANILLA + " has a base file; override its keys with "
                    + "Strata.themes().override instead");
        }
        this.bases.put(id, theme);
        this.version.incrementAndGet();
    }

    @Override
    public synchronized Optional<Theme> base(final ThemeId id) {
        return Optional.ofNullable(this.bases.get(id));
    }

    @Override
    public List<ThemeDocument> bases(final ThemeId id) {
        return VANILLA.equals(id) ? this.vanilla : List.of();
    }

    @Override
    public synchronized List<ThemeDocument> documents(final ThemeId id) {
        return this.cache.computeIfAbsent(id, this::read);
    }

    /**
     * Returns {@link #VANILLA}, so screens opened without a theme and {@code ThemeRef.platform()} look like the
     * game.
     *
     * @return {@link #VANILLA}
     */
    @Override
    public ThemeId defaultTheme() {
        return VANILLA;
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

    /**
     * The ids of vanilla's built-in resource packs in 1.20.1.
     */
    private static final Set<String> VANILLA_PACKS = Set.of("vanilla", "programmer_art", "high_contrast");

    /**
     * Returns whether a resource comes from a mod's own resources rather than from the player, a server or a world,
     * or from vanilla. Minecraft 1.20.1 has no known pack info, so this goes by the pack: mods' packs are built in,
     * and the only built-in packs that are not mods' are vanilla's own.
     *
     * @param resource the resource
     * @return true if its pack is built in and not one of vanilla's
     */
    static boolean fromMod(final Resource resource) {
        return resource.isBuiltin() && !VANILLA_PACKS.contains(resource.sourcePackId());
    }

    private List<ThemeDocument> read(final ThemeId id) {
        final ResourceLocation location = ResourceLocation.tryBuild(id.namespace(), "strata/themes/" + id.path() + ".json");
        if (location == null) {
            return List.of();
        }
        final List<ThemeDocument> players = new ArrayList<>();
        final List<ThemeDocument> mods = new ArrayList<>();
        for (final Resource resource : this.minecraft.getResourceManager().getResourceStack(location)) {
            try {
                final String pack = resource.sourcePackId();
                final String text;
                try (InputStream in = resource.open()) {
                    text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                final ThemeDocument document = new ThemeDocument(pack + "/" + location, text, pack);
                (MinecraftThemeSource.fromMod(resource) ? mods : players).add(document);
            } catch (final IOException ignored) {
                // An unreadable file is left out, as the ThemeSource contract asks
            }
        }
        // Mods' files go above the player's packs, whatever the game's order; each group keeps the game's order
        final List<ThemeDocument> documents = new ArrayList<>(players);
        documents.addAll(mods);
        return List.copyOf(documents);
    }

    /**
     * Reads the vanilla theme's base file from Strata-Minecraft's own jar.
     *
     * @throws IllegalStateException if the file is missing, which means the jar was built wrong
     */
    private static ThemeDocument vanillaFile() {
        try (InputStream in = MinecraftThemeSource.class.getResourceAsStream(VANILLA_FILE)) {
            if (in == null) {
                throw new IllegalStateException("Strata-Minecraft's vanilla theme is missing from the class path: "
                        + VANILLA_FILE);
            }
            return new ThemeDocument(VANILLA_FILE.substring(1), new String(in.readAllBytes(), StandardCharsets.UTF_8),
                    VANILLA_LAYER);
        } catch (final IOException e) {
            throw new UncheckedIOException("Cannot read Strata-Minecraft's vanilla theme", e);
        }
    }
}
