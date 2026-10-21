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
package org.inspirenxe.strata.fabric;

import net.fabricmc.api.ClientModInitializer;
import org.inspirenxe.strata.minecraft.StrataMinecraft;

/**
 * Fabric's client entry point, listed in {@code fabric.mod.json}. It starts Strata-Minecraft, so
 * {@code Strata.screens()} works for dependent mods from then on.
 */
public final class StrataFabric implements ClientModInitializer {

    /**
     * Creates the entry point; Fabric Loader calls this.
     */
    public StrataFabric() {
    }

    @Override
    public void onInitializeClient() {
        StrataMinecraft.install();
    }
}
