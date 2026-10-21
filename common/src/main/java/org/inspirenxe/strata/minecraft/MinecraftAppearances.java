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
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.inspirenxe.strata.api.appearance.Appearance;
import org.inspirenxe.strata.core.StrataCore;

/**
 * The appearances Strata-Minecraft gives vanilla types, beneath anything a mod registers for them:
 * <ul>
 *     <li>{@link ItemStack}: the {@linkplain MinecraftVisuals#item(ItemStack) item visual} with every decoration, its
 *     hover name, and the tooltip vanilla shows for it in a slot, asked anew each time so it follows F3+H.</li>
 *     <li>{@link GameProfile}: the player's face with the hat layer, and the player's name.</li>
 * </ul>
 */
final class MinecraftAppearances {
    private static boolean installed;

    private MinecraftAppearances() {
    }

    /**
     * Registers the defaults the first time it is called. Every {@link MinecraftPlatform} calls it.
     */
    static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        StrataCore.registerDefaultAppearance(ItemStack.class, Appearance.<ItemStack>builder()
                .icon(MinecraftVisuals::item)
                .name(stack -> MinecraftText.of(stack.getHoverName()))
                .tooltip(MinecraftTooltips::item)
                .build());
        StrataCore.registerDefaultAppearance(GameProfile.class, Appearance.<GameProfile>builder()
                .icon(MinecraftVisuals::playerFace)
                .name(profile -> MinecraftText.of(Component.literal(profile.getName())))
                .build());
    }
}
