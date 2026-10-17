# Strata on Minecraft 26.3

The Minecraft adapter draws Strata through vanilla's GUI pipeline and takes input from vanilla's screen callbacks, so Strata screens look and behave like vanilla ones. The adapter in `common` is written against vanilla classes only. The `neoforge` and `fabric` jars compile the same code, add their loader's metadata, and nest Strata's `api`, `spi` and `core` jars.

This page covers what is specific to Minecraft. Strata itself, with its components, layout, events and themes, is documented in the [Strata repository](https://github.com/InspireNXE/Strata/tree/main/docs). Setup for each loader is in the [README](../README.md#using-strata-in-a-mod).

Code blocks that hold a `--8<--` line include a marked section of `docs-examples/src/main/java/org/inspirenxe/strata/examples/minecraft/MinecraftExamples.java`, which the build compiles. The name after the colon is the section; open the file to read the code.

The adapter compiles against the real 26.3 classes, and its input mapping and canvas are unit tested against them, but no Strata screen has been opened in a running client yet. See [Status](#status).

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

`MinecraftThemes.vanilla()` draws buttons, check boxes, sliders and text boxes with vanilla's own sprites, so a screen looks like part of the game. Escape closes the screen as it does for any vanilla screen, because Strata passes on input it does not use.

## The lifecycle

| Vanilla calls | `StrataScreen` does |
|---|---|
| `init()`, the first time | Creates a host for this screen, sizes it to the screen, and opens the Strata screen, which calls its `build` |
| `init()`, again (window resize, GUI scale change) | Resizes the host. The tree is kept, not rebuilt. |
| `extractRenderState(...)` | Lets vanilla draw first, then lays out if needed and draws the Strata tree on top |
| `removed()` | Closes the Strata screen (`closed()` runs) and drops the host |

A Strata screen that calls `handle.close()` gets vanilla's `onClose()`, the same as pressing Escape. Opening the same `StrataScreen` again after it was removed builds a new tree.

Minecraft keeps its own screen stack, so each `StrataScreen` holds exactly one Strata screen and one host. `hidesBackground()` has no effect, and vanilla decides what is drawn behind a screen. Dialogs opened through `factory.overlays()` stay inside one `StrataScreen`, so they need no vanilla screen of their own, and the screen behind them keeps updating.

### Returning to a parent screen

Vanilla's `onClose()` closes the screen and returns to the game, or to the title screen when no world is loaded. It does not return to the screen that opened it. To go back to a parent, such as a settings page opened from another page, override it:

```java
--8<-- "minecraft/MinecraftExamples.java:parent"
```

## Units and drawing

One UI unit is one GUI pixel: screen pixels divided by the GUI scale. A 200-unit panel is 200 GUI pixels wide and grows with the player's GUI scale setting, exactly like vanilla widgets. A screen at GUI scale 4 has half as many units across as at GUI scale 2, so use anchors, stretching and weighted grids rather than fixed positions.

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

A missing sprite draws vanilla's missing-texture pattern. Layout itself stays in floats, so a panel at x = 10.4 with width 20.2 fills from pixel 10 to pixel 31. Design with whole numbers and screens look the same in Swing and in game.

### Your own sprites

Put images in your mod's resources at `assets/<modid>/textures/gui/sprites/<path>.png` and name them `SpriteId.of("<modid>", "<path>")`. Vanilla's gui atlas collects that directory from every mod and resource pack, so no registration is needed. Add a `.png.mcmeta` beside a nine-slice image, as described under [Nine-slices](#nine-slices).

```java
--8<-- "minecraft/MinecraftExamples.java:theme"
```

Resource packs can override your sprites like any others. New or changed images need a resource reload (F3+T) in a running game.

Strata's Swing adapter reads `assets/<namespace>/sprites/<path>.png` by default. To preview the same images in Swing, give `SwingSprites` a `SpriteResolver` for Minecraft's layout, such as `id -> "assets/" + id.namespace() + "/textures/gui/sprites/" + id.path() + ".png"`, and pass the `SwingSprites` to `StrataPanel` or `SwingSnapshot.render`.

### Nine-slices

Vanilla reads the slicing from the sprite's `.png.mcmeta` file and ignores the `border` of `Paint.nineSlice`. Other adapters use that `border`. Keep the two in agreement so every engine draws the same:

```json
{
  "gui": {
    "scaling": {
      "type": "nine_slice",
      "width": 200,
      "height": 20,
      "border": 4
    }
  }
}
```

This is vanilla's format, checked against the 26.3 sources. `width` and `height` are the size, in GUI pixels, that the image represents. `border` is either one number for all four sides or an object with `left`, `top`, `right` and `bottom`, and the left and right borders together must be narrower than `width` (top and bottom likewise against `height`). An optional `"stretch_inner": true` stretches the edges and center instead of tiling them.

Without an `.mcmeta`, Minecraft stretches the image like a plain sprite, so a nine-slice paint with no `.mcmeta` looks the same as a sprite paint in game. Swing reads borders in the image's own pixels and stretches edges and centers. For both to match:

- give the image the same pixel size as the `.mcmeta`'s `width` and `height`;
- use the same border in the `.mcmeta` and in `Paint.nineSlice`;
- either set `"stretch_inner": true`, or design edges and center that look the same tiled or stretched, such as flat colors.

### The vanilla theme

`MinecraftThemes.vanilla()` uses vanilla's own sprites:

| Widget | Sprites | Text |
|---|---|---|
| Button | `widget/button`, `widget/button_highlighted` (hovered, focused and pressed), `widget/button_disabled` | White with a shadow, `0xFFA0A0A0` when disabled |
| Text box | `widget/text_field`, `widget/text_field_highlighted` when focused. Vanilla has no disabled field sprite, so a disabled box uses the normal one. | `0xFFE0E0E0`, `0xFF707070` when disabled |
| Check box | `widget/checkbox`, `widget/checkbox_highlighted` (hovered and focused), `widget/checkbox_selected`, `widget/checkbox_selected_highlighted`. The mark is part of the sprites. Vanilla has no disabled sprites. | White with a shadow, `0xFFA0A0A0` when disabled |
| Slider | `widget/slider` for the track, `widget/slider_highlighted` while focused, `widget/slider_handle` for the thumb, `widget/slider_handle_highlighted` while hovered, focused or pressed. No disabled sprites. | White with a shadow, `0xFFA0A0A0` when disabled |
| Scroll panel | `widget/scroller` for the thumb, `widget/scroller_background` for the track | |
| Label | | White with a shadow |

There is no separate pressed sprite in vanilla, so a pressed button looks hovered. The default check box size (17, the line height of 9 plus 8), the slider's height (20) and its thumb width (8) match vanilla's widgets. Strata's scroll panels also follow vanilla: a wheel notch scrolls 10 units, and the thumb is at least 32 units long and at most the track minus 8.

Vanilla's own slider needs Enter before the arrow keys change it. Strata's slider responds to the arrow keys whenever it has focus.

Every key the theme sets:

| Key | Value |
|---|---|
| `button.normal` | nine-slice `minecraft:widget/button`, border 3 |
| `button.hovered` | nine-slice `minecraft:widget/button_highlighted`, border 3 |
| `button.pressed` | nine-slice `minecraft:widget/button_highlighted`, border 3 |
| `button.disabled` | nine-slice `minecraft:widget/button_disabled`, border 1 |
| `button.text` | `0xFFFFFFFF` with shadow |
| `button.text.disabled` | `0xFFA0A0A0` with shadow |
| `label.text` | `0xFFFFFFFF` with shadow |
| `checkbox.normal` | sprite `minecraft:widget/checkbox` |
| `checkbox.hovered` | sprite `minecraft:widget/checkbox_highlighted` |
| `checkbox.checked` | sprite `minecraft:widget/checkbox_selected` |
| `checkbox.checked.hovered` | sprite `minecraft:widget/checkbox_selected_highlighted` |
| `checkbox.text` | `0xFFFFFFFF` with shadow |
| `checkbox.text.disabled` | `0xFFA0A0A0` with shadow |
| `slider.track` | nine-slice `minecraft:widget/slider`, border 1 |
| `slider.track.focused` | nine-slice `minecraft:widget/slider_highlighted`, border 1 |
| `slider.thumb` | nine-slice `minecraft:widget/slider_handle`, border 2 (3 at the bottom) |
| `slider.thumb.hovered` | nine-slice `minecraft:widget/slider_handle_highlighted`, border 2 (3 at the bottom) |
| `slider.text` | `0xFFFFFFFF` with shadow |
| `slider.text.disabled` | `0xFFA0A0A0` with shadow |
| `textbox.normal` | nine-slice `minecraft:widget/text_field`, border 1 |
| `textbox.focused` | nine-slice `minecraft:widget/text_field_highlighted`, border 1 |
| `textbox.disabled` | nine-slice `minecraft:widget/text_field`, border 1 |
| `textbox.selection` | solid `0x800000FF` |
| `textbox.caret` | solid `0xFFD0D0D0` |
| `textbox.text` | `0xFFE0E0E0` with shadow |
| `textbox.text.disabled` | `0xFF707070` with shadow |
| `textbox.text.readonly` | `0xFFA0A0A0` with shadow |
| `scroll.thumb` | nine-slice `minecraft:widget/scroller`, border 1 |
| `scroll.track` | nine-slice `minecraft:widget/scroller_background`, border 1 |
| `list.row.hovered` | solid `0x30FFFFFF` |
| `list.row.selected` | solid `0x60FFFFFF` |
| `list.placeholder` | `0xFFA0A0A0` with shadow |
| `combobox.normal` | nine-slice `minecraft:widget/button`, border 3 |
| `combobox.hovered` | nine-slice `minecraft:widget/button_highlighted`, border 3 |
| `combobox.disabled` | nine-slice `minecraft:widget/button_disabled`, border 1 |
| `combobox.popup` | solid `0xF0101010` |
| `combobox.text` | `0xFFFFFFFF` with shadow |
| `combobox.text.disabled` | `0xFFA0A0A0` with shadow |
| `window.background` | nine-slice `minecraft:popup/background`, border 6 |
| `window.title.text` | `0xFFFFFFFF` with shadow |
| `window.close` | `0xFFA0A0A0` with shadow |
| `window.close.hovered` | `0xFFFFFFFF` with shadow |
| `tab.normal` | nine-slice `minecraft:widget/tab`, border 2 (0 at the bottom) |
| `tab.hovered` | nine-slice `minecraft:widget/tab_highlighted`, border 2 (0 at the bottom) |
| `tab.selected` | nine-slice `minecraft:widget/tab_selected`, border 2 (0 at the bottom) |
| `tab.text` | `0xFFA0A0A0` with shadow |
| `tab.text.selected` | `0xFFFFFFFF` with shadow |
| `tab.text.disabled` | `0xFF606060` with shadow |
| `progress.track` | solid `0xFF000000` |
| `progress.fill` | solid `0xFF008200` |
| `progress.text` | `0xFFFFFFFF` with shadow |
| `separator` | solid `0xB9FFFFFF` |
| `overlay.backdrop` | solid `0x60000000` |

`button.focus` and `checkbox.focus` are not set, so vanilla-themed buttons and check boxes show focus through their hovered sprites only. Vanilla's check box sprites include the check mark, so `checkbox.mark` is not set either, and vanilla has no disabled check box or slider sprites. Vanilla has no sprites for list rows, progress bars or separators, so they are plain colors. The tooltip keys are not set, so tooltips use Strata's fallback, a dark box with a purple edge in vanilla's classic tooltip colors. The borders match the sprites' `.png.mcmeta` files in 26.3. Minecraft reads the `.mcmeta`; other engines use the borders.

## Text

Text draws in the screen's font, which follows the player's resource packs.

| Style | In game |
|---|---|
| Bold | Vanilla bold, one pixel wider per character |
| Italic | Vanilla italic |
| Underline, strikethrough | Vanilla's |
| Shadow | Vanilla's, a darker copy one pixel down and to the right |

`MinecraftTextMetrics` measures with `Font.width` and `Font.lineHeight`. Widths are whole GUI pixels, including vanilla's extra pixel for bold, and the line height is the font's, 9 for the default font. Text that just fits in Swing may wrap or overflow in game, so let components measure and leave slack in fixed widths around text.

Avoid vanilla's `§` formatting codes in Strata text. Strata does not interpret them, and vanilla's text drawing may, so the same text would look different in Swing and in game.

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

Because Minecraft sends no modifiers with pointer moves, scrolls or typed characters, those events report `Modifiers.NONE`. A Shift+scroll or Ctrl+drag check works in Swing and never fires in game. If you need modifier state during a drag, remember it from the `POINTER_PRESS` that started it. A scroll sensitivity other than 1 turns notched wheels into fractional, `PRECISE` scrolls.

### Keys are SDL scancodes

Since 26.3, Minecraft's key values are SDL scancodes, which name physical key positions, not GLFW key codes. The adapter maps them through the `InputConstants` names, never raw numbers, and so should you if you ever handle vanilla key events yourself. Scancodes Strata has no key for, such as F13 to F24 and the keypad comma, arrive as `Key.UNKNOWN`.

### Typed characters need text input mode

Minecraft 26.3 sends no characters at all until a text field reports focus. When a Strata text box, or any component whose `acceptsTextInput()` is true, gains focus, the adapter calls `Minecraft.onTextInputFocusChange` for the hosting screen, and it stops text input when focus leaves. If a custom component takes typing but never receives `CHAR` events, it is missing that override.

While a text box has focus, it also consumes every key that types a character, so vanilla shortcuts on those keys, such as the inventory key or the chat key, do not fire while the player types.

### Escape

Escape is not consumed by any built-in widget, so vanilla receives it and closes the screen, even while a text box has focus. To keep a screen open on Escape, consume the key in a root handler, or subclass `StrataScreen` and override vanilla's `shouldCloseOnEsc()`.

### Strata claims the whole screen

A press counts as handled whenever it hits a Strata component, and a root anchored with `FILL` covers the whole screen. Vanilla widgets added to a `StrataScreen` subclass would therefore never be clicked. Mixing vanilla widgets and Strata components in one screen is not supported yet; see [Not available yet](#not-available-yet).

### macOS

On macOS, vanilla uses Command for its edit shortcuts. Strata reports Command as `Modifier.SUPER` and its text box checks Control, so in a Strata text box on a Mac, copy, cut, paste and select all are Control+C, X, V and A, not Command. A platform shortcut modifier is planned.

## Pausing

Whether a single-player world pauses behind the screen follows vanilla's `isPauseScreen()`, which is true by default. Override it to keep the world running:

```java
--8<-- "minecraft/MinecraftExamples.java:nopause"
```

## Threading

Everything in the adapter runs on Minecraft's render thread, the same thread vanilla screens use. Never touch a Strata component from the server thread, a network thread or your own threads; use `Minecraft.getInstance().execute(...)`.

## Differences from Swing

Strata's Swing adapter is the quickest way to work on a screen. When the same screen runs in game, these behaviors change:

| Topic | Minecraft 26.3 | Swing |
|---|---|---|
| One UI unit | One GUI pixel (follows GUI scale) | `scale` pixels, fixed when the panel is made |
| Rounding | Fills and clips round outward to whole GUI pixels; text to the nearest pixel | None. Java2D draws fractional coordinates. |
| Font | The player's font, from resource packs | An AWT font, Dialog 9 by default |
| Text widths | Whole GUI pixels; bold adds one per character | Fractional |
| Line height | 9 for the default font | The AWT font's height |
| Modifiers with pointer moves, scrolls, characters | Never reported (`NONE`) | Reported |
| Modifiers with button presses and keys | Reported | Reported |
| Edit shortcuts on macOS | Text box uses Control, vanilla uses Command | Control |
| Key repeat | Held keys repeat at the system rate | Same |
| Typed characters | Only while a text-input component has focus | Always |
| Horizontal scrolling | From the device, flipped to Strata's direction | Shift plus the vertical wheel |
| Escape | Closes the screen unless consumed | Does nothing unless you handle it |
| Clipboard | Vanilla's | The system clipboard; none when headless |
| Missing sprite | Vanilla's missing-texture pattern | Magenta |
| Sprite location | `assets/<modid>/textures/gui/sprites/<path>.png`, through the gui atlas | `assets/<namespace>/sprites/<path>.png` on the classpath, or wherever a `SpriteResolver` points |
| Nine-slice slicing | From the sprite's `.png.mcmeta`; the `border` argument is ignored | From the `border` argument, in image pixels |
| Nine-slice edges and center | Tiled, unless the `.mcmeta` sets `stretch_inner` | Stretched |
| `NOT_ALLOWED` cursor | Shown | Shows the default arrow |
| Screen stack | Vanilla's; one Strata screen per `StrataScreen` | `StrataPanel.screens()` |
| `hidesBackground()` | No effect | Skips drawing screens below |
| Pausing | Single-player pauses unless `isPauseScreen()` is false | Not applicable |
| UI thread | The render thread | The event dispatch thread |

## Loader notes

| | NeoForge | Fabric |
|---|---|---|
| Jar | `strata-minecraft-neoforge` | `strata-minecraft-fabric` |
| Contents | `common`, plus Strata's `api`, `spi` and `core` jars nested with `jarJar` | `common`, plus Strata's `api`, `spi` and `core` jars nested with `include` |
| Entry point | An empty `@Mod("strata")` class, client only | None. The jar only declares the mod. |
| Depends on | NeoForge `[26.3.0.83,)`, Minecraft `[26.3, 26.4)` | Fabric Loader `>=0.19.5`, Minecraft `~26.3`, Java `>=25`. Not Fabric API. |
| Side | `CLIENT` | `"environment": "client"` |
| Dev client | `./gradlew :neoforge:runClient` | `./gradlew :fabric:runClient` |

The mod id is `strata` on both loaders. The Fabric build uses Loom 1.18.4, which does not remap, so dependencies use `implementation` instead of `modImplementation`.

Because `StrataScreen` is an ordinary vanilla screen, loader screen events, such as NeoForge's `ScreenEvent`s, fire for it like for any other screen.

## Common questions

### Which Minecraft versions are supported?

26.3 only. The mod metadata accepts `[26.3, 26.4)`. Versions before 26.x will not be supported: 26.3 changed the GUI and input APIs (`GuiGraphicsExtractor`, SDL scancodes) that the adapter is built on.

### Does Strata need Fabric API?

No. The Fabric jar depends only on Fabric Loader, Minecraft and Java.

### Does Strata use mixins?

No. It hosts screens through vanilla's own `Screen` class.

### Does it work on a dedicated server?

Strata is client-only. Keep classes that touch it out of server code, the same as you would for vanilla screen classes.

### Can I draw on the HUD?

Not yet. HUD layers are planned.

### Can I put vanilla buttons in a Strata screen, or Strata components in a vanilla screen?

Not yet. Both directions are planned as hosting adapters. See [Not available yet](#not-available-yet).

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| Nothing shows up | `setScreen` ran off the client thread. Use `Minecraft.getInstance().execute(...)` from other threads. |
| Closing goes back to the game instead of the previous screen | That is vanilla's default `onClose()`. Override it, as in [Returning to a parent screen](#returning-to-a-parent-screen). |
| Escape closes the screen while typing | The text box does not consume Escape. Consume it in a root handler, or override `shouldCloseOnEsc()`. |
| Typing does nothing in a custom component | Override `acceptsTextInput()` to return true. See [Typed characters need text input mode](#typed-characters-need-text-input-mode). |
| Modifier checks never pass in game | Minecraft sends no modifiers with moves, scrolls or characters. |
| Vanilla widgets in a `StrataScreen` never get clicks | Strata claims every press that hits one of its components, and a `FILL` root covers the screen. Mixing them is not supported yet. |
| Text is cut off in game but fine in Swing | Minecraft's font is wider in places and rounds to whole pixels. Leave slack around text, or let it measure. |
| Things are a pixel off in game | Fractional positions round outward to whole GUI pixels. Use whole numbers. |
| Purple and black checkerboard | The sprite is missing from your mod's resources at `assets/<modid>/textures/gui/sprites/<path>.png`, or the game needs a resource reload. |
| A nine-slice stretches | The sprite has no `.png.mcmeta` with `nine_slice` scaling. |
| A nine-slice looks different in Swing and in game | See [Nine-slices](#nine-slices). |
| "Cannot find a variant" or loader attribute errors in a mod project | `common` is compiled into each loader jar and carries a loader attribute. Depend on `strata-minecraft-neoforge` or `strata-minecraft-fabric`, not on `common`. |

## Status

| Area | How it is tested |
|---|---|
| Input mapping | Against every key constant in the real 26.3 `InputConstants` |
| The canvas | Against a mocked `GuiGraphicsExtractor`, including item visuals made from real item stacks |
| Item tooltips | Styling against real chat components |
| The vanilla theme | It defines every key the built-in widgets require |
| Loader packaging | The processed mod metadata and the NeoForge entry point |
| The examples on this page | `docs-examples` compiles them against `common` and the vanilla classes |

Nothing has run inside a Minecraft client yet. Minecraft drawing (item rendering included), vanilla's handling of small nine-slices, text input focus and mod loading on both loaders are covered only by compiling against the real classes and by unit tests with mocks. Publishing to Maven and depending on Strata from a separate mod project have not been tried.

## Not available yet

- HUD overlays. Strata on the in-game HUD, through NeoForge's `RegisterGuiLayersEvent` and Fabric's `HudElementRegistry`, is planned.
- Vanilla widgets inside Strata, and Strata components inside vanilla screens. Both are planned as hosting adapters (`MinecraftUi.wrap` and a `StrataElement`), not mixins.
- Narration of Strata components. Vanilla narrates the screen's title only.
- Controller and gamepad navigation.
- Tooltip images, such as a bundle's contents, in item tooltips.
- Drawing beyond fills, gradients, sprites, text and items. Vanilla offers no public hook for custom GUI geometry, and Fabric has none at all.
- Changing a `StrataScreen`'s theme after it is created. `StrataScreen` takes its theme in the constructor and has no setter.
