plugins {
    kotlin("jvm") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.ksidelta"

dependencies {
    implementation(project(":library:store"))
    implementation(project(":library:books"))
    implementation(project(":library:http-client"))
    implementation(project(":library:serialization"))
    implementation(project(":library:google"))
    implementation(project(":library:logger"))
    implementation(project(":library:email"))
    implementation(project(":library:session"))

    implementation("io.ktor:ktor-server-content-negotiation:3.0.3")
    implementation("io.ktor:ktor-serialization-jackson:3.0.3")
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

tasks.named("shadowJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
    manifest {
        attributes(mapOf(Pair("Main-Class", "com.ksidelta.app.libruch.Main")))
    }
}