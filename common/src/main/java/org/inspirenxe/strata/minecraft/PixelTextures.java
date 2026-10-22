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

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.inspirenxe.strata.api.visual.PixelBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The game textures a canvas draws pixel buffers from: one per buffer, made the first time the buffer is drawn,
 * written again whenever the buffer's version moved, and released once the buffer has gone undrawn for
 * {@value #IDLE_FRAMES} frames, when its buffer is collected, or when the canvas is done. Use it on the render
 * thread only.
 */
final class PixelTextures {
    /**
     * How many frames a texture may go undrawn before it is released.
     */
    static final int IDLE_FRAMES = 600;
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    private final Textures textures;
    // Weak, so a buffer no one holds can go; its texture is then released once it has been idle long enough
    private final Map<PixelBuffer, Entry> byBuffer = new WeakHashMap<>();
    private final List<Entry> entries = new ArrayList<>();
    private long frame;

    /**
     * Creates the textures of one canvas.
     *
     * @param textures makes and releases the game textures
     */
    PixelTextures(final Textures textures) {
        this.textures = textures;
    }

    /**
     * Starts a frame, releasing the textures of buffers not drawn for {@value #IDLE_FRAMES} frames.
     */
    void nextFrame() {
        this.frame++;
        for (int i = this.entries.size() - 1; i >= 0; i--) {
            final Entry entry = this.entries.get(i);
            if (this.frame - entry.drawn > IDLE_FRAMES) {
                this.entries.remove(i);
                this.byBuffer.values().remove(entry);
                entry.texture.close();
            }
        }
    }

    /**
     * Returns the texture to draw a buffer from, making it the first time and writing the buffer's pixels again
     * when its version moved.
     *
     * @param buffer the buffer
     * @return the texture's id
     */
    Identifier texture(final PixelBuffer buffer) {
        Entry entry = this.byBuffer.get(buffer);
        if (entry == null) {
            final Identifier id = Identifier.fromNamespaceAndPath("strata", "pixels/" + NEXT_ID.getAndIncrement());
            entry = new Entry(id, this.textures.create(id, buffer.width(), buffer.height()));
            this.byBuffer.put(buffer, entry);
            this.entries.add(entry);
        }
        if (!entry.written || entry.version != buffer.version()) {
            entry.texture.write(buffer.array());
            entry.version = buffer.version();
            entry.written = true;
        }
        entry.drawn = this.frame;
        return entry.id;
    }

    /**
     * Releases every texture, for a canvas whose screen closed.
     */
    void close() {
        for (final Entry entry : this.entries) {
            entry.texture.close();
        }
        this.entries.clear();
        this.byBuffer.clear();
    }

    /**
     * Returns how many textures are held, for tests.
     *
     * @return the count
     */
    int size() {
        return this.entries.size();
    }

    /**
     * Makes the game textures; tests replace the game's.
     */
    interface Textures {
        /**
         * The game's own: a {@link DynamicTexture} over a {@link NativeImage}, registered with the texture manager
         * under its id and sampled nearest, so pixels stay sharp.
         */
        Textures GAME = (id, width, height) -> {
            final NativeImage image = new NativeImage(width, height, false);
            final DynamicTexture texture = new DynamicTexture(id::toString, image);
            Minecraft.getInstance().getTextureManager().register(id, texture);
            return new Texture() {
                @Override
                public void write(final int[] argb) {
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            // NativeImage takes ARGB here and stores it as the ABGR the GPU reads
                            image.setPixel(x, y, argb[y * width + x]);
                        }
                    }
                    texture.upload();
                }

                @Override
                public void close() {
                    // Removing it from the texture manager closes the texture and its image
                    Minecraft.getInstance().getTextureManager().release(id);
                }
            };
        };

        /**
         * Makes a texture of a size, registered under an id.
         *
         * @param id the id to draw it by
         * @param width the width in pixels
         * @param height the height in pixels
         * @return the texture
         */
        Texture create(Identifier id, int width, int height);
    }

    /**
     * One game texture.
     */
    interface Texture {
        /**
         * Copies a buffer's pixels into the texture and uploads them.
         *
         * @param argb the pixels, row by row
         */
        void write(int[] argb);

        /**
         * Releases the texture.
         */
        void close();
    }

    /**
     * A buffer's texture, and the version and frame it was last written and drawn at.
     */
    private static final class Entry {
        private final Identifier id;
        private final Texture texture;
        private long version;
        private boolean written;
        private long drawn;

        Entry(final Identifier id, final Texture texture) {
            this.id = id;
            this.texture = texture;
        }
    }
}
