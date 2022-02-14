plugins {
    id("java")
    id("maven-publish")
}

dependencies {
    implementation(libs.minecommons)
    implementation(libs.sokol)
    compileOnly(libs.findBugs)

    testImplementation(libs.bundles.junit)
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
