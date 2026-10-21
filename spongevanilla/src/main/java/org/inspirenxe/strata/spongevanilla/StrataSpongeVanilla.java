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
package org.inspirenxe.strata.spongevanilla;

import org.inspirenxe.strata.sponge.StrataSponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.plugin.builtin.jvm.Plugin;

/**
 * SpongeVanilla's entry point, listed in {@code META-INF/sponge_plugins.json}. It marks the jar as the {@code strata}
 * plugin and, on a client, starts Strata-Minecraft when Sponge constructs the plugin, so {@code Strata.screens()}
 * works for plugins that depend on {@code strata} from then on. On a dedicated server it does nothing, because Strata
 * draws on the client only.
 */
@Plugin("strata")
public final class StrataSpongeVanilla {

    /**
     * Creates the entry point; Sponge calls this.
     */
    public StrataSpongeVanilla() {
    }

    /**
     * Starts Strata-Minecraft if the game is a client. Sponge sends this to each plugin in dependency order, so
     * plugins that depend on {@code strata} hear it after Strata has started.
     *
     * @param event the construction event
     */
    @Listener
    public void onConstruct(final ConstructPluginEvent event) {
        if (event.game().platform().type().isClient()) {
            StrataSponge.install();
        }
    }
}
