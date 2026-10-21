<p align="center"><img src=".github/assets/logo.svg" width="128" alt="Strata-Minecraft logo"></p>

# Strata-Minecraft

Strata-Minecraft runs [Strata](https://github.com/InspireNXE/Strata), an engine-agnostic GUI library for Java, inside Minecraft. Screens go through vanilla's GUI pipeline and take input from vanilla's screen callbacks, so they look and behave like vanilla screens and follow the player's resource packs.

## Loaders

Each Minecraft version has a branch of its own: `main` for the newest, `mc/1.21.1` and `mc/1.20.1` for the versions most mods settled on.

| Minecraft | Branch | <img src=".github/assets/loader-neoforge.svg" width="24" alt=""> NeoForge | <img src=".github/assets/loader-forge.svg" width="24" alt=""> Forge | <img src=".github/assets/loader-fabric.svg" width="24" alt=""> Fabric | <img src=".github/assets/loader-sponge.svg" width="24" alt=""> Sponge |
|---|---|---|---|---|---|
| 26.3 | `main` | 26.3.0.83 or newer, SpongeNeo optional | 66.0.13 or newer, SpongeForge optional | Loader 0.19.5 or newer | SpongeVanilla for SpongeAPI 21 |
| 1.21.1 | `mc/1.21.1` | 21.1.264 or newer, SpongeNeo optional | 52.1.20 or newer, SpongeForge optional | Loader 0.19.5 or newer | Through SpongeNeo and SpongeForge, SpongeAPI 12 |
| 1.20.1 | `mc/1.20.1` | | 47.4.28 or newer | Loader 0.19.5 or newer | |

Fabric API is not needed at run time on any version.

Strata is a client-side mod with the id `strata` on every loader. Players install the jar for their loader next to your mod. Nothing is published yet.

## Showing a screen

Screens are written against Strata's API exactly as for any other engine, and opened from any thread:

```java
Strata.screens().open(new SettingsScreen());
```

The adapter shows it in a vanilla `Screen` in the vanilla theme. Input Strata does not use goes on to vanilla, so Escape still closes the screen. [Using Strata in a mod](notes/minecraft.md#using-strata-in-a-mod) has the dependency setup for each loader.

## Modules

| Module | What it is |
|---|---|
| `common` | The adapter, written against vanilla classes only. |
| `sponge` | The code that needs SpongeAPI, which backs Strata's causes with Sponge's. |
| `neoforge`, `forge`, `fabric`, `spongevanilla` | The mod and plugin jars, named `strata-minecraft-<module>` in the group `org.inspirenxe`. |
| `docs` | The code the documentation shows, compiled so it stays correct. |

## Building

Building needs JDK 25 and a Strata checkout beside this one at `../Strata`, which the build includes from source. `strataDir` in `gradle.properties` names another checkout.

```bash
git clone https://github.com/InspireNXE/Strata.git
git clone https://github.com/InspireNXE/Strata-Minecraft.git
cd Strata-Minecraft
./gradlew build
```

`./gradlew :neoforge:runClient`, `./gradlew :forge:runClient` and `./gradlew :fabric:runClient` start a client with Strata loaded. SpongeVanilla has no client task, because its release jars start servers only. Strata on its own opens no screens, so you need a mod or a small test to open one.

On Windows, if Gradle fails with "Unable to establish loopback connection", point the JDK's Unix domain sockets at a local folder:

```bash
export JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=$PWD/.gradle/udstmp"
```

## Documentation

- [notes/minecraft.md](notes/minecraft.md): using Strata in Minecraft, from loader setup and opening a screen to items and container screens.
- [notes/DESIGN.md](notes/DESIGN.md): how the adapter maps Strata's SPI onto 26.3, with its decisions and open questions.
- [notes/ROADMAP.md](notes/ROADMAP.md): what is left before Almura's screens can run on Strata in game.

Strata itself is documented on the [documentation site](https://inspirenxe.github.io/Strata/).

## License

Strata-Minecraft is released under the Apache License, Version 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE). It is not affiliated with Mojang or Microsoft. The NeoForge, Forge, Fabric and Sponge marks belong to their projects.
