---
title: NetcdfFile
last_updated: 2019-12-31
sidebar: netcdfJavaTutorial_sidebar 
permalink: reading_cdm.html
toc: false
---
## Tutorial: Working with NetcdfFile

A `NetcdfFile` provides read-only access to datasets through the netCDF API (to write data, use `NetcdfFileWriteable`).
Use the static `NetcdfFiles.open` methods to open a netCDF file, an HDF5 file, or any other file which has an &nbsp;[`IOServiceProvider`](writing_iosp.html) implementation that can read the file with the NetCDF API.
Use [`NetcdfDataset.openFile`](netcdf_dataset.html) for more general reading capabilities, including **OPeNDAP**, **NcML**, and **THREDDS** datasets.

Read access for some file types is provided through optional modules and must be included in your netCDF build as [artifacts](using_netcdf_java_artifacts.html).
To see what module you will need to include for your data, read more about [CDM file types](file_types.html).

## Opening a NetcdfFile

A simple way to open a NetcdfFile:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/cdmdatasets/ReadingCdmTutorial.java&openNCFile %}
{% endcapture %}
{{ rmd | markdownify }}

The `NetcdfFiles` class will open local files for which an `IOServiceProvider` implementation exists.
The current set of files that can be opened by the CDM are [here](file_types.html).

When you open any of these files, the IOSP populates the `NetcdfFile` with a set of `Variable`, `Dimension`, `Attribute`, and possibly `Group`, `Structure`, and `EnumTypedef` objects that describe what data is available for reading from the file. 
These objects are called the *structural metadata* of the dataset, and they are read into memory at the time the file is opened. The data itself is not read until requested.

If `NetcdfFiles.open` is given a filename that ends with `.Z`, `.zip`, `.gzip`, .gz, or `.bz2`, it will uncompress the file before opening, preferably in the same directory as the original file. 
See [DiskCache](#writing-temporary-files-to-the-disk-cache) for more details.
 
#### Using ToolsUI to browse the metadata of a dataset

The NetCDF Tools User Interface (aka ToolsUI) is a program for browsing and debugging NetCDF files.
You can download toolsUI.jar from the [netCDF-Java downloads page](https://www.unidata.ucar.edu/downloads/netcdf-java/){:target="_blank"}.
You can then run ToolsUI from the command line using a command similar to:

~~~bash
java -Xmx1g -jar toolsUI.jar
~~~

{% include image.html file="netcdf-java/tutorial/cdmdatasets/ToolsUIViewer.jpg" alt="Tools UI Viewer" caption="" %}

In this screen shot, the *Viewer* tab is shown displaying a NetCDF file in a tree view (on the left), and a table view of the variables (on the right).
By selecting a `Variable`, right clicking to get the context menu, and choosing *Show Declaration*, you can also display the `Variable`'s declaration in CDL in a popup window. 
The *NCDump Data* option from the same context menu will allow you to dump all or part of a `Variable`'s data values from a window like this:

{% include image.html file="netcdf-java/tutorial/cdmdatasets/TUIvarDump.jpg" alt="Tools UI Viewer" caption="" %}

Note that you can edit the `Variable`'s ranges (`T(0:30:10, 1, 0:3)` in this example) to dump just a subset of the data.
These are expressed with Fortran 90 array section syntax, using zero-based indexing.
For example, `varName( 12:22 , 0:100:2, :, 17)` specifies an array section for a four dimensional variable.
The first dimension includes all the elements from 12 to 22 inclusive, the second dimension includes the elements from 0 to 100 inclusive with a stride of 2, the third includes all the elements in that dimension, and the fourth includes just the 18th element.

The following code to dump data from your program is equivalent to the above ToolsUI actions:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/cdmdatasets/ReadingCdmTutorial.java&toolsUIDataDump %}
{% endcapture %}
{{ rmd | markdownify }}

## Reading data from a Variable

If you want all the data in a variable, use:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/cdmdatasets/ReadingCdmTutorial.java&readAllVarData %}
{% endcapture %}
{{ rmd | markdownify }}

When you want to subset the data, you have a number of options, all of which have situations where they are the most convenient.
Take, for example, the 3D variable `T` in the above example:

~~~
double T(time=31, lat=3, lon=4);
~~~

and you want to extract the third time step, and all lat and lon points, then use:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/cdmdatasets/ReadingCdmTutorial.java&readByOriginAndSize %}
{% endcapture %}
{{ rmd | markdownify }}
  
Notice that the result of reading a 3D Variable is a 3D Array.
To make it a 2D array call Array.reduce(), which removes any dimensions of length 1.

Or suppose you want to loop over all time steps, and make it general to handle any sized 3 dimensional variable:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/cdmdatasets/ReadingCdmTutorial.java&readInLoop %}
{% endcapture %}
{{ rmd | markdownify }}
 
In this case, we call reduce(0), to reduce dimension 0, which we know has length one, but leave the other two dimensions alone.

Note that `varShape` holds the total number of elements that can be read from the variable; `origin` is the starting index, and `size` is the number of elements to read.
This is different from the Fortran 90 array syntax, which uses the starting and ending array indices (inclusive):

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/cdmdatasets/ReadingCdmTutorial.java&readSubset %}
{% endcapture %}
{{ rmd | markdownify }}
  
If you want strided access, you can use the Fortran 90 string routine:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/cdmdatasets/ReadingCdmTutorial.java&readByStride %}
{% endcapture %}
{{ rmd | markdownify }}

#### Reading with Range Objects

For general programing, use the read method that takes a `List` of `ucar.ma2.Range` objects. 
A `Range` follows the Fortran 90 array syntax, taking the starting and ending indices (inclusive), and an optional stride:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/cdmdatasets/ReadingCdmTutorial.java&readByRange %}
{% endcapture %}
{{ rmd | markdownify }}

For example, to loop over all time steps of the 3D variable `T`, taking every second lat and every second lon point:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/cdmdatasets/ReadingCdmTutorial.java&readInLoopRanges %}
{% endcapture %}
{{ rmd | markdownify }}

The `Section` class encapsulates a list of `Range` objects and contains a number of useful methods for moving between 
lists of `Ranges` and origin, shape arrays. To create a `Section` from a list of `Ranges`: 

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/cdmdatasets/ReadingCdmTutorial.java&convertRangesToSection %}
{% endcapture %}
{{ rmd | markdownify }}

## Reading Scalar Data

There are convenience routines in the `Variable` class for reading scalar variables:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/cdmdatasets/ReadingCdmTutorial.java&readScalars %}
{% endcapture %}
{{ rmd | markdownify }}

The `readScalarDouble()` routine, for example, will read a scalar variable's single value as a double, converting it to double if needed.
This can also be used for 1D variables with dimension length = 1, e.g.:

~~~
 double height_above_ground(level=1);
~~~
 
Scalar routines are available in for data types: `byte`, `double`, `float`, `int`, `long`, `short`, and `String`.
The `String` scalar method:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/cdmdatasets/ReadingCdmTutorial.java&readStringScalar %}
{% endcapture %}
{{ rmd | markdownify }}
 
can be used on scalar `String` or `char`, as well as 1D `char` variables of any size, such as:

~~~
 char varname(name_strlen=77); 
~~~

## Manipulating data in Arrays

Once you have read the data in, you usually have an `Array` object to work with.
The shape of the `Array` will match the shape of the `Variable` (if all data was read) or the shape of the `Section` (if a subset was read).
There are a number of ways to access data in the `Array`.
Here is an example of accessing data in a 3D array, keeping track of index:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/cdmdatasets/ReadingCdmTutorial.java&iterateForLoop %}
{% endcapture %}
{{ rmd | markdownify }}

If you want to iterate over all the data in a variable of any rank, without keeping track of the indices, you can use the `IndexIterator`:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/cdmdatasets/ReadingCdmTutorial.java&indexIterator %}
{% endcapture %}
{{ rmd | markdownify }}

You can also just iterate over a subset of the data defined by a `List` of `Ranges` use the `RangeIterator`.
The following iterates over every 5th point of each dimension in an `Array` of arbitrary rank:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/cdmdatasets/ReadingCdmTutorial.java&rangeIterator %}
{% endcapture %}
{{ rmd | markdownify }}
    
In these examples, the data will be converted to double if needed.

If you know the Array's rank and type, you can cast to the appropriate subclass and use the `get()` and `set()` methods, for example:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/cdmdatasets/ReadingCdmTutorial.java&castDataArray %}
{% endcapture %}
{{ rmd | markdownify }}

There are a number of *index reordering* methods that operate on an `Array`, and return another `Array` with the same backing data storage, but with the indices modified in various ways:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/cdmdatasets/ReadingCdmTutorial.java&indexManipulation %}
{% endcapture %}
{{ rmd | markdownify }}

The backing data storage for an `Array` is a 1D Java array of the corresponding type (`double[]` for `ArrayDouble`, etc) with length `Array.getSize()`.
You can work directly with the Java array by extracting it from the `Array`:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/cdmdatasets/ReadingCdmTutorial.java&get1DArray %}
{% endcapture %}
{{ rmd | markdownify }}

If the `Array` has the same type as the request, and the indices have not been reordered, this will return the backing array, otherwise it will return a copy with the requested type and correct index ordering.

## Writing temporary files to the disk cache

There are a number of places where the library needs to write files to disk.
If you end up using the file more than once, its useful to cache these files.

1. If a filename ends with `.Z`, `.zip`, `.gzip`, `.gz`, or `.bz2`, `NetcdfFile.open` will write an uncompressed file of the same name, but without the suffix.
2. The *GRIB IOSP* writes an index file with the same name and a `.gbx` extension.
   Other IOSPs may do similar things in the future.
3. *Nexrad2* files that are compressed will be uncompressed to a file with an `.uncompress` prefix.

Before `NetcdfFile.open` writes the temporary file, it looks to see if it already exists.
By default, it prefers to place the temporary file in the same directory as the original file.
If it does not have write permission in that directory, by default it will use the directory `${user_home}/.unidata/cache/`.
You can change the directory by calling `ucar.nc2.util.DiskCache.setRootDirectory(String cacheDir)`.

You might want to always write temporary files to the cache directory, in order to manage them in a central place.
To do so, call `ucar.nc2.util.DiskCache.setCachePolicy( boolean alwaysInCache)` with parameter `alwaysInCache = true`.
You may want to limit the amount of space the disk cache uses (unless you always have data in writeable directories, so that the disk cache is never used).
To scour the cache, call `DiskCache.cleanCache()`.
For long-running applications, you might want to do this periodically in a background timer thread, as in the following example.

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/cdmdatasets/ReadingCdmTutorial.java&scourCache %}
{% endcapture %}
{{ rmd | markdownify }}

## Opening remote files on an HTTP Server

Files can be made accessible over the network by simply placing them on an HTTP (web) server, like Apache.
The server must be configured to set the `Content-Length` and `Accept-Ranges: bytes` headers.
The client that wants to read these files just uses the usual `NetcdfFile.open(String location, …)` method to open a file.
The location contains the URL of the file, for example: `https://www.unidata.ucar.edu/staff/caron/test/mydata.nc`.
In order to use this option you need to have `HttpClient.jar` in your classpath.
The `ucar.nc2` library uses the HTTP 1.1 protocol's `Range` command to get ranges of bytes from the remote file.
The efficiency of the remote access depends on how the data is accessed.
Reading large contiguous regions of the file should generally be good, while skipping around the file and reading small amounts of data will be poor.
In many cases, reading data from a `Variable` should give good performance because a `Variable`'s data is stored contiguously, and so can be read with a minimal number of server requests.
A record `Variable`, however, is spread out across the file, so can incur a separate request for each record index.
In that case you may do better copying the file to a local drive, or putting the file into a THREDDS server which will more efficiently subset the file on the server.

## Opening remote files on AWS S3

Files stored as single objects on AWS S3 can also be accessed using `NetcdfFiles` and `NetcdfDatasets`.
For more information, please see the object store section of the [Dataset URL](dataset_urls.html#object-stores) documentation.