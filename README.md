# Strata-Minecraft

Strata-Minecraft runs [Strata](https://github.com/InspireNXE/Strata), an engine-agnostic GUI library for Java games, inside Minecraft 26.3 on NeoForge and Fabric. Strata screens drawn here go through vanilla's GUI pipeline and take input from vanilla's screen callbacks, so they look and behave like vanilla screens.

| Module | What it is |
|---|---|
| `common` | The adapter, written against vanilla classes only: `StrataScreen`, `MinecraftThemes`, `MinecraftVisuals`, `MinecraftTooltips`, `MinecraftCanvas`, `MinecraftPlatform`, `MinecraftTextMetrics` and `MinecraftInput`, all in `org.inspirenxe.strata.minecraft`. |
| `neoforge` | The `strata` mod jar for NeoForge: `common` compiled in, Strata's `api`, `spi` and `core` jars nested with `jarJar`, and an empty `@Mod` entry point. |
| `fabric` | The `strata` mod jar for Fabric: `common` compiled in, and Strata's jars nested with `include`. |
| `docs-examples` | The code the documentation shows, compiled against `common` and the vanilla classes so it stays correct. |

The jars are named `strata-minecraft-<module>`, in the group `org.inspirenxe`. The mod id is `strata` on both loaders.

## Showing a screen

A screen is written against Strata's API exactly as it would be for any other engine. `StrataScreen` is a vanilla `Screen` that hosts it, so you open it like any other screen, on the client thread:

```java
Minecraft.getInstance().gui.setScreen(
        new StrataScreen(Component.literal("Settings"), new SettingsScreen(), MinecraftThemes.vanilla()));
```

Input that Strata does not use goes on to vanilla, so Escape still closes the screen. [docs/minecraft.md](docs/minecraft.md) covers the rest: the lifecycle, units, sprites and nine-slices, the vanilla theme, items and item tooltips, input, threading, and how the game differs from Strata's Swing adapter.

## Requirements

| What | Version |
|---|---|
| Java | 25 or newer, for building and at run time |
| Gradle | 9.7.1, through the wrapper in the repository |
| Minecraft | 26.3 exactly; the mod metadata accepts `[26.3, 26.4)` |
| NeoForge | 26.3.0.83 or newer |
| Fabric Loader | 0.19.5 or newer. Fabric API is not required at run time. |
| Strata | A checkout of the Strata repository next to this one |

## Building

Strata is not published yet, so this build compiles it from source through a Gradle composite build. `settings.gradle` includes `../Strata` with `includeBuild` and substitutes its `api`, `spi` and `core` projects for the `org.inspirenxe:strata-*` dependencies. Clone both repositories side by side:

```bash
git clone https://github.com/InspireNXE/Strata.git
git clone https://github.com/InspireNXE/Strata-Minecraft.git
cd Strata-Minecraft
./gradlew build
```

The directory names matter: the build expects Strata at `../Strata`. A change in the Strata checkout is picked up by the next Strata-Minecraft build, with no publishing step in between.

The build compiles `common` against vanilla through NeoForm, runs the tests against the real vanilla classes, runs Javadoc on `common` with every doclint check, and fails on any Javadoc warning, including a missing comment. It also compiles the documentation examples and builds both mod jars in each loader module's `build/libs`.

| Command | Does |
|---|---|
| `./gradlew :neoforge:runClient` | Starts a NeoForge client with Strata loaded |
| `./gradlew :fabric:runClient` | Starts a Fabric client with Strata loaded |
| `./gradlew :common:javadoc` | Builds the adapter's Javadoc into `common/build/docs/javadoc` |

Strata on its own opens no screens. You need a mod, or a small test of your own, to open one.

On Windows, if Gradle fails with "Unable to establish loopback connection", the JDK's NIO pipe could not create its Unix domain socket in `%TEMP%`. Point it at a local directory first:

```bash
export JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=$PWD/.gradle/udstmp"
```

## Using Strata in a mod

Strata ships as its own client-side mod with the mod id `strata`. Players install the Strata jar for their loader next to your mod.

Until Strata is published, publish both builds to your local Maven repository: run `./gradlew publishToMavenLocal` in the Strata checkout, then in this one. The loader jar lists Strata's `api`, `spi` and `core` as run-time dependencies only, so add `strata-api` for compiling.

This has not been tried from a separate mod project yet. If a snippet below fails for you, please open an issue with the error.

NeoForge, in a ModDevGradle project:

```groovy
repositories {
    mavenLocal()
}

dependencies {
    implementation 'org.inspirenxe:strata-minecraft-neoforge:0.1.0-SNAPSHOT'
    compileOnly 'org.inspirenxe:strata-api:0.1.0-SNAPSHOT'
}
```

Declare the dependency in `META-INF/neoforge.mods.toml`, so the game refuses to start without Strata instead of failing later:

```toml
[[dependencies.examplemod]]
modId = "strata"
type = "required"
versionRange = "*"
ordering = "NONE"
side = "CLIENT"
```

Fabric, in a Loom project. Loom 1.18 does not remap, so use `implementation`, not `modImplementation`:

```groovy
repositories {
    mavenLocal()
}

dependencies {
    implementation 'org.inspirenxe:strata-minecraft-fabric:0.1.0-SNAPSHOT'
    compileOnly 'org.inspirenxe:strata-api:0.1.0-SNAPSHOT'
}
```

Declare the dependency in `fabric.mod.json`:

```json
"depends": {
    "strata": "*"
}
```

The examples accept any Strata version, because both loaders sort `0.1.0-SNAPSHOT` before `0.1.0`, so a range such as `[0.1.0,)` would reject the current build. Pin a real range once releases exist.

Only use the `org.inspirenxe.strata.api` packages and the `org.inspirenxe.strata.minecraft` package from your mod. The loader jar also carries `spi` and `core`, because the game needs them at run time, but they are not part of the API and change without notice.

Strata is a client-side mod (`side = "CLIENT"` on NeoForge, `"environment": "client"` on Fabric). Keep every class that touches Strata out of code that runs on a dedicated server, the same as you would for vanilla screen classes.

Both loaders can also nest one mod jar inside another: NeoForge with `jarJar`, Fabric with `include`. Nesting the Strata jar in your mod means players do not have to install it themselves. This has not been tried yet. If several mods nest different Strata versions, the loader picks one, so keep your version range honest.

## Documentation

- [docs/minecraft.md](docs/minecraft.md): using Strata in Minecraft, from opening a screen to item tooltips.
- [docs/DESIGN.md](docs/DESIGN.md): how the adapter maps Strata's SPI onto 26.3, the verified 26.3 facts, and the decisions and open questions.
- [docs/ROADMAP.md](docs/ROADMAP.md): what is left before Almura's screens can run on Strata in game.

Strata's own documentation, for components, layout, events, themes and writing screens, is in the [Strata repository](https://github.com/InspireNXE/Strata).

Strata-Minecraft is released under the MIT License.
