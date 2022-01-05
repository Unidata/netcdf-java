import java.nio.file.Files
import java.nio.file.Paths
import java.security.DigestInputStream
import java.security.MessageDigest

import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

// bug in IntelliJ in which `libs` shows up as not being accessible
// see https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION")

plugins {
    id("cdm.java-conventions")
    alias(libs.plugins.protobufPlugin)
    application
    alias(libs.plugins.shadowPlugin)
    alias(libs.plugins.nexusPlugin)
}

description = "Provides a graphical interface to the CDM library."
// the location where the toolsUI fatJar and toolsUI distribution zip file will be stored
// everything in this directory will also be published to the netcdf-java downloads raw
// repo on the unidata nexus server.
val toolsUIArtifactDir = rootProject.buildDir.resolve("toolsUI")

dependencies {
    implementation(platform(project(":netcdf-java-platform")))
    testImplementation(platform(project(":netcdf-java-testing-platform")))

    implementation(project(":cdm-core"))
    implementation(project(":bufr"))
    implementation(project(":grib"))
    implementation(project(":toolsui:uibase"))
    implementation(project(":udunits"))

    implementation(libs.bounce)
    implementation(libs.guava)
    implementation(libs.jdom2)
    implementation(libs.jfreechart)
    implementation(libs.jgoodies)
    implementation(libs.lgooddatepicker)
    implementation(libs.protobufJava)
    implementation(libs.re2j)
    implementation(libs.slf4j)

    runtimeOnly(project(":cdm-s3"))
    runtimeOnly(project(":netcdf4"))

    runtimeOnly(libs.logbackClassic)

    testImplementation(libs.junit)

    testRuntimeOnly(project(":cdm-test-utils"))
}

tasks.test {
    // Tell java to use ucar.util.prefs.PreferencesExtFactory to generate preference objects
    // Important for ucar.util.prefs.TestJavaUtilPreferences
    systemProperties(
        Pair("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory")
    )
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "ToolsUI"
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

application {
    applicationName = "toolsUI"
    mainClass.set("ucar.ui.ToolsUI")
}

tasks.startScripts {
    applicationName = "toolsUI"
}

tasks.distZip {
    archiveBaseName.set("toolsUI")
    destinationDirectory.set(toolsUIArtifactDir)
}

tasks.shadowJar {
    archiveBaseName.set("toolsUI")
    archiveClassifier.set("")
    destinationDirectory.set(toolsUIArtifactDir)
    manifest {
        attributes(
            "Implementation-Title" to "ToolsUI",
            "Main-Class" to "ucar.ui.ToolsUI"
        )
    }
    exclude(
        "AUTHORS",
        "DATE",
        "LICENCE",
        "LICENSE",
        "NOTICE",
        "*.txt",
        "META-INF/INDEX.LIST",
        "META-INF/DEPENDENCIES",
        "META-INF/LICENSE",
        "META-INF/NOTICE",
        "META-INF/*.SF",
        "META-INF/*.DSA",
        "META-INF/*.RSA",
        "META-INF/*.txt",
        "META-INF/*.xml",
    )
    // Transformations
    append("META-INF/spring.handlers")
    append("META-INF/spring.schemas")
    mergeServiceFiles()
}

fun writeChecksums() {
    val files = fileTree(toolsUIArtifactDir).matching {
        include("**/*.jar", "**/*.zip")
    }
    listOf("MD5", "SHA-1", "SHA-256", "SHA-512").forEach { algorithm ->
        val md = MessageDigest.getInstance(algorithm)
        val buffer = ByteArray(2048)
        files.forEach { sourceFile ->
            Files.newInputStream(Paths.get(sourceFile.absolutePath)).use { inputStream ->
                DigestInputStream(inputStream, md).use { dis ->
                    while (dis.read(buffer) != -1) {
                        // just need to read inputStream through the DigestInputStream to get
                        // the message digest
                    }
                }
            }
            val ext = algorithm.toLowerCase().replace("-", "")
            val outputFilename = toolsUIArtifactDir.resolve("${sourceFile.name}.${ext}").toString()
            File(outputFilename).writeText(
                buildString {
                    for (digestByte in md.digest()) {
                        append(
                             ((digestByte.toInt()).and(0xff) + 0x100).toString(16).substring(1)
                        )
                    }
                }
            )
        }
    }
}

val checksumTask = tasks.register("createChecksums") {
    dependsOn(tasks.distZip)
    dependsOn(tasks.shadowJar)
    doLast {
        writeChecksums()
    }
}

tasks.publishToRawRepo {
    group = "publishing"
    description = "Publish toolsUI artifacts to Nexus downloads under /version/."
    host = "https://artifacts.unidata.ucar.edu/"
    repoName = "downloads-netcdf-java"

    publishSrc = toolsUIArtifactDir.toString()
    destPath = "$version".split('-')[0]
    username = project.properties["nexus.username"].toString()
    password = project.properties["nexus.password"].toString()
    dependsOn(checksumTask)
}

tasks.register("publish") {
    group = "publishing"
    dependsOn(tasks.publishToRawRepo)
}

// disable unused tasks
tasks.distTar {
    enabled = false
}

tasks.shadowDistTar {
    enabled = false
}

tasks.shadowDistZip {
    enabled = false
}

tasks.startShadowScripts {
    enabled = false
}

tasks.runShadow {
    enabled = false
}