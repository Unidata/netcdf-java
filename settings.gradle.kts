/*
 * Copyright (c) 2025 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

dependencyResolutionManagement {
  repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
  repositories {
    mavenCentral()
    // be very specific about where non-maven-central-based artifacts are sourced
    exclusiveContent {
      forRepository {
        maven { url = uri("https://artifacts.unidata.ucar.edu/repository/unidata-3rdparty/") }
      }
      filter { includeModule("org.bounce", "bounce") }
    }
    exclusiveContent {
      forRepository {
        maven { url = uri("https://artifacts.unidata.ucar.edu/repository/unidata-releases/") }
      }
      filter {
        includeModule("edu.ucar", "jj2000")
        includeModule("edu.wisc.ssec", "visad-mcidas-slim-ucar-ns")
        includeModule("edu.wisc.ssec", "visad")
      }
    }
  }
  versionCatalogs { create("ncjLibs") { from(files("gradle/ncj.libs.versions.toml")) } }
}

includeBuild("build-logic-ncj")

rootProject.name = "netcdf-java"

//
// platforms used by all
//
include("netcdf-java-platform")

include("netcdf-java-testing-platform")

//
// no subproject dependencies
//
include(":libaec-native")

project(":libaec-native").projectDir = file("native-compression/libaec-native")

include(":libblosc2-native")

project(":libblosc2-native").projectDir = file("native-compression/libblosc2-native")

include(":libzstd-native")

project(":libzstd-native").projectDir = file("native-compression/libzstd-native")

//
// critical subprojects
// tricky main/test interdependencies...not circular, however
//
include("httpservices") // needed by cdm-test-utils main, cdm-core main

include("udunits") // needed by cdm-test-utils main, cdm-core main

include(":cdm-core")

project(":cdm-core").projectDir = file("cdm/core")

include("cdm-test-utils") // needed by many for test, its main depends on cdm:core, httpservices

//
// these subprojects depend on one or more of the critical subprojects
// but otherwise stand alone
//
include("bufr")

include(":cdm-image")

project(":cdm-image").projectDir = file("cdm/image")

include("dap4")

include(":libaec-jna")

project(":libaec-jna").projectDir = file("native-compression/libaec-jna")

include(":libblosc2-jna")

project(":libblosc2-jna").projectDir = file("native-compression/libblosc2-jna")

include(":libzstd-jna")

project(":libzstd-jna").projectDir = file("native-compression/libzstd-jna")

include("netcdf4")

include("opendap")

include("uibase")

include("waterml")

//
// branch of grib dependent subprojects
//
include("grib") // main needs :native-compression:libaec-jna

include("cdm-image") // runtime depends on grib

project(":cdm-image").projectDir = file("cdm/image")

include("cdm-misc") // test depends on grib

project(":cdm-misc").projectDir = file("cdm/misc")

include(":gcdm") // runtime depends on grib

project(":gcdm").projectDir = file("cdm/gcdm")

include("legacy") // main depends on grib

include("cdm-mcidas") // main depends on grib

project(":cdm-mcidas").projectDir = file("visad/mcidas")

include("cdm-vis5d") // main depends on visad:mcidas

project(":cdm-vis5d").projectDir = file("visad/vis5d")

//
// branch of cdm-radial dependent subprojects
//
include("cdm-radial") // test depends on netcdf4

project(":cdm-radial").projectDir = file("cdm/radial")

include("cdm-s3") // test depends on cdm:radial

project(":cdm-s3").projectDir = file("cdm/s3")

include("cdm-zarr") // test depends on cdm:s3

project(":cdm-zarr").projectDir = file("cdm/zarr")

include("docs") // test depends on cdm:s3, grib

//
// many subproject dependencies
//
include(":uicdm")

include(":cdm-test")

include(":uber-jars")

include("code-coverage-report")

include("netcdf-java-bom")
