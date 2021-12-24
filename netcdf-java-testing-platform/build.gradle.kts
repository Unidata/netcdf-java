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

        // tuntime constraints
        runtime(libs.logbackClassic)
    }
}