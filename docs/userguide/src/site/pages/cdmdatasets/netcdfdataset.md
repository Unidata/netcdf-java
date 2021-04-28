---
title: NetcdfDataset - enhanced files, coordinate systems
last_updated: 2019-07-23
sidebar: userguide_sidebar 
permalink: netcdf_dataset.html
toc: false
---
## Tutorial: Working with NetcdfDataset

The `ucar.nc2.dataset` classes are an extension to the NetCDF API that provide support for

* processing standard attributes for scale/offset and missing data
* general and georeferencing coordinate systems
* the NetCDF Markup Language (NcML)
* remote access to OpenDAP, ADDE and THREDDS datasets.

NcML is an XML document format that allows you to create "virtual" netCDF datasets, including combining multiple netCDF files into one dataset. 
The [NcML](/thredds/ncml/current/basic_ncml_tutorial.html){:target="_blank"} tutorial explains how to create virtual datasets.

#### Using NetcdfDataset.openFile to open a NetcdfFile

The preferred way to open a `NetcdfFile` is through the `NetcdfDatasets.openFile` factory method:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/cdmdatasets/NetcdfDatasetTutorial.java&openNCFile %}
{% endcapture %}
{{ rmd | markdownify }}

`NetcdfDatasets.openFile` does the following:

* Opens an *OPeNDAP remote dataset*, if the location is a URL that starts with `http:`, `dods:`, `dap4:` (version 4.4+)
* Opens a *CdmRemote dataset*, if the location is a URL that starts with `cdmremote:`
* Opens a *THREDDS dataset*, if the location look like `thredds:<catalog>#<datasetId>`
* Opens an *NcML dataset*, if the location ends with `.xml` or `.ncml`, or is a URL starting with `ncmdl:` (version 4.4+)
* Otherwise, calls `NetcdfFile.open`, which handles local file or HTTP access to any CDM file.

For more information, see the [Dataset URL](dataset_urls.html) documentation.

#### Using NetcdfDataset.openDataset to open an enhanced NetcdfDataset

When you want the Netcdf-Java library to deal with missing values and scale/offset unpacking, and to identify coordinate systems, 
you should use the `NetcdfDatasets.openDataset` factory call:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/cdmdatasets/NetcdfDatasetTutorial.java&openEnhancedDataset %}
{% endcapture %}
{{ rmd | markdownify }}

Note that `NetcdfDataset` is a subclass of `NetcdfFile`, and so can be used wherever a `NetcdfFile` is used. `NetcdfDatasets.openDataset` does the following:

* Calls `NetcdfDatasets.openFile` and wraps the returned `NetcdfFile` in a `NetcdfDataset`, if necessary.
* Processes missing values and scale/offset attributes, modifying `Variable` data types if necessary
* Calls the appropriate `CoordinateSystemBuilder` class to identify the coordinate systems and populate the `Coordinate` objects.

#### Packed data variables and missing values

When you open a `NetcdfDataset` in *enhanced mode* (the default), any `Variables` that have the attributes `scale_factor` and/or `add_offset` are considered to be *packed data* `Variables`, whose data should be converted with the formula:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/cdmdatasets/NetcdfDatasetTutorial.java&unpackData %}
{% endcapture %}
{{ rmd | markdownify }}

Usually the packed data type is `byte` or `short` and the unpacked type is `float` or `double`, so the data type of the packed data `Variable` is promoted to `float` or `double`.
Missing data is indicated by the `valid_min`, `valid_max`, `valid_range`, `missing_value`, or `FillValue` attributes. When a `Variable` has any of these attributes, the `VariableDS.hasMissing` method returns `true`. 
You can test for missing values with the `VariableDS.isMissing(value)` method.

To open a `NetcdfDataset` in enhanced mode in Tools UI, toggle the enhance button 
{% include inline_image.html file="cdmdatasets/enhanceButton.jpg" alt="Enhance button" %} on the ToolsUI Viewer tab to ON.

#### Coordinate systems

`NetcdfDataset` will try to identify the coordinate systems in the dataset by calling a `CoordSystemBuilder` class that knows how to interpret the `Conventions` for that dataset. 
The information is placed in `Coordinate` objects that follow this abstract model (see the javadoc for the specifics):

{% include image.html file="coordsystems/CoordSys.png" alt="Tools UI Coord Sys" caption="" %}

To write your `CoordinateSystemBuilder`, see [here](coord_system_builder.html). To see the list of `CoordinateBuilder` classes, look at the source code in the `ucar.nc2.dataset.conv` package.

When writing netCDF files, we recommend using the Climate and Forcast (CF) Convention, if possible. 
When an IOSP reads in a non-netCDF file, it should choose a `Convention` to encode the coordinate systems.

#### Using ToolsUI to view Coordinate Systems

You can use ToolsUI *CoordSys* tab to view the coordinate systems that have been constructed. 
The view consists of 3 tables that show the data variables, the coordinate systems, and the coordinate axes.

{% include image.html file="cdmdatasets/TUIcoordSys.jpg" alt="Tools UI Coord Sys" caption="" %}

### Advanced Use of NetcdfDataset (version 4.2+)

The following is applicable to version 4 of the Netcdf-Java library. Netcdf-Java version 2.2 effectively has only two enhance modes, `All` and `None`.

#### NetcdfDataset.Enhance

The enhancements made when a `NetcdfDataset` is opened are described by `NetcdfDataset.Enhance`, an enumerated type with the following possible values:

* `ScaleMissing` : process scale/offset/missing attributes, and convert data
* `ScaleMissingDefer` : calculate scale/offset/missing info, but do not automatically convert data
* `CoordSystems` : just add coordinate systems
* `ConvertEnums`: convert enums to Strings
 
When using the `ScaleMissing` enhance mode, scale/offset/missing attributes are processed when the dataset is opened, 
and the data type of a `Variable` is promoted if necessary to match the unpacked data type. Data is automatically converted when read.

When using the `ScaleMissingDefer` enhance mode, scale/offset/missing attributes are processed when the dataset is opened, 
but the data type of a `Variable` is NOT promoted, and data is not converted. After reading data, you can convert the entire `Array` with `VariableEnhanced.convertArray(Array data)`, 
or convert single values with conversion methods such as `VariableEnhanced.convertScaleOffsetMissing(byte value)`.

When using `CoordSystems` enhance mode, `CoordSysBuilder` is called to populate the coordinate system objects in the `NetcdfDataset` when the dataset is opened.

When using `ConvertEnums` enhance mode, `Variables` of type `enum` are promoted to `String` types and data is automatically converted using the `EnumTypedef` objects, 
which are maps of the stored integer values to `String` values.

The enhancement of a dataset can be controlled by passing in a `Set` of `Enhance` to `NetcdfDataset.openDataset`. The default enhance mode is:

~~~java
 Set<Enhance> EnhanceAll = Collections.unmodifiableSet
                            (EnumSet.of(Enhance.ScaleMissing, Enhance.CoordSystems, Enhance.ConvertEnums));
~~~

and can be changed through `NetcdfDataset.setDefaultEnhanceMode(Set<Enhance> mode)`.

The simplest factory method, `NetcdfDataset.openDataset(location)`, uses the default enhance mode. 
Other factory methods with a boolean enhance parameter, such as `NetcdfDataset.openDataset(String location, boolean enhance, CancelTask cancelTask)`, 
use the default enhance mode if enhance is `true`, and `EnhanceMode.None` if enhance is `false`. Other classes, such as `GridDataset`, also use the default enhance mode.

#### Advanced options when opening

The most general factory method for opening a `NetcdfDataset` allows one to explicitly set the `EnhanceMode`:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/cdmdatasets/NetcdfDatasetTutorial.java&openEnhancedDatasetOptions %}
{% endcapture %}
{{ rmd | markdownify }}

One can also set the buffer size used for reading data, pass in a `CancelTask` object to allow user cancelling, 
and pass an arbitrary object to the `IOServiceProvider` that handles the dataset. 
These last 3 parameters correspond to the parameters in the similar factory method for opening a `NetcdfFile`:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/cdmdatasets/NetcdfDatasetTutorial.java&openNCFileOptions %}
{% endcapture %}
{{ rmd | markdownify }}
  
#### Caching NetcdfDataset and NetcdfFile

Advanced applications, like servers, might want to enable the caching of `NetcdfDataset` and `NetcdfFile` objects in memory, for performance. 
Caching is safe to use in a multithreaded environment, such as a servlet container like Tomcat. Caching keeps resources, such as file handles, open, 
so cache sizes should be carefully considered.

To enable caching, you must first call `NetcdfDataset.initNetcdfFileCache(int minElementsInMemory, int maxElementsInMemory, int period);`  
where `minElementsInMemory` is the number of objects to keep in the cache when cleaning up, `maxElementsInMemory` sets a threshold to trigger 
a cleanup if the cache exceeds it, and `period` specifies the time in seconds to do periodic cleanups.

One then calls the `acquireFile` or `acquireDataset` factory methods instead of `openFile` and `openDataset`. For example:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/cdmdatasets/NetcdfDatasetTutorial.java&cacheFiles %}
{% endcapture %}
{{ rmd | markdownify }}

Note that when done with the file, the `close` method is called as usual; instead of actually closing the file, it is left in the cache for subsequent acquiring.

Note also that calling `NetcdfDataset.shutdown` is crucial for terminating background threads that otherwise can prevent process termination.