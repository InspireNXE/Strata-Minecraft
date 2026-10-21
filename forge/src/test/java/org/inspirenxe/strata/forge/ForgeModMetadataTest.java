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
package org.inspirenxe.strata.forge;

import net.minecraftforge.fml.common.Mod;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the processed {@code mods.toml} and the entry point that ship in the Forge jar.
 */
class ForgeModMetadataTest {

    private static String metadata() throws IOException {
        try (InputStream in = ForgeModMetadataTest.class.getClassLoader().getResourceAsStream("META-INF/mods.toml")) {
            assertNotNull(in, "mods.toml is on the classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void everyPropertyIsExpanded() throws IOException {
        assertFalse(metadata().contains("${"), "no unexpanded Gradle property is left");
    }

    @Test
    void declaresTheModAndItsRequirements() throws IOException {
        final String toml = metadata().replace(" ", "");
        assertTrue(toml.contains("modId=\"strata\""), "the mod id is strata");
        assertTrue(toml.contains("versionRange=\"[26.3,26.4)\""), "it requires Minecraft 26.3");
        assertTrue(toml.contains("side=\"CLIENT\""), "its dependencies are client-side");
    }

    @Test
    void entryPointMatchesTheModId() {
        final Mod mod = StrataForge.class.getAnnotation(Mod.class);
        assertNotNull(mod, "the entry point is annotated with @Mod");
        assertEquals("strata", mod.value());
    }
}
