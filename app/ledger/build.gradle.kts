plugins {
    kotlin("jvm") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.ksidelta"

dependencies {
    implementation(project(":library:google"))
    implementation(project(":library:http-client"))
    implementation(project(":library:store"))
    implementation(project(":library:serialization"))
    implementation(project(":library:banking"))
    implementation(project(":library:mt940"))
    implementation(project(":library:memoize"))
    implementation(project(":library:logger"))
    implementation(project(":library:table"))
    implementation(project(":library:session"))
    implementation(project(":library:cache"))

    implementation("io.ktor:ktor-server-core:3.0.3")
    implementation("io.ktor:ktor-server-netty:3.0.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
