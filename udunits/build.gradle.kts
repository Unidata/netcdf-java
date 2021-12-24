plugins {
    id("cdm.library-conventions")
}

// Matches Maven's "project.description"
description = "The ucar.units Java package for the Unidata formatted unit specifications."

dependencies {
    implementation(enforcedPlatform(project(":netcdf-java-platform")))
    testImplementation(enforcedPlatform(project(":netcdf-java-testing-platform")))

    implementation(libs.jsr305)

    testImplementation(libs.junit)
    testImplementation(libs.slf4j)
    testImplementation(libs.truth)

    testRuntimeOnly(project(":cdm-test-utils"))

    testRuntimeOnly(libs.logbackClassic)
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "UDUNITS",
            "Implementation-URL" to "https://www.unidata.ucar.edu/software/udunits/",
        )
    }
}
