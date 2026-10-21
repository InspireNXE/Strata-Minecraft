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

import net.minecraft.client.Minecraft;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenController;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.theme.ThemeRef;

import java.util.Objects;

/**
 * Strata-Minecraft's platform controller, which {@code Strata.screens()} returns once {@link StrataMinecraft#install()}
 * has run. It shows each screen in a new {@link StrataScreen} titled with the screen's {@link Screen#title()}:
 * <ul>
 *     <li>When a Strata screen is showing, the new one opens above it, and closing the new one shows it again with
 *     its tree as it was.</li>
 *     <li>Otherwise the new screen replaces whatever vanilla shows, and closing it returns to the game.</li>
 *     <li>Called off the render thread, it shows the screen on the render thread's next task, and the handle works
 *     from then on.</li>
 * </ul>
 * <p>{@code open(screen)} uses {@code ThemeRef.platform()}, which {@link MinecraftThemeSource#defaultTheme()} makes
 * {@link MinecraftThemeSource#VANILLA}.</p>
 */
final class MinecraftScreens implements ScreenController {
    /**
     * The one controller.
     */
    static final MinecraftScreens INSTANCE = new MinecraftScreens();

    private MinecraftScreens() {
    }

    @Override
    public ScreenHandle open(final Screen screen, final ThemeRef theme) {
        Objects.requireNonNull(screen, "screen");
        Objects.requireNonNull(theme, "theme");
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null) {
            throw new IllegalStateException("Screens cannot open before the game has started");
        }
        final StrataScreen opened = StrataScreen.of(minecraft, minecraft.font, screen, theme);
        if (minecraft.isSameThread()) {
            MinecraftScreens.show(minecraft, opened);
        } else {
            minecraft.execute(() -> MinecraftScreens.show(minecraft, opened));
        }
        return opened.handle();
    }

    /**
     * Shows a screen above the Strata screen that is showing, or in place of whatever vanilla shows.
     *
     * @param minecraft the game
     * @param opened the screen to show
     */
    static void show(final Minecraft minecraft, final StrataScreen opened) {
        if (StrataScreen.switcher.current(minecraft) instanceof final StrataScreen current) {
            current.stack(opened);
        } else {
            StrataScreen.switcher.set(minecraft, opened);
        }
    }
}
