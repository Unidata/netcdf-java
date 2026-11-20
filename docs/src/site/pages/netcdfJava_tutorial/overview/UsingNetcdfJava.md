---
title: Using netCDF-Java Maven Artifacts
last_updated: 2025-08-08
sidebar: netcdfJavaTutorial_sidebar
permalink: using_netcdf_java_artifacts.html
toc: false
---

## Using netCDF-Java Maven Artifacts

We make the netCDF-Java library available as Maven artifacts.
To use them in your build, you need to add the Unidata Releases repository:

~~~xml
<!-- In Maven -->
<repositories>
    <repository>
        <id>unidata-all</id>
        <name>Unidata All</name>
        <url>https://artifacts.unidata.ucar.edu/repository/unidata-all/</url>
    </repository>
</repositories>
~~~

~~~groovy
// In Gradle
repositories {
    maven {
        url "https://artifacts.unidata.ucar.edu/repository/unidata-all/"
    }
}
~~~

For maximum compatibility, please include the Unidata repository after any other repositories, including maven central.

Next, select modules based on the functionality you need.
In the minimal case, you’ll just want `cdm-core` and a logger.
`cdm-core` implements the [Common Data Model (CDM)](common_data_model_overview.html) and allows you to read NetCD-3 files (and a number of other file types).
An example using JDK14 logging:

~~~xml
<!-- In Maven -->
<dependency>
  <groupId>edu.ucar</groupId>
  <artifactId>cdm-core</artifactId>
  <version>${netcdfJavaVersion}</version>
  <scope>compile</scope>
</dependency>

<dependency>
  <groupId>org.slf4j</groupId>
  <artifactId>slf4j-jdk14</artifactId>
  <version>${slf4jVersion}</version>
  <scope>runtime</scope>
</dependency>
~~~

~~~groovy
// In Gradle
dependencies {
  implementation "edu.ucar:cdm-core:${netcdfJavaVersion}"
  runtimeOnly "org.slf4j:slf4j-jdk14:${slf4jVersion}"
}
~~~

There are optional modules add support for reading (and sometimes writing) various scientific data formats.
To see which optionals you need for your data format, see [here](file_types.html).

You can include any number of the optional components.
For example, in Maven and Gradle:

~~~xml
<!-- In Maven -->
<dependency>
  <groupId>edu.ucar</groupId>
  <artifactId>bufr</artifactId>
  <version>${netcdfJavaVersion}</version>
  <scope>runtime</scope>
</dependency>

<dependency>
  <groupId>edu.ucar</groupId>
  <artifactId>cdm-image</artifactId>
  <version>${netcdfJavaVersion}</version>
  <scope>runtime</scope>
</dependency>

<dependency>
  <groupId>edu.ucar</groupId>
  <artifactId>grib</artifactId>
  <version>${netcdfJavaVersion}</version>
  <scope>runtime</scope>
</dependency>

<dependency>
  <groupId>edu.ucar</groupId>
  <artifactId>netcdf4</artifactId>
  <version>${netcdfJavaVersion}</version>
  <scope>runtime</scope>
</dependency>

<dependency>
  <groupId>edu.ucar</groupId>
  <artifactId>opendap</artifactId>
  <version>${netcdfJavaVersion}</version>
  <scope>runtime</scope>
</dependency>

<dependency>
  <groupId>edu.ucar</groupId>
  <artifactId>cdm-mcidas</artifactId>
  <version>${netcdfJavaVersion}</version>
  <scope>runtime</scope>
</dependency>
~~~

~~~groovy
// In Gradle
dependencies {
  runtimeOnly "edu.ucar:bufr:${netcdfJavaVersion}"
  runtimeOnly "edu.ucar:cdm-image:${netcdfJavaVersion}"
  runtimeOnly "edu.ucar:grib:${netcdfJavaVersion}"
  runtimeOnly "edu.ucar:netcdf4:${netcdfJavaVersion}"
  runtimeOnly "edu.ucar:opendap:${netcdfJavaVersion}"
  runtimeOnly "edu.ucar:cdm-mcidas:${netcdfJavaVersion}"
}
~~~

## Native compression code

### libaec

netCDF-Java `>=v5.8.0` supports libaec compression for GRIB2 messages using JNA to call the libaec C library.
To ease the use of this feature, we distribute a jar file containing the native libraries for the following platforms/architectures:

|---
| platform | x86-64 | aarch64
|:-|:-:|:-:
| Linux | <span class="glyphicon glyphicon-ok" style="color: green;"></span> | <span class="glyphicon glyphicon-ok" style="color: green;">
| MacOS | <span class="glyphicon glyphicon-ok" style="color: green;"> | <span class="glyphicon glyphicon-ok" style="color: green;">
| Windows | <span class="glyphicon glyphicon-ok" style="color: green;"> | <span class="glyphicon glyphicon-ok" style="color: green;">

If you are using on the of the supported platform/architecture combinations above, you may include the `edu.ucar.unidata:libaec-native:{{ site.libaec_version }}` artifact in your project to bypass the need to install libaec on your system.
Otherwise, libaec will need to be installed and reachable in your system library path in order to read data compressed using libaec.

### C-Blosc2

netCDF-Java `>=v5.10.0` supports blosc compression using JNA to call the C-Blosc2 library.
The `libblosc2-jna` module decodes data written with the HDF5 Blosc filter (id 32001) and the HDF5 Blosc2 filter (id 32026), as well as Blosc compressed chunks in Zarr.
To ease the use of this feature, we distribute a jar file containing the native libraries for the following platforms/architectures:

|---
| platform | x86-64 | aarch64
|:-|:-:|:-:
| Linux | <span class="glyphicon glyphicon-ok" style="color: green;"></span> | <span class="glyphicon glyphicon-ok" style="color: green;">
| MacOS | <span class="glyphicon glyphicon-ok" style="color: green;"> | <span class="glyphicon glyphicon-ok" style="color: green;">
| Windows | <span class="glyphicon glyphicon-ok" style="color: green;"> | <span class="glyphicon glyphicon-ok" style="color: green;">

If you are using on the of the supported platform/architecture combinations above, you may include the `edu.ucar.unidata:libblosc2-native:{{ site.cblosc2_version }}` artifact in your project to bypass the need to install C-Blosc2 on your system.
Otherwise, C-Blosc2 will need to be installed and reachable in your system library path in order to read data compressed using blosc.

## Building with netcdfAll

This is the appropriate option if you’re not using a dependency management tool like Maven or Gradle and you don’t care about jar size or compatibility with other libraries. Simply include netcdfAll-${netcdfJavaVersion}.jar on the classpath when you run your program.
You’ll also need a logger.
Currently, the netcdfAll jar does not include `cdm-s3` due to the size of the AWS S3 SDK dependency, and does not include any `edu.ucar.unidata:*-native` jars (native library binaries for compression).
The netcdfAll jar can be found at https://downloads.unidata.ucar.edu, or (starting with `5.9.0`) on the [GitHub release page](https://github.com/Unidata/netcdf-java/releases).

## Logging

The netCDF-Java library uses the SLF4J logging facade.
This allows applications to choose their own logging implementation, by including the appropriate jar file on the classpath at runtime.
Common choices are `JDK logging` and `Log4J 2`:

### JDK Logging

You must include the SLF4J-to-JDK Logging interface jar: `slf4j-jdk14-${slf4jVersion}.jar`.

The actual logging is implemented in the java.util.log package, part of the Java runtime.

#### To configure JDK logging:

Modify the file `$JAVA_HOME/jre/lib/logging.properties`.
Or, create you own logging properties file and specify it with the `java.util.logging.config.file` system property.

Possible log levels are `SEVERE`, `WARNING`, `INFO`, `CONFIG`, `FINE`, `FINER`, `FINEST`, and `ALL`.

To show only `SEVERE` messages for all loggers, use: `.level= SEVERE`

You can also set the configuration by using `java.util.logging.LogManager` in your application, most likely by creating your own properties file or resources and calling:

~~~java
FileInputStream inputStream = new FileInputStream("my.properties");
LogManager lm = java.util.logging.LogManager.getLogManager();
lm.readConfiguration(inputStream);
~~~

### Log4j 2

You must include the Log4j 2 SLF4J Binding (`log4j-slf4j-impl-${log4j2Version}.jar`) and the Log4j 2 implementation (`log4j-core-${log4j2Version}.jar`) on the classpath.

You should then configure the logging by adding a `log4j2.xml` config file to your classpath.
A minimal version is:

~~~xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
  <Appenders>
    <Console name="Console" target="SYSTEM_OUT">
      <PatternLayout pattern="%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n"/>
    </Console>
  </Appenders>
  <Loggers>
    <Root level="error">
      <AppenderRef ref="Console"/>
    </Root>
  </Loggers>
</Configuration>
~~~

See the [SLF4J manual](https://www.slf4j.org/manual.html) for more possibilities.