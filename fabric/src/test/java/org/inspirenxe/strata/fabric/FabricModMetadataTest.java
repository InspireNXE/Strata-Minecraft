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
package org.inspirenxe.strata.fabric;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the processed {@code fabric.mod.json} that ships in the Fabric jar.
 */
class FabricModMetadataTest {

    private static String metadata() throws IOException {
        try (InputStream in = FabricModMetadataTest.class.getClassLoader().getResourceAsStream("fabric.mod.json")) {
            assertNotNull(in, "fabric.mod.json is on the classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void everyPropertyIsExpanded() throws IOException {
        assertFalse(metadata().contains("${"), "no unexpanded Gradle property is left");
    }

    @Test
    void declaresTheModAndItsRequirements() throws IOException {
        final String json = metadata().replaceAll("\\s+", "");
        assertTrue(json.contains("\"id\":\"strata\""), "the mod id is strata");
        assertTrue(json.contains("\"environment\":\"client\""), "Strata is client-only");
        assertTrue(json.contains("\"minecraft\":\"~1.20.1\""), "it requires Minecraft 1.20.1");
        assertTrue(json.contains("\"java\":\">=17\""), "it requires Java 17");
    }

    @Test
    void startsStrataOnTheClient() throws IOException {
        final String json = metadata().replaceAll("\\s+", "");
        assertTrue(json.contains("\"client\":[\"org.inspirenxe.strata.fabric.StrataFabric\"]"),
                "the client entry point hands Strata its screen controller");
    }
}
