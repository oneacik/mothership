plugins {
    kotlin("jvm") version "2.0.20"
    `java-library`
}

group = "com.ksidelta"

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("reflect"))
    implementation(project(":library:logger"))

    implementation("io.ktor:ktor-client-core:${rootProject.ext.get("ktor.version")}")
    implementation("io.ktor:ktor-client-cio:${rootProject.ext.get("ktor.version")}")
    implementation("io.ktor:ktor-client-content-negotiation:${rootProject.ext.get("ktor.version")}")
    implementation("io.ktor:ktor-serialization-jackson:${rootProject.ext.get("ktor.version")}")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}