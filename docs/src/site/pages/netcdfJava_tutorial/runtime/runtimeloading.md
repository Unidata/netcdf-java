---
title: Runtime Loading
last_updated: 2019-09-14
sidebar: netcdfJavaTutorial_sidebar
permalink: runtime_loading.html
toc: false
---
## Runtime Loading

These are the various classes that can be plugged in at runtime:

### Register an IOServiceProvider

1) The recommended way is to use the [Service Provider](https://docs.oracle.com/javase/tutorial/ext/basics/spi.html)
mechanism and include your IOSP in a jar on the classpath, where it is dynamically loaded at runtime. In your
jar, include a file named **META-INF/services/ucar.nc2.iosp.IOServiceProvider** containing the
name(s) of your implementations, eg:

~~~
ucar.nc2.iosp.fysat.Fysatiosp
ucar.nc2.iosp.gini.Giniiosp
~~~

2) Alternatively, from your code, register your IOSP by calling:

~~~
ucar.nc2.NetcdfFile.registerIOProvider( String className);
~~~

In all cases your class must implement <b>_ucar.nc2.IOServiceProvider_</b>. 
When a <b>_NetcdfFile_</b> is opened, we loop through the <b>_IOServiceProvider_</b> classes and call
~~~
boolean isValidFile( ucar.unidata.io.RandomAccessFile raf)
~~~
on each, until one returns true. This method must be fast and accurate.

### Register a CoordSysBuilder:
~~~
ucar.nc2.dataset.CoordSysBuilder.registerConvention( String conventionName, String className);
~~~ 
The registered class must implement <b>_ucar.nc2.dataset.CoordSysBuilderIF_</b>. The NetcdfDataset is checked if it has a Convention attribute, and if so, it is matched by conventionName. If not, loop through the CoordSysBuilderIF classes and call

 boolean isMine(NetcdfFile ncfile) 
on each, until one returns true. If none are found, use the default _Coordinate Convention.

### Register a CoordTransBuilder:

 ucar.nc2.dataset.CoordTransBuilder.registerTransform( String transformName, String className);
The registered class must implement ucar.nc2.dataset.CoordTransBuilderIF. The Coordinate Transform Variable must have the transform name as one of its parameters.

### Register a FeatureDatasetFactory:
~~~
ucar.nc2.ft.FeatureDatasetFactoryManager.registerFactory( FeatureType featureType, String className);
~~~
The registered class must implement ucar.nc2.ft.FeatureDatasetFactory, see javadoc for that interface.

### Register a GRIB1 or GRIB2 Lookup Table (4.2 and before):
~~~
ucar.grib.grib1.GribPDSParamTable.addParameterUserLookup( String filename);
ucar.grib.grib2.ParameterTable.addParametersUser( String filename);
~~~  
### Register a GRIB1 table (4.3):
~~~
ucar.nc2.grib.grib1.tables.Grib1ParamTables.addParameterTable(int center, int subcenter, int tableVersion, String tableFilename);
~~~
This registers a single table for the given center/subcenter/version.
See <a href="grib_tables.html">GribTables</a> for more information about parameter tables.
GRIB2 table handling is still being developed.
Register a GRIB1 lookup table (4.3):
 ucar.nc2.grib.grib1.tables.Grib1ParamTables.addParameterTableLookup(String lookupFilename);

This registers one or more tables for different center/subcenter/versions.
See <a href="grib_tables.html">GribTables</a> for more information about lookup tables.
GRIB2 table handling is still being developed.

### Register a BUFR Table lookup:
~~~
ucar.nc2.iosp.bufr.tables.BufrTables.addLookupFile( String filename) throws throws FileNotFoundException;
~~~
The file must be a <a href="bufr_tables.html">BUFR table lookup file</a>.

## Runtime Configuration

Instead of calling the above routines in your code, you can pass the CDM library an XML configuration file. Note that your application must call <b>_ucar.nc2.util.xml.RuntimeConfigParser.read()_</b>.

The configuration file looks like this:
~~~
 <?xml version="1.0"?>
 <nj22Config>

1) <ioServiceProvider  class="edu.univ.ny.stuff.FooFiles"/>
2) <coordSysBuilder convention="foo" class="test.Foo"/>
3) <coordTransBuilder name="atmos_ln_sigma_coordinates" type="vertical" class="my.stuff.atmosSigmaLog"/>
4) <featureDatasetFactory featureType="Point" class="gov.noaa.obscure.file.Flabulate"/>
5) <gribParameterTable edition="1" center="58" subcenter="-1" version="128">C:/grib/tables/ons288.xml</gribParameterTable>
6) <gribParameterTableLookup edition="1">C:/grib/tables/ncepLookup.txt</gribParameterTableLookup>
7) <table type="GRIB1" filename="/grib/tables/userlookup.lst"/>
8) <table type="GRIB2" filename="/grib/tables/grib2userparameters" />
9) <bufrtable filename="C:/my/files/lookup.txt" />
10)<grib1Table strict="false"/>
11)<Netcdf4Clibrary>
     <libraryPath>/usr/local/lib</libraryPath>
     <libraryName>netcdf</libraryName>
     <useForReading>false</useForReading>
  </Netcdf4Clibrary>
</nj22Config>
~~~

1. Loads an <b>_IOServiceProvider_</b> with the given class name
2. Loads a <b>_CoordSysBuilderIF_</b> with the given class name, which looks for the given <b>_Convention_</b> attribute value.
3. Loads a <b>_CoordTransBuilderIF_</b> with the given class name, which looks for the given <b>_transformName_</b> in the dataset. The type must be vertical or projection.
4. Loads a <b>_FeatureDatasetFactory_</b> with the given class name which open <b>_FeatureDatasets_</b> of the given featureType.
5. Load a <a href="grib_tables.html">GRIB-1 parameter table</a> (as of version 4.3)
6. Load a <a href="grib_tables.html">GRIB-1 parameter table lookup</a> (as of version 4.3)
7. Load a <a href="grib_tables.html">GRIB-1 parameter lookup table</a> (versions < 4.3, deprecated)
8. Load a <a href="grib_tables.html">GRIB-2 parameter lookup table</a> (versions < 4.3, deprecated)
9. Load a <a href="bufr_tables.html">BUFR table lookup</a> file.
10. Turn <a href="grib_tables.html#strict">strict GRIB1 table handling</a> off.
11. Configure how the <a href="netcdf4_c_library.html">NetCDF-4 C library</a> is discovered and used.
    * <b>_libraryPath_</b>: The directory in which the native library is installed.
    * <b>_libraryName_</b>: The name of the native library. This will be used to locate the proper .DLL, .SO, or .DYLIB file within the <b>_libraryPath_</b> directory.
    * <b>_useForReading_</b>: By default, the native library is only used for writing NetCDF-4 files; a pure-Java layer is responsible for reading them. However, if this property is set to true, then it will be used for reading NetCDF-4 (and HDF5) files as well.
    
There are several ways pass the Runtime Configuration XML to the CDM library. From your application, you can pass a <b>_java.io.InputStream_</b> (or JDOM element) to <b>_ucar.nc2.util.xml.RuntimeConfigParser_</b>, as in the following examples:

~~~
  // Example 1: read from file
  StringBuffer errlog = new StringBuffer();
  FileInputStream fis = new FileInputStream( filename);   
  ucar.nc2.util.RuntimeConfigParser.read( fis, errlog);
  System.out.println( errlog);

  // Example 2: read from resource
  ClassLoader cl = this.getClassLoader();
  InputStream is = cl.getResourceAsStream("resources/nj22/configFile.xml");
  ucar.nc2.util.RuntimeConfigParser.read( is, errlog);

  // Example 3: extract JDOM element from a larger XML document:
  Document doc;
  SAXBuilder saxBuilder = new SAXBuilder();
  try {
    doc = saxBuilder.build(filename);
  } catch (JDOMException e) {
    throw new IOException(e.getMessage());
  }
  Element root = doc.getRootElement();
  Element elem = root.getChild("nj22Config");
  if (elem != null)
    ucar.nc2.util.RuntimeConfigParser.read( elem, errlog);
~~~
    
For example, the ToolsUI application allows you to specify this file on the command line with the <b>_-nj22Config_</b> parameter:

~~~
   public void main(String[] args) {

      for (int i = 0; i < args.length; i++) {
        if (args[i].equalsIgnoreCase("-nj22Config") && (i < args.length-1)) {
          String runtimeConfig = args[i+1];
          i++;
          try {
            StringBuffer errlog = new StringBuffer();

    FileInputStream fis = new FileInputStream( runtimeConfig);
 ucar.nc2.util.xml.RuntimeConfigParser.read( fis, errlog);
            System.out.println( errlog);
          } catch (IOException ioe) {
            System.out.println( "Error reading "+runtimeConfig+"="+ioe.getMessage());
          }
        }
      }
    ...
~~~
If none is specified on the command line, it will look for the XML document in <b>_$USER_HOME/.unidata/nj22Config.xml_</b>.

 
## Runtime Loading of IOSP using javax.imageio.spi.ServiceRegistry

(as of version 4.3.9)

You can create an IOSP and have it discovered at runtime automatically.

Your class must implement <b>_ucar.nc2.iosp.IOServiceProvider_</b>
Create a JAR file with a <b>_services_</b> subdirectory in the META-INF directory. This directory contains a file called <b>_ucar.nc2.iosp.IOServiceProvider_</b>, which contains the name(s) of the implementing class(es). For example, if the JAR file contained a class named com.mycompany.MyIOSP, the JAR file would contain a file named:
~~~
META-INF/services/ucar.nc2.iosp.IOServiceProvider 
~~~
containing the line:
~~~
com.mycompany.MyIOSP
~~~
See:
<a href="https://docs.oracle.com/javase/7/docs/api/javax/imageio/spi/ServiceRegistry.html" target="_blank">https://docs.oracle.com/javase/7/docs/api/javax/imageio/spi/ServiceRegistry.html</a>

(thanks to Tom Kunicki at USGS for this contribution)