plugins {
    id("cdm.library-conventions")
}

// Matches Maven's "project.description"
description = "A collection of reusable classes to be used internally for testing across the various THREDDS projects."

dependencies {
    implementation(platform(project(":netcdf-java-platform")))
    implementation(platform(project(":netcdf-java-testing-platform")))

    api(project(":cdm-core"))

    implementation(libs.jdom2)
    implementation(libs.junit)
    implementation(libs.re2j)
    implementation(libs.slf4j)
    implementation(libs.truth)

    testRuntimeOnly(libs.logbackClassic)
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "NetCDF-Java testing utilities"
        )
    }
}