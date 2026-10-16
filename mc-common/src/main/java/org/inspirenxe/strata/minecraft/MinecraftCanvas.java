package org.inspirenxe.strata.minecraft;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.spi.Canvas;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws through {@link GuiGraphicsExtractor}. Minecraft records GUI draws and renders them later, which suits
 * Strata's canvas contract.
 *
 * <p>Sprites come from the {@code minecraft:gui} atlas, which reads {@code textures/gui/sprites}. A sprite's
 * {@code .png.mcmeta} decides its nine-slice borders, so {@link #nineSlice} ignores the border argument. Minecraft
 * draws in whole GUI pixels, so coordinates are rounded here.</p>
 */
public final class MinecraftCanvas implements Canvas {
    private final Map<SpriteId, Identifier> identifiers;
    private final MinecraftText text;
    private GuiGraphicsExtractor graphics;

    public MinecraftCanvas(final Font font) {
        this.identifiers = new HashMap<>();
        this.text = new MinecraftText(font);
    }

    /**
     * Points the canvas at this frame's graphics.
     */
    public void begin(final GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
    }

    public MinecraftText text() {
        return this.text;
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
        this.graphics.text(this.text.font(), this.text.component(span), Math.round(x), Math.round(y),
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
    public void popTransform() {
        this.graphics.pose().popMatrix();
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

    private static CursorType cursorType(final CursorKind cursor) {
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
