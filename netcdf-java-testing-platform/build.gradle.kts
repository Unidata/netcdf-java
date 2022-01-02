plugins {
    `java-platform`
    id("cdm.publication-conventions")
}

// internal Unidata use only

dependencies {
    constraints {
        // compile time constraints
        api(libs.commonsIo)
        api(libs.groovyAll)
        api(libs.junit)
        api(libs.mockitoCore)
        api(libs.slf4j)
        api(libs.spockCore)
        api(libs.truth)
        api(libs.truthJava8Extension)

        // runtime constraints
        runtime(libs.logbackClassic)
    }
}

publishing {
    publications {
        create<MavenPublication>("NcjTestingPlatform") {
            from(components["javaPlatform"])
        }
    }
}