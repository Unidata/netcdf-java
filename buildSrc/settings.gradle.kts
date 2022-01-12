// allow the use of version_catalogs to handle defining all dependencies and versions
// in one location (e.g. gradle/libs.versions.toml)
enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}