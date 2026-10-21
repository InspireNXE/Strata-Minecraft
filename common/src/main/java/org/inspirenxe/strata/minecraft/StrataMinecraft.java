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

import org.inspirenxe.strata.api.Strata;
import org.inspirenxe.strata.core.StrataCore;

/**
 * Starts Strata-Minecraft. The NeoForge and Fabric entry points call {@link #install()} when the mod loads, so mods
 * never need to.
 */
public final class StrataMinecraft {
    private static final System.Logger LOGGER = System.getLogger(StrataMinecraft.class.getName());

    private static boolean installed;

    private StrataMinecraft() {
    }

    /**
     * Hands Strata-Minecraft's screen controller to Strata, so {@code Strata.screens()} opens screens as
     * {@link StrataScreen}s, and checks that Strata's core is found the way mods reach it. Only the first call does
     * anything. A core that cannot be found is logged, not thrown, so the game still starts and the error shows
     * where a mod first asks for Strata.
     */
    public static synchronized void install() {
        if (installed) {
            return;
        }
        installed = true;
        StrataCore.setScreenController(MinecraftScreens.INSTANCE);
        try {
            if (Strata.screens() != MinecraftScreens.INSTANCE) {
                LOGGER.log(System.Logger.Level.ERROR, "Strata's core answered with another screen controller;"
                        + " is a second copy of strata-core loaded?");
            }
        } catch (final IllegalStateException e) {
            LOGGER.log(System.Logger.Level.ERROR, "Strata's core could not be found: " + e.getMessage());
        }
    }
}
