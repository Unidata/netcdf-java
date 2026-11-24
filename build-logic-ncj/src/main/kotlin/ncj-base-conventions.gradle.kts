/*
 * Copyright (c) 2025 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

plugins { base }

val catalogs = extensions.getByType<VersionCatalogsExtension>()

group = "edu.ucar"

version = catalogs.named("ncjLibs").findVersion("netcdf-java").get().requiredVersion

description = "A component to the Unidata netCDF-Java library (aka CDM)."

extra["project.isRelease"] = !version.toString().endsWith("SNAPSHOT")

extra["project.title"] = "CDM modules"

extra["project.vendor"] = "UCAR/Unidata"

extra["project.url"] = "https://www.unidata.ucar.edu/software/netcdf-java/"

val docVersionParts = version.toString().split("-")[0].split(".")

assert(docVersionParts.size == 3)

extra["docVersion"] = docVersionParts[0] + "." + docVersionParts[1]

// list of subprojects that are intended for public use and make up the netCDF-Java project
// this is used by the netcdf-java-bom, docs, and code-coverage-report
// subprojects
val publicArtifacts =
  listOf(
    ":bufr",
    ":cdm-core",
    ":cdm-image",
    ":cdm-mcidas",
    ":cdm-misc",
    ":cdm-radial",
    ":cdm-s3",
    ":cdm-vis5d",
    ":cdm-zarr",
    ":dap4",
    ":gcdm",
    ":grib",
    ":httpservices",
    ":legacy",
    ":libaec-jna",
    ":libaec-native",
    ":libblosc2-jna",
    ":libblosc2-native",
    ":libzstd-jna",
    ":netcdf4",
    ":opendap",
    ":udunits",
    ":waterml",
  )

project.extra["public.artifacts"] = publicArtifacts

// the minimumVersion of java supported
// will be the bytecode produced by the project for all java compilation
// will be used to run the tests (test, not testWithJdkX), generate code coverage reports, etc.
// other versions of java can be used to run the tests, but this is configured in
// ncj-testing-conventions.gradle.kts
project.extra["project.minimumJdkVersion"] = "8"
