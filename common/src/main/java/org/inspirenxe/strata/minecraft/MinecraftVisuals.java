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
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.visual.Visual;

import java.util.Objects;

/**
 * Makes {@link Visual}s that {@link MinecraftCanvas} draws, so mod code can show vanilla objects through the API.
 *
 * <p>An item visual draws the way vanilla draws an item in an inventory slot: the item's GUI model, its enchantment
 * glint, and optionally the stack count and damage bar. Its natural size is 16 by 16 UI units, one GUI pixel each,
 * and the canvas scales it to whatever rectangle it is given.</p>
 *
 * <p>A player face visual draws a player's head from their skin the way vanilla's player list and social screen do:
 * the face, and by default the hat layer over it. Its natural size is 8 by 8, one skin pixel per UI unit, and the
 * canvas stretches it to the rectangle, so set the image size for a larger face:</p>
 *
 * <pre>{@code
 * Image face = factory.image(MinecraftVisuals.playerFace(player.getGameProfile())).setImageSize(16, 16);
 * }</pre>
 *
 * <p>Every visual made here is a snapshot in the sense of Strata's {@link Visual} contract. An item visual copies the
 * stack, so make a new one to show a change. A face visual keeps the profile or skin it was given, and the canvas
 * looks the skin texture up on each draw, so a face fills in by itself once its skin has loaded.</p>
 */
public final class MinecraftVisuals {
    /**
     * The natural size of an item visual: 16 by 16, a vanilla slot's item.
     */
    public static final Size ITEM_SIZE = new Size(16f, 16f);
    /**
     * The natural size of a player face visual: 8 by 8, the face's size on the skin.
     */
    public static final Size PLAYER_FACE_SIZE = new Size(8f, 8f);

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
        return new ItemVisual(stack.copy(), decorations, false, 0);
    }

    /**
     * Returns a visual of a player's face with the hat layer, from a profile that already holds its skin, such as
     * {@code player.getGameProfile()} for a player in the level.
     *
     * @param profile the profile
     * @return the visual, 8 by 8
     * @see #playerFace(ResolvableProfile, boolean)
     */
    public static Visual playerFace(final GameProfile profile) {
        return playerFace(new ResolvableProfile(profile), true);
    }

    /**
     * Returns a visual of a player's face with the hat layer.
     *
     * @param profile the profile, which may name only the player, as
     *                {@code new ResolvableProfile(Optional.of(name), Optional.empty(), new PropertyMap())} does
     * @return the visual, 8 by 8
     * @see #playerFace(ResolvableProfile, boolean)
     */
    public static Visual playerFace(final ResolvableProfile profile) {
        return playerFace(profile, true);
    }

    /**
     * Returns a visual of a player's face from a profile whose skin the game looks up. The canvas asks vanilla's
     * skin manager for the skin every time it draws, so the face shows the built-in skin vanilla picks for the
     * profile's id until the real one has loaded, and the real one after, as vanilla's player list does. A profile
     * that holds only a name or an id is looked up in the background through vanilla's profile cache, as player
     * heads are.
     *
     * @param profile the profile, which may name only the player, as
     *                {@code new ResolvableProfile(Optional.of(name), Optional.empty(), new PropertyMap())} does
     * @param hat whether to draw the skin's hat layer over the face
     * @return the visual, 8 by 8
     */
    public static Visual playerFace(final ResolvableProfile profile, final boolean hat) {
        return new ProfileFaceVisual(profile, hat);
    }

    /**
     * Returns a visual of the face of a skin the caller already has, with the hat layer.
     *
     * @param skin the skin
     * @return the visual, 8 by 8
     */
    public static Visual playerFace(final PlayerSkin skin) {
        return playerFace(skin, true);
    }

    /**
     * Returns a visual of the face of a skin the caller already has, such as one of {@code DefaultPlayerSkin}'s.
     *
     * @param skin the skin; its texture is drawn
     * @param hat whether to draw the skin's hat layer over the face
     * @return the visual, 8 by 8
     */
    public static Visual playerFace(final PlayerSkin skin, final boolean hat) {
        return new SkinFaceVisual(skin, hat);
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
     * (same item, count and components) and everything else is equal.
     *
     * @param stack the stack, a copy owned by the visual and never changed
     * @param decorations the decorations to draw
     * @param fake whether to draw it as vanilla draws a fake slot's item, with no entity holding it
     * @param seed the seed vanilla's item models pick random variants with; vanilla's slots use
     *             {@code x + y * imageWidth}, and lone items 0
     */
    record ItemVisual(ItemStack stack, ItemDecorations decorations, boolean fake, int seed) implements Visual {
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
            return other instanceof ItemVisual(final ItemStack otherStack, final ItemDecorations otherDecorations,
                                               final boolean otherFake, final int otherSeed)
                    && ItemStack.matches(this.stack, otherStack)
                    && this.decorations.equals(otherDecorations)
                    && this.fake == otherFake
                    && this.seed == otherSeed;
        }

        @Override
        public int hashCode() {
            final int stackHash = 31 * ItemStack.hashItemAndComponents(this.stack) + this.stack.getCount();
            return 31 * (31 * (31 * stackHash + this.decorations.hashCode()) + Boolean.hashCode(this.fake)) + this.seed;
        }

        @Override
        public String toString() {
            return "ItemVisual[" + this.stack + ", " + this.decorations + (this.fake ? ", fake" : "") + ", seed " + this.seed + "]";
        }
    }

    /**
     * A player's face, looked up by profile each time it is drawn. Two are equal when their profiles are.
     *
     * @param profile the profile whose skin is drawn
     * @param hat whether the hat layer is drawn
     */
    record ProfileFaceVisual(ResolvableProfile profile, boolean hat) implements Visual {
        ProfileFaceVisual {
            Objects.requireNonNull(profile, "profile");
        }

        @Override
        public Size size() {
            return PLAYER_FACE_SIZE;
        }
    }

    /**
     * A sprite of a texture atlas other than the GUI's, such as the block atlas sprite an empty slot shows.
     *
     * @param atlas the atlas texture
     * @param sprite the sprite in it
     */
    record AtlasSpriteVisual(ResourceLocation atlas, ResourceLocation sprite) implements Visual {
        AtlasSpriteVisual {
            Objects.requireNonNull(atlas, "atlas");
            Objects.requireNonNull(sprite, "sprite");
        }

        @Override
        public Size size() {
            return ITEM_SIZE;
        }
    }

    /**
     * The face of a given skin.
     *
     * @param skin the skin drawn
     * @param hat whether the hat layer is drawn
     */
    record SkinFaceVisual(PlayerSkin skin, boolean hat) implements Visual {
        SkinFaceVisual {
            Objects.requireNonNull(skin, "skin");
        }

        @Override
        public Size size() {
            return PLAYER_FACE_SIZE;
        }
    }
}
