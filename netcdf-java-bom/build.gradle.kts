plugins {
    `java-platform`
    id("cdm.publication-conventions")
}

// netcdf-java library platform
// responsible for generating a maven bill of materials for the project
// only includes libraries intended for public use

dependencies {
    constraints {
        api(project(":bufr"))
        api(project(":cdm-core"))
        api(project(":cdm-s3"))
        api(project(":grib"))
        api(project(":netcdf4"))
        api(project(":udunits"))
    }
}

publishing {
    publications {
        create<MavenPublication>("NcjBom") {
            from(components["javaPlatform"])
        }
    }
}