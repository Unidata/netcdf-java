plugins {
    id("cdm.library-conventions")
    alias(libs.plugins.protobufPlugin)
}

import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

// Matches Maven's "project.description"
description = "Decoder for GRIB 1 and 2 files."

dependencies {
    implementation(enforcedPlatform(project(":netcdf-java-platform")))
    testImplementation(enforcedPlatform(project(":netcdf-java-testing-platform")))

    api(project(":cdm-core"))

    implementation(libs.guava)
    implementation(libs.jcommander)
    implementation(libs.jdom2)
    implementation(libs.jj2000)
    implementation(libs.jsr305)
    implementation(libs.protobufJava)
    implementation(libs.re2j)
    implementation(libs.slf4j)

    testImplementation(project(":cdm-test-utils"))

    testImplementation(libs.jsoup)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.truthJava8Extension)

    testRuntimeOnly(libs.logbackClassic)
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "GRIB 1 and 2 IOSPs and GRIB collections."
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
