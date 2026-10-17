package org.inspirenxe.strata.minecraft;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.inspirenxe.strata.api.input.Modifier.ALT;
import static org.inspirenxe.strata.api.input.Modifier.CONTROL;
import static org.inspirenxe.strata.api.input.Modifier.SHIFT;
import static org.inspirenxe.strata.api.input.Modifier.SUPER;

/**
 * Checks the Minecraft input mapping against the real 26.3 {@link InputConstants}, so a renumbering or a new key
 * in a Minecraft update fails here instead of in game.
 */
class MinecraftInputTest {
    /**
     * Minecraft keys Strata has no constant for.
     */
    private static final Set<String> UNMAPPED = Set.of(
            "KEY_F13", "KEY_F14", "KEY_F15", "KEY_F16", "KEY_F17", "KEY_F18", "KEY_F19", "KEY_F20", "KEY_F21",
            "KEY_F22", "KEY_F23", "KEY_F24", "KEY_NUMPADCOMMA"
    );

    @Test
    void everyMinecraftKeyMapsToADistinctStrataKey() throws IllegalAccessException {
        final Map<Key, String> mappedFrom = new EnumMap<>(Key.class);
        int checked = 0;
        for (final Field field : InputConstants.class.getFields()) {
            if (!field.getName().startsWith("KEY_") || field.getType() != int.class
                    || !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            checked++;
            final Key key = MinecraftInput.key(field.getInt(null));
            if (UNMAPPED.contains(field.getName())) {
                assertEquals(Key.UNKNOWN, key, field.getName() + " has no Strata key");
                continue;
            }
            assertNotEquals(Key.UNKNOWN, key, field.getName() + " is not mapped");
            final String previous = mappedFrom.put(key, field.getName());
            assertNull(previous, field.getName() + " and " + previous + " both map to " + key);
        }
        assertTrue(checked > 100, "found Minecraft's key constants (" + checked + ")");
    }

    @Test
    void namedKeysMapWhereExpected() {
        assertEquals(Key.A, MinecraftInput.key(InputConstants.KEY_A));
        assertEquals(Key.ENTER, MinecraftInput.key(InputConstants.KEY_RETURN));
        assertEquals(Key.KP_ENTER, MinecraftInput.key(InputConstants.KEY_NUMPADENTER));
        assertEquals(Key.ESCAPE, MinecraftInput.key(InputConstants.KEY_ESCAPE));
        assertEquals(Key.LEFT_SUPER, MinecraftInput.key(InputConstants.KEY_LGUI));
        assertEquals(Key.UNKNOWN, MinecraftInput.key(-1));
    }

    @Test
    void mouseButtonsMap() {
        assertEquals(MouseButton.LEFT, MinecraftInput.button(InputConstants.MOUSE_BUTTON_LEFT));
        assertEquals(MouseButton.RIGHT, MinecraftInput.button(InputConstants.MOUSE_BUTTON_RIGHT));
        assertEquals(MouseButton.MIDDLE, MinecraftInput.button(InputConstants.MOUSE_BUTTON_MIDDLE));
        assertEquals(MouseButton.BACK, MinecraftInput.button(InputConstants.MOUSE_BUTTON_4));
        assertEquals(MouseButton.FORWARD, MinecraftInput.button(InputConstants.MOUSE_BUTTON_5));
        assertEquals(MouseButton.UNKNOWN, MinecraftInput.button(InputConstants.MOUSE_BUTTON_8));
    }

    @Test
    void modifiersMap() {
        assertEquals(Modifiers.NONE, MinecraftInput.modifiers(new KeyEvent(InputConstants.KEY_A, 0, 0)));
        assertEquals(Modifiers.of(SHIFT, CONTROL),
                MinecraftInput.modifiers(new KeyEvent(InputConstants.KEY_A, 0, InputConstants.MOD_SHIFT | InputConstants.MOD_CONTROL)));
        assertEquals(Modifiers.of(ALT, SUPER),
                MinecraftInput.modifiers(new KeyEvent(InputConstants.KEY_A, 0, InputConstants.MOD_ALT | InputConstants.MOD_SUPER)));
    }

    @Test
    void horizontalScrollFlipsToStrataDirection() {
        assertEquals(-1f, MinecraftInput.scrollX(1.0));
        assertEquals(0.5f, MinecraftInput.scrollX(-0.5));
    }

    @Test
    void fractionalScrollIsPrecise() {
        assertEquals(ScrollKind.WHEEL, MinecraftInput.scrollKind(0.0, 1.0));
        assertEquals(ScrollKind.WHEEL, MinecraftInput.scrollKind(0.0, -2.0));
        assertEquals(ScrollKind.PRECISE, MinecraftInput.scrollKind(0.0, 0.25));
        assertEquals(ScrollKind.PRECISE, MinecraftInput.scrollKind(0.5, 0.0));
    }
}
