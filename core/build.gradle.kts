plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://gitlab.com/api/v4/groups/9631292/-/packages/maven")
}

dependencies {
    compileOnly(libs.alexandriaCore)

    testImplementation(kotlin("test"))
}
