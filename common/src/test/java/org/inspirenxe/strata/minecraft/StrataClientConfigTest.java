package org.inspirenxe.strata.minecraft;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The player's settings file must force vanilla slot drawing where it says, and never fail a screen.
 */
class StrataClientConfigTest {
    private static final Identifier GRINDER = Identifier.fromNamespaceAndPath("examplemod", "grinder");

    @Test
    void forcingEveryScreenOverridesStrataMode() {
        final StrataClientConfig config = StrataClientConfig.parse("{\"forceVanillaSlots\": true}", "test");

        assertEquals(SlotRendering.VANILLA, config.apply(SlotRendering.STRATA, GRINDER));
        assertEquals(SlotRendering.VANILLA, config.apply(SlotRendering.STRATA, null), "menus without a type too");
    }

    @Test
    void forcingSomeMenuTypesLeavesTheOthersAlone() {
        final StrataClientConfig config = StrataClientConfig.parse(
                "{\"forceVanillaSlotsFor\": [\"examplemod:grinder\", \"not an id!\", 3]}", "test");

        assertEquals(Set.of(GRINDER), config.forceVanillaSlotsFor(), "invalid entries are skipped");
        assertEquals(SlotRendering.VANILLA, config.apply(SlotRendering.STRATA, GRINDER));
        assertEquals(SlotRendering.STRATA, config.apply(SlotRendering.STRATA, Identifier.withDefaultNamespace("generic_9x3")));
        assertEquals(SlotRendering.STRATA, config.apply(SlotRendering.STRATA, null));
    }

    @Test
    void theSettingsNeverForceStrataMode() {
        assertEquals(SlotRendering.VANILLA, StrataClientConfig.DEFAULTS.apply(SlotRendering.VANILLA, GRINDER));
    }

    @Test
    void brokenFilesCountAsTheDefaults() {
        assertEquals(StrataClientConfig.DEFAULTS, StrataClientConfig.parse("{ not json", "test"));
        assertEquals(StrataClientConfig.DEFAULTS, StrataClientConfig.parse("[]", "test"));
        assertEquals(StrataClientConfig.DEFAULTS, StrataClientConfig.parse(
                "{\"forceVanillaSlots\": \"yes\", \"forceVanillaSlotsFor\": \"examplemod:grinder\"}", "test"));
    }

    @Test
    void aMissingFileIsWrittenWithTheDefaults(@TempDir final Path game) throws IOException {
        final Path file = StrataClientConfig.file(game);

        assertEquals(StrataClientConfig.DEFAULTS, StrataClientConfig.load(file));
        assertTrue(Files.exists(file), "the defaults are written for the player to find");
        assertEquals(StrataClientConfig.DEFAULTS, StrataClientConfig.load(file), "and read back the same");

        Files.writeString(file, "{\"forceVanillaSlots\": true}");
        assertEquals(new StrataClientConfig(true, Set.of()), StrataClientConfig.load(file));
    }
}
