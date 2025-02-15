plugins {
    kotlin("jvm") version "2.0.20"
    `java-library`
}

group = "com.ksidelta"

dependencies {
    implementation(project(":library:http-client"))
    implementation(project(":library:logger"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}