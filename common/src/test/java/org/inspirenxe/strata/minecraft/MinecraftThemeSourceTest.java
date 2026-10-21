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

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.theme.ThemeRegistryImpl;
import org.inspirenxe.strata.core.theme.ThemeResolver;
import org.inspirenxe.strata.spi.ThemeDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private static final ResourceLocation VANILLA_FILE = new ResourceLocation("minecraft", "strata/themes/vanilla.json");

    private ResourceManager resources;
    private MinecraftThemeSource source;

    private static Resource resource(final String pack, final String json) {
        return resource(pack, false, json);
    }

    /**
     * A resource from a pack that is built in or not, as the game, Forge and Fabric API mark their own packs.
     */
    private static Resource resource(final String pack, final boolean builtIn, final String json) {
        final PackResources packResources = mock(PackResources.class);
        when(packResources.packId()).thenReturn(pack);
        when(packResources.isBuiltin()).thenReturn(builtIn);
        return new Resource(packResources, () -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }

    private Theme resolveVanilla() {
        return new ThemeResolver(this.source, new ThemeRegistryImpl()).resolve(ThemeRef.of(MinecraftThemeSource.VANILLA));
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
    void vanillasBaseIsTheShippedFile() {
        assertTrue(this.source.base(MinecraftThemeSource.VANILLA).isEmpty());
        final List<ThemeDocument> bases = this.source.bases(MinecraftThemeSource.VANILLA);
        assertEquals(1, bases.size());
        assertEquals(MinecraftThemeSource.VANILLA_LAYER, bases.get(0).layer());
        assertTrue(bases.get(0).content().contains("\"parent\": \"strata:flat\""));
        assertTrue(this.source.bases(ThemeId.of("mymod", "shop")).isEmpty());
    }

    @Test
    void filesComeFromTheResourceStackInPackOrder() {
        final List<Resource> stack = List.of(resource("file/plain", "{}"), resource("file/fancy_pack", "{ \"paints\": {} }"));
        when(this.resources.getResourceStack(VANILLA_FILE)).thenReturn(stack);
        final List<ThemeDocument> documents = this.source.documents(MinecraftThemeSource.VANILLA);
        assertEquals(List.of("{}", "{ \"paints\": {} }"), documents.stream().map(ThemeDocument::content).toList());
        assertEquals("file/fancy_pack/minecraft:strata/themes/vanilla.json", documents.get(1).origin());
        assertEquals("file/fancy_pack", documents.get(1).layer());
    }

    @Test
    void modsFilesComeAfterThePlayersPacks() {
        // The game's order, with a Forge mod pack below a player's pack and a Fabric one above a server pack
        final List<Resource> stack = List.of(
                resource("mod:alpha", true, "alpha"),
                resource("file/fancy_pack", "fancy"),
                resource("high_contrast", true, "contrast"),
                resource("beta", true, "beta"),
                resource("server/0000002A/pack", "server"));
        when(this.resources.getResourceStack(VANILLA_FILE)).thenReturn(stack);
        final List<ThemeDocument> documents = this.source.documents(MinecraftThemeSource.VANILLA);
        assertEquals(List.of("fancy", "contrast", "server", "alpha", "beta"),
                documents.stream().map(ThemeDocument::content).toList());
        assertEquals(List.of("file/fancy_pack", "high_contrast", "server/0000002A/pack", "mod:alpha", "beta"),
                documents.stream().map(ThemeDocument::layer).toList());
    }

    @Test
    void aModsKeysBeatThePlayersPacks() {
        final List<Resource> stack = List.of(
                resource("mod:alpha", true, "{ \"paints\": { \"button.normal\": \"#000001\" } }"),
                resource("file/fancy_pack", "{ \"paints\": { \"button.normal\": \"#000002\","
                        + " \"button.hovered\": \"#000002\" } }"));
        when(this.resources.getResourceStack(VANILLA_FILE)).thenReturn(stack);
        final Theme theme = this.resolveVanilla();
        assertEquals(Paint.solid(0xFF000001), theme.paint(Button.STYLE_NORMAL).orElseThrow());
        assertEquals(Optional.of("mod:alpha"), theme.layer(Button.STYLE_NORMAL));
        assertEquals(Paint.solid(0xFF000002), theme.paint(Button.STYLE_HOVERED).orElseThrow());
        assertEquals(Optional.of("file/fancy_pack"), theme.layer(Button.STYLE_HOVERED));
    }

    @Test
    void aPackRestylesTheVanillaTheme() {
        final List<Resource> stack =
                List.of(resource("file/fancy_pack", "{ \"text\": { \"button.text\": { \"color\": \"#FFD700\" } } }"));
        when(this.resources.getResourceStack(VANILLA_FILE)).thenReturn(stack);
        final Theme theme = this.resolveVanilla();
        assertEquals(0xFFFFD700, theme.textStyle(Button.STYLE_TEXT).orElseThrow().color());
        assertTrue(theme.textStyle(Button.STYLE_TEXT).orElseThrow().shadow());
        assertEquals(Optional.of("file/fancy_pack"), theme.layer(Button.STYLE_TEXT));
        assertEquals(Paint.nineSlice(SpriteId.of("minecraft", "widget/button"), Insets.of(3f)),
                theme.paint(Button.STYLE_NORMAL).orElseThrow());
        assertEquals(Optional.of(MinecraftThemeSource.VANILLA_LAYER), theme.layer(Button.STYLE_NORMAL));
    }

    @Test
    void aPackCannotSwapTheVanillaThemesParent() {
        final List<Resource> stack = List.of(resource("file/fancy_pack", "{ \"parent\": \"mymod:shop\" }"));
        when(this.resources.getResourceStack(VANILLA_FILE)).thenReturn(stack);
        assertEquals(Paint.nineSlice(SpriteId.of("minecraft", "widget/button"), Insets.of(3f)),
                this.resolveVanilla().paint(Button.STYLE_NORMAL).orElseThrow());
    }

    @Test
    void aReloadDropsTheCache() {
        this.source.documents(MinecraftThemeSource.VANILLA);
        this.source.documents(MinecraftThemeSource.VANILLA);
        verify(this.resources, times(1)).getResourceStack(VANILLA_FILE);

        final long version = this.source.version();
        this.source.onResourceManagerReload(this.resources);
        assertNotEquals(version, this.source.version());
        this.source.documents(MinecraftThemeSource.VANILLA);
        verify(this.resources, times(2)).getResourceStack(VANILLA_FILE);
    }

    @Test
    void anInvalidIdHasNoFiles() {
        assertTrue(this.source.documents(ThemeId.of("Bad Namespace", "shop")).isEmpty());
    }

    @Test
    void registeringAThemeChangesTheVersion() {
        final long version = this.source.version();
        this.source.register(ThemeId.of("mymod", "shop"), Themes.flat());
        assertNotEquals(version, this.source.version());
        assertEquals(Themes.flat().paints(),
                this.source.base(ThemeId.of("mymod", "shop")).map(Theme::paints).orElseThrow());
    }

    @Test
    void vanillaCannotBeRegistered() {
        assertThrows(IllegalArgumentException.class,
                () -> this.source.register(MinecraftThemeSource.VANILLA, Themes.flat()));
    }
}
