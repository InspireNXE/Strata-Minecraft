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

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.minecraft.MinecraftVisuals.AtlasSpriteVisual;
import org.inspirenxe.strata.minecraft.MinecraftVisuals.ItemDecorations;
import org.joml.Matrix4f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks that the Minecraft canvas turns Strata's drawing into the right vanilla calls, against the real 1.20.1
 * classes with the graphics, font and render system calls faked. Rendering itself needs a running game and is not
 * covered here.
 */
class MinecraftCanvasTest {
    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");
    private static final ResourceLocation TOASTS = new ResourceLocation("textures/gui/toasts.png");

    private GuiGraphics graphics;
    private Font font;
    private PoseStack pose;
    private FakeBackend backend;
    private MinecraftCanvas canvas;
    private GameProfile resolved;

    @BeforeAll
    static void bootstrap() {
        // Item stacks need vanilla's registries
        VanillaBootstrap.run();
    }

    @BeforeEach
    void setUp() {
        this.graphics = mock(GuiGraphics.class);
        this.font = mock(Font.class);
        this.pose = new PoseStack();
        when(this.graphics.pose()).thenReturn(this.pose);
        this.backend = new FakeBackend();
        this.canvas = new MinecraftCanvas(this.font, profile -> {
            this.resolved = profile;
            return DefaultPlayerSkin.getDefaultSkin(UUIDUtil.getOrCreatePlayerUUID(profile));
        }, this.backend);
        this.canvas.begin(this.graphics);
    }

    @Test
    void fillsSnapEachEdgeDownToWholePixelsAndIgnoreDepth() {
        this.canvas.fill(new Rect(1.5f, 2.5f, 3f, 3f), 0xFFFF0000);

        verify(this.graphics).fill(RenderType.guiOverlay(), 1, 2, 4, 5, 0xFFFF0000);
    }

    @Test
    void transparentFillsAreSkipped() {
        this.canvas.fill(new Rect(0, 0, 10, 10), 0x00FFFFFF);

        verify(this.graphics, never()).fill(any(RenderType.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void gradientsKeepTheirColors() {
        this.canvas.fillGradient(new Rect(0, 0, 10, 20), 0xFF000000, 0xFFFFFFFF);

        verify(this.graphics).fillGradient(RenderType.guiOverlay(), 0, 0, 10, 20, 0xFF000000, 0xFFFFFFFF, 0);
    }

    @Test
    void clipsUseTheScissorStack() {
        this.canvas.pushClip(new Rect(0.5f, 1f, 9f, 9.5f));
        this.canvas.popClip();

        verify(this.graphics).enableScissor(0, 1, 9, 10);
        verify(this.graphics).disableScissor();
    }

    @Test
    void clipsFollowTheTranslationAndScale() {
        this.canvas.pushTranslation(5f, 7f);
        this.canvas.pushScale(2f);
        this.canvas.pushClip(new Rect(1f, 1f, 10f, 4f));

        verify(this.graphics).enableScissor(7, 9, 27, 17);
    }

    @Test
    void translationsPushAndPopThePose() {
        this.canvas.pushTranslation(5f, 7f);
        final Matrix4f inside = new Matrix4f(this.pose.last().pose());
        this.canvas.popTransform();

        assertEquals(5f, inside.m30(), "translated by x");
        assertEquals(7f, inside.m31(), "translated by y");
        assertTrue(this.pose.clear(), "popTransform restores the base pose");
    }

    @Test
    void nextLayerFlushesAndClearsTheDepth() {
        this.canvas.nextLayer();

        verify(this.graphics).flush();
        assertEquals(1, this.backend.depthClears);
    }

    @Test
    void buttonSpritesDrawFromTheWidgetSheetAsVanillaSlicesThem() {
        this.canvas.sprite(SpriteId.of("minecraft", "widget/button"), new Rect(2, 3, 20, 10), 0x80FFFFFF);

        final InOrder order = inOrder(this.graphics);
        order.verify(this.graphics).setColor(1f, 1f, 1f, 128 / 255f);
        order.verify(this.graphics).blitNineSliced(WIDGETS, 2, 3, 20, 10, 20, 4, 20, 4, 200, 20, 0, 66);
        order.verify(this.graphics).setColor(1f, 1f, 1f, 1f);
        assertEquals(1, this.backend.blits, "blending is on and the depth test off");
    }

    @Test
    void untintedSpritesLeaveTheColorAlone() {
        this.canvas.sprite(SpriteId.of("minecraft", "widget/button_highlighted"), new Rect(2, 3, 20, 10), 0xFFFFFFFF);

        verify(this.graphics).blitNineSliced(WIDGETS, 2, 3, 20, 10, 20, 4, 20, 4, 200, 20, 0, 86);
        verify(this.graphics, never()).setColor(anyFloat(), anyFloat(), anyFloat(), anyFloat());
    }

    @Test
    void checkboxesStretchTheirCell() {
        this.canvas.sprite(SpriteId.of("minecraft", "widget/checkbox_selected"), new Rect(0, 0, 17, 17), 0xFFFFFFFF);

        verify(this.graphics).blit(new ResourceLocation("textures/gui/checkbox.png"), 0, 0, 17, 17, 0f, 20f, 20, 20, 64, 64);
    }

    @Test
    void textFieldsDrawAsVanillaFillsThem() {
        this.canvas.sprite(SpriteId.of("minecraft", "widget/text_field"), new Rect(0, 0, 100, 20), 0xFFFFFFFF);

        final InOrder order = inOrder(this.graphics);
        order.verify(this.graphics).fill(RenderType.guiOverlay(), 0, 0, 100, 20, 0xFFA0A0A0);
        order.verify(this.graphics).fill(RenderType.guiOverlay(), 1, 1, 99, 19, 0xFF000000);
    }

    @Test
    void fillsTakeTheTint() {
        this.canvas.nineSlice(SpriteId.of("minecraft", "widget/scroller"), new Rect(0, 0, 6, 40), Insets.of(1f), 0x80FFFFFF);

        verify(this.graphics).fill(RenderType.guiOverlay(), 0, 0, 6, 40, 0x80808080);
        verify(this.graphics).fill(RenderType.guiOverlay(), 0, 0, 5, 39, 0x80C0C0C0);
        assertEquals(0x80408040, MinecraftCanvas.multiply(0xFF80FF80, 0x80808080));
    }

    @Test
    void nineSlicesOfVanillasSlicedRegionsIgnoreTheBorder() {
        this.canvas.nineSlice(SpriteId.of("minecraft", "widget/tab_selected"), new Rect(0, 0, 60, 24), Insets.of(9f), 0xFFFFFFFF);

        verify(this.graphics).blitNineSliced(new ResourceLocation("textures/gui/tab_button.png"), 0, 0, 60, 24, 2, 2, 2, 0, 130, 24, 0, 0);
    }

    @Test
    void nineSlicesOfOtherRegionsAreCutAtTheBorder() {
        this.canvas.nineSlice(SpriteId.of("minecraft", "toast/system"), new Rect(0, 0, 200, 40), Insets.of(4f), 0xFFFFFFFF);

        // The toast is 160 by 32 at 0, 64 in a 256 by 256 sheet; corners at their size, edges and center stretched
        verify(this.graphics).blit(TOASTS, 0, 0, 4, 4, 0f, 64f, 4, 4, 256, 256);
        verify(this.graphics).blit(TOASTS, 4, 0, 192, 4, 4f, 64f, 152, 4, 256, 256);
        verify(this.graphics).blit(TOASTS, 4, 4, 192, 32, 4f, 68f, 152, 24, 256, 256);
        verify(this.graphics).blit(TOASTS, 196, 36, 4, 4, 156f, 92f, 4, 4, 256, 256);
    }

    @Test
    void otherSpritesArePlainTextures() {
        this.canvas.sprite(SpriteId.of("examplemod", "panel"), new Rect(1, 2, 30, 40), 0xFFFFFFFF);

        verify(this.graphics).blit(new ResourceLocation("examplemod", "textures/gui/sprites/panel.png"), 1, 2, 30, 40,
                0f, 0f, 1, 1, 1, 1);
    }

    @Test
    void nineSlicesOfPlainTexturesAreCutAtTheirImageSize() {
        final ResourceLocation texture = new ResourceLocation("strata", "textures/gui/sprites/tooltip/background.png");
        this.backend.sizes.put(texture, new int[] {3, 3});

        this.canvas.nineSlice(SpriteId.of("strata", "tooltip/background"), new Rect(0, 0, 50, 20), Insets.of(1f), 0xFFFFFFFF);
        this.canvas.nineSlice(SpriteId.of("strata", "tooltip/background"), new Rect(0, 0, 50, 20), Insets.of(1f), 0xFFFFFFFF);

        verify(this.graphics, times(2)).blit(texture, 0, 0, 1, 1, 0f, 0f, 1, 1, 3, 3);
        verify(this.graphics, times(2)).blit(texture, 1, 1, 48, 18, 1f, 1f, 1, 1, 3, 3);
        verify(this.graphics, times(2)).blit(texture, 49, 19, 1, 1, 2f, 2f, 1, 1, 3, 3);
        assertEquals(List.of(texture), this.backend.sizeLookups, "the size is read once");
    }

    @Test
    void aMissingTextureStretchesAsAPlainSprite() {
        this.canvas.nineSlice(SpriteId.of("examplemod", "missing"), new Rect(0, 0, 50, 20), Insets.of(4f), 0xFFFFFFFF);

        verify(this.graphics).blit(new ResourceLocation("examplemod", "textures/gui/sprites/missing.png"), 0, 0, 50, 20,
                0f, 0f, 1, 1, 1, 1);
    }

    @Test
    void imageSizesComeFromThePngHeader() throws IOException {
        final byte[] header = {
                (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
                0, 0, 0, 13, 'I', 'H', 'D', 'R',
                0, 0, 0, (byte) 160, 0, 0, 0, 32
        };

        assertArrayEquals(new int[] {160, 32}, MinecraftCanvas.pngSize(new ByteArrayInputStream(header)).orElseThrow());
        assertTrue(MinecraftCanvas.pngSize(new ByteArrayInputStream(new byte[24])).isEmpty(), "not a PNG");
    }

    @Test
    void bordersWiderThanTheRectangleShrink() {
        assertArrayEquals(new int[] {4, 152, 4, 4, 192, 4}, MinecraftCanvas.slices(4, 4, 160, 200));
        assertArrayEquals(new int[] {4, 152, 4, 3, 0, 3}, MinecraftCanvas.slices(4, 4, 160, 6));
        assertArrayEquals(new int[] {6, 0, 4, 6, 10, 4}, MinecraftCanvas.slices(6, 8, 10, 20));
    }

    @Test
    void textDrawsAsAStyledComponent() {
        this.canvas.text(new TextSpan("Hi", TextStyle.DEFAULT.withColor(0xFF00FF00).withBold(true).withShadow(true)), 3.4f, 4.6f);

        final ArgumentCaptor<Component> text = ArgumentCaptor.forClass(Component.class);
        verify(this.graphics).drawString(eq(this.font), text.capture(), eq(3), eq(5), eq(0xFF00FF00), eq(true));
        assertEquals("Hi", text.getValue().getString());
        assertTrue(text.getValue().getStyle().isBold(), "bold carries over");
    }

    @Test
    void emptyOrTransparentTextIsSkipped() {
        this.canvas.text(TextSpan.of(""), 0, 0);
        this.canvas.text(new TextSpan("x", TextStyle.DEFAULT.withColor(0x00FFFFFF)), 0, 0);

        verify(this.graphics, never()).drawString(any(Font.class), any(Component.class), anyInt(), anyInt(), anyInt(), anyBoolean());
    }

    @Test
    void theCursorAFrameAsksForIsShownWhenItEnds() {
        this.canvas.requestCursor(CursorKind.TEXT);
        assertEquals(List.of(), this.backend.cursors, "nothing changes before the frame ends");
        this.canvas.end();

        this.canvas.begin(this.graphics);
        this.canvas.end();
        this.canvas.resetCursor();

        assertEquals(List.of(CursorKind.TEXT, CursorKind.ARROW, CursorKind.ARROW), this.backend.cursors,
                "a frame that asks for nothing shows the arrow");
    }

    @Test
    void cursorsMapToGlfwShapes() {
        assertEquals(GLFW.GLFW_IBEAM_CURSOR, MinecraftCanvas.cursorShape(CursorKind.TEXT));
        assertEquals(GLFW.GLFW_POINTING_HAND_CURSOR, MinecraftCanvas.cursorShape(CursorKind.HAND));
        final Set<Integer> shapes = new HashSet<>();
        for (final CursorKind kind : CursorKind.values()) {
            assertTrue(shapes.add(MinecraftCanvas.cursorShape(kind)), kind + " has a shape of its own");
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
        final Matrix4f[] drawnWith = new Matrix4f[1];
        doAnswer(invocation -> drawnWith[0] = new Matrix4f(this.pose.last().pose()))
                .when(this.graphics).renderItem(any(ItemStack.class), anyInt(), anyInt(), anyInt());

        this.canvas.visual(MinecraftVisuals.item(stack), new Rect(10.4f, 20f, 32f, 32f), 0xFFFFFFFF);

        final ArgumentCaptor<ItemStack> drawn = ArgumentCaptor.forClass(ItemStack.class);
        verify(this.graphics).renderItem(drawn.capture(), eq(0), eq(0), eq(0));
        assertTrue(ItemStack.matches(stack, drawn.getValue()), "the visual's copy of the stack is drawn");
        verify(this.graphics).renderItemDecorations(this.font, drawn.getValue(), 0, 0);
        verify(this.graphics, never()).fill(any(RenderType.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        assertEquals(10f, drawnWith[0].m30(), "x rounds to a whole pixel");
        assertEquals(20f, drawnWith[0].m31());
        assertEquals(2f, drawnWith[0].m00(), "scaled from 16 to 32 wide");
        assertEquals(2f, drawnWith[0].m11(), "scaled from 16 to 32 high");
        assertTrue(this.pose.clear(), "the pose is restored");
        assertEquals(1, this.backend.depthClears, "the item's depth is cleared, so later drawing covers it");
    }

    @Test
    void itemVisualsCanSkipDecorations() {
        this.canvas.visual(MinecraftVisuals.item(new ItemStack(Items.DIAMOND, 3), ItemDecorations.NONE), new Rect(0, 0, 16, 16), 0xFFFFFFFF);

        verify(this.graphics).renderItem(any(ItemStack.class), eq(0), eq(0), eq(0));
        verify(this.graphics, never()).renderItemDecorations(any(Font.class), any(ItemStack.class), anyInt(), anyInt());
        verify(this.graphics, never()).drawString(any(Font.class), anyString(), anyInt(), anyInt(), anyInt(), anyBoolean());
    }

    @Test
    void theCountAloneDrawsWhereVanillaDrawsIt() {
        when(this.font.width("12")).thenReturn(11);

        this.canvas.visual(MinecraftVisuals.item(new ItemStack(Items.DIAMOND, 12), new ItemDecorations(true, false)),
                new Rect(0, 0, 16, 16), 0xFFFFFFFF);

        verify(this.graphics).drawString(this.font, "12", 6, 9, 0xFFFFFFFF, true);
        verify(this.graphics, never()).renderItemDecorations(any(Font.class), any(ItemStack.class), anyInt(), anyInt());
    }

    @Test
    void theDamageBarAloneDrawsWhereVanillaDrawsIt() {
        final ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(sword.getMaxDamage() / 2);

        this.canvas.visual(MinecraftVisuals.item(sword, new ItemDecorations(false, true)), new Rect(0, 0, 16, 16), 0xFFFFFFFF);

        verify(this.graphics).fill(RenderType.guiOverlay(), 2, 13, 15, 15, 0xFF000000);
        verify(this.graphics).fill(eq(RenderType.guiOverlay()), eq(2), eq(13), eq(2 + sword.getBarWidth()), eq(14), anyInt());
    }

    @Test
    void emptyStacksAndTransparentTintsDrawNothing() {
        this.canvas.visual(MinecraftVisuals.item(ItemStack.EMPTY), new Rect(0, 0, 16, 16), 0xFFFFFFFF);
        this.canvas.visual(MinecraftVisuals.item(new ItemStack(Items.DIAMOND)), new Rect(0, 0, 16, 16), 0x00FFFFFF);

        verify(this.graphics, never()).renderItem(any(ItemStack.class), anyInt(), anyInt(), anyInt());
    }

    @Test
    void otherVisualsDrawThePlaceholder() {
        final Visual foreign = () -> MinecraftVisuals.ITEM_SIZE;

        this.canvas.visual(foreign, new Rect(0, 0, 16, 16), 0xFFFFFFFF);

        verify(this.graphics).fill(RenderType.guiOverlay(), 0, 0, 16, 16, MinecraftCanvas.MISSING_VISUAL_COLOR);
        verify(this.graphics, never()).renderItem(any(ItemStack.class), anyInt(), anyInt(), anyInt());
    }

    @Test
    void atlasSpritesDrawFromTheirAtlas() {
        final TextureAtlasSprite helmet = mock(TextureAtlasSprite.class);
        this.backend.atlasSprite = helmet;

        this.canvas.visual(new AtlasSpriteVisual(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET),
                new Rect(4, 5, 16, 16), 0xFFFFFFFF);

        assertEquals(List.of(InventoryMenu.BLOCK_ATLAS, InventoryMenu.EMPTY_ARMOR_SLOT_HELMET), this.backend.atlasLookups);
        verify(this.graphics).blit(4, 5, 0, 16, 16, helmet);
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
        assertNotEquals(new MinecraftVisuals.ItemVisual(new ItemStack(Items.DIAMOND, 3), ItemDecorations.ALL, 7), visual, "seeds differ");
    }

    @Test
    void itemsDrawWithTheirSeed() {
        this.canvas.visual(new MinecraftVisuals.ItemVisual(new ItemStack(Items.EMERALD), ItemDecorations.ALL, 7),
                new Rect(0, 0, 16, 16), 0xFFFFFFFF);

        verify(this.graphics).renderItem(any(ItemStack.class), eq(0), eq(0), eq(7));
    }

    @Test
    void playerFacesDrawTheFaceAndHatFromTheSkin() {
        final ResourceLocation skin = DefaultPlayerSkin.getDefaultSkin(UUID.randomUUID());

        this.canvas.visual(MinecraftVisuals.playerFace(skin), new Rect(10.5f, 20f, 16f, 16f), 0xFFFFFFFF);

        verify(this.graphics).blit(skin, 10, 20, 16, 16, 8f, 8f, 8, 8, 64, 64);
        verify(this.graphics).blit(skin, 10, 20, 16, 16, 40f, 8f, 8, 8, 64, 64);
    }

    @Test
    void playerFacesCanSkipTheHatAndKeepTheirTint() {
        final ResourceLocation skin = DefaultPlayerSkin.getDefaultSkin(UUID.randomUUID());

        this.canvas.visual(MinecraftVisuals.playerFace(skin, false), new Rect(0, 0, 8, 8), 0x80FFFFFF);

        verify(this.graphics).setColor(1f, 1f, 1f, 128 / 255f);
        verify(this.graphics).blit(skin, 0, 0, 8, 8, 8f, 8f, 8, 8, 64, 64);
        verify(this.graphics, never()).blit(any(ResourceLocation.class), anyInt(), anyInt(), anyInt(), anyInt(), eq(40f),
                anyFloat(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void profileFacesLookTheSkinUpWhenDrawn() {
        final GameProfile profile = new GameProfile(null, "Ana");
        final Visual face = MinecraftVisuals.playerFace(profile);

        this.canvas.visual(face, new Rect(0, 0, 8, 8), 0xFFFFFFFF);

        assertEquals(profile, this.resolved, "the canvas asks for the profile's skin");
        verify(this.graphics).blit(DefaultPlayerSkin.getDefaultSkin(UUIDUtil.getOrCreatePlayerUUID(profile)), 0, 0, 8, 8,
                8f, 8f, 8, 8, 64, 64);
    }

    @Test
    void transparentOrEmptyFacesDrawNothing() {
        final ResourceLocation skin = DefaultPlayerSkin.getDefaultSkin();
        this.canvas.visual(MinecraftVisuals.playerFace(skin), new Rect(0, 0, 8, 8), 0x00FFFFFF);
        this.canvas.visual(MinecraftVisuals.playerFace(skin), new Rect(0, 0, 0, 8), 0xFFFFFFFF);

        verify(this.graphics, never()).blit(any(ResourceLocation.class), anyInt(), anyInt(), anyInt(), anyInt(), anyFloat(),
                anyFloat(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void playerFaceVisualsCompareByWhatTheyDraw() {
        final ResourceLocation skin = DefaultPlayerSkin.getDefaultSkin();

        assertEquals(MinecraftVisuals.PLAYER_FACE_SIZE, MinecraftVisuals.playerFace(skin).size());
        assertEquals(MinecraftVisuals.playerFace(skin), MinecraftVisuals.playerFace(skin, true));
        assertNotEquals(MinecraftVisuals.playerFace(skin), MinecraftVisuals.playerFace(skin, false), "the hat differs");
        assertEquals(MinecraftVisuals.playerFace(new GameProfile(null, "Ana")), MinecraftVisuals.playerFace(new GameProfile(null, "Ana")));
        assertNotEquals(MinecraftVisuals.playerFace(new GameProfile(null, "Ana")), MinecraftVisuals.playerFace(new GameProfile(null, "Ben")));
    }

    /**
     * Records the render system calls the canvas makes, and answers image sizes from a map.
     */
    private static final class FakeBackend implements MinecraftCanvas.Backend {
        private final Map<ResourceLocation, int[]> sizes = new HashMap<>();
        private final List<ResourceLocation> sizeLookups = new ArrayList<>();
        private final List<CursorKind> cursors = new ArrayList<>();
        private final List<ResourceLocation> atlasLookups = new ArrayList<>();
        private TextureAtlasSprite atlasSprite;
        private int blits;
        private int depthClears;

        @Override
        public Optional<int[]> textureSize(final ResourceLocation texture) {
            this.sizeLookups.add(texture);
            return Optional.ofNullable(this.sizes.get(texture));
        }

        @Override
        public TextureAtlasSprite atlasSprite(final ResourceLocation atlas, final ResourceLocation sprite) {
            this.atlasLookups.add(atlas);
            this.atlasLookups.add(sprite);
            return this.atlasSprite;
        }

        @Override
        public void prepareBlit() {
            this.blits++;
        }

        @Override
        public void clearDepth() {
            this.depthClears++;
        }

        @Override
        public void setCursor(final CursorKind cursor) {
            this.cursors.add(cursor);
        }
    }
}
