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

var aecVersion = "1.1.7"
var build = "0"

version = "${aecVersion}.${build}"

description = "Jar distribution of native libraries for libaec compression."

project.extra["project.title"] = "Native libraries for libaec."

// zip file produced by GitHub workflow
val libaecNative = "libaec-native-${aecVersion}-cd42ee111b1aed88c6b4defba428eb7642aee91e.zip"

// sha256 checksum from GitHub workflow output
val expectedChecksum = "43d0d9ca73c3faf6677f65b2ca219812e1e70806e2269bc5bd6b4b6de9f35bee"

val resourceZip = file("$rootDir/project-files/native/libaec/$libaecNative")
val fetchNativeResources =
  tasks.register("fetchNativeResources") {
    outputs.file(resourceZip)
    doLast {
      if (!resourceZip.exists()) {
        logger.info("Fetching native libaec libraries.")
        var actualChecksum = ""
        val resourceUrl =
          "https://downloads.unidata.ucar.edu/netcdf-java/native/libaec/$libaecNative"
        URL(resourceUrl).openStream().use { ips ->
          val dips = DigestInputStream(ips, MessageDigest.getInstance("SHA-256"))
          resourceZip.outputStream().use { ops -> dips.copyTo(ops) }
          actualChecksum = dips.messageDigest.digest().toHexString()
        }
        if (actualChecksum != expectedChecksum) {
          throw RuntimeException(
            String.format(
              "Error: checksum on libaec.zip does not match expected value.\n" +
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
      name = "NativeReleases"
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
  .forEach {
    // always disable publish task from ncj-artifact-publishing-conventions plugin
    if (it.name.contains("ToReleasesRepository")) {
      it.enabled = false
    } else {
      // for everything else, decide what to do based on the system property
      it.enabled = System.getProperty("unidata.native.publish")?.toBoolean() ?: false
    }
  }
