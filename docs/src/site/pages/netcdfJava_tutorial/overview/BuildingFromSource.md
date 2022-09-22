---
title: Building From Source
last_updated: 2020-04-06
sidebar: netcdfJavaTutorial_sidebar
permalink: building_from_source.html
toc: false
---

## Assembly

The netCDF-Java source code is hosted on GitHub, and — as of v4.6.1 — we use Gradle to build it.
Ant and Maven builds are no longer supported.
To build, you need Git and JDK 8, JDK 11, or JDK 14 installed.

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

By default, the current branch head is set to `maint-5.x`, which is our main development branch.
If you’d like to build a released version instead, you can see all the release tags using
~~~bash
git tag
~~~

We recommend that you choose the latest release.
To choose release version {{site.docset_version}}.0, for example, you’ll need to checkout that version's tag:

~~~bash
git checkout v{{site.docset_version}}.0
~~~

Next, use the Gradle wrapper to execute the assemble task:

~~~bash
./gradlew assemble
~~~

There will be various artifacts within the `<subproject>/build/libs/` subdirectories.
For example, the `cdm-core.jar` file will be in `cdm/core/build/libs/`.
The uber jars, such as `toolsUI.jar` and `netcdfAll.jar`, will be found in `build/libs/`.

## Publishing

NetCDF-Java is comprised of several modules, many of which you can use within your own projects, as described [here](using_netcdf_java_artifacts.html).
At Unidata, we publish the artifacts that those modules generate to our Nexus repository.

However, it may happen that you need artifacts for the in-development version of netCDF-Java in your local branch, which we usually don’t upload to Nexus.
We do publish nightly SNAPSHOTS, but those may not have the development changes you are currently working on. 
Never fear: you can build them yourself and publish them to your local Maven repository!

~~~
git checkout maint-5.x
./gradlew publishToMavenLocal
~~~

You will find the artifacts in `~/.m2/repository/edu/ucar/`.
If you’re building your projects using Maven, artifacts in your local repo will be preferred over remote ones by default; you don’t have to do any additional configuration in order for them to be picked up.
If you’re building with Gradle, you’ll need to do [a little more work](https://docs.gradle.org/current/userguide/declaring_repositories.html#sub:maven_local){:target="_blank"}.

