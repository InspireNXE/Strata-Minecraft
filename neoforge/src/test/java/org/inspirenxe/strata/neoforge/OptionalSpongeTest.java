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
package org.inspirenxe.strata.neoforge;

import org.inspirenxe.strata.api.Strata;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the NeoForge jar starts without SpongeAPI and declares it as an optional dependency. SpongeAPI is
 * compileOnly here, so these tests run, like a plain NeoForge game, without it.
 */
class OptionalSpongeTest {

    @Test
    void spongeApiIsNotOnTheClasspath() {
        assertThrows(ClassNotFoundException.class, () -> Class.forName("org.spongepowered.api.Sponge"),
                "the tests run without SpongeAPI, as a game without SpongeNeo does");
    }

    @Test
    void entryPointLinksWithoutSponge() {
        assertDoesNotThrow(() -> Class.forName(StrataNeoForge.class.getName(), true, StrataNeoForge.class.getClassLoader())
                .getDeclaredMethods(), "the entry point loads and resolves its methods without SpongeAPI");
    }

    @Test
    void startsWithoutSponge() {
        StrataNeoForge.install(false);
        assertNotNull(Strata.screens(), "Strata-Minecraft started without SpongeAPI");
    }

    @Test
    void declaresSpongeApiAsOptional() throws IOException {
        final String toml;
        try (InputStream in = OptionalSpongeTest.class.getClassLoader().getResourceAsStream("META-INF/neoforge.mods.toml")) {
            assertNotNull(in, "neoforge.mods.toml is on the classpath");
            toml = new String(in.readAllBytes(), StandardCharsets.UTF_8).replace(" ", "").replace("\r", "");
        }
        assertTrue(toml.contains("modId=\"" + StrataNeoForge.SPONGE_API + "\"\ntype=\"optional\"\n"
                + "versionRange=\"[12.0.0,13)\"\nordering=\"AFTER\""),
                "SpongeAPI is optional, version 12, and constructed before Strata when present");
    }
}
