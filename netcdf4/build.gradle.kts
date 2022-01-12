plugins {
    id("cdm.library-conventions")
}

// Matches Maven's "project.description"
description = "Reads and writes NetCDF by loading the NetCDF C library and accessing it through JNA."

dependencies {
    implementation(platform(project(":netcdf-java-platform")))
    testImplementation(platform(project(":netcdf-java-testing-platform")))

    api(project(":cdm-core"))

    implementation(libs.guava)
    implementation(libs.jna)
    implementation(libs.jsr305)
    implementation(libs.slf4j)

    testImplementation(project(":cdm-test-utils"))

    testImplementation(libs.truth)
    testImplementation(libs.groovyAll)
    implementation(libs.protobufJava)
    testImplementation(libs.spockCore)
    testRuntimeOnly(libs.logbackClassic)
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "NetCDF-4 IOSP.",
            "Implementation-URL" to "https://www.unidata.ucar.edu/software/netcdf/"
        )
    }
}
