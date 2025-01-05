plugins {
    kotlin("jvm") version "2.0.20"
}

group = "com.ksidelta"

dependencies {
    implementation(project(":library:google"))
    implementation(project(":library:http-client"))

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
