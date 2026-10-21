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
package org.inspirenxe.strata.examples.minecraft;

import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.Strata;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.Image;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.api.theme.ThemeOverrides;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.examples.quickstart.SettingsScreen;
import org.inspirenxe.strata.minecraft.MinecraftText;
import org.inspirenxe.strata.minecraft.MinecraftThemeSource;
import org.inspirenxe.strata.minecraft.MinecraftTooltips;
import org.inspirenxe.strata.minecraft.MinecraftVisuals;
import org.inspirenxe.strata.minecraft.SlotRendering;
import org.inspirenxe.strata.minecraft.StrataContainerScreen;
import org.inspirenxe.strata.minecraft.StrataScreen;
import org.inspirenxe.strata.minecraft.control.ItemDisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Showing Strata screens in Minecraft 1.21.1. This code is the same on NeoForge, Forge and Fabric.
 */
public final class MinecraftExamples {

    private MinecraftExamples() {
    }

    static void open() {
        // --8<-- [start:open]
        // From a key mapping, a command, a packet handler or any other thread
        Strata.screens().open(new SettingsScreen());
        // --8<-- [end:open]
    }

    static void openInTheme() {
        // --8<-- [start:thread]
        // The vanilla look with one key changed, for this screen only
        Strata.screens().open(new SettingsScreen(), ThemeRef.platform()
                .override(Button.STYLE_NORMAL, Paint.solid(0xFF2E7D32)));
        // --8<-- [end:thread]
    }

    // --8<-- [start:above]
    /**
     * A list whose button opens a details screen above it. Closing the details screen shows the list again, as the
     * player left it.
     */
    public static final class ListScreen implements org.inspirenxe.strata.api.screen.Screen {
        private ScreenHandle handle;

        @Override
        public org.inspirenxe.strata.api.component.Component<?> build(final ComponentFactory factory) {
            return factory.anchorPanel()
                    .add(factory.button("Details")
                            .onEvent(Button.PRESSED, event -> this.handle.open(new SettingsScreen()))
                            .setAnchor(Anchor.CENTER))
                    .setAnchor(Anchor.FILL);
        }

        @Override
        public org.inspirenxe.strata.api.text.Text title() {
            return org.inspirenxe.strata.api.text.Text.of("Listings");
        }

        @Override
        public void opened(final ScreenHandle handle) {
            this.handle = handle;
        }
    }
    // --8<-- [end:above]

    static void openLive() {
        // --8<-- [start:live]
        // A subclass is a vanilla screen like any other; open it on the client thread
        Minecraft.getInstance().setScreen(new LiveScreen(Component.literal("Settings"), new SettingsScreen()));
        // --8<-- [end:live]
    }

    // --8<-- [start:nopause]
    /**
     * A Strata screen that keeps a single-player world running behind it.
     */
    public static final class LiveScreen extends StrataScreen {

        public LiveScreen(final Component title, final org.inspirenxe.strata.api.screen.Screen content) {
            super(title, content, ThemeRef.platform());
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
    // --8<-- [end:nopause]

    // --8<-- [start:parent]
    /**
     * A Strata screen that goes back to the vanilla screen that opened it, such as the options screen, instead of to
     * the game.
     */
    public static final class ChildScreen extends StrataScreen {
        private final net.minecraft.client.gui.screens.Screen parent;

        public ChildScreen(final Component title, final org.inspirenxe.strata.api.screen.Screen content,
                           final net.minecraft.client.gui.screens.Screen parent) {
            super(title, content, ThemeRef.platform());
            this.parent = parent;
        }

        @Override
        public void onClose() {
            // Escape and handle.close() both end up here
            this.minecraft.setScreen(this.parent);
        }
    }
    // --8<-- [end:parent]

    static ThemeRef modTheme() {
        // --8<-- [start:theme]
        // Reads assets/examplemod/textures/gui/sprites/widget/fancy_button.png from the gui atlas
        final SpriteId fancy = SpriteId.of("examplemod", "widget/fancy_button");
        // Vanilla as resource packs restyle it, with this mod's button on top
        final ThemeRef theme = ThemeRef.platform()
                .override(Button.STYLE_NORMAL, Paint.nineSlice(fancy, Insets.of(4f)));
        // --8<-- [end:theme]
        return theme;
    }

    static Theme fixedTheme() {
        // --8<-- [start:fixed]
        // A theme built in code ignores resource packs
        final Theme theme = Themes.flat().toBuilder()
                .paint(Button.STYLE_NORMAL, Paint.solid(0xFF2E7D32))
                .build();
        // --8<-- [end:fixed]
        return theme;
    }

    static Subscription everywhere() {
        // --8<-- [start:everywhere]
        // Every Strata screen that shows the vanilla theme gets this mod's button, over every pack's theme files
        final SpriteId fancy = SpriteId.of("examplemod", "widget/fancy_button");
        final Subscription buttons = Strata.themes().override(MinecraftThemeSource.VANILLA, ThemeOverrides.of()
                .with(Button.STYLE_NORMAL, Paint.nineSlice(fancy, Insets.of(4f))));

        // A theme of the mod's own, which follows packs for every key it leaves alone
        Strata.themes().define(ThemeId.of("examplemod", "dark"), MinecraftThemeSource.VANILLA, ThemeOverrides.of()
                .with(Window.STYLE_BACKGROUND, Paint.solid(0xF0101010)));
        // --8<-- [end:everywhere]
        return buttons;
    }

    static Image itemIcon(final ComponentFactory factory, final ItemStack stack) {
        // --8<-- [start:item]
        // Drawn by vanilla's item renderer with the count and damage bar, and vanilla's tooltip lines on hover
        final Image icon = factory.image(MinecraftVisuals.item(stack));
        icon.setTooltip(MinecraftTooltips.item(stack));

        // Twice the size, without decorations
        final Image large = factory.image(MinecraftVisuals.item(stack, MinecraftVisuals.ItemDecorations.NONE))
                .setImageSize(32f, 32f);
        // --8<-- [end:item]
        return large;
    }

    static ItemDisplay itemDisplay(final ComponentFactory factory, final ItemStack stack, final long price) {
        // --8<-- [start:display]
        // Vanilla's tooltip lines, made again each time the pointer arrives, with a price line added
        final ItemDisplay display = new ItemDisplay(factory, stack)
                .setTooltipFunction(shown -> {
                    final List<Component> lines = new ArrayList<>(Screen.getTooltipFromItem(Minecraft.getInstance(), shown));
                    lines.add(Component.literal(price + " coins").withStyle(ChatFormatting.GOLD));
                    return MinecraftTooltips.lines(lines);
                });
        // Later, when the listing changes
        display.setStack(stack.copyWithCount(1));
        // --8<-- [end:display]
        return display;
    }

    static Image playerFace(final ComponentFactory factory, final Player player) {
        // --8<-- [start:face]
        // A player in the level carries their skin already
        final Image face = factory.image(MinecraftVisuals.playerFace(player.getGameProfile())).setImageSize(16f, 16f);

        // Only a name: the default skin shows until the real one has loaded
        final Image seller = factory.image(MinecraftVisuals.playerFace(new ResolvableProfile(Optional.of("Ana"), Optional.empty(), new PropertyMap())))
                .setImageSize(12f, 12f);
        // --8<-- [end:face]
        return seller;
    }

    static Label formattedText(final ComponentFactory factory) {
        // --8<-- [start:text]
        // A chat component, in the vanilla theme's white with a shadow where it sets no style
        final Label title = factory.label(MinecraftText.of(
                Component.translatable("container.inventory").withStyle(ChatFormatting.YELLOW)));

        // Section codes, over a gray base without a shadow
        final Label motd = factory.label(MinecraftText.of("§aOnline §r- 12 players"))
                .setStyle(MinecraftText.DEFAULT_STYLE.withColor(0xFFA0A0A0).withShadow(false));
        // --8<-- [end:text]
        return motd;
    }

    // --8<-- [start:container]
    /**
     * A crate's screen: a three-row chest menu with the title above the slots, drawn by Strata unless the player
     * forces vanilla drawing.
     */
    static StrataContainerScreen<ChestMenu> crateScreen(final ChestMenu menu, final Inventory inventory, final Component title) {
        // 176 by 168 is vanilla's size for three rows; the menu's slot positions assume it
        return new StrataContainerScreen<>(menu, inventory, title, 176, 168,
                (ui, crate, slots) -> ui.anchorPanel()
                        .add(ui.stackPanel().setSpacing(4)
                                .add(ui.label(MinecraftText.of(title)))
                                .add(slots)
                                .setAnchor(Anchor.CENTER))
                        .setAnchor(Anchor.FILL),
                ThemeRef.platform(), SlotRendering.STRATA);
    }
    // --8<-- [end:container]
}
