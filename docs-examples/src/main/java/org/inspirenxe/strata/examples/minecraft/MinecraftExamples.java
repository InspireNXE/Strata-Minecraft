package org.inspirenxe.strata.examples.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.widget.Button;
import org.inspirenxe.strata.examples.quickstart.SettingsScreen;
import org.inspirenxe.strata.minecraft.MinecraftThemes;
import org.inspirenxe.strata.minecraft.StrataScreen;

/**
 * Showing Strata screens in Minecraft 26.3. This code is the same on NeoForge and Fabric.
 */
public final class MinecraftExamples {

    private MinecraftExamples() {
    }

    static void open() {
        // --8<-- [start:open]
        Minecraft.getInstance().gui.setScreen(
                new StrataScreen(Component.literal("Settings"), new SettingsScreen(), MinecraftThemes.vanilla()));
        // --8<-- [end:open]
    }

    static void openFromAnyThread() {
        // --8<-- [start:thread]
        // Network handlers and other threads must hop onto the client thread first
        final Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.gui.setScreen(
                new StrataScreen(Component.literal("Settings"), new SettingsScreen(), MinecraftThemes.vanilla())));
        // --8<-- [end:thread]
    }

    // --8<-- [start:nopause]
    /**
     * A Strata screen that keeps a single-player world running behind it.
     */
    public static final class LiveScreen extends StrataScreen {

        public LiveScreen(final Component title, final org.inspirenxe.strata.api.screen.Screen content) {
            super(title, content, MinecraftThemes.vanilla());
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
    // --8<-- [end:nopause]

    // --8<-- [start:parent]
    /**
     * A Strata screen that goes back to the screen that opened it, instead of to the game.
     */
    public static final class ChildScreen extends StrataScreen {
        private final net.minecraft.client.gui.screens.Screen parent;

        public ChildScreen(final Component title, final org.inspirenxe.strata.api.screen.Screen content,
                           final net.minecraft.client.gui.screens.Screen parent) {
            super(title, content, MinecraftThemes.vanilla());
            this.parent = parent;
        }

        @Override
        public void onClose() {
            // Escape and handle.close() both end up here
            this.minecraft.gui.setScreen(this.parent);
        }
    }
    // --8<-- [end:parent]

    static Theme modTheme() {
        // --8<-- [start:theme]
        // Reads assets/examplemod/textures/gui/sprites/widget/fancy_button.png from the gui atlas
        final SpriteId fancy = SpriteId.of("examplemod", "widget/fancy_button");
        final Theme theme = MinecraftThemes.vanilla().toBuilder()
                .paint(Button.STYLE_NORMAL, Paint.nineSlice(fancy, Insets.of(4f)))
                .build();
        // --8<-- [end:theme]
        return theme;
    }
}
