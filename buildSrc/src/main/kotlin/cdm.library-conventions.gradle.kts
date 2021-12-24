// CDM libraries are java projects that are published
plugins {
    `java-library`
    id("cdm.java-conventions")
}

java {
    withJavadocJar()
    withSourcesJar()
}

