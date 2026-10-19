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
