// bug in IntelliJ in which `libs` shows up as not being accessible
// see https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION")

plugins {
    base
    alias(libs.plugins.owaspDepCheckPlugin)
}

subprojects {
    // See buildSrc/src/main/kotlin/cdm.java-conventions.gradle.kts and
    // buildSrc/src/main/kotlin/cdm.library-conventions.gradle.kts for
    // more details on how cdm projects are built.
    project.version = "8.0.0-SNAPSHOT"
    project.group = "edu.ucar"
}

tasks.wrapper {
    gradleVersion = "7.3.3"
    distributionSha256Sum = "c9490e938b221daf0094982288e4038deed954a3f12fb54cbf270ddf4e37d879"
    distributionType = Wrapper.DistributionType.ALL
}

dependencyCheck {
    analyzers.retirejs.enabled = false
    analyzers.assemblyEnabled = false
    data.setDirectory("$rootDir/project-files/owasp-dependency-check/nvd")
    scanConfigurations = listOf("compileClasspath", "runtimeClasspath")
    suppressionFile = "$rootDir/project-files/owasp-dependency-check/dependency-check-suppression.xml"
    // fail the build if any vulnerable dependencies are identified (any CVSS score > 0).
    failBuildOnCVSS = 0F
}

// shortcut to run toolsUI
tasks.register("toolsui") {
    group = "application"
    dependsOn(":toolsui:uicdm:run")
}