plugins {
    id("cdm.library-conventions")
    id("application")
    alias(libs.plugins.protobufPlugin)
    alias(libs.plugins.execforkPlugin)
}

import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.plugins
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

// Matches Maven's "project.description"
description = "gRPC client and server implementation of CDM Remote Procedure Calls (gCDM)."

dependencies {
    implementation(enforcedPlatform(project(":netcdf-java-platform")))
    implementation(enforcedPlatform(libs.grpcBom))
    testImplementation(enforcedPlatform(project(":netcdf-java-testing-platform")))

    implementation(project(":cdm-core"))

    implementation(libs.grpcProtobuf)
    implementation(libs.grpcStub)
    implementation(libs.guava)
    implementation(libs.protobufJava)
    implementation(libs.slf4j)

    compileOnly(libs.tomcatAnnotationsApi)

    runtimeOnly(project(":bufr"))
    runtimeOnly(project(":grib"))

    runtimeOnly(libs.grpcNettyShaded)
    runtimeOnly(libs.slf4jJdk14)


    testImplementation(project(":cdm-test-utils"))

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.commonsIo)
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "CDM Remote Procedure Calls"
        )
    }
}

application {
    mainClass.set("ucar.gcdm.server.GcdmServer")
}

val startDaemonTask = tasks.register<com.github.psxpaul.task.JavaExecFork>("startDaemon") {
    classpath = sourceSets.main.get().runtimeClasspath
    main = "ucar.gcdm.server.GcdmServer"
    jvmArgs = listOf("-Xmx512m", "-Djava.awt.headless=true")
    standardOutput = "$buildDir/gcdm_logs/gcdm.log"
    errorOutput = "$buildDir/gcdm_logs/gcdm-error.log"
    stopAfter = tasks.test.get()
    waitForPort = 16111
    waitForOutput = "Server started, listening on 16111"
}

tasks.test {
    dependsOn(startDaemonTask)
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:${libs.versions.protobuf.get()}"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${libs.versions.grpc.get()}"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}

// handle proto generated source and class files
sourceSets {
    main {
        java {
            srcDir("build/generated/source/proto/main/java")
            srcDir("build/generated/source/proto/main/grpc")
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