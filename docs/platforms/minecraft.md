# Minecraft 26.3

The Minecraft adapter draws Strata through vanilla's GUI pipeline and takes input from vanilla's screen callbacks, so Strata screens look and behave like vanilla ones. The adapter in `mc-common` is written against vanilla classes only; the NeoForge and Fabric jars contain the same code plus their loader's metadata.

Setup for each loader is on the [installation page](../getting-started/installation.md#minecraft-mods).

!!! warning "Not yet run in game"
    The adapter compiles against the real 26.3 classes, and its input mapping and canvas are unit tested against them, but no Strata screen has been opened in a running client yet. See [Status and limitations](../help/status.md).

## Opening a screen

`StrataScreen` is a vanilla `Screen` subclass that hosts one Strata screen:

```java
--8<-- "minecraft/MinecraftExamples.java:open"
```

In 26.3, screens are opened through `Minecraft.gui.setScreen`, not the `Minecraft.setScreen` of older versions. Call it on the client (render) thread. From anywhere else, such as a packet handler, hop onto it first:

```java
--8<-- "minecraft/MinecraftExamples.java:thread"
```

The title is vanilla's screen title, which vanilla uses for narration. The code is the same on NeoForge and Fabric. Open it from whatever triggers your UI: a key mapping handler, a command, an item use, or a button on another screen. Each loader registers those differently, so see your loader's documentation for the trigger itself.

## The lifecycle

| Vanilla calls | `StrataScreen` does |
|---|---|
| `init()`, the first time | Creates a host for this screen, sizes it to the screen, and opens the Strata screen, which calls its `build` |
| `init()`, again (window resize, GUI scale change) | Resizes the host. The tree is kept, not rebuilt. |
| `extractRenderState(...)` | Lets vanilla draw first, then lays out if needed and draws the Strata tree on top |
| `removed()` | Closes the Strata screen (`closed()` runs) and drops the host |

A Strata screen that calls `handle.close()` gets vanilla's `onClose()`, the same as pressing Escape. Opening the same `StrataScreen` again after it was removed builds a new tree.

### Returning to a parent screen

Vanilla's `onClose()` closes the screen and returns to the game, or to the title screen when no world is loaded. It does not return to the screen that opened it. To go back to a parent, such as a settings page opened from another page, override it:

```java
--8<-- "minecraft/MinecraftExamples.java:parent"
```

## Units and drawing

One UI unit is one GUI pixel: screen pixels divided by the GUI scale. A 200-unit panel is 200 GUI pixels wide and grows with the player's GUI scale setting, exactly like vanilla widgets.

`MinecraftCanvas` records drawing into vanilla's `GuiGraphicsExtractor`, which renders it later in the frame:

| Strata draws | Vanilla does |
|---|---|
| Fills and gradients | `fill` and `fillGradient`, with the rectangle rounded outward to whole GUI pixels (left and top down, right and bottom up), so fractional layouts are never cut short |
| Sprites | `blitSprite` with the `GUI_TEXTURED` pipeline, from the `minecraft:gui` sprite atlas. The tint is vanilla's sprite color. |
| Nine-slices | The same as sprites. Vanilla applies the scaling in the sprite's `.png.mcmeta`, and Strata's border argument is ignored. |
| Text | A chat component in the screen's font, positioned to the nearest pixel, with vanilla's bold, italic, underline, strikethrough and shadow |
| Clips | Vanilla's scissor stack, which intersects nested clips and follows the pose |
| Translations | The pose matrix |
| Layers between screens | A new vanilla stratum |
| Cursors | `requestCursor` with vanilla's cursor types, which include every `CursorKind` |
| Item visuals | `item` and `itemDecorations`, under a pose scaled from 16 by 16 to the rectangle. See [Items](#items). |
| Other visuals | A magenta box |

A missing sprite draws vanilla's missing-texture pattern.

### Your own sprites

Put images in your mod's resources at `assets/<modid>/textures/gui/sprites/<path>.png` and name them `SpriteId.of("<modid>", "<path>")`. Vanilla's gui atlas collects that directory from every mod and resource pack, so no registration is needed. Add a `.png.mcmeta` beside a nine-slice image; the [themes guide](../guides/theming.md#nine-slices) shows the format.

```java
--8<-- "minecraft/MinecraftExamples.java:theme"
```

Resource packs can override your sprites like any others. New or changed images need a resource reload (F3+T) in a running game.

### The vanilla theme

`MinecraftThemes.vanilla()` uses vanilla's own sprites:

| Widget | Sprites | Text |
|---|---|---|
| Button | `widget/button`, `widget/button_highlighted` (hovered, focused and pressed), `widget/button_disabled` | White with a shadow, `0xFFA0A0A0` when disabled |
| Text box | `widget/text_field`, `widget/text_field_highlighted` when focused. Vanilla has no disabled field sprite, so a disabled box uses the normal one. | `0xFFE0E0E0`, `0xFF707070` when disabled |
| Check box | `widget/checkbox`, `widget/checkbox_highlighted` (hovered and focused), `widget/checkbox_selected`, `widget/checkbox_selected_highlighted`. The mark is part of the sprites. Vanilla has no disabled sprites. | White with a shadow, `0xFFA0A0A0` when disabled |
| Slider | `widget/slider` for the track, `widget/slider_highlighted` while focused, `widget/slider_handle` for the thumb, `widget/slider_handle_highlighted` while hovered, focused or pressed. No disabled sprites. | White with a shadow, `0xFFA0A0A0` when disabled |
| Label | | White with a shadow |

There is no separate pressed sprite in vanilla, so a pressed button looks hovered. The default check box size (17), the slider's height (20) and its thumb width (8) match vanilla's widgets.

Vanilla's own slider needs Enter before the arrow keys change it. Strata's slider responds to the arrow keys whenever it has focus.

## Items

`MinecraftVisuals.item(stack)` turns an `ItemStack` into a `Visual`, which an `Image` shows and a custom component can draw with `Painter.visual`. `MinecraftTooltips.item(stack)` builds the tooltip vanilla shows for the stack in a slot:

```java
--8<-- "minecraft/MinecraftExamples.java:item"
```

An item visual is 16 by 16 UI units, the size of an item in a slot, and stretches to the size it is drawn at. Its position rounds to the nearest GUI pixel. It draws through vanilla's GUI item rendering with the current player and level, so items whose look depends on them, such as compasses and clocks, look the same as in the inventory. The glint of enchanted items is included.

| Decorations | What draws over the item |
|---|---|
| `ItemDecorations.ALL`, the default | Vanilla's own slot decorations: the damage bar on damaged items, the cooldown overlay while the player has the item on cooldown, and the count when it is not 1 |
| `ItemDecorations.NONE` | Nothing |
| `new ItemDecorations(count, damageBar)` with one of them off | The other one, at vanilla's position and in its colors. The cooldown overlay is left out. |

The stack is copied when the visual is made, so later changes to it do not show. Make a new visual instead. Two item visuals are equal when vanilla's `ItemStack.matches` says their stacks are (the same item, count and components) and their decorations are the same. Vanilla items cannot be tinted, so `Image.setTint` has no effect on them, except that a fully transparent tint hides the item.

### Item tooltips

`MinecraftTooltips.item(stack)` asks vanilla for the stack's tooltip lines, the same ones a slot shows. It reads the advanced tooltips setting (F3+H) when it is called, so the lines include vanilla's extra ones, such as the item id, while it is on. The lines become a Strata `Tooltip.lines` tooltip:

- Each run keeps its color, bold, italic, underline and strikethrough. Text without a color is white.
- Every run has a shadow, as vanilla tooltip text does.
- Obfuscated text shows as plain text. Custom fonts and shadow colors are dropped.

The tooltip is drawn by Strata on the theme's `tooltip.background`, or on the classic dark box with a purple edge when the theme has no such key, as `MinecraftThemes.vanilla()` does. It is built once, so build it again when the stack changes, or when the player toggles F3+H and you want the new lines. `MinecraftTooltips.lines` and `MinecraftTooltips.spans` convert any vanilla `Component`s the same way, for tooltips and labels of your own.

Tooltip images are not drawn yet. A bundle shows its text lines but not its grid of contents, and the extra gap vanilla leaves below an item's name is missing.

## Input

`StrataScreen` forwards vanilla's input callbacks to its host. Input Strata does not use goes on to vanilla's `Screen` handling.

| Vanilla callback | Becomes | Notes |
|---|---|---|
| `mouseMoved` | pointer move | No modifiers |
| `mouseClicked`, `mouseReleased` | button press, release | Modifiers from the event. Strata counts clicks itself and ignores vanilla's double-click flag. |
| `mouseScrolled` | scroll | No modifiers. Horizontal scroll is flipped to Strata's direction. Whole amounts count as `WHEEL`, fractions as `PRECISE`. Amounts already include the player's scroll sensitivity. |
| `keyPressed`, `keyReleased` | key | Vanilla sends held keys as more presses; the screen turns them into `REPEAT`. |
| `charTyped` | character | No modifiers |

### Keys are SDL scancodes

Since 26.3, Minecraft's key values are SDL scancodes, which name physical key positions, not GLFW key codes. The adapter maps them through the `InputConstants` names, never raw numbers, and so should you if you ever handle vanilla key events yourself. Scancodes Strata has no key for, such as F13 to F24 and the keypad comma, arrive as `Key.UNKNOWN`.

### Typed characters need text input mode

Minecraft 26.3 sends no characters at all until a text field reports focus. When a Strata text box, or any component whose `acceptsTextInput()` is true, gains focus, the adapter calls `Minecraft.onTextInputFocusChange` for the hosting screen, and it stops text input when focus leaves. If a custom component takes typing but never receives `CHAR` events, it is missing that override.

While a text box has focus, it also consumes every key that types a character, so vanilla shortcuts on those keys, such as the inventory key or the chat key, do not fire while the player types.

### Escape

Escape is not consumed by any built-in widget, so vanilla receives it and closes the screen, even while a text box has focus. To keep a screen open on Escape, consume the key in a root handler, or subclass `StrataScreen` and override vanilla's `shouldCloseOnEsc()`.

### Strata claims the whole screen

A press counts as handled whenever it hits a Strata component, and a root anchored with `FILL` covers the whole screen. Vanilla widgets added to a `StrataScreen` subclass would therefore never be clicked. Mixing vanilla widgets and Strata components in one screen is not supported yet; see [Status and limitations](../help/status.md).

### macOS

On macOS, vanilla uses Command for its edit shortcuts. Strata reports Command as `Modifier.SUPER` and its text box checks Control, so in a Strata text box on a Mac, copy, cut, paste and select all are Control+C, X, V and A, not Command. A platform shortcut modifier is planned.

## Pausing

Whether a single-player world pauses behind the screen follows vanilla's `isPauseScreen()`, which is true by default. Override it to keep the world running:

```java
--8<-- "minecraft/MinecraftExamples.java:nopause"
```

## Threading

Everything in the adapter runs on Minecraft's render thread, the same thread vanilla screens use. Never touch a Strata component from the server thread, a network thread or your own threads; use `Minecraft.getInstance().execute(...)`.

## Loader notes

| | NeoForge | Fabric |
|---|---|---|
| Jar | `strata-mc-neoforge` | `strata-mc-fabric` |
| Entry point | An empty `@Mod("strata")` class, client only | None. The jar only declares the mod. |
| Depends on | NeoForge `[26.3.0.83,)`, Minecraft `[26.3, 26.4)` | Fabric Loader `>=0.19.5`, Minecraft `~26.3`, Java `>=25`. Not Fabric API. |
| Side | `CLIENT` | `"environment": "client"` |
| Dev client | `./gradlew :mc-neoforge:runClient` | `./gradlew :mc-fabric:runClient` |

The Fabric build uses Loom 1.18.4, which does not remap, so dependencies use `implementation` instead of `modImplementation`.

Because `StrataScreen` is an ordinary vanilla screen, loader screen events, such as NeoForge's `ScreenEvent`s, fire for it like for any other screen.

## Not available yet

- HUD overlays. Strata on the in-game HUD, through NeoForge's `RegisterGuiLayersEvent` and Fabric's `HudElementRegistry`, is planned.
- Vanilla widgets inside Strata, and Strata components inside vanilla screens. Both are planned as hosting adapters (`MinecraftUi.wrap` and a `StrataElement`), not mixins.
- Narration of Strata components. Vanilla narrates the screen's title only.
- Controller and gamepad navigation.
- Tooltip images, such as a bundle's contents, in item tooltips.
- Drawing beyond fills, gradients, sprites, text and items. Vanilla offers no public hook for custom GUI geometry, and Fabric has none at all.
- Changing a `StrataScreen`'s theme after it is created.
