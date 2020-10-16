---
title: Writing an IOSP - Overview
last_updated: 2019-07-23
sidebar: netcdfJavaTutorial_sidebar
permalink: writing_iosp.html
toc: false
---

## Writing an IOSP for Netdf-Java (version 4+)

A client uses the `NetcdfFiles`, `NetcdfDatasets`, or one of the *Scientific Feature Type APIs* to read data from a CDM file. 
These provide a rich and sometimes complicated API to the client. Behind the scenes, when any of these APIs actually read from a dataset, 
however, they use a very much simpler interface, the *I/O Service Provider* or *IOSP* for short. The Netcdf Java library has many implementations 
of this interface, one for each different file format that it knows how to read. This design pattern is called a *Service Provider*.

IOSPs are managed by the `NetcdfFiles` class. When a client requests a dataset (by calling `NetcdfFiles.open`), the file is opened as a `ucar.unidata.io.RandomAccessFile` 
(an improved version of `java.io.RandomAccessFile`). Each registered IOSP is then asked *is this your file?* by calling `isValidFile`. 
The first one that returns `true` claims it. When you implement `isValidFile` in your IOSP, it must be very fast and accurate.

### The ucar.nc2.IOServiceProvider interface

When implementing an IOSP, your class should extend `ucar.nc2.iosp.AbstractIOServiceProvider`. 
This provides default implementation of some of the methods in the `IOServiceProvider` interface, so minimally, 
you only have to implement a few methods:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/writingiosp/OverviewIospTutorial.java&getIOSP %}
{% endcapture %}
{{ rmd | markdownify }}

You must define your file type and assign your IOSP a unique id with the `getFileTypeId`, `getFileTypeDescription`, and 
`getFileTypeDescription` methods. See the [CDM File Types documentation](file_types.html) for more information.

*Note:* As of netCDF-Java version 5, IOSPs utilize a Builder design pattern to create immutable `NetcdfFile` objects. 
The Builder pattern replaces `open` and `close` with `build` and `buildFinish`. The `isBuilder` method indicates whether an IOSP is 
following the Builder pattern. Your IOSP should have an `isBuilder` method that returns `true` and should implement `build` instead of `open`. 

### Design goals for IOSP implementations

* Allow access to the dataset through the netCDF/CDM API
* Allow user access to every interesting bit of information in the dataset
* Hide details related to file format (eg links, file structure details)
* Try to mimic data access efficiency of netCDF-3
* Create good use metadata: accurate coordinate systems, enable classification by scientific data type
* Create good discovery metadata in the global attributes: title, creator, version, date created, etc.
* Follow standards and good practices
 

### Design issues for IOSP implementors

* What are the netCDF objects to expose? Should I use netCDF-3 or full netCDF4/CDM data model? Attributes vs Variables?
* How do I make data access efficient? What are the common use cases?
* How much work should I do in the `open` method? Can/should I defer some processing?
* Should I cache data arrays? Can I provide efficient strided access?
* What to do if dataset is not self-contained : external tables, hardcoding? 
