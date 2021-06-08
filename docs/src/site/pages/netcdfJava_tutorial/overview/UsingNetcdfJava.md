---
title: Using netCDF-Java Maven Artifacts
last_updated: 2019-07-23
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

Next, select modules based on the functionality you need.
In the minimal case, you’ll just want `cdm-core` and a logger.
`cdm` implements the CDM data model and allows you to read NetCD-3 files (and a number of other file types).
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
  compile "edu.ucar:cdm-core:${netcdfJavaVersion}"
  runtime "org.slf4j:slf4j-jdk14:${slf4jVersion}"
}
~~~

There are optional modules add support for reading (and sometimes writing) various scientific data formats.
The formats associated with each module are:

* `bufr`: BUFR
* `cdm-image`: GINI and FYSAT
* `cdm-radial`: Radial (eg radar) datasets
* `cdm-s3`: Enable RandomAccessFile level access to CDM datasets stored on AWS S3
* `cdm-misc` : Miscellaneous formats (see [here](#cdm-misc-module) for a list)
* `grib`: GRIB-1 and GRIB-2
* `netcdf4`: NetCDF-4. Writing requires the NetCDF-4 C library to be installed.
* `opendap`: OPeNDAP
* `cdm-mcidas`: GEMPAK grid, station, and sounding; McIDAS grid; and ADDE image and station
* `cdm-vis5d` : Vis5d grids

You can include any number of the above components.
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
  runtime "edu.ucar:bufr:${netcdfJavaVersion}"
  runtime "edu.ucar:cdm-image:${netcdfJavaVersion}"
  runtime "edu.ucar:grib:${netcdfJavaVersion}"
  runtime "edu.ucar:netcdf4:${netcdfJavaVersion}"
  runtime "edu.ucar:opendap:${netcdfJavaVersion}"
  runtime "edu.ucar:cdm-mcidas:${netcdfJavaVersion}"
}
~~~

## cdm-misc module

The `cdm-misc` module contains the following miscellaneous IOSPs:

 * Defense Meteorological Satellite Program (DMSP) format
 * GrADS:
   * raw lat/lon binary grid files (DTYPE not defined)
   * ensembles defined by NAMES in one file or as a template
   * time templates
   * gaussian latitudes
 * GTOPO Topograpic data
 * NLDN Lightning
 * NMC legacy (pre-BUFR) obs data
 * USPLN Lightning
 * NCEI
   * Global Historical Climatology Network Monthly (GHCNM)
   * Integrated Global Radiosonde Archive

## Building with netcdfAll

This is the appropriate option if you’re not using a dependency management tool like Maven or Gradle and you don’t care about jar size or compatibility with other libraries. Simply include netcdfAll-${netcdfJavaVersion}.jar on the classpath when you run your program.
You’ll also need a logger.
Currently does not include `cdm-s3` due to the size of the AWS S3 SDK dependency.

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