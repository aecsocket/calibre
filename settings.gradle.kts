enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }

    plugins {
        kotlin("jvm") version "1.7.10"
        id("org.jetbrains.dokka") version "1.7.10"

        id("io.papermc.paperweight.userdev") version "1.3.6"
        id("com.github.johnrengelman.shadow") version "7.1.2"
        id("xyz.jpenilla.run-paper") version "1.0.6"
    }
}

rootProject.name = "calibre"

listOf(
    "core",
    "paper",
).forEach {
    val name = "${rootProject.name}-$it"
    include(name)
    project(":$name").apply {
        projectDir = file(it)
    }
}
