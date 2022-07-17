plugins {
    kotlin("jvm")
    id("io.papermc.paperweight.userdev")
    id("com.github.johnrengelman.shadow")
    id("xyz.jpenilla.run-paper")
}

val minecraft = libs.versions.minecraft.get()

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://gitlab.com/api/v4/groups/9631292/-/packages/maven")
    maven("https://jitpack.io")
}

dependencies {
    api(projects.calibreCore)
    paperDevBundle("$minecraft-R0.1-SNAPSHOT")
    
    // shaded

    implementation(libs.bstatsPaper)
    implementation(libs.packetEventsApi)
    implementation(libs.packetEventsSpigot)

    // dependencies

    compileOnly(libs.alexandriaCore)
    compileOnly(libs.alexandriaPaper)

    compileOnly(libs.sokolCore)
    compileOnly(libs.sokolPaper)

    // library loader

    compileOnly(libs.kotlinStdlib)
    compileOnly(libs.kotlinReflect)
    compileOnly(libs.configurateCore)
    compileOnly(libs.cloudPaper)
    compileOnly(libs.configurateExtraKotlin)
    compileOnly(libs.adventureExtraKotlin)

    testImplementation(kotlin("test"))
}

tasks {
    shadowJar {
        mergeServiceFiles()
        exclude("kotlin/")
        listOf(
            "com.github.retrooper.packetevents",
            "io.github.retrooper.packetevents",
            "org.bstats",
            "com.google.gson",
            "org.jetbrains",
            "org.intellij"
        ).forEach { relocate(it, "${project.group}.lib.$it") }
    }

    assemble {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion(minecraft)
    }
}
