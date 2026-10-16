package org.inspirenxe.strata.minecraft;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.InputWithModifiers;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;

/**
 * Maps Minecraft 26.3 input to Strata's input types.
 *
 * <p>Since 26.3 Minecraft's key values are SDL scancodes, which name physical key positions, so every mapping goes
 * through the {@link InputConstants} names and never through raw numbers. Scancodes Strata has no key for (F13 to
 * F24, the keypad comma) map to {@link Key#UNKNOWN}.</p>
 *
 * <p>On macOS Minecraft treats Command as the edit-shortcut modifier. Strata reports Command as
 * {@link Modifier#SUPER} and Control as {@link Modifier#CONTROL}, so Strata's text box shortcuts use Control there
 * too.</p>
 */
public final class MinecraftInput {

    private MinecraftInput() {
    }

    /**
     * Reads the modifiers of a Minecraft input event.
     *
     * @param input a key or mouse event
     * @return the held modifiers
     */
    public static Modifiers modifiers(final InputWithModifiers input) {
        Modifiers modifiers = Modifiers.NONE;
        if (input.hasShiftDown()) {
            modifiers = modifiers.with(Modifier.SHIFT);
        }
        if (input.hasControlDown()) {
            modifiers = modifiers.with(Modifier.CONTROL);
        }
        if (input.hasAltDown()) {
            modifiers = modifiers.with(Modifier.ALT);
        }
        if ((input.modifiers() & InputConstants.MOD_SUPER) != 0) {
            modifiers = modifiers.with(Modifier.SUPER);
        }
        return modifiers;
    }

    /**
     * Converts Minecraft's horizontal scroll to Strata's direction. Minecraft passes the engine's value, positive
     * toward the right; Strata counts positive toward the left.
     *
     * @param scrollX the horizontal amount Minecraft reported
     * @return the amount in Strata's direction
     */
    public static float scrollX(final double scrollX) {
        return (float) -scrollX;
    }

    /**
     * Guesses the scroll device from the amounts: whole numbers come from a notched wheel, fractions from a trackpad
     * or smooth wheel (or a wheel sensitivity other than 1).
     *
     * @param scrollX the horizontal amount Minecraft reported
     * @param scrollY the vertical amount Minecraft reported
     * @return the scroll kind
     */
    public static ScrollKind scrollKind(final double scrollX, final double scrollY) {
        return scrollX == Math.rint(scrollX) && scrollY == Math.rint(scrollY) ? ScrollKind.WHEEL : ScrollKind.PRECISE;
    }

    /**
     * Maps a Minecraft mouse button.
     *
     * @param button the button Minecraft reported
     * @return the button; buttons 4 and 5 are back and forward, anything else unknown
     */
    public static MouseButton button(final int button) {
        return switch (button) {
            case InputConstants.MOUSE_BUTTON_LEFT -> MouseButton.LEFT;
            case InputConstants.MOUSE_BUTTON_RIGHT -> MouseButton.RIGHT;
            case InputConstants.MOUSE_BUTTON_MIDDLE -> MouseButton.MIDDLE;
            case InputConstants.MOUSE_BUTTON_4 -> MouseButton.BACK;
            case InputConstants.MOUSE_BUTTON_5 -> MouseButton.FORWARD;
            default -> MouseButton.UNKNOWN;
        };
    }

    /**
     * Maps a Minecraft key, given as the scancode from {@code KeyEvent.key()}.
     *
     * @param scancode the SDL scancode
     * @return the key, or {@link Key#UNKNOWN}
     */
    public static Key key(final int scancode) {
        return switch (scancode) {
            case InputConstants.KEY_A -> Key.A;
            case InputConstants.KEY_B -> Key.B;
            case InputConstants.KEY_C -> Key.C;
            case InputConstants.KEY_D -> Key.D;
            case InputConstants.KEY_E -> Key.E;
            case InputConstants.KEY_F -> Key.F;
            case InputConstants.KEY_G -> Key.G;
            case InputConstants.KEY_H -> Key.H;
            case InputConstants.KEY_I -> Key.I;
            case InputConstants.KEY_J -> Key.J;
            case InputConstants.KEY_K -> Key.K;
            case InputConstants.KEY_L -> Key.L;
            case InputConstants.KEY_M -> Key.M;
            case InputConstants.KEY_N -> Key.N;
            case InputConstants.KEY_O -> Key.O;
            case InputConstants.KEY_P -> Key.P;
            case InputConstants.KEY_Q -> Key.Q;
            case InputConstants.KEY_R -> Key.R;
            case InputConstants.KEY_S -> Key.S;
            case InputConstants.KEY_T -> Key.T;
            case InputConstants.KEY_U -> Key.U;
            case InputConstants.KEY_V -> Key.V;
            case InputConstants.KEY_W -> Key.W;
            case InputConstants.KEY_X -> Key.X;
            case InputConstants.KEY_Y -> Key.Y;
            case InputConstants.KEY_Z -> Key.Z;
            case InputConstants.KEY_0 -> Key.NUM_0;
            case InputConstants.KEY_1 -> Key.NUM_1;
            case InputConstants.KEY_2 -> Key.NUM_2;
            case InputConstants.KEY_3 -> Key.NUM_3;
            case InputConstants.KEY_4 -> Key.NUM_4;
            case InputConstants.KEY_5 -> Key.NUM_5;
            case InputConstants.KEY_6 -> Key.NUM_6;
            case InputConstants.KEY_7 -> Key.NUM_7;
            case InputConstants.KEY_8 -> Key.NUM_8;
            case InputConstants.KEY_9 -> Key.NUM_9;
            case InputConstants.KEY_SPACE -> Key.SPACE;
            case InputConstants.KEY_APOSTROPHE -> Key.APOSTROPHE;
            case InputConstants.KEY_COMMA -> Key.COMMA;
            case InputConstants.KEY_MINUS -> Key.MINUS;
            case InputConstants.KEY_PERIOD -> Key.PERIOD;
            case InputConstants.KEY_SLASH -> Key.SLASH;
            case InputConstants.KEY_SEMICOLON -> Key.SEMICOLON;
            case InputConstants.KEY_EQUALS -> Key.EQUALS;
            case InputConstants.KEY_LBRACKET -> Key.LEFT_BRACKET;
            case InputConstants.KEY_BACKSLASH -> Key.BACKSLASH;
            case InputConstants.KEY_RBRACKET -> Key.RIGHT_BRACKET;
            case InputConstants.KEY_GRAVE -> Key.GRAVE;
            case InputConstants.KEY_ESCAPE -> Key.ESCAPE;
            case InputConstants.KEY_RETURN -> Key.ENTER;
            case InputConstants.KEY_TAB -> Key.TAB;
            case InputConstants.KEY_BACKSPACE -> Key.BACKSPACE;
            case InputConstants.KEY_INSERT -> Key.INSERT;
            case InputConstants.KEY_DELETE -> Key.DELETE;
            case InputConstants.KEY_LEFT -> Key.LEFT;
            case InputConstants.KEY_RIGHT -> Key.RIGHT;
            case InputConstants.KEY_UP -> Key.UP;
            case InputConstants.KEY_DOWN -> Key.DOWN;
            case InputConstants.KEY_PAGEUP -> Key.PAGE_UP;
            case InputConstants.KEY_PAGEDOWN -> Key.PAGE_DOWN;
            case InputConstants.KEY_HOME -> Key.HOME;
            case InputConstants.KEY_END -> Key.END;
            case InputConstants.KEY_CAPSLOCK -> Key.CAPS_LOCK;
            case InputConstants.KEY_NUMLOCK -> Key.NUM_LOCK;
            case InputConstants.KEY_SCROLLLOCK -> Key.SCROLL_LOCK;
            case InputConstants.KEY_PRINTSCREEN -> Key.PRINT_SCREEN;
            case InputConstants.KEY_PAUSE -> Key.PAUSE;
            case InputConstants.KEY_F1 -> Key.F1;
            case InputConstants.KEY_F2 -> Key.F2;
            case InputConstants.KEY_F3 -> Key.F3;
            case InputConstants.KEY_F4 -> Key.F4;
            case InputConstants.KEY_F5 -> Key.F5;
            case InputConstants.KEY_F6 -> Key.F6;
            case InputConstants.KEY_F7 -> Key.F7;
            case InputConstants.KEY_F8 -> Key.F8;
            case InputConstants.KEY_F9 -> Key.F9;
            case InputConstants.KEY_F10 -> Key.F10;
            case InputConstants.KEY_F11 -> Key.F11;
            case InputConstants.KEY_F12 -> Key.F12;
            case InputConstants.KEY_NUMPAD0 -> Key.KP_0;
            case InputConstants.KEY_NUMPAD1 -> Key.KP_1;
            case InputConstants.KEY_NUMPAD2 -> Key.KP_2;
            case InputConstants.KEY_NUMPAD3 -> Key.KP_3;
            case InputConstants.KEY_NUMPAD4 -> Key.KP_4;
            case InputConstants.KEY_NUMPAD5 -> Key.KP_5;
            case InputConstants.KEY_NUMPAD6 -> Key.KP_6;
            case InputConstants.KEY_NUMPAD7 -> Key.KP_7;
            case InputConstants.KEY_NUMPAD8 -> Key.KP_8;
            case InputConstants.KEY_NUMPAD9 -> Key.KP_9;
            case InputConstants.KEY_MULTIPLY -> Key.KP_MULTIPLY;
            case InputConstants.KEY_ADD -> Key.KP_ADD;
            case InputConstants.KEY_NUMPADENTER -> Key.KP_ENTER;
            case InputConstants.KEY_NUMPADEQUALS -> Key.KP_EQUALS;
            case InputConstants.KEY_LSHIFT -> Key.LEFT_SHIFT;
            case InputConstants.KEY_RSHIFT -> Key.RIGHT_SHIFT;
            case InputConstants.KEY_LCONTROL -> Key.LEFT_CONTROL;
            case InputConstants.KEY_RCONTROL -> Key.RIGHT_CONTROL;
            case InputConstants.KEY_LALT -> Key.LEFT_ALT;
            case InputConstants.KEY_RALT -> Key.RIGHT_ALT;
            case InputConstants.KEY_LGUI -> Key.LEFT_SUPER;
            case InputConstants.KEY_RGUI -> Key.RIGHT_SUPER;
            default -> Key.UNKNOWN;
        };
    }
}
