plugins {
    `java-platform`
}

// internal Unidata use only

dependencies {
    constraints {
        // compile time constraints
        api(libs.junit)
        api(libs.slf4j)
        api(libs.truth)
        api(libs.truthJava8Extension)
        api(libs.commonsIo)
        api(libs.groovyAll)
        api(libs.mockitoCore)
        api(libs.spockCore)

        // tuntime constraints
        runtime(libs.logbackClassic)
    }
}