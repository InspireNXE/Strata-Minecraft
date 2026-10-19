package org.inspirenxe.strata.minecraft;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;

/**
 * Starts vanilla's registries once per test JVM, for tests that make real item stacks.
 */
public final class VanillaBootstrap {
    private static boolean done;

    private VanillaBootstrap() {
    }

    /**
     * Runs vanilla's {@code Bootstrap} and binds the items' default components, which a world load does in game.
     * Later calls do nothing.
     */
    public static synchronized void run() {
        if (done) {
            return;
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createWorldLookup())
                .forEach(DataComponentInitializers.PendingComponents::apply);
        done = true;
    }
}
