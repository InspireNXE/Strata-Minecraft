<p align="center"><img src=".github/assets/logo.svg" width="128" alt="Strata-Minecraft logo"></p>

# Strata-Minecraft

Strata-Minecraft runs [Strata](https://github.com/InspireNXE/Strata), an engine-agnostic GUI library for Java, inside Minecraft. Screens go through vanilla's GUI pipeline and take input from vanilla's screen callbacks, so they look and behave like vanilla screens and follow the player's resource packs.

## Loaders

This is the `mc/1.21.1` branch, for Minecraft 1.21.1. The `main` branch is for the newest Minecraft, and `mc/1.20.1` for 1.20.1.

| | Loader | Minecraft | Versions |
|---|---|---|---|
| <img src=".github/assets/loader-neoforge.svg" width="40" alt=""> | NeoForge | 1.21.1 | NeoForge 21.1.264 or newer. SpongeNeo is optional. |
| <img src=".github/assets/loader-forge.svg" width="40" alt=""> | Forge | 1.21.1 | Forge 52.1.20 or newer. SpongeForge is optional. |
| <img src=".github/assets/loader-fabric.svg" width="40" alt=""> | Fabric | 1.21.1 | Fabric Loader 0.19.5 or newer. Fabric API is not needed at run time. |

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
| `neoforge`, `forge`, `fabric` | The mod jars, named `strata-minecraft-<module>` in the group `org.inspirenxe`. |
| `docs` | The code the documentation shows, compiled so it stays correct. |

## Building

Building needs JDK 25, which runs Gradle and Strata's javadoc, and a checkout of Strata's `java17` branch beside this one at `../Strata-java17`, which the build includes from source; `strataDir` in `gradle.properties` names it. The mod compiles for Java 21, and Gradle fetches that JDK itself.

```bash
git clone -b java17 https://github.com/InspireNXE/Strata.git Strata-java17
git clone -b mc/1.21.1 https://github.com/InspireNXE/Strata-Minecraft.git
cd Strata-Minecraft
./gradlew build
```

`./gradlew :neoforge:runClient`, `./gradlew :forge:runClient` and `./gradlew :fabric:runClient` start a client with Strata loaded. Strata on its own opens no screens, so you need a mod or a small test to open one.

On Windows, if Gradle fails with "Unable to establish loopback connection", point the JDK's Unix domain sockets at a local folder:

```bash
export JAVA_TOOL_OPTIONS="-Djdk.net.unixdomain.tmpdir=$PWD/.gradle/udstmp"
```

## Documentation

- [notes/minecraft.md](notes/minecraft.md): using Strata in Minecraft, from loader setup and opening a screen to items and container screens.
- [notes/DESIGN.md](notes/DESIGN.md): how the adapter maps Strata's SPI onto Minecraft, with its decisions and open questions. Section 10 covers the branches for older versions.
- [notes/ROADMAP.md](notes/ROADMAP.md): what is left before Almura's screens can run on Strata in game.

Strata itself is documented on the [documentation site](https://inspirenxe.github.io/Strata/).

## License

Strata-Minecraft is released under the Apache License, Version 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE). It is not affiliated with Mojang or Microsoft. The NeoForge, Forge, Fabric and Sponge marks belong to their projects.
