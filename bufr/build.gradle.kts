import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

// bug in IntelliJ in which `libs` shows up as not being accessible
// see https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION")

plugins {
    id("cdm.library-conventions")
    alias(libs.plugins.protobufPlugin)
}

// Matches Maven's "project.description"
description = "Decoder for BUFR files."

dependencies {
    implementation(platform(project(":netcdf-java-platform")))
    testImplementation(platform(project(":netcdf-java-testing-platform")))

    api(project(":cdm-core"))

    implementation(libs.guava)
    implementation(libs.jcommander)
    implementation(libs.jdom2)
    implementation(libs.jsr305)
    implementation(libs.protobufJava)
    implementation(libs.re2j)
    implementation(libs.slf4j)

    testImplementation(project(":cdm-test-utils"))

    testImplementation(libs.junit)
    testImplementation(libs.truth)

    testRuntimeOnly(libs.logbackClassic)
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "BUFR IOSP"
        )
    }
}


protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
}

// handle proto generated source and class files
sourceSets {
    main {
        java {
            srcDir("build/generated/source/proto/main/java")
        }
    }
}

tasks.jacocoTestReport {
    val sourceFileTree: ConfigurableFileTree = fileTree(sourceDirectories.files)
    val classFileTree: ConfigurableFileTree = fileTree(sourceDirectories.files)
    // Exclude proto generated sources and classes from the coverage reports
    sourceDirectories.setFrom(sourceFileTree.exclude("**/*Proto.java"))
    classDirectories.setFrom(classFileTree.exclude("**/*Proto.class", "**/*Proto$*.class"))
}