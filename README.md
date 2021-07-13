# Calibre

Platform-agnostic, customisable and modular gun library, with extensive customisation and developer
API, based on the Sokol data-driving library.

---

Calibre provides an alternative to typical Minecraft gun plugins - and even gun mods - by approaching
the issue from another angle. It provides no inbuilt guns, however hands over all control to the
server owner. It uses the Sokol data-driving library to allow you to create complex item trees, each
with their own functions (for example, a laser attachment that can either be placed on a gun, or held
in a player's hand - either way, it produces a functioning laser beam).

You can find a full comparison to other gun libraries in the wiki: TODO

## Paper

Calibre is exposed as a Paper plugin, however the item configuration is done in Sokol's configuration
files.

### Dependencies

* [Java >=16](https://adoptopenjdk.net/?variant=openjdk16&jvmVariant=hotspot)
* [Paper >=1.17.1](https://papermc.io/)
* [Minecommons >=1.2](https://gitlab.com/aecsocket/minecommons)
* [Sokol >=1.2](https://gitlab.com/aecsocket/sokol)
* [ProtocolLib >=4.7.0](https://www.spigotmc.org/resources/protocollib.1997/)

### [Download](https://gitlab.com/api/v4/projects/20514863/jobs/artifacts/master/raw/paper/build/libs/calibre-paper-1.1.jar?job=build)

### Documentation

TODO

### Permissions

TODO

### Commands

TODO

## Development Setup

### Coordinates

#### Maven

Repository
```xml
<repository>
    <id>gitlab-calibre-minecommons</id>
    <url>https://gitlab.com/api/v4/projects/20514863/packages/maven</url>
</repository>
```
Dependency
```xml
<dependency>
    <groupId>com.gitlab.aecsocket.calibre</groupId>
    <artifactId>[MODULE]</artifactId>
    <version>[VERSION]</version>
</dependency>
```

#### Gradle

Repository
```kotlin
maven("https://gitlab.com/api/v4/projects/20514863/packages/maven")
```

Dependency
```kotlin
implementation("com.gitlab.aecsocket.calibre", "[MODULE]", "[VERSION]")
```

### Usage

#### [Javadoc](https://aecsocket.gitlab.io/calibre)

### Modules

* Core `core`

Implementations:
* Paper `paper`
