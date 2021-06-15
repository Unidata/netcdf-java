---
title: Runtime loading
last_updated: 2021-06-08
sidebar: netcdfJavaTutorial_sidebar
permalink: runtime_loading.html
toc: false
---
## Runtime Loading

These are the various classes that can be plugged in at runtime:

### Register an IOServiceProvider

1) The recommended way is to use the [Service Provider](https://docs.oracle.com/javase/tutorial/ext/basics/spi.html){:target="_blank"}
mechanism and include your IOSP in a JAR on the classpath, where it is dynamically loaded at runtime. In your
JAR, include a file named `META-INF/services/ucar.nc2.iosp.IOServiceProvider` containing the
name(s) of your implementations, eg:

~~~
ucar.nc2.iosp.fysat.Fysatiosp
ucar.nc2.iosp.gini.Giniiosp
~~~

2) Alternatively, from your code, register your IOSP by calling:

{% capture rmd %}
{% includecodeblock netcdf-java&src/test/java/examples/runtime/runtimeloadingTutorial.java&registerIOSP %}
{% endcapture %}
{{ rmd | markdownify }}

In both cases, your class must implement `ucar.nc2.iosp.IOServiceProvider`. 
When `NetcdfFiles.open()` is called, we call `getIosp()` and loop through the `IOServiceProvider` classes calling `isValidFile()` on each until one returns `true`. This method is fast and accurate to insure efficient registration.

### Register a CoordSystemBuilder:

From your code, register your `CoordSystemBuilder` by calling:

{% capture rmd %}
{% includecodeblock netcdf-java&src/test/java/examples/runtime/runtimeloadingTutorial.java&registerCoordSystemBuilder %}
{% endcapture %}
{{ rmd | markdownify }}

The registered class must implement `ucar.nc2.dataset.CoordSystemFactory`. The `NetcdfDataset` is checked if it has a `Convention` attribute, and if so, 
it is matched by `conventionName`. If not, loop through the `CoordSystemFactory` classes and call `findConventionByIsMine()` on each, until one returns `true`.
If none are found, use the default `_Coordinate` convention.

### Register a TransformBuilder:
{% capture rmd %}
{% includecodeblock netcdf-java&src/test/java/examples/runtime/runtimeloadingTutorial.java&registerCoordTransBuilder %}
{% endcapture %}
{{ rmd | markdownify }}

The registered class must implement `ucar.nc2.internal.dataset.CoordTransformFactory`. The Coordinate Transform `Variable` must have the transform name as one of its parameters.

### Register a FeatureDatasetFactory:
{% capture rmd %}
{% includecodeblock netcdf-java&src/test/java/examples/runtime/runtimeloadingTutorial.java&registerFeatureDatasetFactory %}
{% endcapture %}
{{ rmd | markdownify }}

The registered class must implement `ucar.nc2.ft.FeatureDatasetFactory`.

### Register a GRIB1 table:
{% capture rmd %}
{% includecodeblock netcdf-java&src/test/java/examples/runtime/runtimeloadingTutorial.java&registerGRIBTable %}
{% endcapture %}
{{ rmd | markdownify }}

This registers a single table for the given center/subcenter/version.
See [GribTables](../developer/grib_tables.html) for more information about parameter tables.
*Note:* GRIB2 table handling is still being developed.

### Register a GRIB1 lookup table:
{% capture rmd %}
{% includecodeblock netcdf-java&src/test/java/examples/runtime/runtimeloadingTutorial.java&registerGRIBLookupTable %}
{% endcapture %}
{{ rmd | markdownify }}

This registers one or more tables for different center/subcenter/versions.
See [GribTables](../developer/grib_tables.html)  for more information about lookup tables.

*NOTE:* GRIB2 table handling is still being developed.

### Register a BUFR Table lookup:
{% capture rmd %}
{% includecodeblock netcdf-java&src/test/java/examples/runtime/runtimeloadingTutorial.java&registerBUFRTable %}
{% endcapture %}
{{ rmd | markdownify }}

The file must be a [BUFR table lookup file](../developer/bufr_tables.html).

## Runtime Configuration

Instead of calling the above routines in your code, you can pass the CDM library an XML configuration file. 
Note that your application must call `ucar.nc2.util.xml.RuntimeConfigParser.read()`.

The configuration file looks like this:
~~~xml
 <?xml version='1.0' encoding='UTF-8'?>
 <runtimeConfig>
1) <ioServiceProvider  class='edu.univ.ny.stuff.FooFiles'/>
2) <coordSystemBuilderFactory convention='foo' class='test.Foo'/>
3) <coordTransformFactory name='atmos_ln_sigma_coordinates' type='vertical' class='my.stuff.atmosSigmaLog'/>
4) <featureDatasetFactory featureType='Point' class='gov.noaa.obscure.file.Flabulate'/>
5) <gribParameterTable edition='1' center='58' subcenter='-1' version='128'>C:/grib/tables/ons288.xml</gribParameterTable>
6) <gribParameterTableLookup edition='1'>C:/grib/tables/ncepLookup.txt</gribParameterTableLookup>
7) <bufrtable filename='C:/my/files/lookup.txt' />
8) <grib1Table strict='false'/>
9) <Netcdf4Clibrary>
     <libraryPath>/usr/local/lib</libraryPath>
     <libraryName>netcdf</libraryName>
     <useForReading>false</useForReading>
   </Netcdf4Clibrary>
</runtimeConfig>
~~~

1. Loads an `IOServiceProvider` with the given class name
2. Loads a `CoordSysBuilderIF` with the given class name, which looks for the given `Convention` attribute value.
3. Loads a `CoordTransformFactory` with the given class name, which looks for the given `transformName` in the dataset. The type must be vertical or projection.
4. Loads a `FeatureDatasetFactory` with the given class name which open `FeatureDatasets` of the given `featureType`.
5. Load a [GRIB-1 parameter table](../developer/grib_tables.html) (as of version 4.3)
6. Load a [GRIB-1 parameter table lookup](../developer/grib_tables.html#standard-table-mapping) (as of version 4.3)
7. Load a [BUFR table lookup](../developer/bufr_tables.html) file.
8. Turn [strict GRIB1 table handling](../developer/grib_tables.html#strict) off.
9. Configure how the [NetCDF-4 C library](netcdf4_c_library.html) is discovered and used.
    * `libraryPath`: The directory in which the native library is installed.
    * `libraryName`: The name of the native library. This will be used to locate the proper `.DLL`, `.SO`, or `.DYLIB` file within the `libraryPath` directory.
    * `useForReading`: By default, the native library is only used for writing NetCDF-4 files; a pure-Java layer is responsible for reading them. 
    However, if this property is set to `true`, then it will be used for reading NetCDF-4 (and HDF5) files as well.
    
There are several ways pass the Runtime Configuration XML to the CDM library. From your application, you can pass a `java.io.InputStream` (or JDOM element) to 
`ucar.nc2.internal.util.xml.RuntimeConfigParser`, as in the following examples:

{% capture rmd %}
{% includecodeblock netcdf-java&src/test/java/examples/runtime/runtimeloadingTutorial.java&passConfigurationToCDM %}
{% endcapture %}
{{ rmd | markdownify }}
    
For example, the ToolsUI application allows you to specify this file on the command line with the `-nj22Config` parameter:

{% capture rmd %}
{% includecodeblock netcdf-java&src/test/java/examples/runtime/runtimeloadingTutorial.java&passConfigurationToolsUI %}
{% endcapture %}
{{ rmd | markdownify }}

If none is specified on the command line, it will look for the XML document in `$USER_HOME/.unidata/nj22Config.xml`.
