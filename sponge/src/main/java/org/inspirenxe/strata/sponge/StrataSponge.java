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
package org.inspirenxe.strata.sponge;

import org.inspirenxe.strata.minecraft.StrataMinecraft;

/**
 * Starts Strata-Minecraft on a client that has SpongeAPI: SpongeVanilla, or NeoForge with SpongeNeo installed. The
 * SpongeVanilla plugin calls {@link #install()} when Sponge constructs it, and the NeoForge mod calls it in place of
 * {@link StrataMinecraft#install()} when NeoForge reports SpongeAPI loaded.
 *
 * <p>This is where Strata-Minecraft hands Strata the parts that Sponge implements better than Strata's defaults, such
 * as a cause stack backed by Sponge's once Strata has causes. There are none yet, so it starts Strata-Minecraft as
 * the other loaders do.</p>
 */
public final class StrataSponge {

    private StrataSponge() {
    }

    /**
     * Starts Strata-Minecraft with the Sponge-backed parts. Only the first call does anything. Call it on the client
     * only, after Sponge has been constructed.
     */
    public static void install() {
        StrataMinecraft.install();
    }
}
