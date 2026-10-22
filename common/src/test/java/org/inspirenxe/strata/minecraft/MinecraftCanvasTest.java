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

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
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
import static org.mockito.ArgumentMatchers.anyFloat;
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
    private ResolvableProfile resolved;

    @BeforeAll
    static void bootstrap() {
        // Item stacks need vanilla's registries and their items' default components, which a world load binds
        VanillaBootstrap.run();
    }

    @BeforeEach
    void setUp() {
        this.graphics = mock(GuiGraphicsExtractor.class);
        this.font = mock(Font.class);
        this.pose = new Matrix3x2fStack(16);
        when(this.graphics.pose()).thenReturn(this.pose);
        this.canvas = new MinecraftCanvas(this.font, profile -> {
            this.resolved = profile;
            return DefaultPlayerSkin.get(profile.partialProfile());
        });
        this.canvas.begin(this.graphics);
    }

    @Test
    void fillsSnapEachEdgeDownToWholePixels() {
        this.canvas.fill(new Rect(1.5f, 2.5f, 3f, 3f), 0xFFFF0000);

        verify(this.graphics).fill(1, 2, 4, 5, 0xFFFF0000);
    }

    @Test
    void fillsThinnerThanAGuiPixelSnapToDevicePixels() {
        this.canvas.begin(this.graphics, 3f);

        // A hairline one device pixel high, which whole GUI pixels would lose
        this.canvas.fill(new Rect(1f, 2f + 1f / 3f, 10f, 1f / 3f), 0xFFFF0000);

        verify(this.graphics).fill(3, 7, 33, 8, 0xFFFF0000);
        // The pose was scaled down to device pixels for the fill, then restored
        assertEquals(new Matrix3x2f(), this.pose);

        // A fill a GUI pixel or more each way still snaps to GUI pixels
        this.canvas.fill(new Rect(1.5f, 2.5f, 3f, 3f), 0xFF00FF00);
        verify(this.graphics).fill(1, 2, 4, 5, 0xFF00FF00);
    }

    @Test
    void thinGradientsSnapToDevicePixels() {
        this.canvas.begin(this.graphics, 2f);

        this.canvas.fillGradient(new Rect(0.5f, 0f, 0.5f, 4f), 0xFF000000, 0xFFFFFFFF);

        verify(this.graphics).fillGradient(1, 0, 2, 8, 0xFF000000, 0xFFFFFFFF);
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

        verify(this.graphics).enableScissor(0, 1, 9, 10);
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
    void theAscentIsVanillasBaseline() {
        // Vanilla places every font's glyphs on a baseline 7 below the top of the 9-pixel line
        assertEquals(7f, this.canvas.textMetrics().ascent(TextStyle.DEFAULT));
        assertEquals(9f, this.canvas.textMetrics().lineHeight(TextStyle.DEFAULT));
    }

    @Test
    void itemVisualsDrawWithVanillaItemRenderingScaledToTheRect() {
        final ItemStack stack = new ItemStack(Items.DIAMOND, 3);
        final Matrix3x2f[] drawnWith = new Matrix3x2f[1];
        doAnswer(invocation -> drawnWith[0] = new Matrix3x2f(this.pose)).when(this.graphics).item(any(ItemStack.class), anyInt(), anyInt(), anyInt());

        this.canvas.visual(MinecraftVisuals.item(stack), new Rect(10.4f, 20f, 32f, 32f), 0xFFFFFFFF);

        final ArgumentCaptor<ItemStack> drawn = ArgumentCaptor.forClass(ItemStack.class);
        verify(this.graphics).item(drawn.capture(), eq(0), eq(0), eq(0));
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

        verify(this.graphics).item(any(ItemStack.class), eq(0), eq(0), eq(0));
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

        verify(this.graphics, never()).item(any(ItemStack.class), anyInt(), anyInt(), anyInt());
    }

    @Test
    void otherVisualsDrawThePlaceholder() {
        final Visual foreign = () -> MinecraftVisuals.ITEM_SIZE;

        this.canvas.visual(foreign, new Rect(0, 0, 16, 16), 0xFFFFFFFF);

        verify(this.graphics).fill(0, 0, 16, 16, MinecraftCanvas.MISSING_VISUAL_COLOR);
        verify(this.graphics, never()).item(any(ItemStack.class), anyInt(), anyInt(), anyInt());
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
        assertNotEquals(new MinecraftVisuals.ItemVisual(new ItemStack(Items.DIAMOND, 3), ItemDecorations.ALL, true, 0), visual, "fake differs");
        assertNotEquals(new MinecraftVisuals.ItemVisual(new ItemStack(Items.DIAMOND, 3), ItemDecorations.ALL, false, 7), visual, "seeds differ");
    }

    @Test
    void fakeSlotItemsDrawAsVanillaDrawsThemWithTheirSeed() {
        this.canvas.visual(new MinecraftVisuals.ItemVisual(new ItemStack(Items.DIAMOND), ItemDecorations.ALL, true, 42),
                new Rect(0, 0, 16, 16), 0xFFFFFFFF);
        this.canvas.visual(new MinecraftVisuals.ItemVisual(new ItemStack(Items.EMERALD), ItemDecorations.ALL, false, 7),
                new Rect(0, 0, 16, 16), 0xFFFFFFFF);

        verify(this.graphics).fakeItem(any(ItemStack.class), eq(0), eq(0), eq(42));
        verify(this.graphics).item(any(ItemStack.class), eq(0), eq(0), eq(7));
    }

    @Test
    void playerFacesDrawTheFaceAndHatFromTheSkin() {
        final PlayerSkin skin = DefaultPlayerSkin.getDefaultSkin();
        final Identifier texture = skin.body().texturePath();

        this.canvas.visual(MinecraftVisuals.playerFace(skin), new Rect(10.5f, 20f, 16f, 16f), 0xFFFFFFFF);

        verify(this.graphics).blit(RenderPipelines.GUI_TEXTURED, texture, 10, 20, 8f, 8f, 16, 16, 8, 8, 64, 64, 0xFFFFFFFF);
        verify(this.graphics).blit(RenderPipelines.GUI_TEXTURED, texture, 10, 20, 40f, 8f, 16, 16, 8, 8, 64, 64, 0xFFFFFFFF);
    }

    @Test
    void playerFacesCanSkipTheHatAndKeepTheirTint() {
        final PlayerSkin skin = DefaultPlayerSkin.getDefaultSkin();

        this.canvas.visual(MinecraftVisuals.playerFace(skin, false), new Rect(0, 0, 8, 8), 0x80FFFFFF);

        verify(this.graphics).blit(RenderPipelines.GUI_TEXTURED, skin.body().texturePath(), 0, 0, 8f, 8f, 8, 8, 8, 8, 64, 64, 0x80FFFFFF);
        verify(this.graphics, never()).blit(any(), any(Identifier.class), anyInt(), anyInt(), eq(40f), anyFloat(),
                anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void profileFacesLookTheSkinUpWhenDrawn() {
        final ResolvableProfile profile = ResolvableProfile.createUnresolved("Ana");
        final Visual face = MinecraftVisuals.playerFace(profile);

        this.canvas.visual(face, new Rect(0, 0, 8, 8), 0xFFFFFFFF);

        assertEquals(profile, this.resolved, "the canvas asks for the profile's skin");
        verify(this.graphics).blit(RenderPipelines.GUI_TEXTURED, DefaultPlayerSkin.get(profile.partialProfile()).body().texturePath(),
                0, 0, 8f, 8f, 8, 8, 8, 8, 64, 64, 0xFFFFFFFF);
    }

    @Test
    void transparentOrEmptyFacesDrawNothing() {
        this.canvas.visual(MinecraftVisuals.playerFace(DefaultPlayerSkin.getDefaultSkin()), new Rect(0, 0, 8, 8), 0x00FFFFFF);
        this.canvas.visual(MinecraftVisuals.playerFace(DefaultPlayerSkin.getDefaultSkin()), new Rect(0, 0, 0, 8), 0xFFFFFFFF);

        verify(this.graphics, never()).blit(any(), any(Identifier.class), anyInt(), anyInt(), anyFloat(), anyFloat(),
                anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void playerFaceVisualsCompareByWhatTheyDraw() {
        final PlayerSkin skin = DefaultPlayerSkin.getDefaultSkin();

        assertEquals(MinecraftVisuals.PLAYER_FACE_SIZE, MinecraftVisuals.playerFace(skin).size());
        assertEquals(MinecraftVisuals.playerFace(skin), MinecraftVisuals.playerFace(skin, true));
        assertNotEquals(MinecraftVisuals.playerFace(skin), MinecraftVisuals.playerFace(skin, false), "the hat differs");
        assertEquals(MinecraftVisuals.playerFace(ResolvableProfile.createUnresolved("Ana")),
                MinecraftVisuals.playerFace(ResolvableProfile.createUnresolved("Ana")));
        assertNotEquals(MinecraftVisuals.playerFace(ResolvableProfile.createUnresolved("Ana")),
                MinecraftVisuals.playerFace(ResolvableProfile.createUnresolved("Ben")));
    }
}
