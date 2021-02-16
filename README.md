# Calibre

## Advanced, scalable and modular gun framework, with implementation for Paper servers

---

### What is Calibre?

Calibre is a modular and flexible component framework, with implementations for weapons such as guns, melee weapons at grenades.
It targets the latest version of Minecraft and the main platform is Paper latest, however it can be adapted to other platforms.

## [Wiki](https://gitlab.com/aecsocket/calibre/-/wikis/home)
## [Download Calibre](https://gitlab.com/aecsocket/calibre/-/jobs/artifacts/master/raw/paper/target/calibre-paper-1.0-SNAPSHOT.jar?job=build)

### What can Calibre do?

No screenshots or videos yet, but go to the wiki to see a list of features.

### For server owners

To configure the plugin, go to [the wiki](https://gitlab.com/aecsocket/calibre/-/wikis/home).

### For developers

Available on Jitpack.

Latest version: `master-SNAPSHOT`
Current version: (none, use latest)

#### Maven coordinates

Repository:

        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>

Core:

    <dependency>
        <groupId>com.gitlab.aecsocket.calibre</groupId>
        <artifactId>calibre-core</artifactId>
        <version>[the version]</version>
    </dependency>

Paper:

    <dependency>
        <groupId>com.gitlab.aecsocket.calibre</groupId>
        <artifactId>calibre-paper</artifactId>
        <version>[the version]</version>
    </dependency>