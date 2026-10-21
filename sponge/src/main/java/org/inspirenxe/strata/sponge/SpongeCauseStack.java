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
package org.inspirenxe.strata.sponge;

import org.inspirenxe.strata.api.event.cause.Cause;
import org.inspirenxe.strata.api.event.cause.CauseFrame;
import org.inspirenxe.strata.api.event.cause.CauseKey;
import org.inspirenxe.strata.api.event.cause.CauseKeys;
import org.inspirenxe.strata.api.event.cause.CauseStack;
import org.inspirenxe.strata.api.event.cause.Origin;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.CauseStackManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Strata's cause stack backed by Sponge's, the client's {@link CauseStackManager}. Each Strata frame opens a Sponge
 * frame, and what is pushed through it goes on Sponge's chain, so a Sponge event fired from a Strata handler, such as
 * an economy transaction a button starts, sees the control and whatever else the frames pushed. The other way round,
 * a Strata event's cause is Sponge's current cause, with the root, the chain and its order as Sponge has them.
 *
 * <p>Strata's context keys, {@link CauseKeys#ORIGIN} included, stay beside Sponge's context rather than being
 * registered as Sponge {@code EventContextKey}s. The stack keeps them in its own frames and merges them into each
 * cause it makes.</p>
 *
 * <p>{@link #current()} keeps one view per Sponge cause and Strata context, so while nothing changes every event
 * shares one. Until Sponge's client is available, frames carry Strata's context alone, and causes have an empty
 * chain. Use it on the client thread only, as Sponge requires.</p>
 */
public final class SpongeCauseStack implements CauseStack {
    private final Supplier<Optional<CauseStackManager>> manager;
    private final List<FrameImpl> frames;
    private org.spongepowered.api.event.Cause viewedCause;
    private Cause view;
    private boolean contextChanged;

    /**
     * Creates a stack backed by the cause stack of Sponge's client, {@code Sponge.client().causeStackManager()}.
     */
    public SpongeCauseStack() {
        this(() -> Sponge.isClientAvailable() ? Optional.of(Sponge.client().causeStackManager()) : Optional.empty());
    }

    /**
     * Creates a stack backed by the manager a supplier gives, for tests.
     *
     * @param manager gives Sponge's manager, or empty while there is none
     */
    SpongeCauseStack(final Supplier<Optional<CauseStackManager>> manager) {
        this.manager = Objects.requireNonNull(manager, "manager");
        this.frames = new ArrayList<>();
    }

    @Override
    public CauseFrame pushFrame() {
        final FrameImpl frame = new FrameImpl(this.manager.get().map(CauseStackManager::pushCauseFrame).orElse(null));
        this.frames.add(frame);
        return frame;
    }

    @Override
    public Cause current() {
        final org.spongepowered.api.event.Cause cause = this.manager.get().map(CauseStackManager::currentCause)
                .orElse(null);
        if (this.view == null || this.contextChanged || cause != this.viewedCause) {
            this.view = new SpongeCause(cause, this.mergedContext());
            this.viewedCause = cause;
            this.contextChanged = false;
        }
        return this.view;
    }

    @Override
    public <V> Optional<V> context(final CauseKey<V> key) {
        Objects.requireNonNull(key, "key");
        final Object value = this.find(key);
        return value == null ? Optional.empty() : Optional.of(key.type().cast(value));
    }

    @Override
    public Origin origin() {
        final Object origin = this.find(CauseKeys.ORIGIN);
        return origin == null ? Origin.CODE : (Origin) origin;
    }

    private Object find(final CauseKey<?> key) {
        for (int i = this.frames.size() - 1; i >= 0; i--) {
            final Object value = this.frames.get(i).get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Map<CauseKey<?>, Object> mergedContext() {
        final Map<CauseKey<?>, Object> context = new IdentityHashMap<>();
        for (int i = this.frames.size() - 1; i >= 0; i--) {
            final FrameImpl frame = this.frames.get(i);
            for (int k = 0; k < frame.contextCount; k++) {
                context.putIfAbsent(frame.keys[k], frame.values[k]);
            }
        }
        return context;
    }

    private final class FrameImpl implements CauseFrame {
        private final CauseStackManager.StackFrame sponge;
        private CauseKey<?>[] keys;
        private Object[] values;
        private int contextCount;
        private boolean closed;

        FrameImpl(final CauseStackManager.StackFrame sponge) {
            this.sponge = sponge;
        }

        @Override
        public CauseFrame push(final Object cause) {
            Objects.requireNonNull(cause, "cause");
            this.requireOpen();
            if (this.sponge != null) {
                this.sponge.pushCause(cause);
            }
            return this;
        }

        @Override
        public <V> CauseFrame put(final CauseKey<V> key, final V value) {
            Objects.requireNonNull(key, "key");
            key.type().cast(Objects.requireNonNull(value, "value"));
            this.requireOpen();
            SpongeCauseStack.this.contextChanged = true;
            for (int i = 0; i < this.contextCount; i++) {
                if (this.keys[i] == key) {
                    this.values[i] = value;
                    return this;
                }
            }
            if (this.keys == null) {
                this.keys = new CauseKey<?>[6];
                this.values = new Object[6];
            } else if (this.contextCount == this.keys.length) {
                this.keys = Arrays.copyOf(this.keys, this.contextCount * 2);
                this.values = Arrays.copyOf(this.values, this.contextCount * 2);
            }
            this.keys[this.contextCount] = key;
            this.values[this.contextCount++] = value;
            return this;
        }

        private Object get(final CauseKey<?> key) {
            for (int i = 0; i < this.contextCount; i++) {
                if (this.keys[i] == key) {
                    return this.values[i];
                }
            }
            return null;
        }

        private void requireOpen() {
            if (this.closed) {
                throw new IllegalStateException("The cause frame was closed");
            }
        }

        @Override
        public void close() {
            if (this.closed) {
                return;
            }
            final List<FrameImpl> frames = SpongeCauseStack.this.frames;
            final int at = frames.lastIndexOf(this);
            if (at < 0) {
                this.closed = true;
                return;
            }
            // Top down, so Sponge sees its frames close in order
            for (int i = frames.size() - 1; i >= at; i--) {
                final FrameImpl frame = frames.remove(i);
                frame.closed = true;
                if (frame.contextCount > 0) {
                    SpongeCauseStack.this.contextChanged = true;
                }
                if (frame.sponge != null) {
                    frame.sponge.close();
                }
            }
        }
    }

    /**
     * A Strata cause over a Sponge one: the chain is Sponge's, the context Strata's.
     */
    private static final class SpongeCause implements Cause {
        private final org.spongepowered.api.event.Cause cause;
        private final Map<CauseKey<?>, Object> context;

        SpongeCause(final org.spongepowered.api.event.Cause cause, final Map<CauseKey<?>, Object> context) {
            this.cause = cause;
            this.context = context;
        }

        @Override
        public Optional<Object> root() {
            return this.cause == null ? Optional.empty() : Optional.of(this.cause.root());
        }

        @Override
        public <T> Optional<T> first(final Class<T> type) {
            return this.cause == null ? Optional.empty() : this.cause.first(type);
        }

        @Override
        public <T> List<T> all(final Class<T> type) {
            return this.cause == null ? List.of() : List.copyOf(this.cause.allOf(type));
        }

        @Override
        public List<Object> all() {
            return this.cause == null ? List.of() : List.copyOf(this.cause.all());
        }

        @Override
        public <V> Optional<V> context(final CauseKey<V> key) {
            final Object value = this.context.get(key);
            return value == null ? Optional.empty() : Optional.of(key.type().cast(value));
        }

        @Override
        public String toString() {
            return "SpongeCause[cause=" + this.cause + ", context=" + this.context + "]";
        }
    }
}
