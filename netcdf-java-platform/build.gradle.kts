plugins {
    `java-platform`
}

dependencies {
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