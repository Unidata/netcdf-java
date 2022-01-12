plugins {
    id("cdm.java-conventions")
}

description = "UI elements that are independent of the CDM"

dependencies {
    implementation(platform(project(":netcdf-java-platform")))
    testImplementation(platform(project(":netcdf-java-testing-platform")))

    implementation(libs.autoValueAnnotations)
    implementation(libs.guava)
    implementation(libs.jdom2)
    implementation(libs.jgoodies)
    implementation(libs.jsr305)
    implementation(libs.slf4j)

    annotationProcessor(libs.autoValue)

    testImplementation(project(":cdm-core"))

    testImplementation(libs.junit)
    testImplementation(libs.mockitoCore)
    testImplementation(libs.truth)

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
            "Implementation-Title" to "UI base library"
        )
    }
}

tasks.jacocoTestReport {
    val sourceFileTree: ConfigurableFileTree = fileTree(sourceDirectories.files)
    val classFileTree: ConfigurableFileTree = fileTree(sourceDirectories.files)
    // Exclude AutoValue generated sources and classes from the coverage reports
    sourceDirectories.setFrom(sourceFileTree.exclude("**/AutoValue_*.java"))
    classDirectories.setFrom(classFileTree.exclude("**/AutoValue_*.class", "**/AutoValue_*$*.class"))
}