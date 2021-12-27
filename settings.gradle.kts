rootProject.name = "netcdf-java"

// allow the use of version_catalogs to handle defining all dependencies and versions
// in one location (e.g. gradle/libs.versions.toml)
enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Only allow dependencies from repositories explicitly listed here
    //repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    // Don't let plugins add repositories - this will make sure we know exactly which external
    // repositories are in use by the project.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

include("udunits")
include("netcdf-java-platform")
include("netcdf-java-testing-platform")
include("cdm-core")
include("cdm-test-utils")