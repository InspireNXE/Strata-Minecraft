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

/**
 * Who draws the slots of a {@link StrataContainerScreen}. Slot positions, input, networking, the carried stack and
 * slot tooltips are vanilla's in both modes; only the drawing of the slots themselves differs.
 */
public enum SlotRendering {
    /**
     * Vanilla draws the slots, their items, highlights and drag previews, so whatever other mods draw on slots
     * through vanilla's container screen shows too. The default, and what the player can force for every screen.
     */
    VANILLA,
    /**
     * Strata draws the slots from the theme's slot keys and item visuals. Other mods' drawing on slots does not
     * show on the screen.
     */
    STRATA
}
