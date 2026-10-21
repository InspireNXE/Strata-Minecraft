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

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.overlay.Overlays;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.core.StrataCore;
import org.inspirenxe.strata.spi.HeldKeys;
import org.inspirenxe.strata.spi.ScreenHost;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A vanilla container screen whose contents are a Strata tree, for menus a server opens with
 * {@code player.openMenu}. Register it for the menu's type as you would any container screen:
 *
 * <pre>{@code
 * // Forge, in FMLClientSetupEvent, which makes MenuScreens.register public
 * event.enqueueWork(() -> MenuScreens.register(GRINDER, (menu, inventory, title) -> new StrataContainerScreen<>(menu,
 *         inventory, title, GrinderScreen::build, ThemeRef.platform())));
 * // Fabric, in the client entry point; Fabric API opens MenuScreens.register
 * MenuScreens.register(GRINDER, (menu, inventory, title) -> new StrataContainerScreen<>(menu, inventory, title,
 *         GrinderScreen::build, ThemeRef.platform()));
 * }</pre>
 *
 * <p>Everything other code can observe stays vanilla's, so recipe viewers, inventory mods and any server work with
 * the screen as with any other container screen:</p>
 * <ul>
 *     <li>It is an {@link AbstractContainerScreen} over the mod's own menu. Slots stay where the menu put them,
 *     relative to {@code leftPos} and {@code topPos}, which follow the {@link MenuPanel}'s position after every
 *     layout.</li>
 *     <li>Strata sees every press first, so popups close and focus moves as on any Strata screen. A press, release
 *     or drag also goes to vanilla's own container handling when no modal dialog is open and the pointer is over
 *     the menu panel, over the tree's root, or over nothing. Releases and drags follow the press. Scrolls over the
 *     panel go to vanilla first. Keys go to Strata first and, unless it uses them or a modal dialog is open, to
 *     vanilla, so the hotbar, drop, pick-block and inventory keys work as the player bound them.</li>
 *     <li>The screen draws the carried stack after Strata, over everything, and the hovered slot's tooltip after
 *     it, except while the pointer is outside the panel or a modal dialog is open. Vanilla's own drawing inside the
 *     panel leaves the carried stack out, except during a drag, when its previews need the stack and both draw
 *     it.</li>
 *     <li>The slots are drawn by vanilla or by Strata, as {@link SlotRendering} describes. The player can force
 *     vanilla drawing in {@code config/strata-client.json}, read each time a screen opens: see
 *     {@link #slotRendering()}. In Strata mode the screen still does what vanilla's own slot drawing does
 *     besides drawing: it tracks the hovered slot, and drops a slot that stops accepting a dragged stack from the
 *     drag.</li>
 *     <li>Vanilla's title and inventory labels are not drawn; put labels in the tree. The world behind is dimmed
 *     with vanilla's gradient, as for any container screen, and the game does not pause.</li>
 * </ul>
 *
 * <p>If the tree does not hold the panel, or holds it inside a {@link ScrollPanel}, the screen throws
 * {@link IllegalStateException} at the first layout. While the panel is hidden or removed later, no input goes to
 * vanilla and no slot is hovered. Use the screen on the render thread.</p>
 *
 * @param <M> the menu type
 */
public class StrataContainerScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    private static final int DEFAULT_IMAGE_WIDTH = 176;
    private static final int DEFAULT_IMAGE_HEIGHT = 166;
    // Far enough away that vanilla's slot geometry never matches the pointer
    private static final int HIDDEN_POS = -100_000;

    private final ContainerContent<? super M> content;
    private final Theme theme;
    private final ThemeRef themeRef;
    private final SlotRendering requested;
    private final HeldKeys heldKeys;
    private final Set<Integer> vanillaButtons;
    private final PanelOwner owner;
    private SlotRendering rendering;
    private MinecraftCanvas canvas;
    private ScreenHost host;
    private MenuPanel panel;
    private int quickCraftType;
    private GuiGraphics frameGraphics;
    private int frameMouseX;
    private int frameMouseY;
    private float frameA;
    /**
     * The first half of a character typed as two {@code char}s, until the second arrives.
     */
    private char highSurrogate;

    /**
     * Creates the screen for a menu of vanilla's default size, 176 by 166, with vanilla drawing the slots.
     *
     * @param menu the menu, from the screen constructor vanilla calls
     * @param inventory the player's inventory, from the same call
     * @param title the title, from the same call; used for narration
     * @param content builds the Strata tree around the slots
     * @param theme the theme to resolve, for example {@code ThemeRef.platform()}, the vanilla theme
     */
    public StrataContainerScreen(final M menu, final Inventory inventory, final Component title,
                                 final ContainerContent<? super M> content, final ThemeRef theme) {
        this(menu, inventory, title, DEFAULT_IMAGE_WIDTH, DEFAULT_IMAGE_HEIGHT, content, theme, SlotRendering.VANILLA);
    }

    /**
     * Creates the screen with a theme that follows resource packs.
     *
     * @param menu the menu, from the screen constructor vanilla calls
     * @param inventory the player's inventory, from the same call
     * @param title the title, from the same call; used for narration
     * @param imageWidth the width of the slot area, which the menu's slot positions are relative to
     * @param imageHeight the height of the slot area
     * @param content builds the Strata tree around the slots
     * @param theme the theme to resolve, for example {@code ThemeRef.platform()}, the vanilla theme
     * @param slotRendering who should draw the slots; the player's settings may turn {@link SlotRendering#STRATA}
     *                      into {@link SlotRendering#VANILLA}
     */
    public StrataContainerScreen(final M menu, final Inventory inventory, final Component title, final int imageWidth,
                                 final int imageHeight, final ContainerContent<? super M> content, final ThemeRef theme,
                                 final SlotRendering slotRendering) {
        super(menu, inventory, title);
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.content = Objects.requireNonNull(content, "content");
        this.theme = null;
        this.themeRef = Objects.requireNonNull(theme, "theme");
        this.requested = Objects.requireNonNull(slotRendering, "slotRendering");
        this.heldKeys = new HeldKeys();
        this.vanillaButtons = new HashSet<>();
        this.owner = new PanelOwner();
    }

    /**
     * Creates the screen with a fixed theme.
     *
     * @param menu the menu, from the screen constructor vanilla calls
     * @param inventory the player's inventory, from the same call
     * @param title the title, from the same call; used for narration
     * @param imageWidth the width of the slot area, which the menu's slot positions are relative to
     * @param imageHeight the height of the slot area
     * @param content builds the Strata tree around the slots
     * @param theme the theme, which resource packs never change; to look like the game and follow packs, pass
     *              {@code ThemeRef.platform()} instead
     * @param slotRendering who should draw the slots; the player's settings may turn {@link SlotRendering#STRATA}
     *                      into {@link SlotRendering#VANILLA}
     */
    public StrataContainerScreen(final M menu, final Inventory inventory, final Component title, final int imageWidth,
                                 final int imageHeight, final ContainerContent<? super M> content, final Theme theme,
                                 final SlotRendering slotRendering) {
        super(menu, inventory, title);
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.content = Objects.requireNonNull(content, "content");
        this.theme = Objects.requireNonNull(theme, "theme");
        this.themeRef = null;
        this.requested = Objects.requireNonNull(slotRendering, "slotRendering");
        this.heldKeys = new HeldKeys();
        this.vanillaButtons = new HashSet<>();
        this.owner = new PanelOwner();
    }

    /**
     * Returns who draws the slots: what the constructor asked for, unless the player forces
     * {@link SlotRendering#VANILLA} with {@code "forceVanillaSlots": true} or by listing the menu's type in
     * {@code "forceVanillaSlotsFor"}. Decided when the screen first opens.
     *
     * @return the mode in use; the requested one before the screen has opened
     */
    public SlotRendering slotRendering() {
        return this.rendering == null ? this.requested : this.rendering;
    }

    @Override
    protected void init() {
        super.init();
        if (this.host == null) {
            this.rendering = StrataClientConfig.load(StrataClientConfig.file(this.minecraft.gameDirectory.toPath()))
                    .apply(this.requested, menuTypeId(this.menu));
            this.canvas = new MinecraftCanvas(this.font);
            final MinecraftPlatform platform = new MinecraftPlatform(this.minecraft, this.canvas.textMetrics(), this);
            this.host = this.themeRef != null
                    ? StrataCore.createScreenHost(platform, this.themeRef, this::onClose)
                    : StrataCore.createScreenHost(platform, this.theme, this::onClose);
            this.host.resize(this.width, this.height);
            this.host.open(factory -> {
                this.panel = new MenuPanel(factory, this.owner);
                return this.content.build(factory, this.menu, this.panel);
            });
        } else {
            this.host.resize(this.width, this.height);
        }
        this.placeMenu(true);
    }

    @Override
    public void removed() {
        if (this.host != null) {
            this.host.close();
            this.host = null;
        }
        if (this.canvas != null) {
            this.canvas.resetCursor();
        }
        this.heldKeys.clear();
        this.highSurrogate = 0;
        this.vanillaButtons.clear();
        super.removed();
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float a) {
        this.placeMenu(false);
        this.frameGraphics = graphics;
        this.frameMouseX = mouseX;
        this.frameMouseY = mouseY;
        this.frameA = a;
        // Vanilla's background, the dimmed world; vanilla's container drawing runs inside the panel
        this.renderBackground(graphics);
        if (this.rendering == SlotRendering.STRATA) {
            // Vanilla sets this while drawing its slots, which Strata mode skips
            this.hover(this.pointerOverMenu() ? this.slotAt(mouseX, mouseY) : null);
        }
        this.canvas.begin(graphics);
        this.host.render(this.canvas);
        // Over everything Strata drew
        this.canvas.nextLayer();
        this.renderCarried(graphics, mouseX, mouseY);
        if (this.pointerOverMenu()) {
            this.renderTooltip(graphics, mouseX, mouseY);
        }
        this.canvas.end();
    }

    @Override
    protected void renderBg(final GuiGraphics graphics, final float a, final int mouseX, final int mouseY) {
        // Strata draws the screen
    }

    @Override
    protected void renderLabels(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        // Strata draws the screen's text
    }

    /**
     * Draws the carried stack at the pointer as vanilla does, with the count a drag leaves on it.
     */
    private void renderCarried(final GuiGraphics graphics, final int mouseX, final int mouseY) {
        ItemStack carried = this.menu.getCarried();
        if (carried.isEmpty()) {
            return;
        }
        String count = null;
        if (this.isQuickCrafting && this.quickCraftSlots.size() > 1) {
            // Vanilla keeps its own count private, so it is worked out again
            carried = carried.copyWithCount(quickCraftRemainder(carried, this.quickCraftSlots, this.quickCraftType));
            if (carried.isEmpty()) {
                count = ChatFormatting.YELLOW + "0";
            }
        }
        graphics.pose().pushPose();
        try {
            // Vanilla's offset for a floating item
            graphics.pose().translate(0f, 0f, 232f);
            graphics.renderItem(carried, mouseX - 8, mouseY - 8);
            graphics.renderItemDecorations(this.font, carried, mouseX - 8, mouseY - 8, count);
        } finally {
            graphics.pose().popPose();
        }
    }

    private void drawVanillaSlots() {
        // Vanilla's own container drawing: slots, highlights and drag previews, with other mods' additions. The
        // carried stack is drawn last, over Strata, so vanilla leaves it out unless a drag's previews need it.
        final ItemStack carried = this.menu.getCarried();
        final boolean hideCarried = !this.isQuickCrafting && !carried.isEmpty();
        if (hideCarried) {
            this.menu.setCarried(ItemStack.EMPTY);
        }
        try {
            super.render(this.frameGraphics, this.frameMouseX, this.frameMouseY, this.frameA);
        } finally {
            if (hideCarried) {
                this.menu.setCarried(carried);
            }
            // Clears the depth vanilla's items leave, so Strata content after the panel covers them
            this.canvas.nextLayer();
        }
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    public void mouseMoved(final double x, final double y) {
        this.host.pointerMoved((float) x, (float) y, Modifiers.NONE);
    }

    @Override
    public boolean mouseClicked(final double x, final double y, final int button) {
        final Overlays overlays = this.host.factory().overlays();
        final int openBefore = overlays.open().size();
        final boolean used = this.host.pointerButton((float) x, (float) y, MinecraftInput.button(button), true,
                MinecraftInput.heldModifiers());
        if (overlays.open().size() < openBefore || !this.pointerOverMenu()) {
            // The press closed a popup, or landed on Strata content
            return used;
        }
        final boolean wasQuickCrafting = this.isQuickCrafting;
        this.vanillaButtons.add(button);
        super.mouseClicked(x, y, button);
        if (!wasQuickCrafting && this.isQuickCrafting) {
            // Vanilla keeps the drag's type private; it follows from the button that started it
            this.quickCraftType = button == InputConstants.MOUSE_BUTTON_LEFT ? 0
                    : button == InputConstants.MOUSE_BUTTON_RIGHT ? 1 : 2;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(final double x, final double y, final int button) {
        final boolean used = this.host.pointerButton((float) x, (float) y, MinecraftInput.button(button), false,
                MinecraftInput.heldModifiers());
        if (this.vanillaButtons.remove(button)) {
            super.mouseReleased(x, y, button);
            return true;
        }
        return used;
    }

    @Override
    public boolean mouseDragged(final double x, final double y, final int button, final double dx, final double dy) {
        if (this.vanillaButtons.contains(button)) {
            return super.mouseDragged(x, y, button, dx, dy);
        }
        // Strata follows drags through mouseMoved
        return false;
    }

    @Override
    public boolean mouseScrolled(final double x, final double y, final double delta) {
        if (this.pointerOverMenu() && super.mouseScrolled(x, y, delta)) {
            return true;
        }
        // Minecraft 1.20.1 scrolls on one axis only
        return this.host.scroll((float) x, (float) y, 0f, (float) delta, MinecraftInput.scrollKind(0.0, delta),
                Modifiers.NONE);
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        final Key key = MinecraftInput.key(keyCode);
        final KeyAction action = this.heldKeys.press(key);
        if (this.host.key(key, action, MinecraftInput.modifiers(modifiers))) {
            return true;
        }
        if (hasModal(this.host.factory().overlays())) {
            return false;
        }
        if (this.pointerOverMenu()) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        // Vanilla finds the hovered slot by position alone; keep slot keys away from slots Strata content covers
        final Slot hovered = this.hoveredSlot;
        this.hoveredSlot = null;
        try {
            return super.keyPressed(keyCode, scanCode, modifiers);
        } finally {
            this.hoveredSlot = hovered;
        }
    }

    @Override
    public boolean keyReleased(final int keyCode, final int scanCode, final int modifiers) {
        final Key key = MinecraftInput.key(keyCode);
        this.heldKeys.release(key);
        return this.host.key(key, KeyAction.RELEASE, MinecraftInput.modifiers(modifiers))
                || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char character, final int modifiers) {
        if (Character.isHighSurrogate(character)) {
            this.highSurrogate = character;
            return true;
        }
        final int codepoint = Character.isLowSurrogate(character) && this.highSurrogate != 0
                ? Character.toCodePoint(this.highSurrogate, character) : character;
        this.highSurrogate = 0;
        return this.host.charTyped(codepoint, Modifiers.NONE) || super.charTyped(character, modifiers);
    }

    // ------------------------------------------------------------------
    // The panel and the pointer
    // ------------------------------------------------------------------

    /**
     * Lays the tree out and moves vanilla's origin to the panel.
     *
     * @param first whether this is the layout after opening or resizing, which checks where the panel is
     */
    private void placeMenu(final boolean first) {
        this.host.layout();
        final MenuPanel shown = this.panel;
        if (shown == null || !shown.isAttached()) {
            if (first) {
                throw new IllegalStateException("The content of a StrataContainerScreen must add its MenuPanel to the tree");
            }
            this.hideMenu();
            return;
        }
        shown.ancestor(ScrollPanel.class).ifPresent(scroll -> {
            throw new IllegalStateException("A MenuPanel cannot be inside a ScrollPanel: " + scroll);
        });
        if (!isShown(shown)) {
            this.hideMenu();
            return;
        }
        final Rect bounds = shown.bounds();
        this.leftPos = Math.round(bounds.x());
        this.topPos = Math.round(bounds.y());
    }

    private void hideMenu() {
        this.leftPos = HIDDEN_POS;
        this.topPos = HIDDEN_POS;
        this.hover(null);
    }

    /**
     * Sets the hovered slot, as vanilla's drawing does.
     */
    private void hover(final Slot slot) {
        this.hoveredSlot = slot;
    }

    /**
     * Returns how many of the carried stack a drag keeps, as vanilla's private {@code recalculateQuickCraftRemaining}
     * works it out.
     *
     * @param carried the carried stack
     * @param slots the slots in the drag
     * @param type the drag's type: 0 spreads evenly, 1 places one each, 2 clones
     * @return the count left on the cursor
     */
    static int quickCraftRemainder(final ItemStack carried, final Set<Slot> slots, final int type) {
        if (carried.isEmpty()) {
            return 0;
        }
        if (type == 2) {
            return carried.getMaxStackSize();
        }
        int remainder = carried.getCount();
        for (final Slot slot : slots) {
            final ItemStack inSlot = slot.getItem();
            final int carry = inSlot.isEmpty() ? 0 : inSlot.getCount();
            final int max = Math.min(carried.getMaxStackSize(), slot.getMaxStackSize(carried));
            final int count = Math.min(AbstractContainerMenu.getQuickCraftPlaceCount(slots, type, carried) + carry, max);
            remainder -= count - carry;
        }
        return remainder;
    }

    private static boolean isShown(final org.inspirenxe.strata.api.component.Component<?> component) {
        for (org.inspirenxe.strata.api.component.Component<?> c = component; c != null; c = c.parent()) {
            if (!c.isVisible()) {
                return false;
            }
        }
        return true;
    }

    private boolean pointerOverMenu() {
        return this.host != null && this.panel != null && this.leftPos != HIDDEN_POS
                && routesToMenu(this.host.hovered(), this.host.factory().overlays(), this.panel);
    }

    /**
     * Returns whether pointer input belongs to vanilla's container handling: no modal overlay is open, and the
     * pointer is over nothing, over the root of the content, or over the panel or anything inside it.
     *
     * @param hovered the deepest Strata component under the pointer
     * @param overlays the host's overlays
     * @param panel the menu panel
     * @return true to hand the input to vanilla
     */
    static boolean routesToMenu(final Optional<org.inspirenxe.strata.api.component.Component<?>> hovered,
                                final Overlays overlays, final MenuPanel panel) {
        if (hasModal(overlays)) {
            return false;
        }
        if (hovered.isEmpty()) {
            return true;
        }
        final org.inspirenxe.strata.api.component.Component<?> target = hovered.get();
        if (overlays.of(target).isPresent()) {
            return false;
        }
        if (target.parent() == null) {
            return true;
        }
        for (org.inspirenxe.strata.api.component.Component<?> c = target; c != null; c = c.parent()) {
            if (c == panel) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasModal(final Overlays overlays) {
        for (final Overlay overlay : overlays.open()) {
            if (overlay.isModal()) {
                return true;
            }
        }
        return false;
    }

    private Slot slotAt(final double x, final double y) {
        for (final Slot slot : this.menu.slots) {
            if (slot.isActive() && this.isHovering(slot.x, slot.y, 16, 16, x, y)) {
                return slot;
            }
        }
        return null;
    }

    // Forge deprecates vanilla's registries in favor of its own, which hold the same entries
    @SuppressWarnings("deprecation")
    private static ResourceLocation menuTypeId(final AbstractContainerMenu menu) {
        try {
            return BuiltInRegistries.MENU.getKey(menu.getType());
        } catch (final UnsupportedOperationException noType) {
            // The player's inventory and other menus made without a type
            return null;
        }
    }

    /**
     * Gives the panel what it needs from this screen without making it public.
     */
    private final class PanelOwner implements MenuPanel.Owner {
        @Override
        public AbstractContainerMenu menu() {
            return StrataContainerScreen.this.menu;
        }

        @Override
        public int imageWidth() {
            return StrataContainerScreen.this.imageWidth;
        }

        @Override
        public int imageHeight() {
            return StrataContainerScreen.this.imageHeight;
        }

        @Override
        public SlotRendering slotRendering() {
            return StrataContainerScreen.this.slotRendering();
        }

        @Override
        public Slot hoveredSlot() {
            return StrataContainerScreen.this.hoveredSlot;
        }

        @Override
        public boolean isQuickCrafting() {
            return StrataContainerScreen.this.isQuickCrafting;
        }

        @Override
        public Set<Slot> quickCraftSlots() {
            return StrataContainerScreen.this.quickCraftSlots;
        }

        @Override
        public int quickCraftType() {
            return StrataContainerScreen.this.quickCraftType;
        }

        @Override
        public void dropFromDrag(final Slot slot) {
            // The carried stack's count follows, since renderCarried works it out from the slots each frame
            StrataContainerScreen.this.quickCraftSlots.remove(slot);
        }

        @Override
        public void drawVanillaSlots() {
            StrataContainerScreen.this.drawVanillaSlots();
        }
    }
}
