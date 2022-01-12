plugins {
    `java-platform`
    id("cdm.publication-conventions")
}

javaPlatform {
    allowDependencies()
}

// only includes compile and runtime dependencies for projects listed in the netcdf-java-bom
dependencies {
    api(platform(libs.protobufBom))
    api(platform(libs.awsSdkBom))
    constraints {
        // compile time constraints
        api(libs.autoValue)
        api(libs.autoValueAnnotations)
        api(libs.awsS3Sdk)
        api(libs.jcommander)
        api(libs.jdom2)
        api(libs.jj2000)
        api(libs.jna)
        api(libs.jsr305)
        api(libs.protobufJava)
        api(libs.re2j)
        api(libs.slf4j)
    }
}

publishing {
    publications {
        create<MavenPublication>("NcjPlatform") {
            from(components["javaPlatform"])
        }
    }
}
