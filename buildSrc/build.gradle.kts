repositories {
    gradlePluginPortal()
}
// libs.versions will show an error in IntelliJ, but it does not impact the ability to import
// or build the project (false positive)
// https://youtrack.jetbrains.com/issue/KTIJ-19370
dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:${libs.versions.spotlessPlugin.get()}")
}

plugins {
    `kotlin-dsl`
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}
