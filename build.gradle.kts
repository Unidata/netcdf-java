/*
 * Copyright (c) 2025-2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

plugins {
  id("ncj-java-base-conventions")
  alias(ncjLibs.plugins.spotless)
  id("ncj-versions-conventions")
}

description = "The Unidata netCDF-Java library (aka CDM)."

// To upgrade gradle, update the version and expected checksum values below
// and run ./gradlew wrapper twice
tasks.wrapper {
  distributionType = Wrapper.DistributionType.ALL
  gradleVersion = "9.5.0"
  distributionSha256Sum = "a3c4ba4aca8f0075688b9c5b18939fd28e8cb4357c227da5c1d9f38343791439"
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

// we need some things from ncj-java-base-conventions for the root project,
// but not because we are making a jar
tasks.jar { enabled = false }
