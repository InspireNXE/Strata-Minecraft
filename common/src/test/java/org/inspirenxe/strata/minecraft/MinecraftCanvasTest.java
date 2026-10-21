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

import com.mojang.authlib.properties.PropertyMap;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
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
import org.inspirenxe.strata.minecraft.MinecraftVisuals.AtlasSpriteVisual;
import org.inspirenxe.strata.minecraft.MinecraftVisuals.ItemDecorations;
import org.joml.Matrix4f;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks that the Minecraft canvas turns Strata's drawing into the right vanilla calls, against the real 1.21.1
 * classes with the graphics, font and render system calls faked. Rendering itself needs a running game and is not
 * covered here.
 */
class MinecraftCanvasTest {
    private GuiGraphics graphics;
    private Font font;
    private PoseStack pose;
    private FakeBackend backend;
    private MinecraftCanvas canvas;
    private ResolvableProfile resolved;

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
            return DefaultPlayerSkin.get(offlineId(profile));
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
    void spritesDrawFromTheGuiAtlasWithTheirTint() {
        this.canvas.sprite(SpriteId.of("minecraft", "widget/button"), new Rect(2, 3, 20, 10), 0x80FFFFFF);

        final InOrder order = inOrder(this.graphics);
        order.verify(this.graphics).setColor(1f, 1f, 1f, 128 / 255f);
        order.verify(this.graphics).blitSprite(ResourceLocation.fromNamespaceAndPath("minecraft", "widget/button"), 2, 3, 20, 10);
        order.verify(this.graphics).setColor(1f, 1f, 1f, 1f);
        assertEquals(1, this.backend.blits, "blending is on and the depth test off");
    }

    @Test
    void untintedSpritesLeaveTheColorAlone() {
        this.canvas.sprite(SpriteId.of("minecraft", "widget/button"), new Rect(2, 3, 20, 10), 0xFFFFFFFF);

        verify(this.graphics, never()).setColor(anyFloat(), anyFloat(), anyFloat(), anyFloat());
    }

    @Test
    void nineSlicesLeaveSlicingToTheSpriteMetadata() {
        this.backend.scalings.put(ResourceLocation.withDefaultNamespace("widget/text_field"),
                new GuiSpriteScaling.NineSlice(200, 20, new GuiSpriteScaling.NineSlice.Border(1, 1, 1, 1)));

        this.canvas.nineSlice(SpriteId.of("minecraft", "widget/text_field"), new Rect(0, 0, 100, 20), Insets.of(1), 0xFFFFFFFF);

        verify(this.graphics).blitSprite(ResourceLocation.withDefaultNamespace("widget/text_field"), 0, 0, 100, 20);
    }

    @Test
    void nineSlicesOfPlainSpritesAreCutAtTheBorder() {
        // A 160 by 32 sprite at 16, 48 in a 256 by 256 atlas, as toast/system has no metadata
        this.backend.sprite = sprite(160, 32, 16, 48, 256, 256);

        this.canvas.nineSlice(SpriteId.of("minecraft", "toast/system"), new Rect(0, 0, 200, 40), Insets.of(4), 0xFFFFFFFF);

        final ResourceLocation atlas = this.backend.sprite.atlasLocation();
        // Corners at their size, edges and center stretched
        verify(this.graphics).blit(atlas, 0, 0, 4, 4, 16f, 48f, 4, 4, 256, 256);
        verify(this.graphics).blit(atlas, 4, 0, 192, 4, 20f, 48f, 152, 4, 256, 256);
        verify(this.graphics).blit(atlas, 196, 0, 4, 4, 172f, 48f, 4, 4, 256, 256);
        verify(this.graphics).blit(atlas, 4, 4, 192, 32, 20f, 52f, 152, 24, 256, 256);
        verify(this.graphics).blit(atlas, 196, 36, 4, 4, 172f, 76f, 4, 4, 256, 256);
        verify(this.graphics, never()).blitSprite(any(ResourceLocation.class), anyInt(), anyInt(), anyInt(), anyInt());
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
        final TextureAtlasSprite helmet = sprite(16, 16, 0, 0, 512, 512);
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
        assertNotEquals(new MinecraftVisuals.ItemVisual(new ItemStack(Items.DIAMOND, 3), ItemDecorations.ALL, true, 0), visual, "fake differs");
        assertNotEquals(new MinecraftVisuals.ItemVisual(new ItemStack(Items.DIAMOND, 3), ItemDecorations.ALL, false, 7), visual, "seeds differ");
    }

    @Test
    void fakeSlotItemsDrawAsVanillaDrawsThemWithTheirSeed() {
        this.canvas.visual(new MinecraftVisuals.ItemVisual(new ItemStack(Items.DIAMOND), ItemDecorations.ALL, true, 42),
                new Rect(0, 0, 16, 16), 0xFFFFFFFF);
        this.canvas.visual(new MinecraftVisuals.ItemVisual(new ItemStack(Items.EMERALD), ItemDecorations.ALL, false, 7),
                new Rect(0, 0, 16, 16), 0xFFFFFFFF);

        verify(this.graphics).renderFakeItem(any(ItemStack.class), eq(0), eq(0), eq(42));
        verify(this.graphics).renderItem(any(ItemStack.class), eq(0), eq(0), eq(7));
    }

    @Test
    void playerFacesDrawTheFaceAndHatFromTheSkin() {
        final PlayerSkin skin = DefaultPlayerSkin.get(UUID.randomUUID());

        this.canvas.visual(MinecraftVisuals.playerFace(skin), new Rect(10.5f, 20f, 16f, 16f), 0xFFFFFFFF);

        verify(this.graphics).blit(skin.texture(), 10, 20, 16, 16, 8f, 8f, 8, 8, 64, 64);
        verify(this.graphics).blit(skin.texture(), 10, 20, 16, 16, 40f, 8f, 8, 8, 64, 64);
    }

    @Test
    void playerFacesCanSkipTheHatAndKeepTheirTint() {
        final PlayerSkin skin = DefaultPlayerSkin.get(UUID.randomUUID());

        this.canvas.visual(MinecraftVisuals.playerFace(skin, false), new Rect(0, 0, 8, 8), 0x80FFFFFF);

        verify(this.graphics).setColor(1f, 1f, 1f, 128 / 255f);
        verify(this.graphics).blit(skin.texture(), 0, 0, 8, 8, 8f, 8f, 8, 8, 64, 64);
        verify(this.graphics, never()).blit(any(ResourceLocation.class), anyInt(), anyInt(), anyInt(), anyInt(), eq(40f),
                anyFloat(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void profileFacesLookTheSkinUpWhenDrawn() {
        final ResolvableProfile profile = named("Ana");
        final Visual face = MinecraftVisuals.playerFace(profile);

        this.canvas.visual(face, new Rect(0, 0, 8, 8), 0xFFFFFFFF);

        assertEquals(profile, this.resolved, "the canvas asks for the profile's skin");
        verify(this.graphics).blit(DefaultPlayerSkin.get(offlineId(profile)).texture(), 0, 0, 8, 8, 8f, 8f, 8, 8, 64, 64);
    }

    @Test
    void transparentOrEmptyFacesDrawNothing() {
        final PlayerSkin skin = DefaultPlayerSkin.get(UUID.randomUUID());
        this.canvas.visual(MinecraftVisuals.playerFace(skin), new Rect(0, 0, 8, 8), 0x00FFFFFF);
        this.canvas.visual(MinecraftVisuals.playerFace(skin), new Rect(0, 0, 0, 8), 0xFFFFFFFF);

        verify(this.graphics, never()).blit(any(ResourceLocation.class), anyInt(), anyInt(), anyInt(), anyInt(), anyFloat(),
                anyFloat(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void playerFaceVisualsCompareByWhatTheyDraw() {
        final PlayerSkin skin = DefaultPlayerSkin.get(UUID.randomUUID());

        assertEquals(MinecraftVisuals.PLAYER_FACE_SIZE, MinecraftVisuals.playerFace(skin).size());
        assertEquals(MinecraftVisuals.playerFace(skin), MinecraftVisuals.playerFace(skin, true));
        assertNotEquals(MinecraftVisuals.playerFace(skin), MinecraftVisuals.playerFace(skin, false), "the hat differs");
        assertEquals(MinecraftVisuals.playerFace(named("Ana")), MinecraftVisuals.playerFace(named("Ana")));
        assertNotEquals(MinecraftVisuals.playerFace(named("Ana")), MinecraftVisuals.playerFace(named("Ben")));
    }

    private static ResolvableProfile named(final String name) {
        return new ResolvableProfile(Optional.of(name), Optional.empty(), new PropertyMap());
    }

    private static UUID offlineId(final ResolvableProfile profile) {
        return UUIDUtil.createOfflinePlayerUUID(profile.name().orElse(""));
    }

    private static TextureAtlasSprite sprite(final int width, final int height, final int x, final int y,
                                             final int atlasWidth, final int atlasHeight) {
        final TextureAtlasSprite sprite = mock(TextureAtlasSprite.class);
        final SpriteContents contents = mock(SpriteContents.class);
        when(contents.width()).thenReturn(width);
        when(contents.height()).thenReturn(height);
        when(sprite.contents()).thenReturn(contents);
        when(sprite.getX()).thenReturn(x);
        when(sprite.getY()).thenReturn(y);
        when(sprite.getU0()).thenReturn((float) x / atlasWidth);
        when(sprite.getU1()).thenReturn((float) (x + width) / atlasWidth);
        when(sprite.getV0()).thenReturn((float) y / atlasHeight);
        when(sprite.getV1()).thenReturn((float) (y + height) / atlasHeight);
        when(sprite.atlasLocation()).thenReturn(ResourceLocation.withDefaultNamespace("textures/atlas/gui.png"));
        return sprite;
    }

    /**
     * Records the render system calls the canvas makes, and answers sprite lookups with stretched sprites unless a
     * test sets a scaling.
     */
    private static final class FakeBackend implements MinecraftCanvas.Backend {
        private final Map<ResourceLocation, GuiSpriteScaling> scalings = new HashMap<>();
        private final Map<TextureAtlasSprite, ResourceLocation> ids = new HashMap<>();
        private final List<CursorKind> cursors = new ArrayList<>();
        private final List<ResourceLocation> atlasLookups = new ArrayList<>();
        private TextureAtlasSprite sprite = MinecraftCanvasTest.sprite(16, 16, 0, 0, 256, 256);
        private TextureAtlasSprite atlasSprite;
        private int blits;
        private int depthClears;

        @Override
        public TextureAtlasSprite sprite(final ResourceLocation id) {
            final TextureAtlasSprite found = this.scalings.containsKey(id) ? mock(TextureAtlasSprite.class) : this.sprite;
            this.ids.put(found, id);
            return found;
        }

        @Override
        public GuiSpriteScaling scaling(final TextureAtlasSprite sprite) {
            return this.scalings.getOrDefault(this.ids.get(sprite), new GuiSpriteScaling.Stretch());
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
