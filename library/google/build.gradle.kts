plugins {
    kotlin("jvm") version "2.0.20"
}

group = "com.ksidelta"
version = "unspecified"

dependencies {
    implementation(project(":library:utils"))
    implementation(project(":library:logger"))
    implementation(project(":library:store"))
    implementation(project(":library:session"))
    implementation(project(":library:http-client"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}