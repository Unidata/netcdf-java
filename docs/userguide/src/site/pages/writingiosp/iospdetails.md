---
title: Writing an IOSP - details
last_updated: 2020-04-06
sidebar: userguide_sidebar
permalink: iosp_details.html
toc: false
---

## Registering a new IOSP

You must register your IOSP at runtime before it can open any files. See [runtime_loading](runtime_loading.html#register-an-ioserviceprovider) 
to learn how to register your IOSP.

*Note:* When registering, an instance of the class will be created. This means that there must be a default constructor that has no arguments. 
Since there is no way to call any other constructor, the simplest thing is to not define a constructor for your class, and the compiler 
will add a default constructor for you. If you do define a constructor with arguments, you must also explicitly add a no-argument constructor. 

If you contribute your IOSP and it becomes part of the CDM release, it will automatically be registered in the `NetcdfFile` class.

#### Troubleshooting
When registering an IOSP, the following exceptions may occur:
* `InstantiationException`: occurs when no default constructor exists
* `ClassNotFoundException`: occurs when the IOSP class name passed in to the register function as a `String` cannot be found by the NetcdfFile `ClassLoader`
(this almost always means that your classpath is wrong)
* `IllegalAccessException`: is thrown if you do not have the rights to access the IOSP class

#### IOSP lifecycle and thread safety

An IOSP is registered by passing in the IOSP class. An object of that class is immediately instantiated and stored. This object is used when `NetcdfFile` 
queries all the IOSPs by calling `isValidFile`. This makes the querying as fast as possible. *Since there is only one IOSP object for the library, the 
`isValidFile` method must be made thread-safe*. To make it thread safe, `isValidFile` must modify only local (heap) variables, not instance or class variables.

When an IOSP claims a file, a new IOSP object is created and `open` is called on it. Therefore *each dataset that is opened has its own IOSP instance assigned to it, 
and so the other methods of the IOSP do not need to be thread-safe*. The `NetcdfFile` keeps a reference to this IOSP object. When the client releases all references 
to the `NetcdfFile`, the IOSP will be garbage collected.

## Important IOSP methods

### The `isValidFile` method

~~~java
 // Check if this is a valid file for this IOServiceProvider. 
 public boolean isValidFile( ucar.unidata.io.RandomAccessFile raf) throws IOException;
~~~

The `isValidFile` method must quickly and accurately determine if the file is one that the IOSP knows how to read. 
If this is done incorrectly, it will interfere with reading other file types. As described in the previous section, it must be thread safe. 
It must also not assume what state the `RandomAccessFile` is in. If the file is not yours, return `false` as quickly as possible. 
An `IOException` must be thrown only if the file is corrupted. Since its unlikely that you can tell if the file is corrupt for any file type, 
you should probably catch `IOExceptions` and return `false` instead.

#### Example 1:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/writingiosp/IospDetailsTutorial.java&isValidExample1 %}
{% endcapture %}
{{ rmd | markdownify }}

*Note:* The file is assumed bad if the IOSP cannot read the first 8 bytes of the file. It is hard to imagine a valid file of less than 8 bytes. 
Still, be careful of your assumptions.

#### Example 2:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/writingiosp/IospDetailsTutorial.java&isValidExample2 %}
{% endcapture %}
{{ rmd | markdownify }}

Since the instantiated `BufrInput` is a local instance, this is a thread-safe example. 
Creating new objects should be avoided when possible for speed, but sometimes it's necessary.
*Note:* In this example, the IOSP catches `IOExceptions` and returns `false`; it would arguably be better for `BufrInput` to return `null`, 
following the rule that `Exceptions` should only be used in exceptional circumstances. Getting passed a file that is not yours is not exceptional.

#### Example 3 (BAD!):

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/writingiosp/IospDetailsTutorial.java&isValidExample3 %}
{% endcapture %}
{{ rmd | markdownify }}

In this example, `isValidFile` violates the thread-safe requirement since the `Grib1Input` and `edition` variables are instance variables.

The mistake might be because you want to use a scanner object and edition in the rest of the methods. Here's the right way to do this:


{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/writingiosp/IospDetailsTutorial.java&isValidExample4 %}
{% endcapture %}
{{ rmd | markdownify }}

The `isValidFile` method creates local variables for everything it has to do. The `build` method has to repeat that, 
but it is allowed to store instance variables that can be used in the rest of the methods, for the duration of the IOSP object.

### The `build` method

~~~java
  // Open existing file, and populate it. Note that you cannot reference the NetcdfFile within this routine.
  public void build(RandomAccessFile raf, Group.Builder rootGroup, CancelTask cancelTask) throws IOException;
~~~

Once an IOSP returns true on `isValidFile`, a new IOSP object is created and `build` is called on it. The job of `build` is to examine the contents of the file 
and create CDM objects that expose all of the interesting information in the file, using the `Group.Builder` API. 
Sticking with the simple Netcdf-3 data model for now, this means populating the `Group.Builder` object with `Dimension`, `Attribute`, and `Variable` objects.

#### `Attribute`

An `Attribute` is a (name, value) pair, where name is a `String`, and value is a 1D array of `Strings` or numbers. 
`Attributes` are thought of as metadata about your data. All `Attributes` are read and kept in memory, so you should not put large data arrays in `Attributes`. 
You can add global `Attributes` that apply to the entire file:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/writingiosp/IospDetailsTutorial.java&addGlobalAttribute %}
{% endcapture %}
{{ rmd | markdownify }}

Or you can add `Attributes` that are contained inside a `Variable` and apply only to that `Variable`, using the `Variable.Builder` API:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/writingiosp/IospDetailsTutorial.java&addVarAttribute %}
{% endcapture %}
{{ rmd | markdownify }}

#### `Dimension`

A `Dimension` describes the index space for the multidimension arrays of data stored in `Variables`. 
A `Dimension` has a `String` name and in integer length. In the Netcdf-3 data model, `Dimensions` are shared between `Variables` and stored globally.

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/writingiosp/IospDetailsTutorial.java&addDimension %}
{% endcapture %}
{{ rmd | markdownify }}

#### `Variable`

The actual data is contained in `Variables`, which are containers for multidimension arrays of data. In the Netcdf-3 data model, `Variables` can have 
type `DataType.BYTE`, `DataType.CHAR`, `DataType.SHORT`, `DataType.INT`, `DataType.FLOAT`, or `DataType.DOUBLE`.

If a `Variable` is unsigned (bytes, shorts or integer data types), you must add the `Unsigned` attribute:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/writingiosp/IospDetailsTutorial.java&unsignedAttribute %}
{% endcapture %}
{{ rmd | markdownify }}

Here is an example creating a `Variable` of type short called "elevation", adding several attributes to it, and adding it to the `Group.Builder`. 
The `Dimensions` lat and lon must already have been added. When setting `Dimensions`, the slowest-varying `Dimension` goes first (C/Java order).

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/writingiosp/IospDetailsTutorial.java&createVariable %}
{% endcapture %}
{{ rmd | markdownify }}

A special kind of `Variable` is a Coordinate `Variable`, which is used to name the coordinate values of a `Dimension`. 
A Coordinate `Variable` has the same name as its single dimension. For example:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/writingiosp/IospDetailsTutorial.java&createCoordinateVariable %}
{% endcapture %}
{{ rmd | markdownify }}

It is often convenient for IOSPs to set the data values of coordinate (or other) `Variables`.

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/writingiosp/IospDetailsTutorial.java&setVariableData %}
{% endcapture %}
{{ rmd | markdownify }}

Here, `Arrays.makeArray` is a convenience method that generates an evenly spaced array of length 180, starting at 90.0 and incrementing -1.0. 
That array is then cached in the `Variable`, and used whenever a client asks for data from the `Variable`. If a `Variable` has cached data, 
then `readData` will never be called for it.

### The `readData` method

~~~java
  // Read data from a top level Variable and return a memory resident Array.
  public ucar.array.Array readArrayData(Variable v2, Section section) throws IOException, InvalidRangeException;
~~~

When a client asks to read data from a `Variable`, the data is taken from the `Variable`'s data cache, if it exists, or the `readData` method of the IOSP is called. 
The client may ask for all of the data, or it may ask for a `hyperslab` of data described by the `Section` parameter. The `Section` contains a `java.util.List` of 
`ucar.array.Range` objects, one for each `Dimension` in the `Variable`, in order of the `Variable`'s `Dimensions`.

Here is an example, that assume the data starts at the start of the file, is in big-endian format, and is stored as a regular array of 16-bit integers on disk:

#### Example 1: Reading the entire `Array`

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/writingiosp/IospDetailsTutorial.java&readExample1 %}
{% endcapture %}
{{ rmd | markdownify }}

The `RandomAccessFile` reads 16-bit integers, advancing automatically. The `Arrays.section` method creates a logical section of the data array, 
returning just the section requested.

For large arrays, reading in all of the data can be too expensive. If your data has a regular layout, you can use `LayoutRegular` helper object:

#### Example 2: Using `ucar.nc2.iosp.LayoutRegular` to read just the requested `Section`:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/writingiosp/IospDetailsTutorial.java&readExample2 %}
{% endcapture %}
{{ rmd | markdownify }}

#### Example 3: Storing `Variable` specific information in SPobject

The previous examples essentially assumed a single data `Variable` whose data starts at byte 0 of the file. 
Typically you want to store various kinds of information on a per-variable basis, to make it easy and fast to respond to the `readData` request. 
For example, suppose there were multiple `Variables` starting at different locations in the file. You might compute these file offsets during the `build` call, 
storing that and other info in a `VarInfo` object:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/writingiosp/IospDetailsTutorial.java&readExample3 %}
{% endcapture %}
{{ rmd | markdownify }}

The `setSPobject` and `getSPobject` methods on the `Variable` are for the exclusive use of the IOSP. Use them in any way you need.

### The `finishBuild` method

~~~java
  // Sometimes the builder needs access to the finished objects. This is called after ncfile.build()
  public void buildFinish(NetcdfFile ncfile);
~~~
 
### Adding Coordinate System Information
Adding [coordinate system](cdm/index.html){:target="_blank"} information is the single most useful thing you can do to your datasets, 
to make them accessible to other programmers. As the IOSP writer, you are in the best position to understand the data in the file and 
correctly interpret it. You should, in fact, understand what the coordinate systems are at the same time you are deciding what the 
`Dimension`, `Variables`, and `Attribute` objects are.

Since there is no `CoordinateSystem` object directly stored in a netCDF file, CoordinateSystem information is encoded using a 
[convention](https://www.unidata.ucar.edu/software/netcdf/conventions.html){:target="_blank"} for adding `Attributes`, 
naming `Variables` and `Dimensions`, etc. in a standard way. The simplest and most direct way to add coordinate systems is to use the 
CDM [_Coordinate Attribute Conventions](coord_attribute_ex.html). Another approach is to follow an existing convention, in particular the 
[CF Convention](http://www.cfconventions.org/){:target="_blank"} is an increasingly important one for gridded model data, 
and work is being done to make it applicable to other kinds of data.

When a client opens your file through the `NetcdfFiles` interface, they see exactly what `Dimension`, `Variable`, and `Attribute` objects 
you have populated the `NetcdfFile` object with, no more and no less. When a client uses the `NetcdfDatasets` interface in enhanced mode, 
the coordinate system information is parsed by a [CoordSysBuilder](coord_system_builder.html) object, and Coordinate Axis, Coordinate System, 
and Coordinate Transform objects are created and made available through the NetcdfDataset API. In some cases, new Variables, 
Dimensions and Attributes may be created. Its very important that the IOSP writer follow an existing Convention and ensure that the 
Coordinate System information is correctly interpreted, particularly if you want to take advantage of the capabilities of the CDM Scientific 
Datatype Layer, such as serving the data through [WCS](https://docs.unidata.ucar.edu/tds/5.0/userguide/wcs_ref.html){:target="_blank"} or the 
Netcdf Subset Service.
