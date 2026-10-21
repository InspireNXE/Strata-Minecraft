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
 * Starts Strata-Minecraft on a client that has SpongeAPI: NeoForge with SpongeNeo installed, or Forge with
 * SpongeForge. The NeoForge and Forge mods call {@link #install()} in place of {@link StrataMinecraft#install()} when
 * their loader reports SpongeAPI loaded.
 *
 * <p>This is where Strata-Minecraft hands Strata the parts Sponge implements in place of Strata's defaults. Today that
 * is the cause stack: Strata's causes are backed by Sponge's, through {@link SpongeCauseStack}.</p>
 */
public final class StrataSponge {

    private StrataSponge() {
    }

    /**
     * Starts Strata-Minecraft with the Sponge-backed parts. Only the first call does anything, and nothing changes if
     * Strata-Minecraft was already started without them. Call it on the client only. It does not touch Sponge itself,
     * so it may run before Sponge's client exists.
     */
    public static void install() {
        StrataMinecraft.install(new SpongeCauseStack());
    }
}
