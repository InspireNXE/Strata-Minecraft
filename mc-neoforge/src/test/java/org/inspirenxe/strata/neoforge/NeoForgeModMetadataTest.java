package org.inspirenxe.strata.neoforge;

import net.neoforged.fml.common.Mod;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks the processed {@code neoforge.mods.toml} and the entry point that ship in the NeoForge jar.
 */
class NeoForgeModMetadataTest {

    private static String metadata() throws IOException {
        try (InputStream in = NeoForgeModMetadataTest.class.getClassLoader().getResourceAsStream("META-INF/neoforge.mods.toml")) {
            assertNotNull(in, "neoforge.mods.toml is on the classpath");
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
        final Mod mod = StrataNeoForge.class.getAnnotation(Mod.class);
        assertNotNull(mod, "the entry point is annotated with @Mod");
        assertEquals("strata", mod.value());
    }
}
