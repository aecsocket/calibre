plugins {
    kotlin("jvm")
    id("maven-publish")
    //id("org.jetbrains.dokka")
}

allprojects {
    group = "com.github.aecsocket.calibre"
    version = "2.1.1"
    description = "Platform-agnostic, modular gun framework"
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
}
