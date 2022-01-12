plugins {
    id("cdm.java-conventions")
}

description = "Classes for CDM unit and integration testing. Relies on having access to " +
        "cdmUnitTest files, which can be obtained at https://github.com/unidata/thredds-test-data."

dependencies {
    testImplementation(platform(project(":netcdf-java-platform")))
    testImplementation(platform(project(":netcdf-java-testing-platform")))

    testImplementation(project(":cdm-core"))
    testImplementation(project(":cdm-s3"))
    testImplementation(project(":bufr"))
    testImplementation(project(":grib"))
    testImplementation(project(":netcdf4"))
    testImplementation(project(":cdm-test-utils"))

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.truthJava8Extension)
    testImplementation(libs.re2j)
    testImplementation(libs.slf4j)
    testImplementation(libs.commonsIo)
    testImplementation(libs.jdom2)
    testImplementation(libs.awsS3Sdk)

    testRuntimeOnly(libs.logbackClassic)
}

val testIndexCreationTask = task<Test>("testIndexCreation") {
    description = "Create index files."
    group = "verification"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath

    filter {
        includeTestsMatching("ucar.nc2.grib.TestGribIndexCreation")
    }

    systemProperties(
        Pair("unidata.testdata.path", System.getProperty("unidata.testdata.path"))
    )
}

tasks.extendedTests {
    dependsOn("testIndexCreation")
    filter {
        excludeTestsMatching("ucar.nc2.grib.TestGribIndexCreation")
    }
}


