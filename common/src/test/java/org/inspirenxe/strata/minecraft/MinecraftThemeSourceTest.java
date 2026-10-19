package org.inspirenxe.strata.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.core.theme.ThemeResolver;
import org.inspirenxe.strata.spi.ThemeDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Theme files read from the resource manager, and the vanilla theme they restyle.
 */
class MinecraftThemeSourceTest {
    private static final Identifier VANILLA_FILE = Identifier.fromNamespaceAndPath("minecraft", "strata/themes/vanilla.json");

    private ResourceManager resources;
    private MinecraftThemeSource source;

    private static Resource resource(final String pack, final String json) {
        final PackResources packResources = mock(PackResources.class);
        when(packResources.packId()).thenReturn(pack);
        return new Resource(packResources, () -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }

    @BeforeEach
    void setUp() {
        final Minecraft minecraft = mock(Minecraft.class);
        this.resources = mock(ResourceManager.class);
        when(minecraft.getResourceManager()).thenReturn(this.resources);
        when(this.resources.getResourceStack(any())).thenReturn(List.of());
        this.source = new MinecraftThemeSource(minecraft);
    }

    @Test
    void vanillaIsABaseTheme() {
        assertEquals(MinecraftThemes.vanilla().paints(), this.source.base(MinecraftThemes.VANILLA).orElseThrow().paints());
    }

    @Test
    void filesComeFromTheResourceStackInPackOrder() {
        final List<Resource> stack = List.of(resource("vanilla", "{}"), resource("fancy_pack", "{ \"paints\": {} }"));
        when(this.resources.getResourceStack(VANILLA_FILE)).thenReturn(stack);
        final List<ThemeDocument> documents = this.source.documents(MinecraftThemes.VANILLA);
        assertEquals(List.of("{}", "{ \"paints\": {} }"), documents.stream().map(ThemeDocument::content).toList());
        assertEquals("fancy_pack/minecraft:strata/themes/vanilla.json", documents.get(1).origin());
    }

    @Test
    void aPackRestylesTheVanillaTheme() {
        final List<Resource> stack =
                List.of(resource("fancy_pack", "{ \"text\": { \"button.text\": { \"color\": \"#FFD700\" } } }"));
        when(this.resources.getResourceStack(VANILLA_FILE)).thenReturn(stack);
        final Theme theme = new ThemeResolver(this.source).resolve(ThemeRef.of(MinecraftThemes.VANILLA));
        assertEquals(0xFFFFD700, theme.textStyle(Button.STYLE_TEXT).orElseThrow().color());
        assertTrue(theme.textStyle(Button.STYLE_TEXT).orElseThrow().shadow());
        assertEquals(MinecraftThemes.vanilla().paint(Button.STYLE_NORMAL), theme.paint(Button.STYLE_NORMAL));
    }

    @Test
    void aReloadDropsTheCache() {
        this.source.documents(MinecraftThemes.VANILLA);
        this.source.documents(MinecraftThemes.VANILLA);
        verify(this.resources, times(1)).getResourceStack(VANILLA_FILE);

        final long version = this.source.version();
        this.source.onResourceManagerReload(this.resources);
        assertNotEquals(version, this.source.version());
        this.source.documents(MinecraftThemes.VANILLA);
        verify(this.resources, times(2)).getResourceStack(VANILLA_FILE);
    }

    @Test
    void anInvalidIdHasNoFiles() {
        assertTrue(this.source.documents(ThemeId.of("Bad Namespace", "shop")).isEmpty());
    }

    @Test
    void registeringAThemeChangesTheVersion() {
        final long version = this.source.version();
        this.source.register(ThemeId.of("mymod", "shop"), MinecraftThemes.vanilla());
        assertNotEquals(version, this.source.version());
        assertEquals(MinecraftThemes.vanilla().paints(),
                this.source.base(ThemeId.of("mymod", "shop")).map(Theme::paints).orElseThrow());
    }
}
