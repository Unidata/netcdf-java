repositories {
    gradlePluginPortal()
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
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}
