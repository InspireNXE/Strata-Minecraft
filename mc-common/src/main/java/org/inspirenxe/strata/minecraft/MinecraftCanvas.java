package org.inspirenxe.strata.minecraft;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.minecraft.MinecraftVisuals.ItemDecorations;
import org.inspirenxe.strata.minecraft.MinecraftVisuals.ItemVisual;
import org.inspirenxe.strata.spi.Canvas;

import java.util.HashMap;
import java.util.Map;

/**
 * The Minecraft {@link Canvas}: records Strata's drawing into vanilla's {@link GuiGraphicsExtractor}, which renders
 * it later in the frame.
 *
 * <p>How it meets the canvas contract on Minecraft 26.3:</p>
 * <ul>
 *     <li>Minecraft draws in whole GUI pixels. Fills and clips round outward (left and top down, right and bottom
 *     up), so a fractional rectangle is never cut short; text positions round to the nearest pixel.</li>
 *     <li>Sprites come from the {@code minecraft:gui} atlas, which reads {@code assets/<namespace>/textures/gui/
 *     sprites/<path>.png}, and draw with vanilla's {@code GUI_TEXTURED} pipeline. The tint is passed as vanilla's
 *     sprite color.</li>
 *     <li>A nine-slice draws as a plain sprite: vanilla applies the scaling in the sprite's {@code .png.mcmeta}
 *     ({@code "type": "nine_slice"}), and the border argument is ignored. A missing sprite draws vanilla's
 *     missing-texture pattern.</li>
 *     <li>Text draws as a chat component in the player's font, with vanilla's bold, italic, underline,
 *     strikethrough and shadow.</li>
 *     <li>Clips use vanilla's scissor stack, which intersects nested clips and follows the pose. Translations and
 *     scales push and pop the pose matrix.</li>
 *     <li>Visuals draw as a magenta placeholder until the adapter makes visuals of its own.</li>
 *     <li>{@link #nextLayer()} starts a new vanilla stratum, so later screens draw over blur and earlier
 *     layers.</li>
 *     <li>Cursors map to vanilla's {@link CursorTypes}.</li>
 * </ul>
 *
 * <p>Call {@link #begin(GuiGraphicsExtractor)} at the start of every frame. Use it on the render thread only.</p>
 */
public final class MinecraftCanvas implements Canvas {
    /**
     * What a visual this canvas cannot draw shows as.
     */
    public static final int MISSING_VISUAL_COLOR = 0xFFFF00FF;

    private final Map<SpriteId, Identifier> identifiers;
    private final MinecraftTextMetrics textMetrics;
    private GuiGraphicsExtractor graphics;

    /**
     * Creates a canvas.
     *
     * @param font the font text is drawn and measured with, usually the screen's
     */
    public MinecraftCanvas(final Font font) {
        this.identifiers = new HashMap<>();
        this.textMetrics = new MinecraftTextMetrics(font);
    }

    /**
     * Points the canvas at this frame's graphics.
     *
     * @param graphics the graphics vanilla passed to the screen's {@code extractRenderState}
     */
    public void begin(final GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
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
        this.graphics.fill(left(rect), top(rect), right(rect), bottom(rect), argb);
    }

    @Override
    public void fillGradient(final Rect rect, final int topArgb, final int bottomArgb) {
        this.graphics.fillGradient(left(rect), top(rect), right(rect), bottom(rect), topArgb, bottomArgb);
    }

    @Override
    public void sprite(final SpriteId sprite, final Rect rect, final int tint) {
        this.graphics.blitSprite(RenderPipelines.GUI_TEXTURED, this.identifier(sprite),
                left(rect), top(rect), right(rect) - left(rect), bottom(rect) - top(rect), tint);
    }

    @Override
    public void nineSlice(final SpriteId sprite, final Rect rect, final Insets border, final int tint) {
        // The sprite's .png.mcmeta "gui.scaling" decides how it stretches
        this.sprite(sprite, rect, tint);
    }

    @Override
    public void text(final TextSpan span, final float x, final float y) {
        if ((span.style().color() >>> 24) == 0 || span.text().isEmpty()) {
            return;
        }
        this.graphics.text(this.textMetrics.font(), this.textMetrics.component(span), Math.round(x), Math.round(y),
                span.style().color(), span.style().shadow());
    }

    @Override
    public void pushClip(final Rect rect) {
        this.graphics.enableScissor(left(rect), top(rect), right(rect), bottom(rect));
    }

    @Override
    public void popClip() {
        this.graphics.disableScissor();
    }

    @Override
    public void pushTranslation(final float dx, final float dy) {
        this.graphics.pose().pushMatrix();
        this.graphics.pose().translate(dx, dy);
    }

    @Override
    public void pushScale(final float scale) {
        this.graphics.pose().pushMatrix();
        this.graphics.pose().scale(scale);
    }

    @Override
    public void popTransform() {
        this.graphics.pose().popMatrix();
    }

    @Override
    public void visual(final Visual visual, final Rect rect, final int tint) {
        if (!(visual instanceof ItemVisual(final ItemStack stack, final ItemDecorations decorations))) {
            this.fill(rect, MISSING_VISUAL_COLOR);
            return;
        }
        if ((tint >>> 24) == 0 || stack.isEmpty() || rect.width() <= 0f || rect.height() <= 0f) {
            return;
        }
        this.graphics.pose().pushMatrix();
        try {
            this.graphics.pose().translate(Math.round(rect.x()), Math.round(rect.y()));
            this.graphics.pose().scale(rect.width() / MinecraftVisuals.ITEM_SIZE.width(),
                    rect.height() / MinecraftVisuals.ITEM_SIZE.height());
            this.graphics.item(stack, 0, 0);
            this.itemDecorations(stack, decorations);
        } finally {
            this.graphics.pose().popMatrix();
        }
    }

    private void itemDecorations(final ItemStack stack, final ItemDecorations decorations) {
        if (decorations.count() && decorations.damageBar()) {
            this.graphics.itemDecorations(this.textMetrics.font(), stack, 0, 0);
            return;
        }
        // The same positions and colors as vanilla's private itemBar and itemCount
        if (decorations.damageBar() && stack.isBarVisible()) {
            this.graphics.fill(2, 13, 15, 15, 0xFF000000);
            this.graphics.fill(2, 13, 2 + stack.getBarWidth(), 14, ARGB.opaque(stack.getBarColor()));
        }
        if (decorations.count() && stack.getCount() != 1) {
            final String count = String.valueOf(stack.getCount());
            this.graphics.text(this.textMetrics.font(), count, 17 - this.textMetrics.font().width(count), 9, 0xFFFFFFFF, true);
        }
    }

    @Override
    public void nextLayer() {
        this.graphics.nextStratum();
    }

    @Override
    public void requestCursor(final CursorKind cursor) {
        this.graphics.requestCursor(cursorType(cursor));
    }

    private Identifier identifier(final SpriteId sprite) {
        return this.identifiers.computeIfAbsent(sprite, id -> Identifier.fromNamespaceAndPath(id.namespace(), id.path()));
    }

    /**
     * Maps a Strata cursor to vanilla's.
     *
     * @param cursor the Strata cursor
     * @return the vanilla cursor
     */
    static CursorType cursorType(final CursorKind cursor) {
        return switch (cursor) {
            case ARROW -> CursorTypes.ARROW;
            case TEXT -> CursorTypes.IBEAM;
            case HAND -> CursorTypes.POINTING_HAND;
            case CROSSHAIR -> CursorTypes.CROSSHAIR;
            case RESIZE_HORIZONTAL -> CursorTypes.RESIZE_EW;
            case RESIZE_VERTICAL -> CursorTypes.RESIZE_NS;
            case RESIZE_ALL -> CursorTypes.RESIZE_ALL;
            case NOT_ALLOWED -> CursorTypes.NOT_ALLOWED;
        };
    }

    private static int left(final Rect rect) {
        return (int) Math.floor(rect.x());
    }

    private static int top(final Rect rect) {
        return (int) Math.floor(rect.y());
    }

    private static int right(final Rect rect) {
        return (int) Math.ceil(rect.right());
    }

    private static int bottom(final Rect rect) {
        return (int) Math.ceil(rect.bottom());
    }
}
