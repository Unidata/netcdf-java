/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
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

var zstdVersion = "1.5.7"
var build = "0"

version = "${zstdVersion}.${build}"

description = "Jar distribution of native libraries for libzstd compression."

project.extra["project.title"] = "Native libraries for libzstd."

// zip file produced by GitHub workflow
val libzstdNative = "libzstd-native-${zstdVersion}-09b2efcff863c5a849f2e2e7fcf4b688bbab9acf.zip"

// sha256 checksum from GitHub workflow output
val expectedChecksum = "892405be6309b134f9cc058efc4ab818f9f5319623d6e7d2c9337cae64698436"

val resourceZip = file("$rootDir/project-files/native/libzstd/$libzstdNative")
val fetchNativeResources =
  tasks.register("fetchNativeResources") {
    outputs.file(resourceZip)
    doLast {
      if (!resourceZip.exists()) {
        logger.info("Fetching native libzstd libraries.")
        var actualChecksum = ""
        val resourceUrl =
          "https://downloads.unidata.ucar.edu/netcdf-java/native/libzstd/$libzstdNative"
        URL(resourceUrl).openStream().use { ips ->
          val dips = DigestInputStream(ips, MessageDigest.getInstance("SHA-256"))
          resourceZip.outputStream().use { ops -> dips.copyTo(ops) }
          actualChecksum = dips.messageDigest.digest().toHexString()
        }
        if (actualChecksum != expectedChecksum) {
          throw RuntimeException(
            String.format(
              "Error: checksum on libzstd.zip does not match expected value.\n" +
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
