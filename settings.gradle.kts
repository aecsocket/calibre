enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }

    plugins {
        id("io.freefair.aggregate-javadoc") version "6.3.0"
        id("com.github.johnrengelman.shadow") version "7.1.0"
        id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
        id("xyz.jpenilla.run-paper") version "1.0.6"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
       /* maven {
            url = uri("https://maven.pkg.github.com/aecsocket/minecommons")
            credentials {
                username = System.getenv("GPR_ACTOR")
                password = System.getenv("GPR_TOKEN")
            }
        }*/
        /*maven {
            url = uri("https://maven.pkg.github.com/aecsocket/sokol")
            credentials {
                username = System.getenv("GPR_ACTOR")
                password = System.getenv("GPR_TOKEN")
            }
        }*/
        maven("https://repo.incendo.org/content/repositories/snapshots/")
        maven("https://papermc.io/repo/repository/maven-public/")
        maven("https://repo.dmulloy2.net/nexus/repository/public/")
        mavenCentral()
    }
}

rootProject.name = "calibre"

subproject("${rootProject.name}-core") {
    projectDir = file("core");
}
subproject("${rootProject.name}-paper") {
    projectDir = file("paper");
}

inline fun subproject(name: String, block: ProjectDescriptor.() -> Unit) {
    include(name);
    project(":$name").apply(block);
}
