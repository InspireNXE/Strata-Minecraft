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

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.spi.ScreenHost;

/**
 * The title a vanilla screen that hosts Strata shows and narrates. A screen whose vanilla title was made from the
 * Strata screen's own title follows the host's {@link ScreenHost#title()} from the start; a screen given a vanilla
 * title keeps it until the Strata screen sets another with {@code ScreenHandle.setTitle}. The host's title is turned
 * into a component again only when it or the game's language changes.
 */
final class HostTitle {
    private final boolean followsHost;
    private Text opened;
    private Text text;
    private Language language;
    private Component component;

    /**
     * Creates the title of one vanilla screen.
     *
     * @param followsHost whether the vanilla title was made from the Strata screen's title, so the host's title
     *                    stands for it at once
     */
    HostTitle(final boolean followsHost) {
        this.followsHost = followsHost;
    }

    /**
     * Notes the title the Strata screen opened with, which the vanilla title stands for until it changes.
     *
     * @param host the host that just opened the screen
     */
    void opened(final ScreenHost host) {
        this.opened = host.title();
    }

    /**
     * Returns the title to show.
     *
     * @param host the host, or {@code null} while the screen is not open
     * @param given the vanilla title the screen was made with
     * @return the host's title as a component, or the given title while it stands for the host's
     */
    Component get(final ScreenHost host, final Component given) {
        if (host == null || host.screen().isEmpty()) {
            return given;
        }
        final Text title = host.title();
        if (!this.followsHost && title.equals(this.opened)) {
            return given;
        }
        final Language current = Language.getInstance();
        if (this.component == null || current != this.language || !title.equals(this.text)) {
            this.text = title;
            this.language = current;
            this.component = MinecraftText.component(title);
        }
        return this.component;
    }
}
