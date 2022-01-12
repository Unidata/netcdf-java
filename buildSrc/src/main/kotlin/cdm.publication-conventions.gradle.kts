plugins {
    `maven-publish`
}

publishing {
    repositories {
        val isSnapshot = version.toString().endsWith("SNAPSHOT")
        maven {
            name = if (isSnapshot) "snapshots" else "releases"
            url = if (isSnapshot) {
                uri("https://artifacts.unidata.ucar.edu/repository/unidata-snapshots/")
            } else {
                uri("https://artifacts.unidata.ucar.edu/repository/unidata-releases/")
            }

            credentials {
                username = project.properties["nexus.username"].toString()
                password = project.properties["nexus.password"].toString()
            }
        }
    }
}
