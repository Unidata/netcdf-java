/*
 * Copyright (c) 2025 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

plugins { id("ncj-java-library-conventions") }

description = "An IOSP for NetCDF-4 that loads the C library to read and write files."

project.extra["project.title"] = "NetCDF-4 IOSP"

project.extra["project.url"] = "https://www.unidata.ucar.edu/software/netcdf/"

// Most of the tests in this subproject require that the native C library be loaded. However, there
// are a handful of tests for which it must NOT be loaded. It's tricky for a single Gradle executor
// to handle both kinds of tests because once Java loads a native library, it remains loaded for
// the duration of the process. So, we must separate the tests (using SourceSets) and run them in
// different tasks.
val unloadedTestSourceSet =
  sourceSets.create("unloadedTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
  }

val unloadedTestImplementation by configurations.getting {
  extendsFrom(configurations.implementation.get(), configurations.testImplementation.get())
}

val unloadedTestRuntimeOnly by configurations.getting

configurations["unloadedTestRuntimeOnly"].extendsFrom(
  configurations.runtimeOnly.get(),
  configurations.testRuntimeOnly.get(),
)

dependencies {
  implementation(platform(project(":netcdf-java-platform")))

  implementation(project(":cdm-core"))

  implementation(ncjLibs.findbugs.jsr305)
  implementation(ncjLibs.guava)
  implementation(ncjLibs.jna)
  implementation(ncjLibs.slf4j.api)

  testImplementation(platform(project(":netcdf-java-testing-platform")))

  testImplementation(project(":cdm-test-utils"))

  testImplementation(ncjLibs.google.truth)
  testImplementation(ncjLibs.jdom2)

  testCompileOnly(ncjLibs.junit4)

  testRuntimeOnly(ncjLibs.junit5.platformLauncher)
  testRuntimeOnly(ncjLibs.junit5.vintageEngine)
  testRuntimeOnly(ncjLibs.logback.classic)
}

val testVersions = project.extra["project.testLtsVersions"]

if (testVersions is List<*>) {
  testVersions.forEach {
    val unloadedTest =
      tasks.register<Test>(
        if (it == project.extra["project.minimumJdkVersion"]) "unloadedTest"
        else "unloadedTest${it}"
      ) {
        description = "Runs tests with netCDF-C unloaded using Java ${it}."
        group = "verification"
        testClassesDirs = sourceSets["unloadedTest"].output.classesDirs
        classpath = sourceSets["unloadedTest"].runtimeClasspath
        useJUnitPlatform()
        javaLauncher.set(
          project.javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(it.toString().toInt())
          }
        )
      }
    if (it == project.extra["project.minimumJdkVersion"]) {
      tasks.test {
        mustRunAfter(unloadedTest)
        dependsOn(unloadedTest)
      }
    } else {
      tasks.named("testWithJdk${it}") {
        mustRunAfter(unloadedTest)
        dependsOn(unloadedTest)
      }
    }
  }
}
