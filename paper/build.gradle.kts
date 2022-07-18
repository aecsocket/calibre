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
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://jitpack.io")
}

dependencies {
    api(projects.calibreCore)
    paperDevBundle("$minecraft-R0.1-SNAPSHOT")

    // plugins
    compileOnly(libs.glossaCore)
    compileOnly(libs.cloudCore)
    compileOnly(libs.cloudPaper)
    compileOnly(libs.configurateExtraKotlin)
    compileOnly(libs.adventureExtraKotlin)
    compileOnly(libs.packetEventsApi)
    compileOnly(libs.packetEventsSpigot)
    compileOnly(libs.alexandriaCore)
    compileOnly(libs.alexandriaPaper)

    compileOnly(libs.sokolCore)
    compileOnly(libs.sokolPaper)

    // shaded
    implementation(libs.bstatsPaper)

    testImplementation(kotlin("test"))
}

tasks {
    shadowJar {
        mergeServiceFiles()
        exclude("kotlin/")
        listOf(
            "org.bstats",

            "org.jetbrains",
            "org.intellij",
        ).forEach { relocate(it, "${project.group}.lib.$it") }
    }

    assemble {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion(minecraft)
    }
}
