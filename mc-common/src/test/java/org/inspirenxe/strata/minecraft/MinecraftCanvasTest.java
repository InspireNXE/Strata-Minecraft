package org.inspirenxe.strata.minecraft;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
}
