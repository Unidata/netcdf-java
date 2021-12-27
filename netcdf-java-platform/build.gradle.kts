plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

// only includes compile and runtime dependencies for projects listed in the netcdf-java-bom
dependencies {
    api(platform(libs.protobufBom))
    constraints {
        // compile time constraints
        api(libs.jsr305)
        api(libs.jcommander)
        api(libs.protobufJava)
        api(libs.re2j)
        api(libs.jdom2)
        api(libs.slf4j)
        api(libs.autoValue)
        api(libs.autoValueAnnotations)
    }
}