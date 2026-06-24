/*
 * Copyright (c) 2025-2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

plugins {
  `kotlin-dsl`
  alias(ncjLibs.plugins.protobuf)
  alias(ncjLibs.plugins.spotless)
  alias(ncjLibs.plugins.versions)
}

dependencies {
  implementation(plugin(ncjLibs.plugins.protobuf))
  implementation(plugin(ncjLibs.plugins.spotless))
  implementation(plugin(ncjLibs.plugins.versions))
}

spotless {
  kotlinGradle {
    target("*.gradle.kts", "**/*.gradle.kts")
    ktfmt().googleStyle()
  }
}

// Helper function that transforms a plugin alias from the version catalog
// into a valid dependency notation
fun plugin(plugin: Provider<PluginDependency>) = plugin.map {
  "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}
