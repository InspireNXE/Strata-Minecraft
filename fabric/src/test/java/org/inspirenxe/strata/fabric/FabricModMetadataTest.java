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
        assertTrue(json.contains("\"minecraft\":\"~26.3\""), "it requires Minecraft 26.3");
        assertTrue(json.contains("\"java\":\">=25\""), "it requires Java 25");
    }
}
