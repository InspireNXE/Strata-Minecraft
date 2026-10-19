package org.inspirenxe.strata.examples.minecraft;

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
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.Image;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.examples.quickstart.SettingsScreen;
import org.inspirenxe.strata.minecraft.MinecraftText;
import org.inspirenxe.strata.minecraft.MinecraftThemes;
import org.inspirenxe.strata.minecraft.MinecraftTooltips;
import org.inspirenxe.strata.minecraft.MinecraftVisuals;
import org.inspirenxe.strata.minecraft.SlotRendering;
import org.inspirenxe.strata.minecraft.StrataContainerScreen;
import org.inspirenxe.strata.minecraft.StrataScreen;
import org.inspirenxe.strata.minecraft.control.ItemDisplay;

import java.util.ArrayList;
import java.util.List;

/**
 * Showing Strata screens in Minecraft 26.3. This code is the same on NeoForge and Fabric.
 */
public final class MinecraftExamples {

    private MinecraftExamples() {
    }

    static void open() {
        // --8<-- [start:open]
        Minecraft.getInstance().gui.setScreen(
                new StrataScreen(Component.literal("Settings"), new SettingsScreen(), ThemeRef.of(MinecraftThemes.VANILLA)));
        // --8<-- [end:open]
    }

    static void openFromAnyThread() {
        // --8<-- [start:thread]
        // Network handlers and other threads must hop onto the client thread first
        final Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.gui.setScreen(
                new StrataScreen(Component.literal("Settings"), new SettingsScreen(), ThemeRef.of(MinecraftThemes.VANILLA))));
        // --8<-- [end:thread]
    }

    // --8<-- [start:nopause]
    /**
     * A Strata screen that keeps a single-player world running behind it.
     */
    public static final class LiveScreen extends StrataScreen {

        public LiveScreen(final Component title, final org.inspirenxe.strata.api.screen.Screen content) {
            super(title, content, ThemeRef.of(MinecraftThemes.VANILLA));
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
            super(title, content, ThemeRef.of(MinecraftThemes.VANILLA));
            this.parent = parent;
        }

        @Override
        public void onClose() {
            // Escape and handle.close() both end up here
            this.minecraft.gui.setScreen(this.parent);
        }
    }
    // --8<-- [end:parent]

    static ThemeRef modTheme() {
        // --8<-- [start:theme]
        // Reads assets/examplemod/textures/gui/sprites/widget/fancy_button.png from the gui atlas
        final SpriteId fancy = SpriteId.of("examplemod", "widget/fancy_button");
        // Vanilla as resource packs restyle it, with this mod's button on top
        final ThemeRef theme = ThemeRef.of(MinecraftThemes.VANILLA)
                .override(Button.STYLE_NORMAL, Paint.nineSlice(fancy, Insets.of(4f)));
        // --8<-- [end:theme]
        return theme;
    }

    static Theme fixedTheme() {
        // --8<-- [start:fixed]
        // A theme built in code ignores resource packs
        final Theme theme = MinecraftThemes.vanilla().toBuilder()
                .paint(Button.STYLE_NORMAL, Paint.solid(0xFF2E7D32))
                .build();
        // --8<-- [end:fixed]
        return theme;
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
        final Image seller = factory.image(MinecraftVisuals.playerFace(ResolvableProfile.createUnresolved("Ana")))
                .setImageSize(12f, 12f);
        // --8<-- [end:face]
        return seller;
    }

    static Label formattedText(final ComponentFactory factory) {
        // --8<-- [start:text]
        // A chat component, in the vanilla theme's white with a shadow where it sets no style
        final Label title = factory.label().setSpans(MinecraftText.spans(
                Component.translatable("container.inventory").withStyle(ChatFormatting.YELLOW)));

        // Section codes, over a gray base without a shadow
        final Label motd = factory.label().setSpans(MinecraftText.spans("§aOnline §r- 12 players",
                MinecraftText.DEFAULT_STYLE.withColor(0xFFA0A0A0).withShadow(false)));
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
                                .add(ui.label().setSpans(MinecraftText.spans(title)))
                                .add(slots)
                                .setAnchor(Anchor.CENTER))
                        .setAnchor(Anchor.FILL),
                ThemeRef.of(MinecraftThemes.VANILLA), SlotRendering.STRATA);
    }
    // --8<-- [end:container]
}
