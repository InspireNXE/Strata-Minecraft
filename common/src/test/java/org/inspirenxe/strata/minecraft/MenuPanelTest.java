package org.inspirenxe.strata.minecraft;

import net.minecraft.resources.Identifier;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.minecraft.MinecraftVisuals.ItemDecorations;
import org.inspirenxe.strata.minecraft.MinecraftVisuals.ItemVisual;
import org.inspirenxe.strata.spi.Canvas;
import org.inspirenxe.strata.spi.ScreenHost;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Checks the menu panel's drawing in both slot rendering modes, and which pointer input goes to vanilla, in a real
 * Strata host with a real menu. The screen itself needs a running game and is not covered here.
 */
class MenuPanelTest {
    private static final int WHITE = 0xFFFFFFFF;
    private static final SpriteId HELMET = SpriteId.of("minecraft", "container/slot/helmet");

    private ScreenHost host;
    private TestMenu menu;
    private Slot diamonds;
    private Slot helmet;
    private Slot inactive;
    private Slot fake;
    private SimpleContainer container;
    private FakeOwner owner;
    private Canvas canvas;

    @BeforeAll
    static void bootstrap() {
        VanillaBootstrap.run();
    }

    @BeforeEach
    void setUp() {
        this.host = TestHosts.create(Themes.flat());
        this.menu = new TestMenu();
        final SimpleContainer container = new SimpleContainer(4);
        this.container = container;
        container.setItem(0, new ItemStack(Items.DIAMOND, 3));
        container.setItem(2, new ItemStack(Items.EMERALD));
        container.setItem(3, new ItemStack(Items.GOLD_INGOT));
        this.diamonds = this.menu.add(new Slot(container, 0, 8, 4));
        this.helmet = this.menu.add(new Slot(container, 1, 26, 4) {
            @Override
            public Identifier getNoItemIcon() {
                return Identifier.withDefaultNamespace("container/slot/helmet");
            }
        });
        this.inactive = this.menu.add(new Slot(container, 2, 8, 22) {
            @Override
            public boolean isActive() {
                return false;
            }
        });
        this.fake = this.menu.add(new Slot(container, 3, 26, 22) {
            @Override
            public boolean isFake() {
                return true;
            }
        });
        this.owner = new FakeOwner(this.menu);
        this.canvas = mock(Canvas.class);
    }

    /**
     * Shows the panel at 10, 20 and renders one frame.
     */
    private MenuPanel render() {
        final MenuPanel panel = new MenuPanel(this.host.factory(), this.owner);
        this.host.open(ui -> ui.absolutePanel().add(panel, 10f, 20f));
        this.host.layout();
        this.host.render(this.canvas);
        return panel;
    }

    @Test
    void itTakesTheImageSize() {
        assertEquals(new Rect(10f, 20f, 40f, 30f), this.render().bounds());
    }

    @Test
    void vanillaModeDrawsTheSlotBackgroundsAndLeavesTheRestToVanilla() {
        this.owner.rendering = SlotRendering.VANILLA;

        this.render();

        verify(this.canvas).sprite(SpriteId.of("minecraft", "container/slot"), new Rect(17f, 23f, 18f, 18f), WHITE);
        verify(this.canvas).sprite(SpriteId.of("minecraft", "container/slot"), new Rect(35f, 23f, 18f, 18f), WHITE);
        verify(this.canvas, never()).sprite(any(SpriteId.class), eq(new Rect(17f, 41f, 18f, 18f)), anyInt());
        assertEquals(1, this.owner.vanillaDraws, "vanilla draws the slots once per frame");
        verify(this.canvas, never()).visual(any(Visual.class), any(Rect.class), anyInt());
    }

    @Test
    void strataModeDrawsItemsAndEmptySlotIconsAndSkipsInactiveSlots() {
        this.render();

        verify(this.canvas).visual(item(new ItemStack(Items.DIAMOND, 3), ItemDecorations.ALL, false, 168), new Rect(18f, 24f, 16f, 16f), WHITE);
        verify(this.canvas).sprite(HELMET, new Rect(36f, 24f, 16f, 16f), WHITE);
        verify(this.canvas).visual(item(new ItemStack(Items.GOLD_INGOT), ItemDecorations.ALL, true, 906), new Rect(36f, 42f, 16f, 16f), WHITE);
        verify(this.canvas, never()).visual(any(Visual.class), eq(new Rect(18f, 42f, 16f, 16f)), anyInt());
        assertEquals(0, this.owner.vanillaDraws);
    }

    @Test
    void theHoveredSlotIsHighlightedBehindAndInFrontOfTheItems() {
        this.owner.hovered = this.diamonds;

        this.render();

        final Rect highlight = new Rect(14f, 20f, 24f, 24f);
        final InOrder order = inOrder(this.canvas);
        order.verify(this.canvas).sprite(SpriteId.of("minecraft", "container/slot_highlight_back"), highlight, WHITE);
        order.verify(this.canvas).visual(any(Visual.class), eq(new Rect(18f, 24f, 16f, 16f)), eq(WHITE));
        order.verify(this.canvas).sprite(SpriteId.of("minecraft", "container/slot_highlight_front"), highlight, WHITE);
    }

    @Test
    void aDragShowsEachSlotsShareOfTheCarriedStack() {
        this.menu.setCarried(new ItemStack(Items.DIAMOND, 10));
        this.owner.quickCrafting = true;
        this.owner.quickCraftSlots.add(this.diamonds);
        this.owner.quickCraftSlots.add(this.helmet);

        this.render();

        // Ten spread over two slots: five each, added to the three already there
        verify(this.canvas).fill(new Rect(18f, 24f, 16f, 16f), 0x80FFFFFF);
        verify(this.canvas).visual(item(new ItemStack(Items.DIAMOND, 8), ItemDecorations.ALL, false, 168), new Rect(18f, 24f, 16f, 16f), WHITE);
        verify(this.canvas).visual(item(new ItemStack(Items.DIAMOND, 5), ItemDecorations.ALL, false, 186), new Rect(36f, 24f, 16f, 16f), WHITE);
        verify(this.canvas, never()).sprite(eq(HELMET), any(Rect.class), anyInt());
    }

    @Test
    void aDragPastTheSlotsLimitShowsTheLimitInYellow() {
        this.menu.setCarried(new ItemStack(Items.DIAMOND, 64));
        this.owner.quickCrafting = true;
        this.owner.quickCraftType = 2;
        this.owner.quickCraftSlots.add(this.diamonds);
        this.owner.quickCraftSlots.add(this.helmet);

        this.render();

        verify(this.canvas).visual(item(new ItemStack(Items.DIAMOND, 64), new ItemDecorations(false, true), false, 168),
                new Rect(18f, 24f, 16f, 16f), WHITE);
        verify(this.canvas).text(new TextSpan("64", TextStyle.DEFAULT.withColor(0xFFFFFF55).withShadow(true)),
                18f + 17f - 2 * TestHosts.CHAR_WIDTH, 33f);
    }

    @Test
    void aDragOverOneSlotDrawsThatSlotEmpty() {
        this.menu.setCarried(new ItemStack(Items.DIAMOND, 10));
        this.owner.quickCrafting = true;
        this.owner.quickCraftSlots.add(this.diamonds);

        this.render();

        verify(this.canvas, never()).visual(any(Visual.class), eq(new Rect(18f, 24f, 16f, 16f)), anyInt());
    }

    @Test
    void aSlotThatStopsTakingTheDraggedStackLeavesTheDrag() {
        this.menu.setCarried(new ItemStack(Items.DIAMOND, 10));
        this.owner.quickCrafting = true;
        this.owner.quickCraftSlots.add(this.diamonds);
        this.owner.quickCraftSlots.add(this.helmet);
        this.owner.quickCraftSlots.add(this.fake);

        this.render();

        assertEquals(List.of(this.fake), this.owner.dropped, "gold ingots do not take diamonds");
        verify(this.canvas).visual(item(new ItemStack(Items.GOLD_INGOT), ItemDecorations.ALL, true, 906), new Rect(36f, 42f, 16f, 16f), WHITE);
    }

    @Test
    void theCarriedRemainderIsCountedAsVanillaCountsIt() {
        final ItemStack carried = new ItemStack(Items.DIAMOND, 10);

        // Five each; the slot with three takes five more
        assertEquals(0, StrataContainerScreen.quickCraftRemainder(carried, List.of(this.diamonds, this.helmet), 0));
        // One each
        assertEquals(8, StrataContainerScreen.quickCraftRemainder(carried, List.of(this.diamonds, this.helmet), 1));
        // Cloning always leaves a full stack
        assertEquals(64, StrataContainerScreen.quickCraftRemainder(carried, List.of(this.diamonds, this.helmet), 2));
        // A full slot takes nothing
        this.container.setItem(0, new ItemStack(Items.DIAMOND, 64));
        assertEquals(5, StrataContainerScreen.quickCraftRemainder(carried, List.of(this.diamonds, this.helmet), 0));
        assertEquals(0, StrataContainerScreen.quickCraftRemainder(ItemStack.EMPTY, List.of(this.diamonds), 0));
    }

    @Test
    void theThemeRestylesTheSlots() {
        this.host.setTheme(Themes.flat().toBuilder().paint(MenuPanel.STYLE_SLOT, Paint.solid(0xFF112233)).build());

        this.render();

        verify(this.canvas).fill(new Rect(17f, 23f, 18f, 18f), 0xFF112233);
    }

    @Test
    void pointerInputGoesToVanillaOverThePanelTheRootOrNothing() {
        final MenuPanel panel = new MenuPanel(this.host.factory(), this.owner);
        final Label label = this.host.factory().label("Grinder");
        final AbsolutePanel root = this.host.factory().absolutePanel().add(panel, 10f, 20f).add(label, 60f, 0f);
        this.host.open(ui -> root);
        this.host.layout();

        assertTrue(routes(Optional.empty(), panel), "over nothing");
        assertTrue(routes(Optional.of(root), panel), "over the root");
        assertTrue(routes(Optional.of(panel), panel), "over the panel");
        assertFalse(routes(Optional.of(label), panel), "over other Strata content");
    }

    @Test
    void overlaysKeepPointerInputInStrata() {
        final MenuPanel panel = new MenuPanel(this.host.factory(), this.owner);
        this.host.open(ui -> ui.absolutePanel().add(panel, 10f, 20f));
        this.host.layout();

        final Label popup = this.host.factory().label("Pick one");
        this.host.factory().overlays().popup(popup, new Rect(0f, 0f, 10f, 10f));
        this.host.layout();
        assertFalse(routes(Optional.of(popup), panel), "over a popup");
        assertTrue(routes(Optional.of(panel), panel), "a popup elsewhere leaves the panel to vanilla");

        this.host.factory().overlays().dialog(this.host.factory().label("Sure?"));
        this.host.layout();
        assertFalse(routes(Optional.of(panel), panel), "a modal dialog takes everything");
    }

    private static Visual item(final ItemStack stack, final ItemDecorations decorations, final boolean fake, final int seed) {
        return new ItemVisual(stack, decorations, fake, seed);
    }

    private boolean routes(final Optional<Component<?>> hovered, final MenuPanel panel) {
        return StrataContainerScreen.routesToMenu(hovered, this.host.factory().overlays(), panel);
    }

    /**
     * A menu of plain slots with no type, as a test can make without a server.
     */
    private static final class TestMenu extends AbstractContainerMenu {
        TestMenu() {
            super(null, 0);
        }

        Slot add(final Slot slot) {
            return this.addSlot(slot);
        }

        @Override
        public ItemStack quickMoveStack(final Player player, final int slotIndex) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(final Player player) {
            return true;
        }
    }

    /**
     * The screen's side of the panel, set by each test. Strata mode with nothing hovered by default.
     */
    private static final class FakeOwner implements MenuPanel.Owner {
        private final AbstractContainerMenu menu;
        private final Set<Slot> quickCraftSlots;
        private SlotRendering rendering;
        private Slot hovered;
        private boolean quickCrafting;
        private int quickCraftType;
        private int vanillaDraws;
        private final List<Slot> dropped = new ArrayList<>();

        FakeOwner(final AbstractContainerMenu menu) {
            this.menu = menu;
            this.quickCraftSlots = new HashSet<>();
            this.rendering = SlotRendering.STRATA;
        }

        @Override
        public AbstractContainerMenu menu() {
            return this.menu;
        }

        @Override
        public int imageWidth() {
            return 40;
        }

        @Override
        public int imageHeight() {
            return 30;
        }

        @Override
        public SlotRendering slotRendering() {
            return this.rendering;
        }

        @Override
        public Slot hoveredSlot() {
            return this.hovered;
        }

        @Override
        public boolean isQuickCrafting() {
            return this.quickCrafting;
        }

        @Override
        public Set<Slot> quickCraftSlots() {
            return this.quickCraftSlots;
        }

        @Override
        public int quickCraftType() {
            return this.quickCraftType;
        }

        @Override
        public void dropFromDrag(final Slot slot) {
            this.dropped.add(slot);
            this.quickCraftSlots.remove(slot);
        }

        @Override
        public void drawVanillaSlots() {
            this.vanillaDraws++;
        }
    }
}
