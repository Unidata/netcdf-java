# NetCDF-Java Gradle Build Docs

NetCDF-Java utilizes the Gradle build system.
This document provides an overview of the build system, and focuses on how to build and the run tests associated with the project.
Currently, netCDF-Java targets Java 11.
We use the Gradle toolchain feature to set the source compatibility and target level for compilation.
You will likely be able to run the build with a version of Java other than 11, but because of the toolchain feature, Java version 11 will be downloaded and used for the build and tests if a recognized JDK 11 cannot be found on your system.
We mostly use the [Eclipse Temurin distribution of OpenJDK](https://adoptium.net/) on our build and tests systems, but do test with a few other vendors occasionally.

## Common Gradle Tasks

* clean the project: `./gradlew clean`
* build jar files: `./gradlew assemble`
  * jar files located in `build/libs/` directory of each project
* run toolsUI: `./gradlew toolsui`
* publish artifacts to the Unidata nexus server: `./gradlew publish`
* run tests: `./gradlew test` (only non-annotated tests)
* check 3rd party dependencies for open CVEs: `./gradlew dependencyCheckAggregate`
  * html report located in `build/reports/dependency-check-report.html`
* build all documentation sets: `./gradlew :docs:buildAllJekyllSites`
  * static html sites found under `docs/build/site/<docset>`
* interactive documentation editing mode: `./gradlew :docs:serve<docset-name>`, where `<docset-name>` is `Developer`, `Ncml`, or `UserGuide`.
  * follow instructions in the terminal to access the site via web browser.
* build public javadoc: `./gradlew :docs:buildJavadocPublicApi`
  * static html javadoc found under `docs/build/javadoc`

Testing is special and is covered in the next section.

## Testing

There are four flavors of test tasks supported by this build system.

1. test
2. extendedTests
3. slowTests
4. specialTests

The `test` task do not rely on having access to any special data or resources other than the netCDF-C library (for tests that write netCDF-4 files).
These tests should run on any system by anyone with a copy of the repository.
If a test class or method is not annotated with a category, the `test` task will run it.

The `extendedTest` task only runs tests annotated with certain categories:
* `NeedsCdmUnitTest`
* `NeedsExternalResource`
* `NotPullRequest`
Tests marked with `NeedsCdmUnitTest` need to have access to a special set of test data.
These data are openly accessible and can be obtained by using `rsync`.
See https://github.com/unidata/thredds-test-data for more details.
For more information about which test categories are included, see `buildSrc/src/main/kotlin/cdm.java-conventions.gradle.kts`.

The `slowTest` task only runs tests annotated specifically with `Slow`.
Like the `extendedTest` tests, `slowTest` tests sometimes need access to the special set of test data.

`specialTests` require access to resources only available to Unidata employees, such as the internal UCAR network.
Currently, only tests marked with `NeedsUcarNetwork` fall into this special situation.
Very few tests are annotated with this category.
Don't feel bad about not running them.
We don't like this restriction either.

## Common Build Scripts

### Base Java Build Script
`buildSrc/src/main/kotlin/cdm.java-conventions.gradle.kts` contains build script logic that is
applied to all java projects.

### Base Publishing Build Script
`buildSrc/src/main/kotlin/cdm.publication-conventions.gradle.kts` contains the basic logic to control publishing of artifacts to the Unidata Nexus server.

### Library Build Script
`buildSrc/src/main/kotlin/cdm.library-conventions.gradle.kts` builds off of the base java and base publishing build script, and contains build script logic that is applied to all published java projects, whether intended for external use or internal use.

## Dependency Management

All dependencies (including test only and gradle plugins) are defined in one location: `gradle/libs.versions.toml`.
Individual project build scripts may reference these directly.
All projects reference the gradle platform projects "netcdf-java-platform" and "netcdf-java-testing-platform".
The platform projects ensure we use the same dependency versions across all of our projects (including transitive dependencies).

### Platform Projects

The gradle platform projects act as a build bill of materials (BOM), in that it will produce a pom file containing the versions of all dependencies used to build the netcdf-java projects that are intended for public use.
Any compile or runtime dependency used by a project meant as a library for public consumption should be listed in `netcdf-java-platform`.
For example, `cdm-core` is meant for public consumption, so it's API and Implementation dependencies should be listed in `netcdf-java-platform/build.gradle.kts`.
While toolsUI is intended for public use, it's not intended to be used as a library.
Therefore, dependencies unique to the `uibase` and `uicdm` projects should not be listed as part of the `netcdf-java-platform`.
Similarly, any test dependency not used by a project meant as a library for public consumption should not be listed in `netcdf-java-testing-platform`.
While the testing platform isn't really intended to be used outside of Unidata, it is shared between the various THREDDS projects for convenience.
We also produce a project bill of materials, `netcdf-java-bom/build.gradle.kts`, which simply lists the library components of netCDF-Java intended for public use.

## Upgrading gradle

To upgrade the gradle wrapper, you will need to edit the `build.gradle.kts` file in the root of the repository.
Look for the wrapper task configuration section:

~~~kotlin
tasks.wrapper {
    gradleVersion = "7.3.3"
    distributionSha256Sum = "c9490e938b221daf0094982288e4038deed954a3f12fb54cbf270ddf4e37d879"
    distributionType = Wrapper.DistributionType.ALL
}
~~~

Update the version and the associated sha256 hash (the has can be found at https://gradle.org/releases/).
Finally, run `./gradlew wrapper` and commit the changed files to git.
Note: You will likely need to upgrade any gradle plugins used by the build.
