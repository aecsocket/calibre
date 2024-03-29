plugins {
    kotlin("jvm")
    id("maven-publish")
    id("org.jetbrains.dokka")
}

allprojects {
    group = "com.gitlab.aecsocket.calibre"
    version = "2.2.2"
    description = "Composable weapons framework"
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

subprojects {
    apply<JavaLibraryPlugin>()
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")

    tasks {
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            kotlinOptions {
                jvmTarget = JavaVersion.VERSION_17.toString()
            }
        }

        test {
            useJUnitPlatform()
        }

        processResources {
            filteringCharset = Charsets.UTF_8.name()
            filter { it
                .replace("@version@", project.version.toString())
                .replace("@description@", project.description.toString())
                .replace("@group@", project.group.toString())
            }
        }
    }

    publishing {
        repositories {
            maven {
                url = uri("${System.getenv("CI_API_V4_URL")}/projects/${System.getenv("CI_PROJECT_ID")}/packages/maven")
                credentials(HttpHeaderCredentials::class) {
                    name = "Job-Token"
                    value = System.getenv("CI_JOB_TOKEN")
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
            }
        }

        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
    }
}
