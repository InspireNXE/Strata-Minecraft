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
 * The Minecraft 1.21.1 adapter, written against vanilla classes only so that the NeoForge, Forge and Fabric jars
 * share it.
 *
 * <p>Most mods need nothing here: {@code Strata.screens()} opens their screens, each in a
 * {@link org.inspirenxe.strata.minecraft.StrataScreen}, a vanilla screen that shows one Strata screen, in
 * {@link org.inspirenxe.strata.minecraft.MinecraftThemeSource#VANILLA}, a theme drawn with vanilla's widget sprites
 * that follows resource packs and is the platform's default theme.
 * {@link org.inspirenxe.strata.minecraft.StrataMinecraft} hands the controller over when the mod loads.
 * {@link org.inspirenxe.strata.minecraft.StrataContainerScreen} does the same for a container menu, with its slots in a
 * {@link org.inspirenxe.strata.minecraft.MenuPanel}. {@link org.inspirenxe.strata.minecraft.MinecraftVisuals} shows
 * item stacks and player faces through the API, {@link org.inspirenxe.strata.minecraft.MinecraftText} turns chat
 * components and {@code §} codes into styled spans, {@link org.inspirenxe.strata.minecraft.MinecraftTooltips} builds
 * tooltips from vanilla text, and {@link org.inspirenxe.strata.minecraft.control} holds components for Minecraft
 * objects. The other classes implement the SPI over vanilla:
 * {@link org.inspirenxe.strata.minecraft.MinecraftCanvas} draws through {@code GuiGraphics},
 * {@link org.inspirenxe.strata.minecraft.MinecraftPlatform} and
 * {@link org.inspirenxe.strata.minecraft.MinecraftTextMetrics} wrap the client and its font, and
 * {@link org.inspirenxe.strata.minecraft.MinecraftInput} translates vanilla's input codes.</p>
 *
 * <p>Everything here runs on the client and must be used on the render thread.</p>
 */
package org.inspirenxe.strata.minecraft;
