plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://gitlab.com/api/v4/groups/9631292/-/packages/maven")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(libs.adventureApi)
    compileOnly(libs.adventureExtraKotlin)
    compileOnly(libs.glossaCore)
    compileOnly(libs.configurateExtraKotlin)
    compileOnly(libs.cloudCore)
    compileOnly(libs.alexandriaCore)
    compileOnly(libs.packetEventsApi)
    compileOnly(libs.sokolCore)


    testImplementation(platform("org.junit:junit-bom:5.9.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
