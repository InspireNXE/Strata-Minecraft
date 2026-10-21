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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.Strata;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.core.theme.ThemeRegistryImpl;
import org.inspirenxe.strata.core.theme.ThemeResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Strata screens opened above one another in vanilla's screen slot, with the game mocked and vanilla's
 * {@code setScreen} played by a stand-in that removes the old screen and initializes the new one, as vanilla does.
 */
class StrataScreenStackTest {
    private Minecraft minecraft;
    private Font font;
    private FakeGui gui;

    @BeforeAll
    static void bootstrap() {
        VanillaBootstrap.run();
    }

    @BeforeEach
    void setUp() {
        this.minecraft = mock(Minecraft.class);
        when(this.minecraft.getResourceManager()).thenReturn(mock(ResourceManager.class));
        this.font = mock(Font.class);
        this.gui = new FakeGui();
        StrataScreen.switcher = this.gui;
    }

    @AfterEach
    void tearDown() {
        StrataScreen.switcher = StrataScreen.Switcher.VANILLA;
    }

    private StrataScreen show(final Counting content) {
        final StrataScreen screen = new StrataScreen(this.minecraft, this.font, Component.literal("Root"), content,
                ThemeRef.platform());
        this.gui.set(this.minecraft, screen);
        return screen;
    }

    @Test
    void aHandleOpensAboveItsScreenAndClosingShowsItAgain() {
        final Counting first = new Counting();
        final StrataScreen root = this.show(first);
        final Counting second = new Counting();

        final ScreenHandle opened = first.handle.open(second);

        final StrataScreen above = (StrataScreen) this.gui.current;
        assertSame(second, above.content());
        assertSame(above, root.child());
        assertSame(root, above.parent());
        assertSame(opened, above.handle());
        assertTrue(root.isOpen());
        assertEquals(0, first.closes);

        opened.close();

        assertSame(root, this.gui.current);
        assertNull(root.child());
        assertTrue(root.isOpen());
        assertEquals(1, first.builds);
        assertEquals(0, first.closes);
        assertEquals(1, second.closes);
    }

    @Test
    void theScreenAboveIsTitledWithTheScreensTitle() {
        final Counting first = new Counting();
        this.show(first);
        final Counting titled = new Counting();
        titled.title = Text.of("Exchange");

        first.handle.open(titled);

        assertEquals("Exchange", this.gui.current.getTitle().getString());
    }

    @Test
    void vanillaMovingOnClosesTheScreensBeneath() {
        final Counting first = new Counting();
        final StrataScreen root = this.show(first);
        final Counting second = new Counting();
        first.handle.open(second);

        this.gui.set(this.minecraft, mock(Screen.class));

        assertFalse(root.isOpen());
        assertEquals(1, first.closes);
        assertEquals(1, second.closes);
    }

    @Test
    void closingAWaitingScreenClosesTheScreensAboveIt() {
        final Counting first = new Counting();
        final StrataScreen root = this.show(first);
        final Counting second = new Counting();
        first.handle.open(second);
        final Counting third = new Counting();
        second.handle.open(third);

        second.handle.close();

        assertSame(root, this.gui.current);
        assertTrue(root.isOpen());
        assertEquals(0, first.closes);
        assertEquals(1, second.closes);
        assertEquals(1, third.closes);
    }

    @Test
    void aCoveredScreensHandleOpensOnTop() {
        final Counting first = new Counting();
        this.show(first);
        final Counting second = new Counting();
        first.handle.open(second);
        final StrataScreen middle = (StrataScreen) this.gui.current;
        final Counting third = new Counting();

        first.handle.open(third);

        final StrataScreen top = (StrataScreen) this.gui.current;
        assertSame(third, top.content());
        assertSame(middle, top.parent());
    }

    @Test
    void theControllerStacksOnlyAboveAStrataScreen() {
        final Counting first = new Counting();
        final StrataScreen root = this.show(first);
        final StrataScreen opened = StrataScreen.of(this.minecraft, this.font, new Counting(), ThemeRef.platform());

        MinecraftScreens.show(this.minecraft, opened);

        assertSame(root, opened.parent());

        this.gui.set(this.minecraft, mock(Screen.class));
        final StrataScreen alone = StrataScreen.of(this.minecraft, this.font, new Counting(), ThemeRef.platform());
        MinecraftScreens.show(this.minecraft, alone);

        assertSame(alone, this.gui.current);
        assertNull(alone.parent());
    }

    @Test
    void screensOpenInTheVanillaThemeByDefault() {
        final Counting first = new Counting();
        this.show(first);

        assertEquals(MinecraftThemeSource.VANILLA_LAYER,
                first.handle.factory().theme().layer("button.normal").orElseThrow());
    }

    @Test
    void theControllerNeedsARunningGame() {
        assertThrows(IllegalStateException.class, () -> MinecraftScreens.INSTANCE.open(new Counting()));
    }

    @Test
    void installingHandsTheControllerToStrata() {
        StrataMinecraft.install();

        assertSame(MinecraftScreens.INSTANCE, Strata.screens());
    }

    @Test
    void theDefaultThemeIsVanilla() {
        final MinecraftThemeSource source = new MinecraftThemeSource(this.minecraft);
        assertEquals(MinecraftThemeSource.VANILLA, source.defaultTheme());
        assertEquals(MinecraftThemeSource.VANILLA_LAYER, new ThemeResolver(source, new ThemeRegistryImpl())
                .resolve(ThemeRef.platform()).layer("button.normal").orElseThrow());
    }

    @Test
    void titlesKeepTheirTextAndStyles() {
        final Component plain = MinecraftText.component(Text.of("Store"));
        assertEquals("Store", plain.getString());
        assertTrue(plain.getSiblings().get(0).getStyle().isEmpty());

        final Component gold = MinecraftText.component(Text.of("Gold").color(0xFFFFAA00).bold());
        assertEquals(0xFFAA00, gold.getSiblings().get(0).getStyle().getColor().getValue());
        assertTrue(gold.getSiblings().get(0).getStyle().isBold());
    }

    /**
     * Plays vanilla's {@code setScreen}: removes the old screen, then sizes and initializes the new one.
     */
    private static final class FakeGui implements StrataScreen.Switcher {
        private Screen current;

        @Override
        public Screen current(final Minecraft minecraft) {
            return this.current;
        }

        @Override
        public void set(final Minecraft minecraft, final Screen screen) {
            if (this.current != null) {
                this.current.removed();
            }
            this.current = screen;
            if (screen instanceof final StrataScreen strata) {
                strata.width = 320;
                strata.height = 240;
                strata.init();
            }
        }
    }

    /**
     * A screen that keeps its handle and counts its builds and closes.
     */
    private static final class Counting implements org.inspirenxe.strata.api.screen.Screen {
        private final List<Object> built = new ArrayList<>();
        private ScreenHandle handle;
        private Text title = Text.empty();
        private int builds;
        private int closes;

        @Override
        public org.inspirenxe.strata.api.component.Component<?> build(final ComponentFactory factory) {
            this.builds++;
            final org.inspirenxe.strata.api.component.Component<?> tree = factory.anchorPanel();
            this.built.add(tree);
            return tree;
        }

        @Override
        public Text title() {
            return this.title;
        }

        @Override
        public void opened(final ScreenHandle handle) {
            this.handle = handle;
        }

        @Override
        public void closed() {
            this.closes++;
        }
    }
}
