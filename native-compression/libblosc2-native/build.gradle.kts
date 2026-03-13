/*
 * Copyright (c) 2025 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

import java.net.URL
import java.security.DigestInputStream
import java.security.MessageDigest

plugins {
  id("ncj-java-base-conventions")
  id("ncj-artifact-publishing-conventions")
}

group = "edu.ucar.unidata"

var bloscVersion = "2.23.1"
var build = "0"

version = "${bloscVersion}.${build}"

description = "Jar distribution of native libraries for libblosc2 compression."

project.extra["project.title"] = "Native libraries for libblosc2."

// zip file produced by GitHub workflow
val libblosc2Native =
  "libblosc2-native-${bloscVersion}-c8d21a734aa58320135f5dfdc36e3d61e316e074.zip"

// sha256 checksum from GitHub workflow output
val expectedChecksum = "6cfb464e7bc666509644d3d419ec6d5a43831178cbf9105d2ecb2dbc97ffdf88"

val resourceZip = file("$rootDir/project-files/native/libblosc2/$libblosc2Native")
val fetchNativeResources =
  tasks.register("fetchNativeResources") {
    outputs.file(resourceZip)
    doLast {
      if (!resourceZip.exists()) {
        logger.info("Fetching native libblosc2 libraries.")
        var actualChecksum = ""
        val resourceUrl =
          "https://downloads.unidata.ucar.edu/netcdf-java/native/libblosc2/$libblosc2Native"
        URL(resourceUrl).openStream().use { ips ->
          val dips = DigestInputStream(ips, MessageDigest.getInstance("SHA-256"))
          resourceZip.outputStream().use { ops -> dips.copyTo(ops) }
          actualChecksum = dips.messageDigest.digest().toHexString()
        }
        if (actualChecksum != expectedChecksum) {
          throw RuntimeException(
            String.format(
              "Error: checksum on libblosc2.zip does not match expected value.\n" +
                "  Expected: %s\n  Actual: %s\n",
              expectedChecksum,
              actualChecksum,
            )
          )
        }
      }
    }
  }

val processNativeResources =
  tasks.register("processNativeResources", Copy::class) {
    inputs.file(resourceZip)
    from(zipTree(resourceZip))
    eachFile { relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray()) }
    destinationDir = layout.buildDirectory.dir("resources/main").get().asFile
    dependsOn(fetchNativeResources)
  }

tasks.processResources { dependsOn(processNativeResources) }

var publishTaskName = "nativeLibs"

publishing {
  // we only publish releases of the native jars
  repositories.clear()
  repositories {
    maven {
      name = "releases"
      url = uri("https://artifacts.unidata.ucar.edu/repository/unidata-releases/")
      credentials {
        username = extra.properties["artifacts.username"] as? String
        password = extra.properties["artifacts.password"] as? String
      }
    }
  }
  publications {
    create<MavenPublication>(publishTaskName) {
      from(components["java"])
      versionMapping {
        usage("java-api") { fromResolutionOf("runtimeClasspath") }
        usage("java-runtime") { fromResolutionResult() }
      }
    }
  }
}

tasks
  .matching { it.group == "publishing" }
  .forEach { it.enabled = System.getProperty("unidata.native.publish")?.toBoolean() ?: false }
