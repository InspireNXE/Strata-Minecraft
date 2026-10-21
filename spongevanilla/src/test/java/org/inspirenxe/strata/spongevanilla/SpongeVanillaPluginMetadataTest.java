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
package org.inspirenxe.strata.spongevanilla;

import org.junit.jupiter.api.Test;
import org.spongepowered.plugin.builtin.jvm.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the processed {@code sponge_plugins.json} and the entry point that ship in the SpongeVanilla jar.
 */
class SpongeVanillaPluginMetadataTest {

    private static String metadata() throws IOException {
        try (InputStream in = SpongeVanillaPluginMetadataTest.class.getClassLoader().getResourceAsStream("META-INF/sponge_plugins.json")) {
            assertNotNull(in, "sponge_plugins.json is on the classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void everyPropertyIsExpanded() throws IOException {
        assertFalse(metadata().contains("${"), "no unexpanded Gradle property is left");
    }

    @Test
    void declaresThePluginAndItsRequirements() throws IOException {
        final String json = metadata().replaceAll("\\s+", "");
        assertTrue(json.contains("\"id\":\"strata\""), "the plugin id is strata");
        assertTrue(json.contains("\"id\":\"spongeapi\",\"version\":\"[21.0.0-SNAPSHOT,22)\",\"load-order\":\"after\""),
                "it requires SpongeAPI 21 and loads after it");
        assertTrue(json.contains("\"id\":\"minecraft\",\"version\":\"[26.3,26.4)\""), "it requires Minecraft 26.3");
    }

    @Test
    void entryPointMatchesThePluginId() throws IOException {
        final String json = metadata().replaceAll("\\s+", "");
        assertTrue(json.contains("\"entrypoints\":[\"" + StrataSpongeVanilla.class.getName() + "\"]"),
                "the entry point is listed");
        final Plugin plugin = StrataSpongeVanilla.class.getAnnotation(Plugin.class);
        assertNotNull(plugin, "the entry point is annotated with @Plugin");
        assertEquals("strata", plugin.value());
    }
}
