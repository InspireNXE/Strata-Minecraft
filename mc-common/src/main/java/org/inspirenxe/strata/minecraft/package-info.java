/**
 * The Minecraft 26.3 adapter, written against vanilla classes only so that the NeoForge and Fabric jars share it.
 *
 * <p>Most mods need only {@link org.inspirenxe.strata.minecraft.StrataScreen}, a vanilla screen that shows one
 * Strata screen, and {@link org.inspirenxe.strata.minecraft.MinecraftThemes#vanilla()}, a theme drawn with
 * vanilla's widget sprites. The other classes implement the SPI over vanilla:
 * {@link org.inspirenxe.strata.minecraft.MinecraftCanvas} draws through {@code GuiGraphicsExtractor},
 * {@link org.inspirenxe.strata.minecraft.MinecraftPlatform} and
 * {@link org.inspirenxe.strata.minecraft.MinecraftTextMetrics} wrap the client and its font, and
 * {@link org.inspirenxe.strata.minecraft.MinecraftInput} translates vanilla's input codes.</p>
 *
 * <p>Everything here runs on the client and must be used on the render thread.</p>
 */
package org.inspirenxe.strata.minecraft;
