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
/**
 * The Minecraft 26.3 adapter, written against vanilla classes only so that the NeoForge and Fabric jars share it.
 *
 * <p>Most mods need only {@link org.inspirenxe.strata.minecraft.StrataScreen}, a vanilla screen that shows one
 * Strata screen, and {@link org.inspirenxe.strata.minecraft.MinecraftThemes#vanilla()}, a theme drawn with
 * vanilla's widget sprites. {@link org.inspirenxe.strata.minecraft.MinecraftVisuals} shows item stacks through the
 * API, and {@link org.inspirenxe.strata.minecraft.MinecraftTooltips} builds tooltips from vanilla text. The other
 * classes implement the SPI over vanilla:
 * {@link org.inspirenxe.strata.minecraft.MinecraftCanvas} draws through {@code GuiGraphicsExtractor},
 * {@link org.inspirenxe.strata.minecraft.MinecraftPlatform} and
 * {@link org.inspirenxe.strata.minecraft.MinecraftTextMetrics} wrap the client and its font, and
 * {@link org.inspirenxe.strata.minecraft.MinecraftInput} translates vanilla's input codes.</p>
 *
 * <p>Everything here runs on the client and must be used on the render thread.</p>
 */
package org.inspirenxe.strata.minecraft;
