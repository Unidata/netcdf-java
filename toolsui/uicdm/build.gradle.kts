plugins {
    id("cdm.java-conventions")
    alias(libs.plugins.protobufPlugin)
    application
}

import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

description = "Provides a graphical interface to the CDM library."

dependencies {
    implementation(enforcedPlatform(project(":netcdf-java-platform")))
    testImplementation(enforcedPlatform(project(":netcdf-java-testing-platform")))

    implementation(project(":cdm-core"))
    implementation(project(":bufr"))
    implementation(project(":grib"))
    implementation(project(":toolsui:uibase"))

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

//tasks.test {
    // Tell java to use ucar.util.prefs.PreferencesExtFactory to generate preference objects
    // Important for ucar.util.prefs.TestJavaUtilPreferences
    //systemProperties(
    //    Pair("java.util.prefs.PreferencesFactory", "ucar.util.prefs.PreferencesExtFactory")
    //)
//}

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
java.sourceSets["main"].java

tasks.jacocoTestReport {
    val sourceFileTree: ConfigurableFileTree = fileTree(sourceDirectories.files)
    val classFileTree: ConfigurableFileTree = fileTree(sourceDirectories.files)
    // Exclude proto generated sources and classes from the coverage reports
    sourceDirectories.setFrom(sourceFileTree.exclude("**/*Proto.java"))
    classDirectories.setFrom(classFileTree.exclude("**/*Proto.class", "**/*Proto$*.class"))
}

application {
    mainClass.set("ucar.ui.ToolsUI")
}
