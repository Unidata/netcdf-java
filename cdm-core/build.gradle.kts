plugins {
    id("cdm.library-conventions")
}

// Matches Maven's "project.description"
description = "The CDM core packages."

dependencies {
    implementation(enforcedPlatform(project(":netcdf-java-platform")))
    testImplementation(enforcedPlatform(project(":netcdf-java-testing-platform")))

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

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "ucar.nc2.writer.Ncdump",
            "Implementation-Title" to "CDM core library"
        )
    }
}