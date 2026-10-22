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

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.Strata;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.event.input.TransferEvent;
import org.inspirenxe.strata.api.layout.AnchorPanel;
import org.inspirenxe.strata.api.transfer.DataFormat;
import org.inspirenxe.strata.api.transfer.Transfer;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.core.theme.ThemeRegistryImpl;
import org.inspirenxe.strata.core.theme.ThemeResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
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
        // The tests play the client thread
        when(this.minecraft.isSameThread()).thenReturn(true);
        final Window window = mock(Window.class);
        when(window.getGuiScale()).thenReturn(3);
        when(this.minecraft.getWindow()).thenReturn(window);
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
    void escapeAsksTheScreenBeforeClosingIt() {
        final Counting first = new Counting();
        final StrataScreen root = this.show(first);
        final Counting second = new Counting();
        second.refuse = true;
        first.handle.open(second);
        final StrataScreen above = (StrataScreen) this.gui.current;

        assertTrue(this.escape(above));

        assertSame(above, this.gui.current);
        assertTrue(above.isOpen());
        assertEquals(1, second.asks);
        assertEquals(0, second.closes);

        second.refuse = false;
        assertTrue(this.escape(above));

        assertSame(root, this.gui.current);
        assertEquals(2, second.asks);
        assertEquals(1, second.closes);
        assertEquals(0, first.asks);
    }

    @Test
    void onCloseAsksButTheHandlesCloseDoesNot() {
        final Counting first = new Counting();
        final StrataScreen root = this.show(first);
        final Counting second = new Counting();
        second.refuse = true;
        first.handle.open(second);
        final StrataScreen above = (StrataScreen) this.gui.current;

        above.onClose();

        assertSame(above, this.gui.current);
        assertEquals(1, second.asks);

        second.handle.close();

        assertSame(root, this.gui.current);
        assertEquals(1, second.asks);
        assertEquals(1, second.closes);
    }

    @Test
    void theHandlesRequestCloseAsksAndClosesTheVanillaScreen() {
        final Counting first = new Counting();
        final StrataScreen root = this.show(first);
        final Counting second = new Counting();
        second.refuse = true;
        final ScreenHandle opened = first.handle.open(second);

        assertFalse(opened.requestClose());
        assertTrue(opened.isOpen());

        second.refuse = false;
        assertTrue(opened.requestClose());

        assertSame(root, this.gui.current);
        assertFalse(opened.isOpen());
        assertEquals(2, second.asks);
        assertEquals(1, second.closes);
    }

    @Test
    void filesDroppedOnTheWindowReachTheComponentUnderThePointer() {
        final Counting first = new Counting();
        final StrataScreen root = this.show(first);
        final List<Transfer> dropped = new ArrayList<>();
        final Probe target = new Probe(first.handle.factory())
                .onEvent(InputEvents.DRAG_OVER, TransferEvent::accept)
                .onEvent(InputEvents.DROP, event -> dropped.add(event.transfer()));
        ((AnchorPanel) first.built.getFirst()).add(target);
        root.host().layout();

        root.mouseMoved(40, 50);
        root.onFilesDrop(List.of(Path.of("ana.png"), Path.of("ben.png")));

        assertEquals(1, dropped.size());
        assertEquals(List.of(Path.of("ana.png"), Path.of("ben.png")), dropped.getFirst().get(DataFormat.FILES).orElseThrow());
    }

    @Test
    void theDensityIsTheGuiScale() {
        final Counting first = new Counting();
        this.show(first);
        final Probe probe = new Probe(first.handle.factory());
        ((AnchorPanel) first.built.getFirst()).add(probe);

        assertEquals(3f, probe.density());
    }

    @Test
    void theVanillaTitleFollowsSetTitle() {
        final Counting first = new Counting();
        final StrataScreen root = this.show(first);

        assertEquals("Root", root.getTitle().getString());

        first.handle.setTitle(Text.of("Renamed"));

        assertEquals("Renamed", root.getTitle().getString());
        assertEquals("Renamed", root.getNarrationMessage().getString());
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
        // Strata.uiThread() is the client thread, and there is no game in a test
        assertFalse(Strata.uiThread().isCurrent());
        assertThrows(IllegalStateException.class, () -> Strata.uiThread().checkCurrent());
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
        assertTrue(plain.getSiblings().getFirst().getStyle().isEmpty());

        final Component gold = MinecraftText.component(Text.of("Gold").color(0xFFFFAA00).bold());
        assertEquals(0xFFAA00, gold.getSiblings().getFirst().getStyle().getColor().getValue());
        assertTrue(gold.getSiblings().getFirst().getStyle().isBold());
    }

    /**
     * Presses and releases Escape on a screen.
     */
    private boolean escape(final StrataScreen screen) {
        final KeyEvent event = new KeyEvent(InputConstants.KEY_ESCAPE, 0, 0);
        final boolean used = screen.keyPressed(event);
        if (this.gui.current == screen) {
            screen.keyReleased(event);
        }
        return used;
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
     * A component 100 by 100 that reads its environment's density.
     */
    private static final class Probe extends CustomComponent<Probe> {
        Probe(final ComponentFactory factory) {
            super(factory);
        }

        float density() {
            return this.environment().pixelsPerUnit();
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(100f, 100f);
        }
    }

    /**
     * A screen that keeps its handle, counts its builds, closes and requests to close, and refuses them when told.
     */
    private static final class Counting implements org.inspirenxe.strata.api.screen.Screen {
        private final List<Object> built = new ArrayList<>();
        private ScreenHandle handle;
        private Text title = Text.empty();
        private int builds;
        private int closes;
        private int asks;
        private boolean refuse;

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
        public boolean closeRequested(final ScreenHandle handle) {
            this.asks++;
            return !this.refuse;
        }

        @Override
        public void closed() {
            this.closes++;
        }
    }
}
