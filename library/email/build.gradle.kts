plugins {
    kotlin("jvm") version "2.0.20"
    `java-library`
}

group = "com.ksidelta"

dependencies {
    implementation("jakarta.mail:jakarta.mail-api:2.0.3")
    implementation("org.eclipse.angus:angus-mail:2.0.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}