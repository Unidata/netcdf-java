---
title: Building from source
last_updated: 2021-05-20
sidebar: userguide_sidebar
permalink: building_from_source.html
toc: false
---

## Assembly

The netCDF-Java source code is hosted on GitHub, and — as of v4.6.1 — we use Gradle to build it.
Ant and Maven builds are no longer supported.
To build, you need Git and at least JDK 11 installed (building with JDK >15 is not yet supported by the version of gradle used by our build).

First, clone the netCDF-Java repository from Github:

~~~bash
git clone -o unidata https://github.com/Unidata/netcdf-java.git netcdf-java
~~~

You’ll have a new folder named netcdf-java in your working directory.
The default name of a remote repository is `origin`.
We used the `-o` flag to rename it to `unidata` to keep things clear.
Change into the netcdf-java directory:

~~~bash
cd netcdf-java
~~~

By default, the current branch head is set to develop, which is our main development branch.

Next, use the Gradle wrapper to execute the assemble task:

~~~bash
./gradlew assemble
~~~

There will be various artifacts within the `<subproject>/build/libs/` subdirectories.
For example, the `cdm-core.jar` file will be in `cdm-core/build/libs/`.
`toolsUI.jar` will be found in `build/libs/`.

## Publishing

NetCDF-Java is comprised of several modules, many of which you can use within your own projects, as described [here](using_netcdf_java_artifacts.html).
At Unidata, we publish the artifacts that those modules generate to our Nexus repository.

However, it may happen that you need artifacts for the your personal in-development version of netCDF-Java, and those will not be available on our Nexus server.
Never fear: you can publish them to your local Maven repository!

~~~
./gradlew publishToMavenLocal
~~~

You will find your artifacts in `~/.m2/repository/edu/ucar/`.
If you’re building your projects using Maven, artifacts in your local repo will be preferred over remote ones by default; you don’t have to do any additional configuration in order for them to be picked up.
If you’re building with Gradle, you’ll need to do [a little more work](https://docs.gradle.org/current/userguide/declaring_repositories.html#sub:maven_local){:target="_blank"}.

