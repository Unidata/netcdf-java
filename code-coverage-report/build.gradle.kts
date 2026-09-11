/*
 * Copyright (c) 2025 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

description =
  "Generate a project-wide code coverage report for the public artifacts of netCDF-Java."

extra["project.title"] = "Project-wide code coverage report"

plugins {
  id("ncj-java-base-conventions")
  alias(ncjLibs.plugins.shadow).apply(false)
  alias(ncjLibs.plugins.cyclonedx.bom).apply(false)
  id("jacoco-report-aggregation").apply(true)
}

dependencies {
  val publicArtifacts = project.extra.get("public.artifacts")
  if (publicArtifacts is List<*>) {
    publicArtifacts.forEach { jacocoAggregation(project(it.toString())) }
    jacocoAggregation(project(":cdm-test"))
  } else {
    logger.error(
      "Cannot access the list of public artifacts. The project-wide code coverage report will be incomplete!"
    )
  }
}

reporting {
  reports {
    create<JacocoCoverageReport>("testCodeCoverageReportAgg") {
      testSuiteName = "test"
    }
  }
}
