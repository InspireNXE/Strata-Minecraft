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

import net.minecraft.locale.Language;
import org.inspirenxe.strata.core.StrataCore;
import org.inspirenxe.strata.spi.Translator;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Translates Strata text with the game's language, so a mod's keys come from its {@code lang} files and resource
 * packs can override them. Strata's own button keys read vanilla's ({@code gui.ok} and the like), so message boxes
 * use the player's language without a translation of their own. The version changes whenever the game swaps its
 * language, which happens when the player picks another one and on every resource reload.
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
            this.version.incrementAndGet();
        }
        return this.version.get();
    }
}
