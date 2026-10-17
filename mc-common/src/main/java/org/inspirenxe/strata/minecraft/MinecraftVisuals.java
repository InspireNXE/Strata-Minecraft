package org.inspirenxe.strata.minecraft;

import net.minecraft.world.item.ItemStack;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.visual.Visual;

import java.util.Objects;

/**
 * Makes {@link Visual}s that {@link MinecraftCanvas} draws, so mod code can show vanilla objects through the API.
 *
 * <p>An item visual draws the way vanilla draws an item in an inventory slot: the item's GUI model, its enchantment
 * glint, and optionally the stack count and damage bar. Its natural size is 16 by 16 UI units, one GUI pixel each,
 * and the canvas scales it to whatever rectangle it is given.</p>
 */
public final class MinecraftVisuals {
    /**
     * The natural size of an item visual: 16 by 16, a vanilla slot's item.
     */
    public static final Size ITEM_SIZE = new Size(16f, 16f);

    private MinecraftVisuals() {
    }

    /**
     * Returns a visual of an item stack with {@link ItemDecorations#ALL all decorations}, as in an inventory slot.
     *
     * @param stack the stack; it is copied, so later changes to it do not show
     * @return the visual, 16 by 16
     */
    public static Visual item(final ItemStack stack) {
        return item(stack, ItemDecorations.ALL);
    }

    /**
     * Returns a visual of an item stack with the chosen decorations.
     *
     * @param stack the stack; it is copied, so later changes to it do not show
     * @param decorations which of the count and damage bar to draw over the item
     * @return the visual, 16 by 16
     */
    public static Visual item(final ItemStack stack, final ItemDecorations decorations) {
        return new ItemVisual(stack.copy(), decorations);
    }

    /**
     * Which of vanilla's slot decorations an item visual draws over the item.
     *
     * <p>With both on, the canvas calls vanilla's own slot decorations, which also draw the cooldown overlay of
     * items the player has on cooldown. With either off, the canvas draws the remaining one itself in vanilla's
     * position and colors, without the cooldown overlay.</p>
     *
     * @param count whether to draw the stack count in the lower right, which vanilla draws only for counts other than
     * one
     * @param damageBar whether to draw the damage bar along the bottom, which vanilla draws only for damaged items
     */
    public record ItemDecorations(boolean count, boolean damageBar) {
        /**
         * The count and the damage bar, as in an inventory slot.
         */
        public static final ItemDecorations ALL = new ItemDecorations(true, true);
        /**
         * The item alone, as an icon.
         */
        public static final ItemDecorations NONE = new ItemDecorations(false, false);
    }

    /**
     * An item stack for the Minecraft canvas. Two are equal when their stacks match by {@link ItemStack#matches}
     * (same item, count and components) and their decorations are equal.
     *
     * @param stack the stack, a copy owned by the visual and never changed
     * @param decorations the decorations to draw
     */
    record ItemVisual(ItemStack stack, ItemDecorations decorations) implements Visual {
        ItemVisual {
            Objects.requireNonNull(stack, "stack");
            Objects.requireNonNull(decorations, "decorations");
        }

        @Override
        public Size size() {
            return ITEM_SIZE;
        }

        @Override
        public boolean equals(final Object other) {
            return other instanceof ItemVisual(final ItemStack otherStack, final ItemDecorations otherDecorations)
                    && ItemStack.matches(this.stack, otherStack)
                    && this.decorations.equals(otherDecorations);
        }

        @Override
        public int hashCode() {
            return 31 * (31 * ItemStack.hashItemAndComponents(this.stack) + this.stack.getCount()) + this.decorations.hashCode();
        }

        @Override
        public String toString() {
            return "ItemVisual[" + this.stack + ", " + this.decorations + "]";
        }
    }
}
