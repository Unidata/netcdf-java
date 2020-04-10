---
title: Writing an IOSP - Details
last_updated: 2020-04-06
sidebar: netcdfJavaTutorial_sidebar
permalink: iosp_details.html
toc: false
---

## Writing an IOSP: Details

### Registering an IOSP

<b>_IOSPs_</b> are loaded by registering them with the <b>_NetcdfFile_</b> class:

~~~
   ucar.nc2.NetcdfFile.registerIOProvider(Class iospClass) throws IllegalAccessException, InstantiationException;

   ucar.nc2.NetcdfFile.registerIOProvider(String className) throws IllegalAccessException, InstantiationException, ClassNotFoundException;
~~~

When registering, an instance of the class will be created. This means that there must be a default constructor that has no arguments. Since there is no way to call any other constructor, the simplest thing is to not define a constructor for your class, and the compiler will add a default constructor for you. If for some reason you do define a constructor with arguments, you must also explicitly add a no-argument constructor.

If you don't have a default constructor, you will get an InstantiationException. When passing the className in as a String, you may get a ClassNotFoundException if your class cannot be found by the NetcdfFile ClassLoader (this almost always means that your classpath is wrong). An IllegalAccessException is thrown in case you do not have the rights to access the IOSP class.

You must register your IOSP before you open any files that it handles. If you contribute your IOSP and it becomes part of the CDM release, it will automatically be registered in the <b>_NetcdfFile_</B> class.

### IOSP Lifecycle and Thread Safety

An IOSP is registered by passing in the IOSP class. An object of that class is immediately instantiated and stored. This object is used when NetcdfFile queries all the IOSPs by calling <b>_isValidFile()_</b>. This makes the querying as fast as possible. <b>_Since there is only one IOSP object for the library, the isValidFile() method must be made thread-safe_</b>. To make it thread safe, <b>_isValidFile()_</b> must modify only local (heap) variables, not instance or class variables.

When an IOSP claims a file, a new IOSP object is created and open() is called on it. Therefore <b>_each dataset that is opened has its own IOSP instance assigned to it, and so the other methods of the IOSP do not need to be thread-safe_</b>. The NetcdfFile keeps a reference to this IOSP object. When the client releases all references to the NetcdfFile, the IOSP will be garbage collected.

### The isValidFile() Method

~~~
 // Check if this is a valid file for this IOServiceProvider. 
 public boolean isValidFile( ucar.unidata.io.RandomAccessFile raf) throws IOException;
~~~

The <b>_isValidFile()_</b> method must quickly and accurately determine if the file is one that the IOSP knows how to read. If this is done incorrectly, it will interfere with reading other file types. As described in the previous section, it must be thread safe. It must also not assume what state the RandomAccessFile is in. If the file is not yours, return false as quickly as possible. <b>_An IOException must be thrown only if the file is corrupted_</b>. Since its unlikely that you can tell if the file is corrupt for any file type, you should probably never throw an IOException.

#### Example 1:

~~~
public class Nexrad2IOServiceProvider implements IOServiceProvider {
  static public final String ARCHIVE2 = "ARCHIVE2";
  static public final String AR2V0001 = "AR2V000";
  
  public boolean isValidFile( RandomAccessFile raf) throws IOException (4) {
1)  raf.seek(0);
    byte[] b = new byte[8];
    raf.read(b);
2)  String test = new String( b);
3)  return test.equals( Level2VolumeScan.ARCHIVE2) || test.equals( Level2VolumeScan.AR2V0001) ;
  }
...
~~~

The IOSP wants to read starting at the first byte of the file. It must not assume that the file is already positioned there.
It reads in 8 bytes and converts to a String.
If it matches known patterns, return true.
The file is assumed bad if it can't even read the first 8 bytes of the file. Its hard to imagine a valid file of less than 8 bytes. Still, be careful of your assumptions.

#### Example 2:

~~~
  public boolean isValidFile( ucar.unidata.io.RandomAccessFile raf ) {
    try {
1)    raf.order( RandomAccessFile.BIG_ENDIAN );
      raf.seek( 0 );
2)    ucar.bufr.BufrInput bi = new ucar.bufr.BufrInput( raf );
      return bi.isValidFile();
3)  } catch (IOException ex) {
       return false;
    }
  }
~~~

1. The IOSP will read in numbers that it expects to be in big-endian format. It must not assume what mode the RandomAccessFile is in.
2. It creates a <b>_BufrInput_</b> object and delegates the work to it. Since this is a local instance, this is thread-safe. Creating new objects should be avoided when possible for speed, but sometimes it's necessary.
3. Catch the IOExceptions! Arguably, it would be better for the <b>_BufrInput_</b> class to return null, following the rule that _Exceptions should only be used in exceptional circumstances_. Getting passed a file that is not yours is not exceptional.

#### Example 3 (BAD!):

~~~
public class Grib1ServiceProvider implements IOServiceProvider {
 private Grib1Input scanner;
  private int edition;
  public boolean isValidFile(RandomAccessFile raf) {
    raf.seek(0);
    raf.order( RandomAccessFile.BIG_ENDIAN );
1)  scanner = new Grib1Input( raf );
2)  edition = scanner.getEdition();
    return (edition == 1);
  }
~~~

1. The IOSP creates a _Grib1Input_ object and delegates the work to it. This is an instance variable, and so <b>_this is not thread-safe_</b>.
2. The _edition_ variable is an instance variable, so <b>_this is not thread-safe_</b>.

The mistake might be because you want a scanner object and edition in the rest of the methods. Here's the right way to do this:

~~~
public class Grib1ServiceProvider implements IOServiceProvider {
 private Grib1Input scanner;
  private int edition;
  public boolean isValidFile(RandomAccessFile raf) {
    raf.seek(0);
    raf.order( RandomAccessFile.BIG_ENDIAN );
 Grib1Input scanner = new Grib1Input( raf );
    int edition = scanner.getEdition();
    return (edition == 1);
  }

  public void open(ucar.unidata.io.RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException; {
    raf.seek(0);
    raf.order( RandomAccessFile.BIG_ENDIAN );
    scanner = new Grib1Input( raf );
    edition = scanner.getEdition();
    ...
  }
~~~

The <b>_isValidFile()_</b> method creates local variables for everything it has to do. The <b>_open()_</b> method has to repeat that, but it is allowed to store instance variables that can be used in the rest of the methods, for the duration of the IOSP object.

### The open() method

~~~
  // Open existing file, and populate ncfile with it.
  public void open(ucar.unidata.io.RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException;
~~~

Once an IOSP returns true on <b>_isValidFile()_</b>, a new IOSP object is created and <b>_open()_</b> is called on it. The job of open is to examine the contents of the file and create Netcdf objects that expose all of the interesting information in the file. Sticking with the simple Netcdf-3 data model for now, this means populating the NetcdfFile object with _Dimension_, _Attribute_ and _Variable_ objects.

#### ucar.nc2.Attribute

An _Attribute_ is a (name, value) pair, where name is a String, and value is a 1D array of Strings or numbers. Attributes are thought of as <b>_metadata_<b> about your data. All attributes are read and kept in memory, so you should not put large data arrays in Attributes. You can add global attributes that apply to the entire file:

~~~
  ncfile.addAttribute(ncfile.getRootGroup(), new Attribute("Conventions", "CF-1.0"));
  ncfile.addAttribute(null, new Attribute("version", 42));
~~~

Or you can add Attributes that are contained inside a Variable, and apply only to that Variable:

~~~
  var.addAttribute( new Attribute("missing_value", Array.factory(new double[] {999.0, -999.0})));
~~~
#### ucar.nc2.Dimension

A Dimension describes the index space for the multidimension arrays of data stored in Variables. A Dimension has a String name and in integer length. In the Netcdf-3 data model, Dimensions are shared between variables, and stored globally.

~~~
 ncfile.addDimension(null, new Dimension("lat", 180, true));
 ncfile.addDimension(null, new Dimension("lon", 360, true));
~~~

#### ucar.nc2.Variable

The actual data is contained in Variables, which are containers for multidimension arrays of data. In the Netcdf-3 data model, Variables can have type DataType.BYTE, DataType.CHAR, DataType.SHORT, DataType.INT, DataType.FLOAT, or DataType.DOUBLE.

If a variable is <b>_unsigned_</b> (bytes, shorts or integer data types), you must add the <b>_Unsigned_</b> attribute:

~~~
   v.addAttribute(new Attribute("_Unsigned", "true"));
~~~

Here is an example creating a Variable of type short called "elevation", adding several attributes to it, and adding it to the NetcdfFile. The Dimensions lat and lon must already have been added. When setting Dimensions, the slowest-varying Dimension goes first (C/Java order).

~~~
 Variable elev = new Variable(ncfile, null, null, "elevation");
   elev.setDataType(DataType.SHORT);
   elev.setDimensions("lat lon");
   elev.addAttribute(new Attribute("units", "m"));
   elev.addAttribute(new Attribute("long_name", "digital elevation in meters above mean sea level"));
   elev.addAttribute(new Attribute("missing_value", (short) -9999));
   ncfile.addVariable(null, elev);
~~~

A special kind of Variable is a Coordinate Variable, which is used to name the coordinate values of a Dimension. A Variable has the same name as its single dimension. For example:

~~~
    Variable lat = new Variable(ncfile, null, null, "lat");
    lat.setDataType(DataType.FLOAT);
    lat.setDimensions("lat");
    lat.addAttribute(new Attribute("units", "degrees_north"));
    ncfile.addVariable(null, lat);
~~~

It is often convenient for IOSPs to set the data values of coordinate (or other) variables.

~~~
  Array data = Array.makeArray(DataType.FLOAT, 180, 90.0, -1.0);
  lat.setCachedData(data, false);
~~~

Here, <b>_Array.makeArray</b>_ is a convenience method that generates an evenly spaced array of length 180, starting at 90.0 and incrementing -1.0. That array is then cached in the Variable, and used whenever a client asks for data from the Variable. If a Variable has cached data, then <b>_readData()_</b> will never be called for it.

### The readData() method

~~~
  // Read data from a top level Variable and return a memory resident Array.
  public ucar.ma2.Array readData(ucar.nc2.Variable v2, Section section) throws java.io.IOException, ucar.ma2.InvalidRangeException;
~~~

When a client asks to read data from a Variable, either the data is taken from the Vaiable's data cache if it exists, or the <b>_readData_</b> method of the IOSP is called. The client may ask for all of the data, or it may ask for a _hyperslab_ of data described by the <b>_section_</b> parameter. The section contains a <b>_java.util.List_</b> of <b>_ucar.ma2.Range_</b> objects, one for each Dimension in the Variable, in order of the Variable's dimensions.

Here is an example, that assume the data starts at the start of the file, is in big-endian format, and is stored as a regular array of 16-bit integers on disk:

#### Example 1: Reading the entire Array

~~~
 public Array readData(Variable v2, Section wantSection) throws IOException, InvalidRangeException {
   raf.seek(0);
   raf.order(RandomAccessFile.BIG_ENDIAN);
   int size = (int) v2.getSize();
   short[] arr = new short[size];

   int count = 0;
   while (count < size) 
     arr[count++] = raf.readShort(); // copy into primitive array
   
   Array data = Array.factory(DataType.SHORT.getPrimitiveClassType(), v2.getShape(), arr);
   return data.section(wantSection.getRanges());
}
~~~

The RandomAccessFile reads 16-bit integers, advancing automatically. The Array.section() method creates a logical section of the data array, returning just the section requested.

For large arrays, reading in all of the data can be too expensive. If your data has a Regular Layout, you can use LayoutRegular helper object:

#### Example 2: Using ucar.nc2.iosp.LayoutRegular to read just the requested Section:

~~~
 public Array readData(Variable v2, Section wantSection) throws IOException, InvalidRangeException {
   raf.seek(0);
   raf.order(RandomAccessFile.BIG_ENDIAN);
   int size = (int) v2.getSize();
   int[] arr = new int[size];

   LayoutRegular layout = new LayoutRegular(0, v2.getElementSize(), -1, v2.getShape(), wantSection)
   while (layout.hasNext()) {
     Layout.Chunk chunk = layout.next();
     raf.seek(chunk.getSrcPos());
     raf.readInt(arr, (int) chunk.getDestElem(), chunk.getNelems()); // copy into primitive array
   }
   return Array.factory(DataType.INTEGER.getPrimitiveClassType(), v2.getShape(), arr);
}
~~~

#### Example 3: Storing Variable specific information in SPobject

The previous examples essentially assumed a single data Variable whose data starts at byte 0 of the file. Typically you want to store various kinds of information on a per-variable basis, to make it easy and fast to respond to the readData request. For example, suppose there were multiple Variable starting at different locations in the file. You might compute these file offsets during the open call, storing that and other info in a VarInfo object:

~~~
 private class VarInfo {
   long filePos;
   int  otherStuff;
 } 
~~~

~~~ 
public void open(RandomAccessFile raf, NetcdfFile ncfile, CancelTask cancelTask) throws IOException {    
  this.raf = raf;
  ...      
  Variable elev = new Variable(ncfile, null, null, "elevation");     
  ...
   
  VarInfo vinfo = new VarInfo();
  vinfo.filePos = calcPosition(); // figure out where the elevation Variable's data starts
  vinfo.otherStuff = 42;
  elev.setSPobject( vinfo);
  ...
}
~~~

Then use that in readData:

~~~
 public Array readData(Variable v2, Section wantSection) throws IOException, InvalidRangeException {
   VarInfo vinfo = (VarInfo) v2.getSPobject();
 
   raf.seek(vinfo.filePos);
   raf.order(RandomAccessFile.BIG_ENDIAN);
   ...
}
~~~

The <b>_setSPobject()_</b> and <b>_getSPobject()_</b> methods on the Variable are for the exclusive use of the IOSP. Use them in any way you need.

### The close() method

~~~
  // Close the file.
  public void close() throws IOException;
~~~

When the close method is called, the IOSP is responsible for releasing any system resources suce as File handles, etc. This usually just means calling close on the RandomAccessFile:

~~~
  public void close() throws IOException {
    raf.close();
  }
~~~
 
### Adding Coordinate System Information
Adding [Coordinate System](common_data_model_overview.html) information is the single most useful thing you can do to your datasets, to make them accessible to other programmers. As the IOSP writer, you are in the best position to understand the data in the file and correctly interpret it. You should, in fact, understand what the Coordinate Systems are at the same time you are deciding what the Dimension, Variables, and Attribute objects are.

Since there is no CoordinateSystem object directly stored in a netCDF file, CoordinateSystem information is encoded using a <a href="https://www.unidata.ucar.edu/software/netcdf/conventions.html" target="_blank">Convention</a> for adding Attributes, naming Variables and Dimensions, etc. in a standard way. The simplest and most direct way to add Coordinate Systems is to use the CDM [_Coordinate Attribute Conventions](coord_attribute_ex.html). Another approach is to follow an existing Convention, in particular the [CF Convention(http://www.cfconventions.org/){:target="_blank"}is an increasingly important one for gridded model data, and work is being done to make it applicable to other kinds of data.

When a client opens your file through the NetcdfFile interface, they see exactly what Dimension, Variables, and Attribute objects you have populated the NetcdfFile object with, no more and no less. When a client uses the NetcdfDataset interface in enhanced mode, the Coordinate System information is parsed by a [CoordSysBuilder](coord_system_builder.html) object, and Coordinate Axis, Coordinate System, and Coordinate Transform objects are created and made available through the NetcdfDataset API. In some cases, new Variables, Dimensions and Attributes may be created. Its very important that the IOSP writer follow an existing Convention and ensure that the Coordinate System information is correctly interpreted, particularly if you want to take advantage of the capabilities of the CDM Scientific Datatype Layer, such as serving the data through [WCS](https://docs.unidata.ucar.edu/tds/5.0/userguide/wcs_ref.html){:target="_blank"} or the Netcdf Subset Service.
