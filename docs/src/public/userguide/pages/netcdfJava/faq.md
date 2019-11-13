---
title: Frequently Asked Questions
last_updated: 2019-07-22
sidebar: netcdfJavaTutorial_sidebar 
permalink: faq.html
toc: false
---
 
## NetCDF-Java FAQ

* [Arrays](#arrays)
* [Caching](#caching)
* [IOSP](#io-service-provider)
* [Logging](#logging)
* [Multithreading](#multithreading)
* [NcML](#ncml)
* [Unsigned types](#unsigned-types)
* [Reading](#reading)
* [Writing](#writing)

### Miscellaneous

* <a href="building_from_source.html">How do I build from source or contribute to the NetCDF Java Library?</a>

### Arrays
#### Q: I want to read data from a NetcdfFile and use another package to process the results, which doesn't use ucar.ma2.Array. How can I get the data out of the Array object most efficiently?

The most efficient way is to use this method on the Array class:

~~~
 public java.lang.Object get1DJavaArray(java.lang.Class wantType);
~~~

which will give back the Java primitive array, without copying if possible. For example if you have a type double Array object:

~~~
 double[] ja = (double []) ma.get1DJavaArray( double.class);
~~~

You then have to see if your chosen package has a constructor that can wrap the Java array without copying it. You will also need the multidimensional shape information: <b>_int[] shape = ma.getShape()_</b>.

#### Q: What are some easy ways to create an Array object?

~~~
Array.factory(DataType dataType, int[] shape, Object javaArray);
Array.makeArray(DataType dtype, int npts, double start, double incr);
Array.makeArray(DataType dtype, List<String> stringValues);
~~~

#### Q: When processing Array data, how do you avoid if-then-else blocks (or switch statements) going through every type (float, long, double, byte, short, etc.) ?

There is no way around switching on primitive type. However, you can safely widen anything to double (except type long may lose precision). So if you have an Array that you know is numeric, you can do:

~~~
 public double[] getArrayAsDoubles(Array a) {
   assert a.getDataType().isNumeric();
   double[] result = new double[(int) a.getSize()];

   IndexIterator iterA = a.getIndexIterator();
   int count = 0;
   while (iterA.hasNext())
     result[count++] = iterA.getDoubleNext();

   return result;
 }
~~~

this is equivilent to casting the actual values as doubles. The "switching on primitive type" is being done by Java's polymorphism, that is, because the class Array has subtypes for each primitive type.

Note that this flattens the Array into a 1D primitive array. Another way to do the same thing, which may be more efficient, and is certainly more concise:

~~~
assert a.getDataType().isNumeric();
double[] result = (double []) a.get1DJavaArray(double.class);
~~~

### Caching
#### Q: How do I use NetcdfFile object caching?

Initialize the object cache by calling <b>_NetcdfDataset.initNetcdfFileCache()_</b>, then open files through <b>_NetcdfDataset.acquireFile()_</b> and <b>_NetcdfDataset.acquireDataset()_</b>. Note that you always close a file in the normal way, ie through <b>_NetcdfFile.close()_</b>. See the javadoc for those methods for more details.

The main reason to use object caching is in a high performance server application.

Q: How do I control where temporary files are placed (Disk Caching)?

See [Disk Caching](disk_caching.html).

### I/O Service Provider

#### Q: What is an IOSP?

An <b>_I/O Service Provider (IOSP)_</b> is a Java class that implements the <b>_ucar.nc2.iosp.IOServiceProvider_</b> interface, and knows how to read and understand the data in some specific scientific file format. An IOSP essentially implements the netCDF API for that file type. The interface is designed to minimize the amount of work an IOSP implementer has to do.

#### Q: I have lots of data in a proprietary format. How do I read it into the CDM?

You write a class that implements the IO Service Provider interface, typically by subclassing ucar.nc2.iosp.AbstractIOServiceProvider. See section 4 of the [tutorial](common_data_model_overview.html).

#### Q: I want to create a NetcdfFile from some other source than a file. But an IOSP is designed to get data from a RandomAccessFile. So what do I do?

An IOSP just has to satisfy the contract of IOServiceProvider. It doesn't matter how it gets satisfied. You can create an IOSP any way you want - its an interface. If you don't need the RandomAccessFile, you can ignore it. Use memory-resident data, a random-number generator, or any other way to satisfy a data request.

You can create a NetcdfFile with the protected constructor, and pass in your IOSP:

~~~
  protected NetcdfFile(IOServiceProvider spi, RandomAccessFile raf, String location, CancelTask cancelTask);
 // need access to protected constructor
 class MyNetcdfFile extends NetcdfFile {
   MyNetcdfFile (IOServiceProvider spi, String name) {
     super(spi, null, name, null);
   }
~~~

Once you have a NetcdfFile, you can wrap it with a NetcdfDataset, a GridDataset, a PointFeatureDataset, etc, and use all the mechanism of subsetting already in those classes. Ultimately those call your IOSP for data, and you must return the data correctly, according to the interface contract.

#### Q: What about the "O" in IOServiceProvider? How does that work?

If you look at <b>_ucar.nc2.iosp.IOServiceProviderWriter_</b>, you can see the start of a possible standard mechanism for writing to different file formats. But it isn't used anywhere that would likely be useful to you. You probably just want to write your own class that takes a NetcdfFile object, and writes it to your file format, in whatever way suits you best. You might find the code at <b>_ucar.nc2.FileWriter2_</b> useful to look at.

### Logging

#### Q: How do I control the error messages coming out of the library?

The netCDF-Java library currently uses <a href="http://www.slf4j.org/" target="_blank">SLF4J logging</a>. This allows you to switch what logging implementation is used. Read more here.

#### Q: Im using the log4j logging package. How do I get rid of the message "log4j:WARN No appenders could be found for logger (ucar.nc2.NetcdfFile). log4j:WARN Please initialize the log4j system properly" ?

Add the following to your startup code:

~~~
    org.apache.log4j.BasicConfigurator.configure();
    org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();
    logger.setLevel(org.apache.log4j.Level.OFF)
~~~

### Multithreading

#### Q: Is the Netcdf-Java library thread-safe?

Underneath a <b>_Variable/NetcdfFile_</b> is (usually) a <b>_java.io.RandomAccessFile_</b> object, which is not thread-safe, because it keeps the state of the file position. So even if all you want to do is read data in multiple threads, you need to synchronize, typically on the <b>_NetcdFile_</b> object. Better is to open a new <b>_NetcdfFile_</b> for each thread. The THREDDS Data Server (TDS) uses a cache of open <b>_NetcdfFile_</b> files by using the <b>_NetcdfDataset.acquireFile()_</b> method, which allows stateless handling of data requests minimizing file opening and closing.

Q: Do I need to synchronize if I use NetcdfDataset.acquireFile()?

The way that the cache is designed to work is that you get back a NetcdfFile object, which should then be used only in a single thread so that you don't need synchronization ("thread-confinement"), eg to answer a single request in a server. Until you release that NetcdfFile object, no one else can get it from the cache. If another request is made for that same NetcdfFile while its locked, a new NetcdfFile is opened. And of course, the cache itself is thread-safe. So if you use it properly, you never have to do synchronization. As a rule, synchronization should not happen in application code, as it is too difficult to do correctly.

### NcML

#### Q: The NcML in my TDS is not working. What should I do?

Generally its much easier to debug NcML outside of the TDS. Here are some guidelines on how to do that.

1. Go to the TDS configuration catalog and extract the NcML:
    1. Find the problem dataset. Inside the <dataset> element will be a <netcdf> element, that is the NcML. Cut and paste into a file, say its called test.ncml (it must have suffix ncml or xml).
    2. Add the XML header to the top of it: <?xml version="1.0" encoding="UTF-8"?>
    3. Remove the recheckEvery attribute if present on the <scan> element.
    4. Make sure the referenced datasets are available. If its an aggregation, a simple thing to do is to copy two or more of the files and put them in the same directory as test.ncml. Use a scan element or explicitly list them in a <netcdf> element, with the location attribute being the relative path name.
    
2. Open test.ncml in the viewer tab of ToolsUI, to check for NcML errors. You now see directly what the modified dataset looks like. Modify test.ncml and re-open it until you get it right. The NcML tab allows you to edit and save the NcML file, but it is a very primitive editor.
3. If its a grid dataset,open it in the FeatureTypes/Grid tab to make sure you see grids, to check for complete coordinate system. If you don't see the grids you expect, the CoordSys tab might be helpful. It takes some expertise to understand how [Coordinate systems work](common_data_model_overview.html). When all else fails, follow the <a href="http://cfconventions.org/Data/cf-conventions/cf-conventions-1.7/build/cf-conventions.html"> CF specification</a>.
4. If its an aggregation, the NcML/Aggregation tab will show you the individual file in the aggregation.
5. If its an FMRC aggregation, the Fmrc/FmrcImpl tab will show you the various datasets found.
6. Once things are working correctly, put your changes back into TDS catalog and restart the server
7. Open your TDS catalog in the ToolsUI/THREDDS tab. Navigate to the dataset, and "open as file" or "open as dataset" (at bottom). You should see the same results as in steps 2 and 3.

### Unsigned Types

#### Q: How do I work with unsigned integer types?

Classic netCDF-3 format has only signed bytes. The CDM library often sees unsigned integers coming from other data formats, and we made the decision not to automatically widen unsigned types, in order to save memory. The data is delivered using Java integer types, which are signed, so its up to the application to check Variable.isUnsigned() or Array.isUnsigned() and do the right thing when doing computations with the data. The library itself handles all conversions and computations correctly, for example Array.getDouble(). See CDM Datatypes for more details on how to work with unsigned data arrays.

The correct way to widen is to use the equivalent of these static methods in ucar.ma2.DataType. The actual conversion code is on the right:

|---
| static method | conversion code
|:- | :-
| DataType.unsignedByteToShort(byte b)   | (short)  (b & 0xff)
| DataType.unsignedShortToInt(short s)   | (int)  (s & 0xffff)
| DataType.long unsignedIntToLong(int i)  | (i < 0) ? (long) i + 4294967296L : (long) i;

Widening is different than taking the absolute value, as this partial table showing byte to short conversion:

~~~
 byte   short
 ...
  125 =  125
  126 =  126
  127 =  127
 -128 =  128
 -127 =  129
 -126 =  130
 -125 =  131
 -124 =  132
 ...
~~~

### Q: How do I specify an unsigned integer type in NcML?

In the following example, the <b>_type_</b> may be byte, short, int or long:

~~~
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" location="nc/cldc.mean.nc">
  ...
  <variable name="temperature" shape="time pressure latitude longitude" type="int">
    <attribute name="long_name" value="Sea Temperature" />   
    <attribute name="units" value="Celsius" />
    <attribute name="_Unsigned" value="true" />
  </variable>

</netcdf>
~~~

### Reading
#### Q: What files can the library read?

See [File Types])/common_data_model_overview.html).

#### Q: How do I read a file of type X?

In general, you [open any CDM file](common_data_model_overview.html) in the same way, and access it through the [extended netCDF data model(/common_data_model_overview.html#data-access-layer-object-model). The whole point of the CDM is to hide the details of the file format. However, some file type may require special handling:

GRIB and BUFR files may require special tables that the CDM doesn't have. Open the file as above and see 1) if you get any error messages, 2) if any of the variables have "Unknown" in their name, and 3) if the data looks wrong. If any of those happen, prepare to enter [GRIB table hell realm](grib_tables.html). (BUFR is arguably worse, but there's nothing yet that you can do about it).

#### Q: Can files be compressed and still be read? How does that work?

If the URL ends with a with ".Z", ".zip", ".gzip", ".gz", or ".bz2", the file is assumed to be <b>_compressed_</b>.

|---
|file suffix | compression type
|:-:|:-
| .Z | unix <a href="http://en.wikipedia.org/wiki/Compress"> compress</a> ( LZW )
| .zip | <a href="http://en.wikipedia.org/wiki/Zip_(file_format)">zip</a> files (assumes only one entry)
| .gzip, .gz | <a href="http://en.wikipedia.org/wiki/Gzip">deflate</a>
| .bz2 | <a href="http://en.wikipedia.org/wiki/Bzip2"> Burrows–Wheeler</a>

The netCDF-Java library will uncompress/unzip and write a new file without the suffix, then read from the uncompressed file. Generally it prefers to place the uncompressed file in the same directory as the original file. If it does not have write permission on that directory, it will use the cache directory defined by ucar.nc2.util.DiskCache.

### Writing
#### Q: Ok, so you read a lot of files, what about writing?

Netcdf-Java library supports writing netCDF-3 file format using the [classic data model]. Writing to the netCDF-4 file format is supported using a <a href="http://en.wikipedia.org/wiki/Java_Native_Interface">JNI</a> interface to the [netCDF C library]( /netcdf4_c_library.html). Writing the full [CDM data model](common_data_model_overview.html) is in beta, as of version 4.5. See:

* Program with complete control: <b>_ucar.nc2.NetcdfFileWriter_</b> javadoc and [NetCDF File Writing tutorial](writing_netcdf.html).
* Copy complete files from a program: <b>_ucar.nc2.FileWriter2_</b> writes CDM files to netCDF-3 or netCDF-4 format.
* Command line file copying: See here for details.

#### Q: What is the relationship of NetCDF with HDF5?

The netCDF-4 file format is built on top of the <a href="http://www.hdfgroup.org/HDF5/">HDF5 file format</a>. NetCDF adds shared dimensions, so it is unfortunately not a strict subset of HDF5. Gory details are here:  <a href="http://www.unidata.ucar.edu/blogs/developer/en/entry/dimensions_scales">Part 1</a>, <a href="http://www.unidata.ucar.edu/blogs/developer/en/entry/dimension_scale2">Part 2</a>, <a href="http://www.unidata.ucar.edu/blogs/developer/en/entry/dimension_scales_part_3">Part 3</a>.

HDF5 is a very complicated format, and we do not plan to write a pure Java version for writing netCDF4 files (we do have a pure Java version for reading both HDF5 and netCDF-4). You must use the JNI interface to the [netCDF C library](netcdf4_c_library.html).

#### Q: Can I stream a NetcdfFile object to a client?

NetCDF is a random access format, so streaming is not possible in general. The way to do this is to write to a disk file (so that you have a random access file), using <b>_ucar.nc2.FileWriter2_</b>, then copy the file to the client. For performance, you can try copying to a solid state disk.

We are working on an experimental streaming format for NetCDF, called [ncstream](ncstream.html), and a remote access protocol called [CdmRemote](cdmremote.html). These are fully functional as of CDM version 4.2, but are still evolving and should only be used if you can tolerate non-stable APIs and formats.

#### Q: What kind of information should I put into my netCDF file to help others read it?

Thank you for asking, See:

* General Guidelines: <a href="http://www.unidata.ucar.edu/software/netcdf/docs/BestPractices.html">http://www.unidata.ucar.edu/software/netcdf/docs/BestPractices.html</a>
* Recommended Conventions: <a href="http://cfconventions.org/">CF Conventions/a>

#### Q: How do I put a valid_range attribute on a unsigned variable?

A valid range is applied to the underlying data before anything else happens. For example here's an signed byte variable, with data values from 0 to 255.

~~~
byte time(time=256);
     :scale_factor = 10.0; // double
     :valid_range = -10, 10; // int

data:

{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 
40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 
78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 
113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, -128, -127, -126, -125, -124, -123, -122, -121, -120, -119, -118, -117, -116, 
-115, -114, -113, -112, -111, -110, -109, -108, -107, -106, -105, -104, -103, -102, -101, -100, -99, -98, -97, -96, -95, -94, -93, -92, -91, -90, -89, 
-88, -87, -86, -85, -84, -83, -82, -81, -80, -79, -78, -77, -76, -75, -74, -73, -72, -71, -70, -69, -68, -67, -66, -65, -64, -63, -62, -61, -60, -59, 
-58, -57, -56, -55, -54, -53, -52, -51, -50, -49, -48, -47, -46, -45, -44, -43, -42, -41, -40, -39, -38, -37, -36, -35, -34, -33, -32, -31, -30, -29, 
-28, -27, -26, -25, -24, -23, -22, -21, -20, -19, -18, -17, -16, -15, -14, -13, -12, -11, -10, -9, -8, -7, -6, -5, -4, -3, -2, -1}
~~~

If you read that through the NetcdfDataset interface, which applies scale/offset and makes values outside of the valid range into NaN, you get:

~~~
double time(time=256);

data:
{0.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, 
NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, 
NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, 
NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, 
NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, 
NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, 
NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, 
NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, 
NaN, -100.0, -90.0, -80.0, -70.0, -60.0, -50.0, -40.0, -30.0, -20.0, -10.0}
~~~

as you can see, the valid_range = -10, 10 is first applied to the raw values, then the scale_factor is applied.

now if you add the _Unsigned = "true", and make the valid_range attribute values use unsigned values:

~~~
byte time(time=256);
     :_Unsigned = "true";
     :scale_factor = 10.0; // double
     :valid_range = 10, 240; // int
~~~

you get:

~~~
 {NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, 100.0, 110.0, 120.0, 130.0, 140.0, 150.0, 160.0, 170.0, 180.0, 190.0, 200.0, 210.0, 220.0, 230.0, 
240.0, 250.0, 260.0, 270.0, 280.0, 290.0, 300.0, 310.0, 320.0, 330.0, 340.0, 350.0, 360.0, 370.0, 380.0, 390.0, 400.0, 410.0, 420.0, 430.0, 440.0, 450.0, 
460.0, 470.0, 480.0, 490.0, 500.0, 510.0, 520.0, 530.0, 540.0, 550.0, 560.0, 570.0, 580.0, 590.0, 600.0, 610.0, 620.0, 630.0, 640.0, 650.0, 660.0, 670.0, 
680.0, 690.0, 700.0, 710.0, 720.0, 730.0, 740.0, 750.0, 760.0, 770.0, 780.0, 790.0, 800.0, 810.0, 820.0, 830.0, 840.0, 850.0, 860.0, 870.0, 880.0, 890.0, 
900.0, 910.0, 920.0, 930.0, 940.0, 950.0, 960.0, 970.0, 980.0, 990.0, 1000.0, 1010.0, 1020.0, 1030.0, 1040.0, 1050.0, 1060.0, 1070.0, 1080.0, 1090.0, 
1100.0, 1110.0, 1120.0, 1130.0, 1140.0, 1150.0, 1160.0, 1170.0, 1180.0, 1190.0, 1200.0, 1210.0, 1220.0, 1230.0, 1240.0, 1250.0, 1260.0, 1270.0, 1280.0, 
1290.0, 1300.0, 1310.0, 1320.0, 1330.0, 1340.0, 1350.0, 1360.0, 1370.0, 1380.0, 1390.0, 1400.0, 1410.0, 1420.0, 1430.0, 1440.0, 1450.0, 1460.0, 1470.0, 
1480.0, 1490.0, 1500.0, 1510.0, 1520.0, 1530.0, 1540.0, 1550.0, 1560.0, 1570.0, 1580.0, 1590.0, 1600.0, 1610.0, 1620.0, 1630.0, 1640.0, 1650.0, 1660.0, 
1670.0, 1680.0, 1690.0, 1700.0, 1710.0, 1720.0, 1730.0, 1740.0, 1750.0, 1760.0, 1770.0, 1780.0, 1790.0, 1800.0, 1810.0, 1820.0, 1830.0, 1840.0, 1850.0, 
1860.0, 1870.0, 1880.0, 1890.0, 1900.0, 1910.0, 1920.0, 1930.0, 1940.0, 1950.0, 1960.0, 1970.0, 1980.0, 1990.0, 2000.0, 2010.0, 2020.0, 2030.0, 2040.0, 
2050.0, 2060.0, 2070.0, 2080.0, 2090.0, 2100.0, 2110.0, 2120.0, 2130.0, 2140.0, 2150.0, 2160.0, 2170.0, 2180.0, 2190.0, 2200.0, 2210.0, 2220.0, 2230.0, 
2240.0, 2250.0, 2260.0, 2270.0, 2280.0, 2290.0, 2300.0, 2310.0, 2320.0, 2330.0, 2340.0, 2350.0, 2360.0, 2370.0, 2380.0, 2390.0, 2400.0, NaN, NaN, NaN, 
NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN, NaN}
~~~

#### Q: I want to convert a GRIB file to netCDF. How do I do that?

If you are converting the entire file, you can do it on the [command line](cdm_utility_programs.html).

You can do it from a Java program like this:

~~~
void convert(String datasetIn, String filenameOut, boolean wantNetcdf4) throws IOException {   

   NetcdfFileWriter.Version version = wantNetcdf4 ? NetcdfFileWriter.Version.netcdf4 : NetcdfFileWriter.Version.netcdf3;

   // open the original dataset
   NetcdfFile ncfileIn = ucar.nc2.dataset.NetcdfDataset.openFile(datasetIn, cancel);

   // copy it to a netCDF file
   FileWriter2 writer = new ucar.nc2.FileWriter2(ncfileIn, filenameOut, version, null);
   NetcdfFile ncfileOut = writer.write(cancel);

   // clean up
   if (ncfileOut != null) ncfileOut.close();
   ncfileIn.close();
~~~

Note that this can be used for any classic model CDM dataset, not just GRIB. So _datasetIn_ above can refer to an NcML file, an OPeNDAP URL, any of these [File Types](file_types.html), etc. See [here](dataset_urls.html) for more details.