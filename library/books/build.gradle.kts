plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    `java-library`
}

group = "com.ksidelta"

dependencies {
    implementation(project(":library:http-client"))
    implementation(project(":library:logger"))
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.18.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}