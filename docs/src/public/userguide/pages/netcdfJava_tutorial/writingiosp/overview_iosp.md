---
title: Writing an IOSP - Overview
last_updated: 2019-07-23
sidebar: netcdfJavaTutorial_sidebar
permalink: writing_iosp.html
toc: false
---

## Writing an IOSP for Netdf-Java (version 4+)

A client uses the <b>NetcdfFile</b>, <b>NetcdfDataset</b>, or one of the <b>Scientific Feature Type APIs</b> to read data from a CDM file. These provide a rich and sometimes complicated API to the client. Behind the scenes, when any of these APIs actually read from a dataset, however, they use a very much simpler interface, the <b>I/O Service Provider</b> or <b>IOSP</b> for short. The Netcdf Java library has many implementations of this interface, one for each different file format that it knows how to read. This design pattern is called a <b>Service Provider</b>.

<b>IOSPs</b> are managed by the <b>NetcdfFile class</b>. When a client requests a dataset (by calling <b>NetcdfFile.open</b>), the file is opened as a <b>ucar.unidata.io.RandomAccessFile</b> (an improved version of <b>java.io.RandomAccessFile</b>). Each registered IOSP is then asked "is this your file?" by calling <b>isValidFile()</b>. The first one that returns true claims it. When you implement <b>isValidFile()</b> in your IOSP, it must be very fast and accurate.

#### The ucar.nc2.IOServiceProvider interface

~~~
public interface ucar.nc2.iosp.IOServiceProvider {
  // Check if this is a valid file for this IOServiceProvider
  // Required if you are registering your IOSP with NetcdfFile.registerIOProvider()
  public boolean isValidFile( ucar.unidata.io.RandomAccessFile raf) throws IOException;

   // Open existing file, and populate ncfile with it.
  public void open(ucar.unidata.io.RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException;

   // Read data from a top level Variable and return a memory resident Array.
  public ucar.ma2.Array readData(ucar.nc2.Variable v2, Section section) throws java.io.IOException, ucar.ma2.InvalidRangeException;

  // Allows reading sections of nested variables
  // Only implement if you have Structures
  public ucar.ma2.Array readSection(ParsedSectionSpec cer) throws IOException, InvalidRangeException;
  
  // Get a structure iterator. iosps with top level sequences must override
  // Only implement if you have Sequences
  public StructureDataIterator getStructureIterator(Structure s, int bufferSize) throws java.io.IOException;

  // Close the file.
  public void close() throws IOException;

  // Extend the file if needed in a way that is compatible with the current metadata.
  public boolean syncExtend() throws IOException;

  // A way to communicate arbitrary information to an iosp.
  public Object sendIospMessage( Object message);
  
  // print Debug info for this object.
  public String toStringDebug(Object o);
  
  // Show debug / underlying implementation details
  public String getDetailInfo();

  // Get a unique id for this file type
  public String getFileTypeId();

  // Get the version of this file type.
  public String getFileTypeVersion();

  // Get a human-readable description for this file type.
  public String getFileTypeDescription();
}
~~~

Your implementataion class should extend <b>ucar.nc2.iosp.AbstractIOServiceProvider</b>. This provides default implementation of some of the methods, so minimally, you only have to implement a few methods:

~~~
public class MyIosp extends ucar.nc2.iosp.AbstractIOServiceProvider {
 1)  public boolean isValidFile(RandomAccessFile raf) throws IOException {}
 2)  public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {}
 3)  public Array readData(Variable v2, Section wantSection) throws IOException, InvalidRangeException {}
 4)  public void close() throws IOException {}
 5)  public String getFileTypeId() {}
 5)  public String getFileTypeVersion() {}
 5)  public String getFileTypeDescription();

    // optional
 6) public ucar.ma2.Array readSection(ParsedSectionSpec cer) throws IOException, InvalidRangeException {}
 7) public StructureDataIterator getStructureIterator(Structure s, int bufferSize) throws java.io.IOException {}
 8) public boolean syncExtend() throws IOException {}
 9) public Object sendIospMessage( Object message) {}
10) public String getDetailInfo() {}
}
~~~

1. You must examine the file that is passed to you, and quickly and accurately determine if it is can be opened by this IOSP. You may not keep any state (ie strore any information) in this call, and it must be thread-safe.
2. You will then be called again with the same file, and an empty NetcdfFile object, which you will populate. If you need to do a lot of I/O, you should periodically check <b>cancelTask.isCancel()</b>, and if its true, return immediately. This allows users to cancel the opening of a dataset if its taking too long.
3. Data will be read from Variable through this call. The section defines the requested data subset.
4. Release all resources, for example, by calling <b>RandomAccessFile.close()</b>.
5. You must give your IOSP a unique id. See <a href="file_types.html">CDM File Types</a>.
6. If you use Structures, data for Variables that are members of Structures are read through this method. If you dont override, the default implementation in <b>AbstractIOServiceProvider</b> is used. Override in order to improve performance
7. If any of your top level variables (not inside of a Structure) are Sequences, this is how the data in them will be accessed, and you must implement it.
8. If the file may change since it was opened, you may optionally implement this routine. The changes must not affect any of the structural metadata. For example, in the NetCDF-3 IOSP, we check to see if the record dimension has grown.
9. This allows applications to pass an arbitrary object to the IOSP, through the <b>NetcdfFile.open( location, buffer_size, cancelTask, spiObject)</b> method. As a rule, you should not count on having such special information available, unless you are controlling all data access in an application.
10. Here you can pass any information that is useful to debugging. It can be viewed through the ToolsUI application.

#### Design goals for IOSP implementations

* Allow access to the dataset through the netCDF/CDM API
* Allow user access to every interesting bit of information in the dataset
* Hide details related to file format (eg links, file structure details)
* Try to mimic data access efficiency of netCDF-3
* Create good use metadata: accurate coordinate systems, enable classification by scientific data type
* Create good discovery metadata in the global attributes: title, creator, version, date created, etc.
* Follow standards and good practices
 

#### Design issues for IOSP implementors

* What are the netCDF objects to expose? Should I use netCDF-3 or full netCDF4/CDM data model? Attributes vs Variables?
* How do I make data access efficient? What are the common use cases?
* How much work should I do in the open() method? Can/should I defer some processing?
* Should I cache data arrays? Can I provide efficient strided access?
* What to do if dataset is not self contained : external tables, hardcoding? 
