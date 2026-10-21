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
import org.inspirenxe.strata.api.event.cause.CauseKeys;
import org.inspirenxe.strata.api.event.cause.Device;
import org.inspirenxe.strata.api.event.cause.Origin;
import org.junit.jupiter.api.Test;
import org.spongepowered.api.event.CauseStackManager;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.EventContextKey;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Sponge-backed stack over a stand-in for Sponge's manager that behaves like Sponge's own: the game sits under
 * every chain, and the current cause is kept until the stack changes.
 */
class SpongeCauseStackTest {
    private static final Object GAME = "game";

    private final FakeManager sponge = new FakeManager();
    private final SpongeCauseStack stack = new SpongeCauseStack(() -> Optional.of(this.sponge));

    @Test
    void framesOpenAndCloseSpongesFrames() {
        try (CauseFrame _ = this.stack.pushFrame().push("button")) {
            assertEquals(1, this.sponge.frames.size(), "a Strata frame is a Sponge frame");
            assertEquals(List.of("button", GAME), this.sponge.currentCause().all(), "pushes go on Sponge's chain");
        }
        assertEquals(0, this.sponge.frames.size());
        assertEquals(List.of(GAME), this.sponge.currentCause().all());
    }

    @Test
    void theChainIsSpongesAndTheContextStratas() {
        try (CauseFrame _ = this.stack.pushFrame().put(CauseKeys.ORIGIN, new Origin.User("Ana"))
                .put(CauseKeys.DEVICE, Device.MOUSE)) {
            try (CauseFrame _ = this.stack.pushFrame().push("group")) {
                final Cause cause = this.stack.current();
                assertEquals(Optional.of("group"), cause.root(), "the direct cause, as Sponge orders it");
                assertEquals(List.of("group", GAME), cause.all());
                assertEquals(Optional.of("group"), cause.first(String.class));
                assertEquals(new Origin.User("Ana"), cause.origin());
                assertEquals(Optional.of(Device.MOUSE), cause.context(CauseKeys.DEVICE));
                assertTrue(this.sponge.currentContext().asMap().isEmpty(), "Strata's keys stay out of Sponge's");
            }
        }
        assertSame(Origin.CODE, this.stack.origin());
        assertEquals(Optional.of(GAME), this.stack.current().root(), "Sponge's chain is never empty");
    }

    @Test
    void oneViewServesEveryEventWhileNothingChanges() {
        try (CauseFrame frame = this.stack.pushFrame().push("a")) {
            final Cause first = this.stack.current();
            assertSame(first, this.stack.current());
            frame.put(CauseKeys.DEVICE, Device.KEYBOARD);
            assertEquals(Optional.of(Device.KEYBOARD), this.stack.current().context(CauseKeys.DEVICE));
            assertTrue(first.context(CauseKeys.DEVICE).isEmpty(), "a view already made does not change");
        }
    }

    @Test
    void closingAFrameBelowTheTopClosesSpongesFramesInOrder() {
        final CauseFrame outer = this.stack.pushFrame().push("outer");
        final CauseFrame inner = this.stack.pushFrame().push("inner");
        outer.close();
        assertEquals(0, this.sponge.frames.size());
        assertEquals(List.of(GAME), this.sponge.currentCause().all());
        assertThrows(IllegalStateException.class, () -> inner.push("late"));
    }

    @Test
    void withoutSpongesClientOnlyTheContextIsKept() {
        final SpongeCauseStack early = new SpongeCauseStack(Optional::empty);
        try (CauseFrame _ = early.pushFrame().push("ignored").put(CauseKeys.ORIGIN, new Origin.User("Ana"))) {
            assertEquals(new Origin.User("Ana"), early.current().origin());
            assertTrue(early.current().all().isEmpty());
        }
    }

    /**
     * Behaves as Sponge's stack does for what Strata uses: frames close in order, popping what they pushed, and the
     * cause is kept until something changes, with the game under every chain.
     */
    private static final class FakeManager implements CauseStackManager {
        private final Deque<Object> causes = new ArrayDeque<>();
        private final List<FakeFrame> frames = new ArrayList<>();
        private org.spongepowered.api.event.Cause cached;

        @Override
        public org.spongepowered.api.event.Cause currentCause() {
            if (this.cached == null) {
                final List<Object> chain = new ArrayList<>(this.causes);
                chain.add(GAME);
                this.cached = org.spongepowered.api.event.Cause.of(EventContext.empty(), chain);
            }
            return this.cached;
        }

        @Override
        public EventContext currentContext() {
            return EventContext.empty();
        }

        @Override
        public CauseStackManager pushCause(final Object obj) {
            this.causes.push(obj);
            this.cached = null;
            return this;
        }

        @Override
        public Object popCause() {
            this.cached = null;
            return this.causes.pop();
        }

        @Override
        public void popCauses(final int n) {
            for (int i = 0; i < n; i++) {
                this.popCause();
            }
        }

        @Override
        public Object peekCause() {
            return this.causes.peek();
        }

        @Override
        public StackFrame pushCauseFrame() {
            final FakeFrame frame = new FakeFrame(this);
            this.frames.add(frame);
            return frame;
        }

        @Override
        public void popCauseFrame(final StackFrame frame) {
            if (this.frames.getLast() != frame) {
                throw new IllegalStateException("Sponge frames must close in order");
            }
            this.frames.removeLast();
            this.popCauses(((FakeFrame) frame).pushed);
        }

        @Override
        public <T> CauseStackManager addContext(final EventContextKey<T> key, final T value) {
            throw new UnsupportedOperationException("Strata keeps its context to itself");
        }

        @Override
        public <T> Optional<T> context(final EventContextKey<T> key) {
            return Optional.empty();
        }

        @Override
        public <T> Optional<T> removeContext(final EventContextKey<T> key) {
            return Optional.empty();
        }
    }

    private static final class FakeFrame implements CauseStackManager.StackFrame {
        private final FakeManager manager;
        private int pushed;

        FakeFrame(final FakeManager manager) {
            this.manager = manager;
        }

        @Override
        public org.spongepowered.api.event.Cause currentCause() {
            return this.manager.currentCause();
        }

        @Override
        public EventContext currentContext() {
            return this.manager.currentContext();
        }

        @Override
        public CauseStackManager.StackFrame pushCause(final Object obj) {
            this.manager.pushCause(obj);
            this.pushed++;
            return this;
        }

        @Override
        public Object popCause() {
            this.pushed--;
            return this.manager.popCause();
        }

        @Override
        public <T> CauseStackManager.StackFrame addContext(final EventContextKey<T> key, final T value) {
            throw new UnsupportedOperationException("Strata keeps its context to itself");
        }

        @Override
        public <T> Optional<T> removeContext(final EventContextKey<T> key) {
            return Optional.empty();
        }

        @Override
        public void close() {
            this.manager.popCauseFrame(this);
        }
    }
}
