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

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinecraftTranslatorTest {

    @Test
    void languageCodesBecomeLocales() {
        assertEquals(Locale.US, MinecraftTranslator.localeOf("en_us"));
        assertEquals(Locale.GERMANY, MinecraftTranslator.localeOf("de_de"));
        assertEquals(new Locale("pt", "BR"), MinecraftTranslator.localeOf("pt_br"));
        assertEquals(Locale.JAPAN, MinecraftTranslator.localeOf("ja_jp"));
    }

    @Test
    void languagesTheJdkLacksFallBack() {
        assertEquals(Locale.ENGLISH, MinecraftTranslator.localeOf("enws"));
        assertEquals(new Locale("es"), MinecraftTranslator.localeOf("esan"));
        assertEquals(Locale.US, MinecraftTranslator.localeOf("lol_us"));
        assertEquals(Locale.ENGLISH, MinecraftTranslator.localeOf("tlh_aa"));
    }
}
