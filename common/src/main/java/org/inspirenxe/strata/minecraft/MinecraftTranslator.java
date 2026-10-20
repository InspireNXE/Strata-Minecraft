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
import net.minecraft.locale.Language;
import org.inspirenxe.strata.core.StrataCore;
import org.inspirenxe.strata.spi.Translator;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Translates Strata text with the game's language, so a mod's keys come from its {@code lang} files and resource
 * packs can override them. Strata's own button keys read vanilla's ({@code gui.ok} and the like), so message boxes
 * use the player's language without a translation of their own. The version changes whenever the game swaps its
 * language, which happens when the player picks another one and on every resource reload.
 *
 * <p>The locale, which dates and times follow, comes from the selected language's code, such as {@code en_us} or
 * {@code de_de}, and is read again with the version. See {@link #localeOf(String)}.</p>
 */
final class MinecraftTranslator implements Translator {
    private static final Map<String, String> VANILLA = Map.of(
            "strata.button.ok", "gui.ok",
            "strata.button.cancel", "gui.cancel",
            "strata.button.yes", "gui.yes",
            "strata.button.no", "gui.no");
    private static boolean installed;

    private final AtomicLong version = new AtomicLong();
    private volatile Language seen;
    private volatile Locale locale = Locale.US;

    private MinecraftTranslator() {
    }

    /**
     * Makes this the process's translator the first time it is called.
     */
    static synchronized void install() {
        if (!installed) {
            installed = true;
            StrataCore.setTranslator(new MinecraftTranslator());
        }
    }

    @Override
    public Optional<String> translate(final String key) {
        final Language language = Language.getInstance();
        if (language.has(key)) {
            return Optional.of(language.getOrDefault(key));
        }
        final String vanilla = VANILLA.get(key);
        return vanilla != null && language.has(vanilla) ? Optional.of(language.getOrDefault(vanilla)) : Optional.empty();
    }

    @Override
    public long version() {
        final Language language = Language.getInstance();
        if (language != this.seen) {
            this.seen = language;
            this.locale = MinecraftTranslator.selectedLocale();
            this.version.incrementAndGet();
        }
        return this.version.get();
    }

    @Override
    public Locale locale() {
        this.version();
        return this.locale;
    }

    /**
     * Returns the locale of the language the player selected, or American English before the game has a language
     * manager.
     */
    private static Locale selectedLocale() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getLanguageManager() == null) {
            return Locale.US;
        }
        return MinecraftTranslator.localeOf(minecraft.getLanguageManager().getSelected());
    }

    /**
     * Maps a game language code to a locale. The part before the underscore is the language and the part after it
     * the region, so {@code de_de} is German in Germany. A code with no region whose language the JDK has no data for
     * is cut to its first two letters when those name one, so {@code enws} reads as English and {@code esan} as
     * Spanish. Any other language the JDK lacks, such as {@code lol_us}, reads as English, the game's own default.
     * A region that is not a known country code is dropped.
     *
     * @param code the code, such as {@code en_us}
     * @return the locale
     */
    static Locale localeOf(final String code) {
        final String[] parts = code.toLowerCase(Locale.ROOT).split("_", 2);
        String language = parts[0];
        if (!Known.LANGUAGES.contains(language) && parts.length == 1 && language.length() > 2
                && Known.LANGUAGES.contains(language.substring(0, 2))) {
            language = language.substring(0, 2);
        }
        if (!Known.LANGUAGES.contains(language)) {
            language = "en";
        }
        final String region = parts.length > 1 ? parts[1].toUpperCase(Locale.ROOT) : "";
        return Known.COUNTRIES.contains(region) ? Locale.of(language, region) : Locale.of(language);
    }

    /**
     * The languages and countries the JDK has data for, read once.
     */
    private static final class Known {
        static final Set<String> LANGUAGES = Arrays.stream(Locale.getAvailableLocales())
                .map(Locale::getLanguage)
                .filter(language -> !language.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        static final Set<String> COUNTRIES = Set.of(Locale.getISOCountries());
    }
}
