/*
 * This file was generated by the Gradle 'init' task.
 *
 * This is a general purpose Gradle build.
 * Learn more about Gradle by exploring our Samples at https://docs.gradle.org/8.10/samples
 */

subprojects {
    tasks.register("prepareKotlinBuildScriptModel")
}

allprojects {
    repositories {
        mavenCentral()
    }
}

ext {
    set("ktor.version", "3.0.3")
}