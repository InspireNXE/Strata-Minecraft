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
 * The parts of Strata-Minecraft that need SpongeAPI, compiled into the SpongeVanilla plugin and into the NeoForge mod.
 *
 * <p>On NeoForge, SpongeAPI is there only when SpongeNeo is installed. The NeoForge mod checks for it before it calls
 * anything in this package, so classes here may name Sponge types freely. Nothing else in the NeoForge jar may.</p>
 */
package org.inspirenxe.strata.sponge;
