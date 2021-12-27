plugins {
    id("cdm.library-conventions")
    alias(libs.plugins.protobuf)
}

// Matches Maven's "project.description"
description = "The CDM core packages."

dependencies {
    implementation(enforcedPlatform(project(":netcdf-java-platform")))
    testImplementation(enforcedPlatform(project(":netcdf-java-testing-platform")))

    api(project(":udunits"))

    implementation(libs.jcommander)
    implementation(libs.guava)
    implementation(libs.protobufJava)
    implementation(libs.re2j)
    implementation(libs.jdom2)
    implementation(libs.slf4j)
    implementation(libs.jsr305)
    implementation(libs.autoValueAnnotations)

    annotationProcessor(libs.autoValue)

    testImplementation(project(":cdm-test-utils"))
    //testImplementation project(':bufr')
    //testImplementation project(':grib')
    testImplementation(libs.truth)
    testImplementation(libs.truthJava8Extension)
    testImplementation(libs.commonsIo)
    testImplementation(libs.junit)
    testImplementation(libs.groovyAll)
    testImplementation(libs.mockitoCore)
    testImplementation(libs.spockCore)

    testRuntimeOnly(libs.logbackClassic)

}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "ucar.nc2.writer.Ncdump",
            "Implementation-Title" to "CDM core library"
        )
    }
    exclude("cdmrfeature.proto", "ncStream.proto", "pointStream.proto")
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