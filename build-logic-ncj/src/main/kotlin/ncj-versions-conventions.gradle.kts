/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins { id("com.github.ben-manes.versions") }

// https://github.com/ben-manes/gradle-versions-plugin
fun isNonStable(version: String): Boolean {
  val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
  val regex = "^[0-9,.v-]+(-r)?$".toRegex()
  val isStable = stableKeyword || regex.matches(version)
  return isStable.not()
}

// don't suggest release candidates if currently on a stable version
tasks.withType<DependencyUpdatesTask> {
  rejectVersionIf { isNonStable(candidate.version) && !isNonStable(currentVersion) }
}
