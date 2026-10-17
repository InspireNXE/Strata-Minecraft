package org.inspirenxe.strata.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/**
 * NeoForge entry point. Strata is a library, so this only marks the jar as the {@code strata} mod; dependent mods
 * open {@code StrataScreen}s themselves.
 */
@Mod(value = "strata", dist = Dist.CLIENT)
public final class StrataNeoForge {
}
