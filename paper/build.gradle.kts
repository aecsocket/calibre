plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    id("xyz.jpenilla.run-paper")
    id("net.minecrell.plugin-yml.bukkit")
}

val minecraftVersion = libs.versions.minecraft.get()

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    api(projects.calibreCore)
    compileOnly("io.papermc.paper", "paper-api", "$minecraftVersion-R0.1-SNAPSHOT")
    implementation(libs.alexandriaPaper) { artifact { classifier = "reobf" } }
    implementation(libs.bstatsPaper)
    implementation(libs.packetEvents)

    //library("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
}

tasks {
    shadowJar {
    }

    assemble {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
    }
}

bukkit {
    name = "Calibre"
    main = "${project.group}.paper.CalibrePlugin"
    apiVersion = "1.18"
    authors = listOf("aecsocket")
    website = "https://aecsocket.github.com/calibre"
}
