enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }

    plugins {
        kotlin("jvm") version "1.6.21"
        id("org.jetbrains.dokka") version "1.6.21"

        id("com.github.johnrengelman.shadow") version "7.1.2"
        id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
        id("xyz.jpenilla.run-paper") version "1.0.6"
        id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
    }
}

rootProject.name = "calibre"

listOf(
    "core",
    "paper"
).forEach {
    val name = "${rootProject.name}-$it"
    include(name)
    project(":$name").apply {
        projectDir = file(it)
    }
}
