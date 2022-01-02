import java.nio.file.Files
import java.nio.file.Paths
import java.security.DigestInputStream
import java.security.MessageDigest

plugins {
    `maven-publish`
}

fun writeChecksums() {
    val files = fileTree("${rootProject.buildDir}/libs").matching {
        include("**/*.jar", "**/*.zip")
    }
    listOf("MD5", "SHA-1", "SHA-256").forEach { algorithm ->
        val md = MessageDigest.getInstance(algorithm)
        val buffer = ByteArray(2048)
        files.forEach { sourceFile ->
            Files.newInputStream(Paths.get(sourceFile.absolutePath)).use { inputStream ->
                DigestInputStream(inputStream, md).use { dis ->
                    while (dis.read(buffer) != -1) {
                        // just need to read inputStream through the DigestInputStream to get
                        // the message digest
                    }
                }
            }
            val ext = algorithm.toLowerCase().replace("-", "")
            val outputFilename = "${buildDir}/libs/${sourceFile.name}.${ext}"
            File(outputFilename).writeText(
                buildString {
                    for (digestByte in md.digest()) {
                        append(((digestByte.toInt()).and(0xff) + 0x100).toString(16).substring(1))
                    }
                }
            )
        }
    }
}

fun classCase(input: String): String {
    return buildString {
        input.split("-").forEach { component ->
            append(component.capitalize())
        }
    }
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
                username = System.getProperty("nexus.username", "")
                password = System.getProperty("nexus.password", "")
            }
        }
    }
}
