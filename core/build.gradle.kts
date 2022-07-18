plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://gitlab.com/api/v4/groups/9631292/-/packages/maven")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    compileOnly(libs.glossaCore)
    compileOnly(libs.cloudCore)
    compileOnly(libs.adventureApi)
    compileOnly(libs.configurateExtraKotlin)
    compileOnly(libs.adventureExtraKotlin)
    compileOnly(libs.alexandriaCore)

    compileOnly(libs.sokolCore)

    testImplementation(kotlin("test"))
}
