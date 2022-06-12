plugins {
    kotlin("jvm")
    id("maven-publish")
    //id("org.jetbrains.dokka")
}

allprojects {
    group = "com.github.aecsocket.calibre"
    version = "2.1.2"
    description = "Platform-agnostic, modular gun framework"
}

repositories {
    mavenLocal()
    mavenCentral()
}

subprojects {
    apply<JavaLibraryPlugin>()
    apply(plugin = "maven-publish")

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
            }
        }
    }

    tasks {
        test {
            useJUnitPlatform()
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(18))
        }
    }
}
