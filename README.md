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

Strata is a client-side mod with the id `strata` on every loader. Players install the jar for their loader next to your mod.

## Showing a screen

Screens are written against Strata's API exactly as for any other engine, and opened from any thread:

```java
Strata.screens().open(new SettingsScreen());
```

The adapter shows it in a vanilla `Screen` in the vanilla theme. Input Strata does not use goes on to vanilla, so Escape still closes the screen.

## Using Strata in a mod

The jars are published to Maven Central in the group `org.inspirenxe`. Until 0.1.0 is released there are snapshots only, in Central's snapshot repository:

```groovy
repositories {
    maven {
        url = uri('https://central.sonatype.com/repository/maven-snapshots/')
        mavenContent {
            snapshotsOnly()
        }
    }
}

dependencies {
    // strata-minecraft-neoforge, -forge or -fabric for your loader
    implementation 'org.inspirenxe:strata-minecraft-neoforge:0.1.0-SNAPSHOT'
    compileOnly 'org.inspirenxe:strata-api:0.1.0-SNAPSHOT'
}
```

The loader jar lists Strata's `api`, `spi` and `core` as run-time dependencies only, so add `strata-api` for compiling. On Fabric, Loom 1.18 does not remap, so use `implementation`, not `modImplementation`. A SpongeVanilla plugin takes `strata-minecraft-spongevanilla` as `compileOnly`, because players install the Strata plugin next to it and two plugin jars cannot hold the same packages.

Declare `strata` as a required client-side dependency in your mod metadata (`neoforge.mods.toml`, `mods.toml`, `fabric.mod.json` or `sponge_plugins.json`), so the game refuses to start without it.

## Modules

| Module | What it is |
|---|---|
| `common` | The adapter, written against vanilla classes only. |
| `sponge` | The code that needs SpongeAPI, which backs Strata's causes with Sponge's. |
| `neoforge`, `forge`, `fabric`, `spongevanilla` | The mod and plugin jars, named `strata-minecraft-<module>` in the group `org.inspirenxe`. |

## Building

Building needs JDK 25. With a Strata checkout beside this one at `../Strata`, the build uses it, so a change to Strata reaches the adapter at once; `strataDir` in `gradle.properties` names another checkout. Without one, the build takes `strata_version` from Maven Central.

```bash
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

Strata's guides, reference and Javadoc are on the [documentation site](https://inspirenxe.github.io/Strata/).

## License

Strata-Minecraft is released under the Apache License, Version 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE). It is not affiliated with Mojang or Microsoft. The NeoForge, Forge, Fabric and Sponge marks belong to their projects.
