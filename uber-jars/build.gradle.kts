/*
 * Copyright (c) 2025 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.security.MessageDigest
import org.cyclonedx.gradle.CyclonedxDirectTask

plugins {
  id("ncj-java-base-conventions")
  alias(ncjLibs.plugins.shadow).apply(false)
  alias(ncjLibs.plugins.cyclonedx.bom).apply(false)
}

description =
  "Manage the creation of uber-jars (netCDFAll, ncIdv, and toolsUI) and their Software Bill of Materials (SBOMs)."

extra["project.title"] = "Uber-jars and SBOMs"

// output location for generated artifacts
val artifactOutputLocation = rootProject.layout.buildDirectory.dir("distributions")

// remove generated artifacts when running clean
tasks.clean { delete(artifactOutputLocation) }

// configurations for the uber-jars
val toolsUI by configurations.creating { extendsFrom(configurations.implementation.get()) }

val ncIdv by configurations.creating { extendsFrom(configurations.implementation.get()) }

val netcdfAll by configurations.creating { extendsFrom(configurations.implementation.get()) }

dependencies {
  implementation(platform(project(":netcdf-java-platform")))

  // common to all uber-jars
  implementation(project(":bufr"))
  implementation(project(":dap4"))
  implementation(project(":cdm-core"))
  implementation(project(":cdm-image"))
  implementation(project(":cdm-mcidas"))
  implementation(project(":cdm-misc"))
  implementation(project(":cdm-radial"))
  implementation(project(":grib"))
  implementation(project(":opendap"))
  implementation(project(":httpservices"))

  // ncIdv specific
  ncIdv(project(":cdm-vis5d"))
  ncIdv(project(":legacy"))
  ncIdv(project(":libaec-native"))

  // netcdfAll specific
  netcdfAll(project(":netcdf4"))

  // :uicdm is toolsUI
  toolsUI(project(":uicdm"))
}

// do not build a jar for the subproject
tasks.jar { enabled = false }

/////////////////////////
// uber-jar generation //
/////////////////////////

val buildToolsUi =
  tasks.register<ShadowJar>("buildToolsUI") {
    archiveBaseName = "toolsUI"
    configurations = listOf(toolsUI)

    doFirst { manifest.attributes(project(":uicdm").tasks.jar.get().manifest.attributes) }
  }

val buildNcIdv =
  tasks.register<ShadowJar>("buildNcIdv") {
    archiveBaseName = "ncIdv"
    configurations = listOf(ncIdv)

    exclude("edu/wisc/**")
    exclude("nom/**")
    exclude("visad/**")

    doFirst { manifest.attributes["Implementation-Title"] = "ncIdv jar" }
  }

val buildNetcdfAll =
  tasks.register<ShadowJar>("buildNetcdfAll") {
    archiveBaseName = "netcdfAll"
    configurations = listOf(netcdfAll)

    doFirst { manifest.attributes(project(":cdm-core").tasks.jar.get().manifest.attributes) }
  }

// common configuration for all uber-jars
val uberJarTasks = listOf(buildNcIdv, buildNetcdfAll, buildToolsUi)

uberJarTasks.forEach {
  it {
    archiveClassifier = ""
    // Transformations
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    transform<com.github.jengelman.gradle.plugins.shadow.transformers.AppendingTransformer> {
      resource = "project.properties"
    }
    transform<
      com.github.jengelman.gradle.plugins.shadow.transformers.ApacheLicenseResourceTransformer
    >()
    transform<
      com.github.jengelman.gradle.plugins.shadow.transformers.ApacheNoticeResourceTransformer
    >()
    mergeServiceFiles()
    filesMatching("logback.xml") {
      duplicatesStrategy = DuplicatesStrategy.FAIL // Or WARN.
    }
    destinationDirectory = artifactOutputLocation
    from(rootDir.absolutePath) {
      include("LICENSE")
      into("META-INF/unidata-license")
    }
  }
}

/////////////////////
// SBOM generation //
/////////////////////

val netcdfAllSbom =
  tasks.register<CyclonedxDirectTask>("netcdfAllSbom") {
    componentName = "netcdfAll"
    group = "build"
    includeConfigs = listOf("netcdfAll")
    xmlOutput = artifactOutputLocation.get().file("netcdfAll-${project.version}-sbom.xml")
    jsonOutput = artifactOutputLocation.get().file("netcdfAll-${project.version}-sbom.json")
    dependsOn(buildNetcdfAll)
  }

val ncIdvSbom =
  tasks.register<CyclonedxDirectTask>("ncIdvSbom") {
    componentName = "ncIdv"
    group = "build"
    includeConfigs = listOf("ncIdv")
    xmlOutput = artifactOutputLocation.get().file("ncIdv-${project.version}-sbom.xml")
    jsonOutput = artifactOutputLocation.get().file("ncIdv-${project.version}-sbom.json")
    dependsOn(buildNcIdv)
  }

val toolsUISbom =
  tasks.register<CyclonedxDirectTask>("toolsUISbom") {
    componentName = "toolsUI"
    group = "build"
    includeConfigs = listOf("toolsUI")
    xmlOutput = artifactOutputLocation.get().file("toolsUI-${project.version}-sbom.xml")
    jsonOutput = artifactOutputLocation.get().file("toolsUI-${project.version}-sbom.json")
    dependsOn(buildToolsUi)
  }

val buildSboms = tasks.register("buildSboms") { dependsOn(netcdfAllSbom, ncIdvSbom, toolsUISbom) }

//////////////////////////////////
// Artifact Checksum generation //
//////////////////////////////////

val createChecksums =
  tasks.register("createChecksums") {
    group = "build"
    description = "Create .sha1, .sha256, and .md5 checksum files for the uber-jars."

    doLast {
      listOf("MD5", "SHA-1", "SHA-256").forEach { algorithm ->
        fileTree(artifactOutputLocation) { include("*.jar", "*-sbom.json", "*-sbom.xml") }
          .forEach { jarFile ->
            MessageDigest.getInstance(algorithm)
              .let { md ->
                md.digest(file(jarFile).readBytes()).let {
                  BigInteger(1, it).toString(16).padStart(md.digestLength * 2, '0')
                }
              }
              .let { checksum ->
                artifactOutputLocation
                  .get()
                  .asFile
                  .resolve("${jarFile.name}.${algorithm.lowercase().replace("-", "")}")
                  .writeText(checksum)
              }
          }
      }
    }
    dependsOn(uberJarTasks, buildSboms)
  }

// aggregate tasks
tasks.assemble { dependsOn(uberJarTasks, buildSboms, createChecksums) }
