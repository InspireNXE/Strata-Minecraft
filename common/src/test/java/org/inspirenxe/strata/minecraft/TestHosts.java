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

import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.StrataCore;
import org.inspirenxe.strata.spi.Capabilities;
import org.inspirenxe.strata.spi.Clipboard;
import org.inspirenxe.strata.spi.Platform;
import org.inspirenxe.strata.spi.ScreenHost;
import org.inspirenxe.strata.spi.TextInputSink;
import org.inspirenxe.strata.spi.ThemeSource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Real Strata hosts over a mocked platform, for testing components without a game.
 */
public final class TestHosts {
    /**
     * The width of every character in the mocked text metrics.
     */
    public static final float CHAR_WIDTH = 6f;

    private TestHosts() {
    }

    /**
     * Makes a host 100 by 100 units, with monospaced text and a clock that stands still.
     *
     * @param theme the host's theme
     * @return the host, with no screen open
     */
    public static ScreenHost create(final Theme theme) {
        final Platform platform = mock(Platform.class);
        when(platform.textMetrics()).thenReturn(new TextMetrics() {
            @Override
            public float width(final TextSpan span) {
                return span.text().length() * CHAR_WIDTH;
            }

            @Override
            public float lineHeight(final TextStyle style) {
                return 9f;
            }
        });
        when(platform.clock()).thenReturn(() -> 0L);
        when(platform.clipboard()).thenReturn(mock(Clipboard.class));
        when(platform.textInput()).thenReturn(mock(TextInputSink.class));
        when(platform.capabilities()).thenReturn(new Capabilities(true, false, true));
        when(platform.themes()).thenReturn(ThemeSource.NONE);
        final ScreenHost host = StrataCore.createScreenHost(platform, theme);
        host.resize(100f, 100f);
        return host;
    }
}
