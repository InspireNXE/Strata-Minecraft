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
package org.inspirenxe.strata.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.inspirenxe.strata.minecraft.StrataMinecraft;
import org.inspirenxe.strata.sponge.StrataSponge;

/**
 * Forge entry point. It marks the jar as the {@code strata} mod and starts Strata-Minecraft on the client when the
 * mod is constructed, so {@code Strata.screens()} works for dependent mods from then on. The jar is marked client
 * side only, and on a dedicated server the constructor does nothing.
 *
 * <p>SpongeAPI is optional. When SpongeForge is installed, Forge lists SpongeAPI as the mod {@value #SPONGE_API},
 * constructs it before Strata, and Strata starts through {@link StrataSponge}. Without it, no Sponge class is
 * loaded.</p>
 */
@Mod("strata")
public final class StrataForge {
    /**
     * The mod id SpongeForge gives SpongeAPI.
     */
    static final String SPONGE_API = "spongeapi";

    /**
     * Starts Strata-Minecraft on the client; Forge calls this when it constructs the mod.
     */
    public StrataForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            StrataForge.install(ModList.get().isLoaded(StrataForge.SPONGE_API));
        }
    }

    /**
     * Starts Strata-Minecraft, through {@link StrataSponge} when SpongeAPI is loaded. Nothing outside
     * {@code org.inspirenxe.strata.sponge} names a Sponge type, so the other branch never needs one.
     *
     * @param sponge whether SpongeAPI is loaded
     */
    static void install(final boolean sponge) {
        if (sponge) {
            StrataSponge.install();
        } else {
            StrataMinecraft.install();
        }
    }
}
