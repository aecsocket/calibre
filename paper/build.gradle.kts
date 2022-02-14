plugins {
    id("java-library")
    id("maven-publish")
    id("com.github.johnrengelman.shadow")
    id("net.minecrell.plugin-yml.bukkit")
    id("xyz.jpenilla.run-paper")
}

dependencies {
    api(projects.calibreCore) {
        exclude("com.github.aecsocket", "minecommons-core")
        exclude("com.github.aecsocket", "sokol-core")
    }
    compileOnly(libs.paper) {
        exclude("junit", "junit")
    }

    implementation(libs.minecommonsPaper)
    implementation(libs.sokolPaper)
    implementation(libs.bstatsPaper)

    compileOnly(libs.protocolLib)
}

tasks {
    shadowJar {
        listOf(
            "io.leangen.geantyref",
            "org.spongepowered.configurate",
            "com.typesafe.config",
            "au.com.bytecode.opencsv",
            "cloud.commandframework",
            "net.kyori.adventure.text.minimessage",
            "net.kyori.adventure.serializer.configurate4",
            "com.github.stefvanschie.inventoryframework",
            "com.github.aecsocket.minecommons",
            "com.github.aecsocket.sokol",
            "org.bstats"
        ).forEach { relocate(it, "${rootProject.group}.${rootProject.name}.lib.$it") }
    }

    assemble {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.forUseAtConfigurationTime().get())
    }
}

bukkit {
    name = "Calibre"
    main = "${project.group}.${rootProject.name}.paper.CalibrePlugin"
    apiVersion = "1.18"
    depend = listOf("ProtocolLib")
    website = "https://github.com/aecsocket/calibre"
    authors = listOf("aecsocket")
}

publishing {
    publications {
        create<MavenPublication>("github") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/aecsocket/calibre")
            credentials {
                username = System.getenv("GPR_ACTOR")
                password = System.getenv("GPR_TOKEN")
            }
        }
    }
}
