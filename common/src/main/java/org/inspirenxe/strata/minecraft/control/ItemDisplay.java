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
package org.inspirenxe.strata.minecraft.control;

import net.minecraft.world.item.ItemStack;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.event.input.PointerEvent;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.minecraft.MinecraftTooltips;
import org.inspirenxe.strata.minecraft.MinecraftVisuals;
import org.inspirenxe.strata.minecraft.MinecraftVisuals.ItemDecorations;

import java.util.Objects;
import java.util.function.Function;

/**
 * An item stack shown the way vanilla shows one in a slot, with its tooltip, like vanilla's
 * {@code ItemDisplayWidget}. It shows the stack only and takes no clicks; for interactive slots, use a
 * {@link org.inspirenxe.strata.minecraft.StrataContainerScreen}.
 *
 * <pre>{@code
 * ItemDisplay display = new ItemDisplay(factory, new ItemStack(Items.DIAMOND, 64));
 * display.setTooltipFunction(stack -> MinecraftTooltips.lines(List.of(stack.getHoverName(), price)));
 * }</pre>
 *
 * <p>It draws an item {@link MinecraftVisuals#item visual} of a copy of the stack, so a later change to the caller's
 * stack does not show until {@link #setStack} is called again. Its desired size is 16 by 16, a slot's item; in
 * larger bounds the item draws as the largest square that fits, centered. An empty stack draws nothing and has no
 * tooltip.</p>
 *
 * <p>Each time the pointer enters it, and when the stack changes while the pointer is over it, the display sets its
 * {@linkplain #setTooltip tooltip} to what the {@linkplain #setTooltipFunction tooltip function} returns for the
 * stack. The default function is {@link MinecraftTooltips#item(ItemStack)}, vanilla's own lines, so the tooltip
 * follows the advanced tooltips setting (F3+H) at the time the pointer arrives. A tooltip with no lines counts as
 * none. Set the function to {@code null} to manage the tooltip yourself.</p>
 *
 * <p>The display is not focusable, reads no theme keys and has no default behavior for input, so clicks pass
 * through to its ancestors. It has no events of its own: only code changes it. Use it on the render thread.</p>
 */
public final class ItemDisplay extends CustomComponent<ItemDisplay> implements HasTooltip<ItemDisplay> {
    private ItemStack stack;
    private ItemDecorations decorations;
    private Visual visual;
    private int tint;
    private Function<? super ItemStack, ? extends Tooltip> tooltipFunction;
    private Tooltip ownTooltip;

    /**
     * Creates a display with no stack, which draws nothing until {@link #setStack} is called.
     *
     * @param factory the factory of the host that will show the display
     */
    public ItemDisplay(final ComponentFactory factory) {
        this(factory, ItemStack.EMPTY);
    }

    /**
     * Creates a display of a stack with all of vanilla's slot decorations.
     *
     * @param factory the factory of the host that will show the display
     * @param stack the stack; it is copied
     */
    public ItemDisplay(final ComponentFactory factory, final ItemStack stack) {
        super(factory);
        this.stack = stack.copy();
        this.decorations = ItemDecorations.ALL;
        this.visual = MinecraftVisuals.item(this.stack, this.decorations);
        this.tint = 0xFFFFFFFF;
        this.tooltipFunction = MinecraftTooltips::item;
    }

    /**
     * Returns the stack shown.
     *
     * @return a copy of the stack; changing it changes nothing shown
     */
    public ItemStack stack() {
        return this.stack.copy();
    }

    /**
     * Shows another stack. The display copies it, and refreshes its tooltip if the pointer is over it.
     *
     * @param stack the stack; {@link ItemStack#EMPTY} shows nothing
     * @return this display
     */
    public ItemDisplay setStack(final ItemStack stack) {
        this.stack = stack.copy();
        this.visual = MinecraftVisuals.item(this.stack, this.decorations);
        if (this.isHovered()) {
            this.refreshTooltip();
        }
        return this;
    }

    /**
     * Returns which of vanilla's slot decorations are drawn over the item.
     *
     * @return the decorations; {@link ItemDecorations#ALL} by default
     */
    public ItemDecorations decorations() {
        return this.decorations;
    }

    /**
     * Sets which of vanilla's slot decorations, the count and the damage bar, are drawn over the item.
     *
     * @param decorations the decorations
     * @return this display
     */
    public ItemDisplay setDecorations(final ItemDecorations decorations) {
        this.decorations = Objects.requireNonNull(decorations, "decorations");
        this.visual = MinecraftVisuals.item(this.stack, decorations);
        return this;
    }

    /**
     * Returns the tint.
     *
     * @return the ARGB tint; {@code 0xFFFFFFFF} by default
     */
    public int tint() {
        return this.tint;
    }

    /**
     * Sets the tint. Minecraft's canvas cannot tint an item, so only an alpha of 0, which hides it, has an effect
     * there.
     *
     * @param tint the ARGB tint
     * @return this display
     */
    public ItemDisplay setTint(final int tint) {
        this.tint = tint;
        return this;
    }

    /**
     * Returns the function that makes the tooltip.
     *
     * @return the function, or {@code null} when the display leaves its tooltip alone
     */
    public Function<? super ItemStack, ? extends Tooltip> tooltipFunction() {
        return this.tooltipFunction;
    }

    /**
     * Sets the function that makes the tooltip each time the pointer enters the display. It gets a copy of the
     * stack, never an empty one, and may return {@code null} for no tooltip. Setting {@code null} removes a tooltip
     * the display set, and leaves one set with {@link #setTooltip} alone.
     *
     * @param tooltipFunction the function, or {@code null} to manage the tooltip yourself
     * @return this display
     */
    public ItemDisplay setTooltipFunction(final Function<? super ItemStack, ? extends Tooltip> tooltipFunction) {
        this.tooltipFunction = tooltipFunction;
        if (tooltipFunction == null) {
            if (this.ownTooltip != null && this.tooltip() == this.ownTooltip) {
                this.setTooltip((Tooltip) null);
            }
            this.ownTooltip = null;
        } else if (this.isHovered()) {
            this.refreshTooltip();
        }
        return this;
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        return MinecraftVisuals.ITEM_SIZE;
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        if (this.stack.isEmpty()) {
            return;
        }
        final float side = Math.min(bounds.width(), bounds.height());
        if (side == bounds.width() && side == bounds.height()) {
            painter.visual(this.visual, bounds, this.tint);
            return;
        }
        painter.visual(this.visual, new Rect(bounds.x() + (bounds.width() - side) * 0.5f,
                bounds.y() + (bounds.height() - side) * 0.5f, side, side), this.tint);
    }

    @Override
    protected void onPointerEnter(final PointerEvent event) {
        this.refreshTooltip();
    }

    @Override
    protected Object snapshotState() {
        return new State(this.stack.copy(), this.decorations, this.tint);
    }

    private void refreshTooltip() {
        if (this.tooltipFunction == null) {
            return;
        }
        Tooltip tooltip = this.stack.isEmpty() ? null : this.tooltipFunction.apply(this.stack.copy());
        if (tooltip instanceof Tooltip.Lines lines && lines.lines().isEmpty()) {
            tooltip = null;
        }
        this.ownTooltip = tooltip;
        this.setTooltip(tooltip);
    }

    /**
     * A display's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param stack a copy of the {@linkplain #stack() stack}, owned by the snapshot
     * @param decorations the {@linkplain #decorations() decorations}
     * @param tint the {@linkplain #tint() tint}
     */
    public record State(ItemStack stack, ItemDecorations decorations, int tint) {
    }
}
