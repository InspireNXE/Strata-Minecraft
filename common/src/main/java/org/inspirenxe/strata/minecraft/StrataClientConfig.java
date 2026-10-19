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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * The player's settings for Strata, from {@code config/strata-client.json} in the game directory. The same file is
 * read on both loaders:
 *
 * <pre>{@code
 * {
 *     "forceVanillaSlots": false,
 *     "forceVanillaSlotsFor": ["examplemod:grinder"]
 * }
 * }</pre>
 *
 * <p>A missing file is written with the defaults, so players can find it. A file that cannot be read or parsed,
 * and entries of the wrong type, are logged and count as the defaults. Menu type ids that are not valid identifiers
 * are skipped.</p>
 *
 * @param forceVanillaSlots whether every container screen draws its slots through vanilla
 * @param forceVanillaSlotsFor the menu types whose screens draw their slots through vanilla
 */
record StrataClientConfig(boolean forceVanillaSlots, Set<Identifier> forceVanillaSlotsFor) {
    /**
     * The settings when the player has changed nothing.
     */
    static final StrataClientConfig DEFAULTS = new StrataClientConfig(false, Set.of());

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DEFAULT_FILE = """
            {
                "forceVanillaSlots": false,
                "forceVanillaSlotsFor": []
            }
            """;

    StrataClientConfig {
        forceVanillaSlotsFor = Set.copyOf(forceVanillaSlotsFor);
    }

    /**
     * Returns the file's path in a game directory.
     *
     * @param gameDirectory the game directory
     * @return {@code config/strata-client.json} inside it
     */
    static Path file(final Path gameDirectory) {
        return gameDirectory.resolve("config").resolve("strata-client.json");
    }

    /**
     * Reads the settings, writing the defaults first when the file does not exist.
     *
     * @param file the file
     * @return the settings
     */
    static StrataClientConfig load(final Path file) {
        final String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (final NoSuchFileException missing) {
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, DEFAULT_FILE, StandardCharsets.UTF_8);
            } catch (final IOException e) {
                LOGGER.warn("Could not write Strata's default client settings to {}", file, e);
            }
            return DEFAULTS;
        } catch (final IOException e) {
            LOGGER.warn("Could not read Strata's client settings from {}", file, e);
            return DEFAULTS;
        }
        return parse(text, file.toString());
    }

    /**
     * Parses the settings.
     *
     * @param text the file's text
     * @param source where it came from, for log messages
     * @return the settings
     */
    static StrataClientConfig parse(final String text, final String source) {
        final JsonObject root;
        try {
            final JsonElement element = JsonParser.parseString(text);
            if (!element.isJsonObject()) {
                LOGGER.warn("Strata's client settings in {} are not a JSON object", source);
                return DEFAULTS;
            }
            root = element.getAsJsonObject();
        } catch (final JsonParseException e) {
            LOGGER.warn("Could not parse Strata's client settings in {}", source, e);
            return DEFAULTS;
        }
        boolean forceAll = false;
        final JsonElement all = root.get("forceVanillaSlots");
        if (all != null) {
            if (all.isJsonPrimitive() && all.getAsJsonPrimitive().isBoolean()) {
                forceAll = all.getAsBoolean();
            } else {
                LOGGER.warn("forceVanillaSlots in {} is not true or false", source);
            }
        }
        final Set<Identifier> types = new HashSet<>();
        final JsonElement some = root.get("forceVanillaSlotsFor");
        if (some != null) {
            if (some.isJsonArray()) {
                for (final JsonElement entry : (JsonArray) some) {
                    final Identifier id = entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()
                            ? Identifier.tryParse(entry.getAsString()) : null;
                    if (id == null) {
                        LOGGER.warn("Skipping {} in forceVanillaSlotsFor in {}: not a menu type id", entry, source);
                    } else {
                        types.add(id);
                    }
                }
            } else {
                LOGGER.warn("forceVanillaSlotsFor in {} is not a list", source);
            }
        }
        return new StrataClientConfig(forceAll, types);
    }

    /**
     * Returns the slot rendering a screen gets after these settings.
     *
     * @param requested what the screen's developer asked for
     * @param menuType the screen's menu type, or null for a menu without one, such as the player's inventory
     * @return {@link SlotRendering#VANILLA} when the player forces it for every screen or for this menu type,
     *     otherwise {@code requested}
     */
    SlotRendering apply(final SlotRendering requested, final Identifier menuType) {
        if (this.forceVanillaSlots || menuType != null && this.forceVanillaSlotsFor.contains(menuType)) {
            return SlotRendering.VANILLA;
        }
        return requested;
    }
}
