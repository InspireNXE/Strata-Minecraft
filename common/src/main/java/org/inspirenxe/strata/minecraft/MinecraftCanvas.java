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

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.minecraft.MinecraftVisuals.AtlasSpriteVisual;
import org.inspirenxe.strata.minecraft.MinecraftVisuals.ItemDecorations;
import org.inspirenxe.strata.minecraft.MinecraftVisuals.ItemVisual;
import org.inspirenxe.strata.minecraft.MinecraftVisuals.ProfileFaceVisual;
import org.inspirenxe.strata.minecraft.MinecraftVisuals.SkinFaceVisual;
import org.inspirenxe.strata.spi.Canvas;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * The Minecraft {@link Canvas}: draws Strata's drawing through vanilla's {@link GuiGraphics} as it is called.
 *
 * <p>How it meets the canvas contract on Minecraft 1.21.1:</p>
 * <ul>
 *     <li>Minecraft draws in whole GUI pixels. Each edge of a fill, sprite or clip moves down to the whole pixel it
 *     lies in, as vanilla's integer coordinates would put it, so a whole-sized rectangle keeps its size wherever it
 *     sits; text positions round to the nearest pixel.</li>
 *     <li>Later drawing covers earlier drawing. Fills use vanilla's {@code gui_overlay} render type and sprites draw
 *     with the depth test off, so neither is hidden by the depth vanilla's text and items leave. After each item,
 *     and at each {@link #nextLayer()}, the canvas clears the depth buffer inside the current clip.</li>
 *     <li>Sprites come from the {@code minecraft:gui} atlas, which reads {@code assets/<namespace>/textures/gui/
 *     sprites/<path>.png}. The tint is passed as vanilla's shader color.</li>
 *     <li>A nine-slice whose sprite has a {@code .png.mcmeta} scaling draws as a plain sprite, which vanilla scales
 *     as the file says, and the border argument is ignored. A sprite without one is cut into nine pieces at the
 *     border, its corners at their size and the rest stretched. A missing sprite draws vanilla's missing-texture
 *     pattern.</li>
 *     <li>Text draws as a chat component in the player's font, with vanilla's bold, italic, underline,
 *     strikethrough and shadow.</li>
 *     <li>Clips use vanilla's scissor stack, which intersects nested clips. Vanilla's scissor ignores the pose, so
 *     the canvas moves each clip by the current translation and scale first. Translations and scales push and pop
 *     the pose.</li>
 *     <li>Item visuals draw with vanilla's GUI item rendering under a pose scaled to the rectangle. Player face
 *     visuals draw the face and hat from the skin texture, stretched over the rectangle snapped to whole pixels and
 *     tinted. Visuals made elsewhere draw as a magenta placeholder.</li>
 *     <li>1.21.1 has no cursor API, so cursors are set on the window through GLFW's standard cursors. A cursor the
 *     system lacks shows as the arrow.</li>
 * </ul>
 *
 * <p>Call {@link #begin(GuiGraphics)} at the start of every frame and {@link #end()} after it, and
 * {@link #resetCursor()} when the screen goes away. Use it on the render thread only.</p>
 */
public final class MinecraftCanvas implements Canvas {
    /**
     * What a visual this canvas cannot draw shows as.
     */
    public static final int MISSING_VISUAL_COLOR = 0xFFFF00FF;
    private static final int WHITE = 0xFFFFFFFF;

    private final Map<SpriteId, ResourceLocation> identifiers;
    private final MinecraftTextMetrics textMetrics;
    private final Function<ResolvableProfile, PlayerSkin> skins;
    private final Backend backend;
    private final Vector3f corner;
    private GuiGraphics graphics;
    private CursorKind cursor;

    /**
     * Creates a canvas.
     *
     * @param font the font text is drawn and measured with, usually the screen's
     */
    public MinecraftCanvas(final Font font) {
        this(font, MinecraftCanvas::skinOf, Backend.VANILLA);
    }

    /**
     * Creates a canvas that looks player skins up with the given function and reaches the render system through
     * the given backend, for tests.
     *
     * @param font the font text is drawn and measured with
     * @param skins returns the skin to draw for a profile, at once
     * @param backend the render system calls the graphics do not cover
     */
    MinecraftCanvas(final Font font, final Function<ResolvableProfile, PlayerSkin> skins, final Backend backend) {
        this.identifiers = new HashMap<>();
        this.textMetrics = new MinecraftTextMetrics(font);
        this.skins = skins;
        this.backend = backend;
        this.corner = new Vector3f();
        this.cursor = CursorKind.ARROW;
    }

    /**
     * Points the canvas at this frame's graphics.
     *
     * @param graphics the graphics vanilla passed to the screen's {@code render}
     */
    public void begin(final GuiGraphics graphics) {
        this.graphics = graphics;
        this.cursor = CursorKind.ARROW;
    }

    /**
     * Ends the frame: shows the cursor the frame asked for, or the arrow if it asked for none.
     */
    public void end() {
        this.backend.setCursor(this.cursor);
    }

    /**
     * Shows the arrow cursor again, for when the screen closes.
     */
    public void resetCursor() {
        this.cursor = CursorKind.ARROW;
        this.backend.setCursor(CursorKind.ARROW);
    }

    /**
     * Returns the text metrics matching what this canvas draws.
     *
     * @return the metrics, for the platform
     */
    public MinecraftTextMetrics textMetrics() {
        return this.textMetrics;
    }

    @Override
    public void fill(final Rect rect, final int argb) {
        if ((argb >>> 24) == 0) {
            return;
        }
        this.graphics.fill(RenderType.guiOverlay(), left(rect), top(rect), right(rect), bottom(rect), argb);
    }

    @Override
    public void fillGradient(final Rect rect, final int topArgb, final int bottomArgb) {
        this.graphics.fillGradient(RenderType.guiOverlay(), left(rect), top(rect), right(rect), bottom(rect),
                topArgb, bottomArgb, 0);
    }

    @Override
    public void sprite(final SpriteId sprite, final Rect rect, final int tint) {
        if ((tint >>> 24) == 0) {
            return;
        }
        this.beginTint(tint);
        try {
            this.graphics.blitSprite(this.identifier(sprite), left(rect), top(rect), right(rect) - left(rect),
                    bottom(rect) - top(rect));
        } finally {
            this.endTint(tint);
        }
    }

    @Override
    public void nineSlice(final SpriteId sprite, final Rect rect, final Insets border, final int tint) {
        final ResourceLocation id = this.identifier(sprite);
        final TextureAtlasSprite atlasSprite = this.backend.sprite(id);
        if (!(this.backend.scaling(atlasSprite) instanceof GuiSpriteScaling.Stretch) || border.equals(Insets.NONE)) {
            // The sprite's .png.mcmeta "gui.scaling" decides how it stretches
            this.sprite(sprite, rect, tint);
            return;
        }
        if ((tint >>> 24) == 0) {
            return;
        }
        final int x = left(rect);
        final int y = top(rect);
        final int width = right(rect) - x;
        final int height = bottom(rect) - y;
        if (width <= 0 || height <= 0) {
            return;
        }
        final int[] columns = slices(Math.round(border.left()), Math.round(border.right()),
                atlasSprite.contents().width(), width);
        final int[] rows = slices(Math.round(border.top()), Math.round(border.bottom()),
                atlasSprite.contents().height(), height);
        this.beginTint(tint);
        try {
            int sourceY = 0;
            int destY = y;
            for (int row = 0; row < 3; row++) {
                int sourceX = 0;
                int destX = x;
                for (int column = 0; column < 3; column++) {
                    this.region(atlasSprite, destX, destY, columns[column + 3], rows[row + 3], sourceX, sourceY,
                            columns[column], rows[row]);
                    sourceX += columns[column];
                    destX += columns[column + 3];
                }
                sourceY += rows[row];
                destY += rows[row + 3];
            }
        } finally {
            this.endTint(tint);
        }
    }

    /**
     * Works out one axis of a nine-slice: the source sizes of the three pieces, then their drawn sizes. Borders
     * wider than the drawn size shrink in proportion.
     *
     * @param start the border at the left or top, in sprite pixels
     * @param end the border at the right or bottom
     * @param source the sprite's size along the axis
     * @param dest the drawn size along the axis
     * @return the source sizes of the first, middle and last pieces, then their drawn sizes
     */
    static int[] slices(final int start, final int end, final int source, final int dest) {
        final int first = Math.max(0, Math.min(start, source));
        final int last = Math.max(0, Math.min(end, source - first));
        int firstDest = first;
        int lastDest = last;
        if (first + last > dest) {
            firstDest = Math.round((float) dest * first / (first + last));
            lastDest = dest - firstDest;
        }
        return new int[] {first, source - first - last, last, firstDest, dest - firstDest - lastDest, lastDest};
    }

    private void region(final TextureAtlasSprite sprite, final int x, final int y, final int width, final int height,
                        final int sourceX, final int sourceY, final int sourceWidth, final int sourceHeight) {
        if (width <= 0 || height <= 0 || sourceWidth <= 0 || sourceHeight <= 0) {
            return;
        }
        final int atlasWidth = Math.round(sprite.contents().width() / (sprite.getU1() - sprite.getU0()));
        final int atlasHeight = Math.round(sprite.contents().height() / (sprite.getV1() - sprite.getV0()));
        this.graphics.blit(sprite.atlasLocation(), x, y, width, height, sprite.getX() + sourceX,
                sprite.getY() + sourceY, sourceWidth, sourceHeight, atlasWidth, atlasHeight);
    }

    @Override
    public void text(final TextSpan span, final float x, final float y) {
        if ((span.style().color() >>> 24) == 0 || span.text().isEmpty()) {
            return;
        }
        this.graphics.drawString(this.textMetrics.font(), this.textMetrics.component(span), Math.round(x),
                Math.round(y), span.style().color(), span.style().shadow());
    }

    @Override
    public void pushClip(final Rect rect) {
        // Vanilla's scissor works in screen coordinates and ignores the pose
        final Matrix4f pose = this.graphics.pose().last().pose();
        pose.transformPosition(rect.x(), rect.y(), 0f, this.corner);
        final float x0 = this.corner.x();
        final float y0 = this.corner.y();
        pose.transformPosition(rect.right(), rect.bottom(), 0f, this.corner);
        final float x1 = this.corner.x();
        final float y1 = this.corner.y();
        this.graphics.enableScissor((int) Math.floor(Math.min(x0, x1)), (int) Math.floor(Math.min(y0, y1)),
                (int) Math.floor(Math.max(x0, x1)), (int) Math.floor(Math.max(y0, y1)));
    }

    @Override
    public void popClip() {
        this.graphics.disableScissor();
    }

    @Override
    public void pushTranslation(final float dx, final float dy) {
        this.graphics.pose().pushPose();
        this.graphics.pose().translate(dx, dy, 0f);
    }

    @Override
    public void pushScale(final float scale) {
        this.graphics.pose().pushPose();
        this.graphics.pose().scale(scale, scale, 1f);
    }

    @Override
    public void popTransform() {
        this.graphics.pose().popPose();
    }

    @Override
    public void visual(final Visual visual, final Rect rect, final int tint) {
        if (visual instanceof final ItemVisual item) {
            this.item(item.stack(), item.decorations(), item.fake(), item.seed(), rect, tint);
        } else if (visual instanceof final ProfileFaceVisual face) {
            this.playerFace(this.skins.apply(face.profile()), face.hat(), rect, tint);
        } else if (visual instanceof final SkinFaceVisual face) {
            this.playerFace(face.skin(), face.hat(), rect, tint);
        } else if (visual instanceof final AtlasSpriteVisual sprite) {
            this.atlasSprite(sprite, rect, tint);
        } else {
            this.fill(rect, MISSING_VISUAL_COLOR);
        }
    }

    private void item(final ItemStack stack, final ItemDecorations decorations, final boolean fake, final int seed,
                      final Rect rect, final int tint) {
        if ((tint >>> 24) == 0 || stack.isEmpty() || rect.width() <= 0f || rect.height() <= 0f) {
            return;
        }
        this.graphics.pose().pushPose();
        try {
            this.graphics.pose().translate(Math.round(rect.x()), Math.round(rect.y()), 0f);
            this.graphics.pose().scale(rect.width() / MinecraftVisuals.ITEM_SIZE.width(),
                    rect.height() / MinecraftVisuals.ITEM_SIZE.height(), 1f);
            if (fake) {
                this.graphics.renderFakeItem(stack, 0, 0, seed);
            } else {
                this.graphics.renderItem(stack, 0, 0, seed);
            }
            this.itemDecorations(stack, decorations);
        } finally {
            this.graphics.pose().popPose();
            this.clearDepth();
        }
    }

    private void playerFace(final PlayerSkin skin, final boolean hat, final Rect rect, final int tint) {
        final int x = left(rect);
        final int y = top(rect);
        final int width = right(rect) - x;
        final int height = bottom(rect) - y;
        if ((tint >>> 24) == 0 || width <= 0 || height <= 0) {
            return;
        }
        // The face and the hat as vanilla's PlayerFaceRenderer reads them from a 64 by 64 skin
        final ResourceLocation texture = skin.texture();
        this.beginTint(tint);
        try {
            this.graphics.blit(texture, x, y, width, height, 8f, 8f, 8, 8, 64, 64);
            if (hat) {
                this.graphics.blit(texture, x, y, width, height, 40f, 8f, 8, 8, 64, 64);
            }
        } finally {
            this.endTint(tint);
        }
    }

    private void atlasSprite(final AtlasSpriteVisual visual, final Rect rect, final int tint) {
        final int x = left(rect);
        final int y = top(rect);
        final int width = right(rect) - x;
        final int height = bottom(rect) - y;
        if ((tint >>> 24) == 0 || width <= 0 || height <= 0) {
            return;
        }
        this.beginTint(tint);
        try {
            this.graphics.blit(x, y, 0, width, height, this.backend.atlasSprite(visual.atlas(), visual.sprite()));
        } finally {
            this.endTint(tint);
        }
    }

    private void itemDecorations(final ItemStack stack, final ItemDecorations decorations) {
        if (decorations.count() && decorations.damageBar()) {
            this.graphics.renderItemDecorations(this.textMetrics.font(), stack, 0, 0);
            return;
        }
        // The same positions and colors as vanilla's renderItemDecorations
        if (decorations.damageBar() && stack.isBarVisible()) {
            this.graphics.fill(RenderType.guiOverlay(), 2, 13, 15, 15, 0xFF000000);
            this.graphics.fill(RenderType.guiOverlay(), 2, 13, 2 + stack.getBarWidth(), 14, stack.getBarColor() | 0xFF000000);
        }
        if (decorations.count() && stack.getCount() != 1) {
            final String count = String.valueOf(stack.getCount());
            this.graphics.pose().pushPose();
            try {
                // Above the item, where vanilla puts the count
                this.graphics.pose().translate(0f, 0f, 200f);
                this.graphics.drawString(this.textMetrics.font(), count, 17 - this.textMetrics.font().width(count), 9,
                        WHITE, true);
            } finally {
                this.graphics.pose().popPose();
            }
        }
    }

    @Override
    public void nextLayer() {
        this.clearDepth();
    }

    @Override
    public void requestCursor(final CursorKind cursor) {
        this.cursor = cursor;
    }

    private void clearDepth() {
        this.graphics.flush();
        this.backend.clearDepth();
    }

    private void beginTint(final int tint) {
        this.backend.prepareBlit();
        if (tint != WHITE) {
            this.graphics.setColor(((tint >> 16) & 0xFF) / 255f, ((tint >> 8) & 0xFF) / 255f, (tint & 0xFF) / 255f,
                    (tint >>> 24) / 255f);
        }
    }

    private void endTint(final int tint) {
        if (tint != WHITE) {
            this.graphics.setColor(1f, 1f, 1f, 1f);
        }
    }

    private ResourceLocation identifier(final SpriteId sprite) {
        return this.identifiers.computeIfAbsent(sprite, id -> ResourceLocation.fromNamespaceAndPath(id.namespace(), id.path()));
    }

    /**
     * Returns the skin vanilla shows for a profile: the loaded one once the profile and its skin have arrived, and
     * the built-in skin vanilla picks for the profile's id until then. A profile that holds only a name or an id is
     * looked up in the background through vanilla's profile cache.
     *
     * @param profile the profile
     * @return the skin to draw now
     */
    static PlayerSkin skinOf(final ResolvableProfile profile) {
        final ResolvableProfile resolved = profile.isResolved() ? profile : profile.resolve().getNow(null);
        if (resolved != null) {
            return Minecraft.getInstance().getSkinManager().getInsecureSkin(resolved.gameProfile());
        }
        return DefaultPlayerSkin.get(profile.id().orElseGet(() -> UUIDUtil.createOfflinePlayerUUID(profile.name().orElse(""))));
    }

    /**
     * Maps a Strata cursor to a GLFW standard cursor shape.
     *
     * @param cursor the Strata cursor
     * @return the GLFW shape
     */
    static int cursorShape(final CursorKind cursor) {
        return switch (cursor) {
            case ARROW -> GLFW.GLFW_ARROW_CURSOR;
            case TEXT -> GLFW.GLFW_IBEAM_CURSOR;
            case HAND -> GLFW.GLFW_POINTING_HAND_CURSOR;
            case CROSSHAIR -> GLFW.GLFW_CROSSHAIR_CURSOR;
            case RESIZE_HORIZONTAL -> GLFW.GLFW_RESIZE_EW_CURSOR;
            case RESIZE_VERTICAL -> GLFW.GLFW_RESIZE_NS_CURSOR;
            case RESIZE_ALL -> GLFW.GLFW_RESIZE_ALL_CURSOR;
            case NOT_ALLOWED -> GLFW.GLFW_NOT_ALLOWED_CURSOR;
        };
    }

    private static int left(final Rect rect) {
        return (int) Math.floor(rect.x());
    }

    private static int top(final Rect rect) {
        return (int) Math.floor(rect.y());
    }

    private static int right(final Rect rect) {
        return (int) Math.floor(rect.right());
    }

    private static int bottom(final Rect rect) {
        return (int) Math.floor(rect.bottom());
    }

    /**
     * What the canvas needs from the game beyond the graphics. Tests replace it, since they have no render system.
     */
    interface Backend {
        /**
         * The running game's.
         */
        Backend VANILLA = new Backend() {
            private final Map<CursorKind, Long> cursors = new EnumMap<>(CursorKind.class);
            private CursorKind shown = CursorKind.ARROW;

            @Override
            public TextureAtlasSprite sprite(final ResourceLocation id) {
                return Minecraft.getInstance().getGuiSprites().getSprite(id);
            }

            @Override
            public GuiSpriteScaling scaling(final TextureAtlasSprite sprite) {
                return Minecraft.getInstance().getGuiSprites().getSpriteScaling(sprite);
            }

            @Override
            public TextureAtlasSprite atlasSprite(final ResourceLocation atlas, final ResourceLocation sprite) {
                return Minecraft.getInstance().getTextureAtlas(atlas).apply(sprite);
            }

            @Override
            public void prepareBlit() {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableDepthTest();
            }

            @Override
            public void clearDepth() {
                RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
            }

            @Override
            public void setCursor(final CursorKind cursor) {
                if (cursor == this.shown) {
                    return;
                }
                this.shown = cursor;
                // A shape the system lacks gives no cursor, which GLFW shows as the arrow
                final long handle = cursor == CursorKind.ARROW ? 0L
                        : this.cursors.computeIfAbsent(cursor, kind -> GLFW.glfwCreateStandardCursor(cursorShape(kind)));
                GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), handle);
            }
        };

        /**
         * Returns a sprite of the GUI atlas.
         */
        TextureAtlasSprite sprite(ResourceLocation id);

        /**
         * Returns how a GUI sprite's {@code .png.mcmeta} says it scales.
         */
        GuiSpriteScaling scaling(TextureAtlasSprite sprite);

        /**
         * Returns a sprite of another atlas, such as the block atlas that slot icons come from.
         */
        TextureAtlasSprite atlasSprite(ResourceLocation atlas, ResourceLocation sprite);

        /**
         * Sets the blending and depth state sprites draw with.
         */
        void prepareBlit();

        /**
         * Clears the depth buffer inside the current scissor.
         */
        void clearDepth();

        /**
         * Shows a cursor on the game's window.
         */
        void setCursor(CursorKind cursor);
    }
}
