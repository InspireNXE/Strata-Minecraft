package org.inspirenxe.strata.minecraft.control;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.core.StrataCore;
import org.inspirenxe.strata.minecraft.MinecraftTooltips;
import org.inspirenxe.strata.minecraft.MinecraftVisuals;
import org.inspirenxe.strata.minecraft.MinecraftVisuals.ItemDecorations;
import org.inspirenxe.strata.minecraft.VanillaBootstrap;
import org.inspirenxe.strata.spi.Canvas;
import org.inspirenxe.strata.spi.Capabilities;
import org.inspirenxe.strata.spi.Clipboard;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.ScreenHost;
import org.inspirenxe.strata.spi.TextInputSink;
import org.inspirenxe.strata.spi.ThemeSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks the item display in a real Strata host over a mocked platform, with real item stacks.
 */
class ItemDisplayTest {
    private ScreenHost host;
    private List<ItemStack> asked;

    @BeforeAll
    static void bootstrap() {
        VanillaBootstrap.run();
    }

    @BeforeEach
    void setUp() {
        final Platform platform = mock(Platform.class);
        when(platform.textMetrics()).thenReturn(new TextMetrics() {
            @Override
            public float width(final TextSpan span) {
                return span.text().length() * 6f;
            }

            @Override
            public float lineHeight(final TextStyle style) {
                return 9f;
            }
        });
        when(platform.clock()).thenReturn(() -> 0L);
        when(platform.clipboard()).thenReturn(mock(Clipboard.class));
        when(platform.textInput()).thenReturn(mock(TextInputSink.class));
        when(platform.capabilities()).thenReturn(new Capabilities(true));
        when(platform.themes()).thenReturn(ThemeSource.NONE);
        this.host = StrataCore.createScreenHost(platform, Themes.flat());
        this.host.resize(100f, 100f);
        this.asked = new ArrayList<>();
    }

    /**
     * Shows the display at 10, 10 with a tooltip function that records what it is asked for.
     */
    private ItemDisplay show(final ItemDisplay display) {
        display.setTooltipFunction(stack -> {
            this.asked.add(stack);
            return MinecraftTooltips.lines(List.of(stack.getHoverName()));
        });
        this.host.open(ui -> ui.absolutePanel().add(display, 10f, 10f));
        this.host.layout();
        return display;
    }

    @Test
    void itsDesiredSizeIsASlotsItem() {
        final ItemDisplay display = this.show(new ItemDisplay(this.host.factory(), new ItemStack(Items.DIAMOND)));

        assertEquals(new Rect(10f, 10f, 16f, 16f), display.bounds());
    }

    @Test
    void itDrawsTheStacksVisualAsTheLargestCenteredSquare() {
        final ItemDisplay display = new ItemDisplay(this.host.factory(), new ItemStack(Items.DIAMOND, 3));
        display.setWidth(40f).setHeight(20f);
        this.show(display);
        final Canvas canvas = mock(Canvas.class);

        this.host.render(canvas);

        verify(canvas).visual(MinecraftVisuals.item(new ItemStack(Items.DIAMOND, 3)), new Rect(20f, 10f, 20f, 20f), 0xFFFFFFFF);
    }

    @Test
    void decorationsAndTintReachTheVisual() {
        final ItemDisplay display = this.show(new ItemDisplay(this.host.factory(), new ItemStack(Items.DIAMOND, 3))
                .setDecorations(ItemDecorations.NONE).setTint(0x80FFFFFF));
        final Canvas canvas = mock(Canvas.class);

        this.host.render(canvas);

        verify(canvas).visual(MinecraftVisuals.item(new ItemStack(Items.DIAMOND, 3), ItemDecorations.NONE),
                display.bounds(), 0x80FFFFFF);
    }

    @Test
    void emptyStacksDrawNothing() {
        this.show(new ItemDisplay(this.host.factory()));
        final Canvas canvas = mock(Canvas.class);

        this.host.render(canvas);

        verify(canvas, never()).visual(any(Visual.class), any(Rect.class), anyInt());
    }

    @Test
    void theStackIsCopiedBothWays() {
        final ItemStack stack = new ItemStack(Items.DIAMOND, 3);
        final ItemDisplay display = new ItemDisplay(this.host.factory(), stack);
        stack.setCount(5);
        display.stack().setCount(7);

        assertEquals(3, display.stack().getCount());
    }

    @Test
    void theTooltipIsMadeWhenThePointerEnters() {
        final ItemDisplay display = this.show(new ItemDisplay(this.host.factory(), new ItemStack(Items.DIAMOND)));
        assertNull(display.tooltip(), "nothing is asked before the pointer arrives");

        this.host.pointerMoved(15f, 15f, Modifiers.NONE);

        assertEquals(1, this.asked.size());
        assertEquals(MinecraftTooltips.lines(List.of(new ItemStack(Items.DIAMOND).getHoverName())), display.tooltip());
    }

    @Test
    void changingTheStackUnderThePointerRefreshesTheTooltip() {
        final ItemDisplay display = this.show(new ItemDisplay(this.host.factory(), new ItemStack(Items.DIAMOND)));
        this.host.pointerMoved(15f, 15f, Modifiers.NONE);

        display.setStack(new ItemStack(Items.EMERALD));

        assertEquals(2, this.asked.size());
        assertTrue(ItemStack.matches(new ItemStack(Items.EMERALD), this.asked.get(1)));

        display.setStack(ItemStack.EMPTY);
        assertNull(display.tooltip(), "an empty stack has no tooltip");
        assertEquals(2, this.asked.size(), "the function never sees an empty stack");
    }

    @Test
    void aTooltipWithNoLinesCountsAsNone() {
        final ItemDisplay display = this.show(new ItemDisplay(this.host.factory(), new ItemStack(Items.DIAMOND)));
        display.setTooltipFunction(stack -> Tooltip.lines(List.of()));

        this.host.pointerMoved(15f, 15f, Modifiers.NONE);

        assertNull(display.tooltip());
    }

    @Test
    void withoutAFunctionTheTooltipIsLeftAlone() {
        final ItemDisplay display = this.show(new ItemDisplay(this.host.factory(), new ItemStack(Items.DIAMOND)));
        this.host.pointerMoved(15f, 15f, Modifiers.NONE);

        display.setTooltipFunction(null);
        assertNull(display.tooltip(), "the tooltip the display made goes");

        final Tooltip own = Tooltip.text("Mine");
        display.setTooltip(own);
        this.host.pointerMoved(50f, 50f, Modifiers.NONE);
        this.host.pointerMoved(15f, 15f, Modifiers.NONE);
        assertSame(own, display.tooltip(), "a tooltip set from code stays");
    }
}
