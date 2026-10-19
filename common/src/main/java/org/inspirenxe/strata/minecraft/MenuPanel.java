package org.inspirenxe.strata.minecraft;

import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.minecraft.MinecraftVisuals.ItemDecorations;

import java.util.List;
import java.util.Set;

/**
 * The area of a {@link StrataContainerScreen} that holds its menu's slots, at the positions the menu gave them.
 *
 * <p>The panel's desired size is the screen's image size, and the screen moves vanilla's {@code leftPos} and
 * {@code topPos} to the panel's top-left corner after every layout, so each slot is where vanilla, recipe viewers
 * and inventory mods expect it. Each active slot gets the {@value #STYLE_SLOT} paint behind it, an 18 by 18 square
 * around the slot's 16 by 16 item. Then:</p>
 * <ul>
 *     <li>With {@link SlotRendering#VANILLA}, vanilla's container drawing runs from inside the panel's paint: the
 *     items, the hover highlight, drag previews and whatever other mods draw on slots. Strata content drawn later,
 *     such as a dialog, covers it.</li>
 *     <li>With {@link SlotRendering#STRATA}, the panel draws each slot itself: the hovered slot's
 *     {@value #STYLE_HIGHLIGHT_BACK} behind the items and {@value #STYLE_HIGHLIGHT_FRONT} over them (24 by 24
 *     around the slot, for slots that are highlightable), the item through an
 *     {@link MinecraftVisuals#item item visual} with its count and damage bar, an empty slot's
 *     {@code getNoItemIcon()} sprite, and while the player drags a stack over slots, each slot's share of it on a
 *     translucent white square, with the count in yellow where it reaches the slot's limit. Inactive slots draw
 *     nothing. Fake slots draw like real ones.</li>
 * </ul>
 *
 * <p>The panel takes no input itself: the screen hands presses over it to vanilla. Get it from
 * {@link ContainerContent#build}; it cannot be made otherwise. Add it to the tree once, and not inside a
 * {@link org.inspirenxe.strata.api.layout.ScrollPanel}, since vanilla places slots in screen coordinates.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_SLOT} (paint, optional): behind each slot. Falls back to vanilla's
 *     {@code container/slot} sprite.</li>
 *     <li>{@value #STYLE_HIGHLIGHT_BACK} (paint, optional): behind the hovered slot's item, in Strata mode. Falls
 *     back to vanilla's {@code container/slot_highlight_back} sprite.</li>
 *     <li>{@value #STYLE_HIGHLIGHT_FRONT} (paint, optional): over the hovered slot's item, in Strata mode. Falls
 *     back to vanilla's {@code container/slot_highlight_front} sprite.</li>
 * </ul>
 */
public final class MenuPanel extends CustomComponent<MenuPanel> {
    /**
     * Style key for the paint behind each slot.
     */
    public static final String STYLE_SLOT = "slot.background";
    /**
     * Style key for the paint behind the hovered slot's item, in Strata mode.
     */
    public static final String STYLE_HIGHLIGHT_BACK = "slot.highlight_back";
    /**
     * Style key for the paint over the hovered slot's item, in Strata mode.
     */
    public static final String STYLE_HIGHLIGHT_FRONT = "slot.highlight_front";

    private static final Paint DEFAULT_SLOT = Paint.sprite(SpriteId.of("minecraft", "container/slot"));
    private static final Paint DEFAULT_HIGHLIGHT_BACK = Paint.sprite(SpriteId.of("minecraft", "container/slot_highlight_back"));
    private static final Paint DEFAULT_HIGHLIGHT_FRONT = Paint.sprite(SpriteId.of("minecraft", "container/slot_highlight_front"));
    // Vanilla's drag preview square and its count at the slot's limit
    private static final int PREVIEW_COLOR = 0x80FFFFFF;
    private static final TextStyle LIMIT_STYLE = TextStyle.DEFAULT.withColor(0xFFFFFF55).withShadow(true);

    private final Owner owner;
    private Paint slotPaint;
    private Paint highlightBack;
    private Paint highlightFront;
    private Rect laidOutFor;
    private Rect[] slotRects;
    private Rect[] itemRects;
    private Rect[] highlightRects;
    private ItemStack[] shown;
    private Visual[] visuals;

    MenuPanel(final ComponentFactory factory, final Owner owner) {
        super(factory);
        this.owner = owner;
        this.slotPaint = DEFAULT_SLOT;
        this.highlightBack = DEFAULT_HIGHLIGHT_BACK;
        this.highlightFront = DEFAULT_HIGHLIGHT_FRONT;
    }

    /**
     * Returns the menu whose slots the panel shows.
     *
     * @return the screen's menu
     */
    public AbstractContainerMenu menu() {
        return this.owner.menu();
    }

    /**
     * Returns who draws the slots.
     *
     * @return the mode the screen ended up with, after the player's settings
     */
    public SlotRendering slotRendering() {
        return this.owner.slotRendering();
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        return new Size(this.owner.imageWidth(), this.owner.imageHeight());
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.slotPaint = theme.paintOr(STYLE_SLOT, DEFAULT_SLOT);
        this.highlightBack = theme.paintOr(STYLE_HIGHLIGHT_BACK, DEFAULT_HIGHLIGHT_BACK);
        this.highlightFront = theme.paintOr(STYLE_HIGHLIGHT_FRONT, DEFAULT_HIGHLIGHT_FRONT);
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        final List<Slot> slots = this.owner.menu().slots;
        this.layOut(bounds, slots);
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).isActive()) {
                drawPaint(painter, this.slotPaint, this.slotRects[i]);
            }
        }
        if (this.owner.slotRendering() == SlotRendering.VANILLA) {
            this.owner.drawVanillaSlots();
            return;
        }
        final Slot hovered = this.owner.hoveredSlot();
        final boolean highlight = hovered != null && hovered.isHighlightable() && hovered.index >= 0 && hovered.index < slots.size();
        if (highlight) {
            drawPaint(painter, this.highlightBack, this.highlightRects[hovered.index]);
        }
        for (int i = 0; i < slots.size(); i++) {
            final Slot slot = slots.get(i);
            if (slot.isActive()) {
                this.drawSlot(painter, i, slot);
            }
        }
        if (highlight) {
            drawPaint(painter, this.highlightFront, this.highlightRects[hovered.index]);
        }
    }

    private void layOut(final Rect bounds, final List<Slot> slots) {
        if (bounds.equals(this.laidOutFor) && this.slotRects.length == slots.size()) {
            return;
        }
        this.laidOutFor = bounds;
        final int count = slots.size();
        this.slotRects = new Rect[count];
        this.itemRects = new Rect[count];
        this.highlightRects = new Rect[count];
        if (this.shown == null || this.shown.length != count) {
            this.shown = new ItemStack[count];
            this.visuals = new Visual[count];
        }
        for (int i = 0; i < count; i++) {
            final float x = bounds.x() + slots.get(i).x;
            final float y = bounds.y() + slots.get(i).y;
            this.itemRects[i] = new Rect(x, y, 16f, 16f);
            this.slotRects[i] = new Rect(x - 1f, y - 1f, 18f, 18f);
            this.highlightRects[i] = new Rect(x - 4f, y - 4f, 24f, 24f);
        }
    }

    private void drawSlot(final Painter painter, final int i, final Slot slot) {
        final AbstractContainerMenu menu = this.owner.menu();
        final Set<Slot> dragged = this.owner.quickCraftSlots();
        final ItemStack carried = menu.getCarried();
        ItemStack stack = slot.getItem();
        boolean preview = false;
        int limit = -1;
        // The same preview vanilla's extractSlot draws while a drag spreads the carried stack
        if (this.owner.isQuickCrafting() && dragged.contains(slot) && !carried.isEmpty()) {
            if (dragged.size() == 1) {
                return;
            }
            if (AbstractContainerMenu.canItemQuickReplace(slot, carried, true) && menu.canDragTo(slot)) {
                preview = true;
                final int max = Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried));
                final int carry = stack.isEmpty() ? 0 : stack.getCount();
                int count = AbstractContainerMenu.getQuickCraftPlaceCount(dragged.size(), this.owner.quickCraftType(), carried) + carry;
                if (count > max) {
                    count = max;
                    limit = max;
                }
                stack = carried.copyWithCount(count);
            }
        }
        final Rect rect = this.itemRects[i];
        if (stack.isEmpty()) {
            final Identifier icon = slot.getNoItemIcon();
            if (icon != null) {
                painter.sprite(SpriteId.of(icon.getNamespace(), icon.getPath()), rect, 0xFFFFFFFF);
            }
            return;
        }
        if (!preview) {
            painter.visual(this.visual(i, stack), rect, 0xFFFFFFFF);
            return;
        }
        painter.fill(rect, PREVIEW_COLOR);
        if (limit < 0) {
            painter.visual(MinecraftVisuals.item(stack), rect, 0xFFFFFFFF);
            return;
        }
        painter.visual(MinecraftVisuals.item(stack, new ItemDecorations(false, true)), rect, 0xFFFFFFFF);
        final TextSpan count = new TextSpan(String.valueOf(limit), LIMIT_STYLE);
        // Where vanilla's itemDecorations puts a count
        painter.text(count, rect.x() + 17f - this.environment().text().width(count), rect.y() + 9f);
    }

    private Visual visual(final int i, final ItemStack stack) {
        if (this.shown[i] == null || !ItemStack.matches(this.shown[i], stack)) {
            this.shown[i] = stack.copy();
            this.visuals[i] = MinecraftVisuals.item(stack);
        }
        return this.visuals[i];
    }

    private static void drawPaint(final Painter painter, final Paint paint, final Rect rect) {
        switch (paint) {
            case Paint.Solid solid -> painter.fill(rect, solid.argb());
            case Paint.Gradient gradient -> painter.fillGradient(rect, gradient.topArgb(), gradient.bottomArgb());
            case Paint.Sprite sprite -> painter.sprite(sprite.sprite(), rect, sprite.tint());
            case Paint.NineSlice nineSlice -> painter.nineSlice(nineSlice.sprite(), rect, nineSlice.border(), nineSlice.tint());
        }
    }

    /**
     * What the panel needs from its screen. {@link StrataContainerScreen} implements it; tests use a fake.
     */
    interface Owner {
        AbstractContainerMenu menu();

        int imageWidth();

        int imageHeight();

        SlotRendering slotRendering();

        /**
         * Returns the slot under the pointer that Strata mode highlights, or null.
         */
        Slot hoveredSlot();

        boolean isQuickCrafting();

        Set<Slot> quickCraftSlots();

        /**
         * Returns vanilla's quick-craft type for the running drag: 0 spreads evenly, 1 places one each, 2 clones.
         */
        int quickCraftType();

        /**
         * Runs vanilla's container drawing for compatibility mode.
         */
        void drawVanillaSlots();
    }
}
