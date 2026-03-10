/*
 * Copyright (c) 2025 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

plugins {
  id("ncj-java-base-conventions")
  alias(ncjLibs.plugins.spotless)
}

description = "The Unidata netCDF-Java library (aka CDM)."

// To upgrade gradle, update the version and expected checksum values below
// and run ./gradlew wrapper twice
tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
  gradleVersion = "9.4.0"
  distributionSha256Sum = "b21468753cb43c167738ee04f10c706c46459cf8f8ae6ea132dc9ce589a261f2"
}

spotless {
  // check all gradle build scripts (build-logic-ncj has its own formatting check)
  kotlinGradle {
    target("*.gradle.kts", "**/*.gradle.kts")
    targetExclude("build-logic-ncj/**/*")
    ktfmt().googleStyle()
  }
}

// Aggregate task for building all public artifacts
// Used to assemble the jars that should be scanned by OWASP Dependency Check
// job on Jenkins
tasks.register("buildPublicArtifacts") {
  group = "build"
  val publicArtifacts = project.extra.get("public.artifacts")
  if (publicArtifacts is List<*>) {
    dependsOn(publicArtifacts.map { ":$it:jar" })
  } else {
    logger.error(
      "Cannot access the list of public artifacts. The project-wide code coverage report will be incomplete!"
    )
  }
  dependsOn(":uber-jars:buildNetcdfAll", ":uber-jars:buildToolsUI", ":uber-jars:buildNcIdv")
}
