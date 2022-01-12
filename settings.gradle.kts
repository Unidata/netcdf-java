rootProject.name = "netcdf-java"

// allow the use of version_catalogs to handle defining all dependencies and versions
// in one location (e.g. gradle/libs.versions.toml)
enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        exclusiveContent {
            forRepository {
                maven {
                    url = uri("https://artifacts.unidata.ucar.edu/repository/unidata-all/")
                }
            }
            filter {
                includeModule("edu.ucar.unidata.nexus", "edu.ucar.unidata.nexus.gradle.plugin")
                includeModule("edu.ucar.unidata.site.jekyll", "edu.ucar.unidata.site.jekyll.gradle.plugin")
                includeModule("edu.ucar.unidata", "unidata-nexus-gradle")
                includeModule("edu.ucar.unidata.site", "jekyll-plugin")
            }
        }
    }
}

dependencyResolutionManagement {
    // Only allow dependencies from repositories explicitly listed here
    //repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    // Don't let plugins add repositories - this will make sure we know exactly which external
    // repositories are in use by the project.
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        mavenCentral()
        exclusiveContent {
            forRepository {
                maven {
                    url = uri("https://artifacts.unidata.ucar.edu/repository/unidata-releases/")
                }
            }
            filter {
                includeModule("edu.ucar", "jj2000")
            }
        }
        exclusiveContent {
            forRepository {
                maven {
                    url = uri("https://artifacts.unidata.ucar.edu/repository/unidata-3rdparty/")
                }
            }
            filter {
                includeModule("org.bounce", "bounce")
            }
        }
        gradlePluginPortal()
    }
}

include("udunits")
include("netcdf-java-platform")
include("netcdf-java-testing-platform")
include("cdm-core")
include("cdm-test-utils")
include("bufr")
include("grib")
include("netcdf4")
include("cdm-s3")
include("netcdf-java-bom")
include("cdm-test")
include("toolsui:uibase")
include("toolsui:uicdm")
include("gcdm")
include("docs")