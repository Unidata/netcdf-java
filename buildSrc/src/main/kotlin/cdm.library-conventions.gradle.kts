// CDM libraries are java projects that are published
plugins {
    id("cdm.java-conventions")
    `java-library`
    id("cdm.publication-conventions")
}

java {
    withJavadocJar()
    withSourcesJar()
}

fun classCase(input: String): String {
    return buildString {
        input.split("-").forEach { component ->
            append(component.capitalize())
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("${classCase(project.name)}MavenJava") {
            from(components["java"])
        }
    }
}
