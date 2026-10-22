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
import org.inspirenxe.strata.api.event.cause.CauseStack;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenController;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.spi.Capabilities;
import org.inspirenxe.strata.spi.Clipboard;
import org.inspirenxe.strata.spi.Clock;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.TextInputSink;
import org.inspirenxe.strata.spi.ThemeSource;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

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
 * {@link MinecraftThemeSource#VANILLA}. {@link #PLATFORM} is the platform Strata is told these screens are shown on,
 * so {@code Strata.uiThread()} is the client thread.</p>
 */
final class MinecraftScreens implements ScreenController {
    /**
     * The one controller.
     */
    static final MinecraftScreens INSTANCE = new MinecraftScreens();
    /**
     * The platform handed to Strata with the controller, whose UI thread {@code Strata.uiThread()} returns.
     */
    static final Platform PLATFORM = new ClientThread();

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

    /**
     * The client thread as a platform, for {@code Strata.uiThread()}. Core reads only its UI thread, the executor and
     * the test, which find the game when they are used, since mods are constructed before the game has a font or a
     * screen. Screens get a {@link MinecraftPlatform} of their own, which answers the same way.
     */
    private static final class ClientThread implements Platform {
        private static final String SCREENS_ONLY = "This platform only names the client thread; screens use their own";

        @Override
        public Optional<Executor> uiExecutor() {
            return Optional.of(task -> MinecraftPlatform.uiExecutor(ClientThread.game()).execute(task));
        }

        @Override
        public Optional<BooleanSupplier> uiThreadCheck() {
            return Optional.of(() -> {
                final Minecraft minecraft = Minecraft.getInstance();
                return minecraft != null && minecraft.isSameThread();
            });
        }

        @Override
        public Optional<CauseStack> causes() {
            return StrataMinecraft.causes();
        }

        @Override
        public TextMetrics textMetrics() {
            throw new UnsupportedOperationException(SCREENS_ONLY);
        }

        @Override
        public Clipboard clipboard() {
            throw new UnsupportedOperationException(SCREENS_ONLY);
        }

        @Override
        public Clock clock() {
            return Clock.SYSTEM;
        }

        @Override
        public TextInputSink textInput() {
            throw new UnsupportedOperationException(SCREENS_ONLY);
        }

        @Override
        public Capabilities capabilities() {
            throw new UnsupportedOperationException(SCREENS_ONLY);
        }

        @Override
        public ThemeSource themes() {
            return MinecraftThemeSource.get(ClientThread.game());
        }

        private static Minecraft game() {
            final Minecraft minecraft = Minecraft.getInstance();
            if (minecraft == null) {
                throw new IllegalStateException("Tasks cannot be handed to the client thread before the game has started");
            }
            return minecraft;
        }
    }
}
