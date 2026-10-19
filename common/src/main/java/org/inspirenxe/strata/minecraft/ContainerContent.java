package org.inspirenxe.strata.minecraft;

import net.minecraft.world.inventory.AbstractContainerMenu;
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;

/**
 * Builds the Strata tree of a {@link StrataContainerScreen}, around the panel that holds the menu's slots.
 *
 * <pre>{@code
 * (factory, menu, slots) -> factory.anchorPanel()
 *         .add(factory.stackPanel().setSpacing(4)
 *                 .add(factory.label("Grinder"))
 *                 .add(slots)
 *                 .setAnchor(Anchor.CENTER))
 *         .setAnchor(Anchor.FILL)
 * }</pre>
 *
 * @param <M> the menu type
 */
@FunctionalInterface
public interface ContainerContent<M extends AbstractContainerMenu> {

    /**
     * Builds the tree. Called once, when the screen first opens, on the render thread.
     *
     * @param factory the factory of the screen's host
     * @param menu the screen's menu
     * @param slots the panel that shows the menu's slots; add it to the tree once, anywhere except inside a
     *              {@link org.inspirenxe.strata.api.layout.ScrollPanel}
     * @return the root of the tree
     */
    Component<?> build(ComponentFactory factory, M menu, MenuPanel slots);
}
