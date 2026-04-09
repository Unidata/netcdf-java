/*
 * Copyright (c) 2025-2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

plugins {
  id("ncj-java-library-conventions")
  id("ncj-protobuf-conventions")
}

description = "Decoder for the GRIB 1 and 2 formats."

project.extra["project.title"] = "GRIB 1 and 2 IOSPs and Feature Collections"

dependencies {
  implementation(platform(project(":netcdf-java-platform")))

  api(project(":cdm-core"))

  implementation(project(":libaec-jna"))

  implementation(ncjLibs.beust.jcommander)
  implementation(ncjLibs.findbugs.jsr305)
  implementation(ncjLibs.guava)
  implementation(ncjLibs.jdom2)
  implementation(ncjLibs.jj2000)
  implementation(ncjLibs.jna)
  implementation(ncjLibs.protobuf)
  implementation(ncjLibs.re2j)
  implementation(ncjLibs.slf4j.api)

  testImplementation(platform(project(":netcdf-java-testing-platform")))

  testImplementation(project(":cdm-test-utils"))
  testImplementation(project(":udunits"))

  testImplementation(ncjLibs.google.truth)
  testImplementation(ncjLibs.jsoup)

  testCompileOnly(ncjLibs.junit4)

  testRuntimeOnly(project(":cdm-s3"))
  testRuntimeOnly(project(":libaec-native"))

  testRuntimeOnly(ncjLibs.junit5.platformLauncher)
  testRuntimeOnly(ncjLibs.junit5.vintageEngine)
  testRuntimeOnly(ncjLibs.logback.classic)
}
