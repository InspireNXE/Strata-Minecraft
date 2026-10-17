package org.inspirenxe.strata.minecraft;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.minecraft.MinecraftVisuals.ItemDecorations;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks that the Minecraft canvas turns Strata's drawing into the right vanilla calls, against the real 26.3
 * classes with the graphics and font mocked. Rendering itself needs a running game and is not covered here.
 */
class MinecraftCanvasTest {
    private GuiGraphicsExtractor graphics;
    private Font font;
    private Matrix3x2fStack pose;
    private MinecraftCanvas canvas;

    @BeforeAll
    static void bootstrap() {
        // Item stacks need vanilla's registries and their items' default components, which a world load binds
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createWorldLookup())
                .forEach(DataComponentInitializers.PendingComponents::apply);
    }

    @BeforeEach
    void setUp() {
        this.graphics = mock(GuiGraphicsExtractor.class);
        this.font = mock(Font.class);
        this.pose = new Matrix3x2fStack(16);
        when(this.graphics.pose()).thenReturn(this.pose);
        this.canvas = new MinecraftCanvas(this.font);
        this.canvas.begin(this.graphics);
    }

    @Test
    void fillsRoundOutwardToWholePixels() {
        this.canvas.fill(new Rect(1.5f, 2.5f, 3f, 3f), 0xFFFF0000);

        verify(this.graphics).fill(1, 2, 5, 6, 0xFFFF0000);
    }

    @Test
    void transparentFillsAreSkipped() {
        this.canvas.fill(new Rect(0, 0, 10, 10), 0x00FFFFFF);

        verify(this.graphics, never()).fill(anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void gradientsKeepTheirColors() {
        this.canvas.fillGradient(new Rect(0, 0, 10, 20), 0xFF000000, 0xFFFFFFFF);

        verify(this.graphics).fillGradient(0, 0, 10, 20, 0xFF000000, 0xFFFFFFFF);
    }

    @Test
    void clipsUseTheScissorStack() {
        this.canvas.pushClip(new Rect(0.5f, 1f, 9f, 9.5f));
        this.canvas.popClip();

        verify(this.graphics).enableScissor(0, 1, 10, 11);
        verify(this.graphics).disableScissor();
    }

    @Test
    void translationsPushAndPopThePose() {
        this.canvas.pushTranslation(5f, 7f);
        final Matrix3x2f inside = new Matrix3x2f(this.pose);
        this.canvas.popTransform();

        assertEquals(5f, inside.m20(), "translated by x");
        assertEquals(7f, inside.m21(), "translated by y");
        assertTrue(this.pose.equals(new Matrix3x2f()), "popTransform restores the identity");
    }

    @Test
    void nextLayerStartsANewStratum() {
        this.canvas.nextLayer();

        verify(this.graphics).nextStratum();
    }

    @Test
    void spritesDrawFromTheGuiAtlasWithTheirTint() {
        this.canvas.sprite(SpriteId.of("minecraft", "widget/button"), new Rect(2, 3, 20, 10), 0x80FFFFFF);

        verify(this.graphics).blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath("minecraft", "widget/button"),
                2, 3, 20, 10, 0x80FFFFFF);
    }

    @Test
    void nineSlicesLeaveSlicingToTheSpriteMetadata() {
        this.canvas.nineSlice(SpriteId.of("minecraft", "widget/text_field"), new Rect(0, 0, 100, 20), Insets.of(1), 0xFFFFFFFF);

        verify(this.graphics).blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.fromNamespaceAndPath("minecraft", "widget/text_field"),
                0, 0, 100, 20, 0xFFFFFFFF);
    }

    @Test
    void textDrawsAsAStyledComponent() {
        this.canvas.text(new TextSpan("Hi", TextStyle.DEFAULT.withColor(0xFF00FF00).withBold(true).withShadow(true)), 3.4f, 4.6f);

        final ArgumentCaptor<Component> text = ArgumentCaptor.forClass(Component.class);
        verify(this.graphics).text(eq(this.font), text.capture(), eq(3), eq(5), eq(0xFF00FF00), eq(true));
        assertEquals("Hi", text.getValue().getString());
        assertTrue(text.getValue().getStyle().isBold(), "bold carries over");
    }

    @Test
    void emptyOrTransparentTextIsSkipped() {
        this.canvas.text(TextSpan.of(""), 0, 0);
        this.canvas.text(new TextSpan("x", TextStyle.DEFAULT.withColor(0x00FFFFFF)), 0, 0);

        verify(this.graphics, never()).text(any(Font.class), any(Component.class), anyInt(), anyInt(), anyInt(), anyBoolean());
    }

    @Test
    void cursorsMapToVanillaCursors() {
        this.canvas.requestCursor(CursorKind.TEXT);
        verify(this.graphics).requestCursor(CursorTypes.IBEAM);

        for (final CursorKind kind : CursorKind.values()) {
            assertTrue(MinecraftCanvas.cursorType(kind) != null, kind + " has a vanilla cursor");
        }
    }

    @Test
    void textMetricsMeasureTheStyledComponent() {
        when(this.font.width(any(FormattedText.class))).thenReturn(12);

        assertEquals(12f, this.canvas.textMetrics().width(new TextSpan("abc", TextStyle.DEFAULT.withItalic(true))));

        final ArgumentCaptor<FormattedText> measured = ArgumentCaptor.forClass(FormattedText.class);
        verify(this.font).width(measured.capture());
        assertEquals("abc", measured.getValue().getString());
    }

    @Test
    void itemVisualsDrawWithVanillaItemRenderingScaledToTheRect() {
        final ItemStack stack = new ItemStack(Items.DIAMOND, 3);
        final Matrix3x2f[] drawnWith = new Matrix3x2f[1];
        doAnswer(invocation -> drawnWith[0] = new Matrix3x2f(this.pose)).when(this.graphics).item(any(ItemStack.class), anyInt(), anyInt());

        this.canvas.visual(MinecraftVisuals.item(stack), new Rect(10.4f, 20f, 32f, 32f), 0xFFFFFFFF);

        final ArgumentCaptor<ItemStack> drawn = ArgumentCaptor.forClass(ItemStack.class);
        verify(this.graphics).item(drawn.capture(), eq(0), eq(0));
        assertTrue(ItemStack.matches(stack, drawn.getValue()), "the visual's copy of the stack is drawn");
        verify(this.graphics).itemDecorations(this.font, drawn.getValue(), 0, 0);
        verify(this.graphics, never()).fill(anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        assertEquals(10f, drawnWith[0].m20(), "x rounds to a whole pixel");
        assertEquals(20f, drawnWith[0].m21());
        assertEquals(2f, drawnWith[0].m00(), "scaled from 16 to 32 wide");
        assertEquals(2f, drawnWith[0].m11(), "scaled from 16 to 32 high");
        assertTrue(this.pose.equals(new Matrix3x2f()), "the pose is restored");
    }

    @Test
    void itemVisualsCanSkipDecorations() {
        this.canvas.visual(MinecraftVisuals.item(new ItemStack(Items.DIAMOND, 3), ItemDecorations.NONE), new Rect(0, 0, 16, 16), 0xFFFFFFFF);

        verify(this.graphics).item(any(ItemStack.class), eq(0), eq(0));
        verify(this.graphics, never()).itemDecorations(any(Font.class), any(ItemStack.class), anyInt(), anyInt());
        verify(this.graphics, never()).text(any(Font.class), anyString(), anyInt(), anyInt(), anyInt(), anyBoolean());
    }

    @Test
    void theCountAloneDrawsWhereVanillaDrawsIt() {
        when(this.font.width("12")).thenReturn(11);

        this.canvas.visual(MinecraftVisuals.item(new ItemStack(Items.DIAMOND, 12), new ItemDecorations(true, false)),
                new Rect(0, 0, 16, 16), 0xFFFFFFFF);

        verify(this.graphics).text(this.font, "12", 6, 9, 0xFFFFFFFF, true);
        verify(this.graphics, never()).itemDecorations(any(Font.class), any(ItemStack.class), anyInt(), anyInt());
    }

    @Test
    void theDamageBarAloneDrawsWhereVanillaDrawsIt() {
        final ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(sword.getMaxDamage() / 2);

        this.canvas.visual(MinecraftVisuals.item(sword, new ItemDecorations(false, true)), new Rect(0, 0, 16, 16), 0xFFFFFFFF);

        verify(this.graphics).fill(2, 13, 15, 15, 0xFF000000);
        verify(this.graphics).fill(eq(2), eq(13), eq(2 + sword.getBarWidth()), eq(14), anyInt());
    }

    @Test
    void emptyStacksAndTransparentTintsDrawNothing() {
        this.canvas.visual(MinecraftVisuals.item(ItemStack.EMPTY), new Rect(0, 0, 16, 16), 0xFFFFFFFF);
        this.canvas.visual(MinecraftVisuals.item(new ItemStack(Items.DIAMOND)), new Rect(0, 0, 16, 16), 0x00FFFFFF);

        verify(this.graphics, never()).item(any(ItemStack.class), anyInt(), anyInt());
    }

    @Test
    void otherVisualsDrawThePlaceholder() {
        final Visual foreign = () -> MinecraftVisuals.ITEM_SIZE;

        this.canvas.visual(foreign, new Rect(0, 0, 16, 16), 0xFFFFFFFF);

        verify(this.graphics).fill(0, 0, 16, 16, MinecraftCanvas.MISSING_VISUAL_COLOR);
        verify(this.graphics, never()).item(any(ItemStack.class), anyInt(), anyInt());
    }

    @Test
    void itemVisualsCopyTheStackAndCompareByMatches() {
        final ItemStack stack = new ItemStack(Items.DIAMOND, 3);
        final Visual visual = MinecraftVisuals.item(stack);
        stack.setCount(5);

        assertEquals(MinecraftVisuals.ITEM_SIZE, visual.size());
        assertEquals(MinecraftVisuals.item(new ItemStack(Items.DIAMOND, 3)), visual, "a later change to the stack does not show");
        assertEquals(MinecraftVisuals.item(new ItemStack(Items.DIAMOND, 3)).hashCode(), visual.hashCode());
        assertNotEquals(MinecraftVisuals.item(new ItemStack(Items.DIAMOND, 5)), visual, "counts differ");
        assertNotEquals(MinecraftVisuals.item(new ItemStack(Items.EMERALD, 3)), visual, "items differ");
        assertNotEquals(MinecraftVisuals.item(new ItemStack(Items.DIAMOND, 3), ItemDecorations.NONE), visual, "decorations differ");
    }
}
