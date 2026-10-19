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
