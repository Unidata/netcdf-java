plugins {
    id("cdm.library-conventions")
}

// Matches Maven's "project.description"
description = "The CDM core packages."

dependencies {
    implementation(platform(project(":netcdf-java-platform")))
    testImplementation(platform(project(":netcdf-java-testing-platform")))

    api(project(":udunits"))

    implementation(libs.autoValueAnnotations)
    implementation(libs.guava)
    implementation(libs.jcommander)
    implementation(libs.jdom2)
    implementation(libs.jsr305)
    implementation(libs.protobufJava)
    implementation(libs.re2j)
    implementation(libs.slf4j)

    annotationProcessor(libs.autoValue)

    testImplementation(project(":bufr"))
    testImplementation(project(":cdm-test-utils"))
    testImplementation(project(":grib"))

    testImplementation(libs.commonsIo)
    testImplementation(libs.groovyAll)
    testImplementation(libs.junit)
    testImplementation(libs.mockitoCore)
    testImplementation(libs.spockCore)
    testImplementation(libs.truth)
    testImplementation(libs.truthJava8Extension)

    testRuntimeOnly(libs.logbackClassic)

}

tasks.clean {
    // created during tests. Cleanup as part of clean task.
    delete(
        "src/test/data/compress/testBzip.nc",
        "src/test/data/compress/testCompress.nc",
        "src/test/data/compress/testGzip.nc",
        "src/test/data/compress/testZip.nc"
    )
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "ucar.nc2.writer.Ncdump",
            "Implementation-Title" to "CDM core library"
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