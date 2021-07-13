plugins {
    id("maven-publish")
}

dependencies {
    compileOnly("com.gitlab.aecsocket.minecommons", "core", "1.2") { isTransitive = true }
    compileOnly("com.gitlab.aecsocket.sokol", "core", "1.2") { isTransitive = true }
}

tasks {
    javadoc {
        val opt = options as StandardJavadocDocletOptions
        opt.links(
                "https://docs.oracle.com/en/java/javase/16/docs/api/",
                "https://aecsocket.gitlab.io/minecommons/javadoc/core/",
                "https://aecsocket.gitlab.io/sokol/javadoc/core/"
        )
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
