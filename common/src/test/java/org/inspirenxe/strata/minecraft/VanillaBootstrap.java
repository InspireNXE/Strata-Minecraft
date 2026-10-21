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

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * Starts vanilla's registries once per test JVM, for tests that make real item stacks.
 */
public final class VanillaBootstrap {
    private static boolean done;

    private VanillaBootstrap() {
    }

    /**
     * Runs vanilla's {@code Bootstrap}, which registers the items with their default components. Later calls do
     * nothing.
     */
    public static synchronized void run() {
        if (done) {
            return;
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        done = true;
    }
}
