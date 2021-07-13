plugins {
    id("java-library")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("xyz.jpenilla.run-paper") version "1.0.3"
}

val minecraftVersion = "1.17"

repositories {
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.dmulloy2.net/nexus/repository/public/")
}

dependencies {
    api(project(":core"))
    compileOnly("io.papermc.paper", "paper-api", "1.17.1-R0.1-SNAPSHOT") {
        exclude("junit", "junit")
    }

    implementation("org.bstats", "bstats-bukkit", "2.2.1")

    // Plugins
    compileOnly("com.gitlab.aecsocket.minecommons", "paper", "1.2")
    compileOnly("com.gitlab.aecsocket.sokol", "paper", "1.2")
    compileOnly("com.comphenix.protocol", "ProtocolLib", "4.7.0")
}

tasks {
    javadoc {
        val opt = options as StandardJavadocDocletOptions
        opt.links(
                "https://docs.oracle.com/en/java/javase/16/docs/api/",
                "https://aecsocket.gitlab.io/minecommons/javadoc/core/",
                "https://aecsocket.gitlab.io/minecommons/javadoc/paper/",
                "https://aecsocket.gitlab.io/sokol/javadoc/core/",
                "https://aecsocket.gitlab.io/paper/javadoc/core/",
                "https://configurate.aoeu.xyz/4.1.1/apidocs/",
                "https://jd.adventure.kyori.net/api/4.8.1/",
                "https://papermc.io/javadocs/paper/1.17/",
                "https://javadoc.commandframework.cloud/",
                "https://aadnk.github.io/ProtocolLib/Javadoc/"
        )
    }

    shadowJar {
        archiveFileName.set("${rootProject.name}-${project.name}-${rootProject.version}.jar")
        listOf(
                "org.bstats"
        ).forEach { relocate(it, "${rootProject.group}.lib.$it") }
    }

    assemble {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion(minecraftVersion)
    }
}

publishing {
    publications {
        create<MavenPublication>("gitlab") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            url = uri("https://gitlab.com/api/v4/projects/20514863/packages/maven")
            credentials(HttpHeaderCredentials::class) {
                name = "Job-Token"
                value = System.getenv("CI_JOB_TOKEN")
            }
            authentication {
                create<HttpHeaderAuthentication>("header")
            }
        }
    }
}
