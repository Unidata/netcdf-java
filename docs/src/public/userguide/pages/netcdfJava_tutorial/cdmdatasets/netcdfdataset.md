---
title: The Common Data Model
last_updated: 2019-06-27
sidebar: netcdfJavaTutorial_sidebar 
permalink: netcdf_dataset.html
toc: false
---
## Tutorial: Working with NetcdfDataset

The <b>_ucar.nc2.dataset_</b> classes are an extension to the NetCDF API which provide support for

* processing standard attributes for scale/offset and missing data
* general and georeferencing coordinate systems
* the NetCDF Markup Language (NcML)
* remote access to OpenDAP, ADDE and THREDDS datasets.

NcML is an XML document format that allows you to create "virtual" netCDF datasets, including combining multiple netCDF files into one dataset. A later section of the tutorial explains NcML and how to create virtual datasets.

#### Using NetcdfDataset.openFile to open a NetcdfFile

The preferred way to open a NetcdfFile is through the </b>_NetcdfDataset.openFile_</b> factory method:

~~~
  String filename = "http://thredds.ucar.edu/thredds/dodsC/model/NCEP/GFS/CONUS_80km/GFS_CONUS_80km_20061019_0000.grib1";
  NetcdfFile ncfile = null;
  try {
    ncfile = NetcdfDataset.openFile(filename, null);
    process( ncfile);
  } catch (IOException ioe) {
    log("trying to open " + filename, ioe);
  } finally { 
    if (null != ncfile) try {
      ncfile.close();
    } catch (IOException ioe) {
      log("trying to close " + filename, ioe);
    }
  }
~~~

NetcdfDataset.openFile does the following:

* Opens an OPeNDAP remote dataset, if the location is a URL that starts with <b>_http:_</b> , <b>_dods:_</b> or <b>_dap4:_</b> (version 4.4+)
* Opens a <a href="cdmremote.html">CdmRemote</a> dataset, if the location is a URL that starts with <b>_cdmremote_</b>:
* Opens a THREDDS dataset, if the location look like <b>_thredds:<catalog>#<datasetId>_</b>
* Opens an NcML dataset, if the location ends with <b>_.xml_</b> or <b>_.ncml_</b> , or its a URL starting with <b>_ncmdl_</b>: (version 4.4+)
* Otherwise, it calls <b>_NetcdfFile.open_</b>, which handles local file or HTTP access to any CDM file.

#### Using NetcdfDataset.openDataset to open an enhanced NetcdfDataset

When you want the Netcdf-Java library to deal with missing values and scale/offset unpacking, and to identify Coordinate Systems, you should use the NetcdfDataset.openDataset_</b> factory call, for example:

~~~
  String filename = "http://thredds.ucar.edu/thredds/dodsC/model/NCEP/GFS/CONUS_80km/GFS_CONUS_80km_20061019_0000.grib1";
  NetcdfDataset ncd = null;
  try {
    ncd = NetcdfDataset.openDataset(filename);
    process( ncd);
  } catch (IOException ioe) {
    log("trying to open " + filename, ioe);
  } finally { 
    if (null != ncd) try {
      ncd.close();
    } catch (IOException ioe) {
      log("trying to close " + filename, ioe);
    }
  }
~~~

Note that <b>_NetcdfDataset_</b> is a subclass of <b>_NetcdfFile_</b>, and so can be used wherever a NetcdfFile is used. <b>_NetcdfDataset.openDataset_</b> does the following:

* Calls <b>_NetcdfDataset.openFile_</b>, and wraps the returned <b>_NetcdfFile_</b> in a <b>_NetcdfDataset_</b> if necessary.
* It processes missing values and scale/offset attributes, modifying <b>_Variable_</b> data types if necessary
* It calls the appropriate <b>_Coordinate System Builder_</b> class to identify the coordinate systems and populate the Coordinate objects.

#### Packed data variables and missing values

When you open a NetcdfDataset in <b>_enhanced mode_</b> (the default), any Variables that have the attributes <b>_scale_factor_</b> and/or <b>_add_offset_</b> are considered to be packed data <b>_Variables_</b>, whose data should be converted with the formula:

~~~
 unpacked_data_value = packed_data_value * scale_factor + add_offset
~~~

usually the packed data type is byte or short, and the unpacked type is float or double, so the data type of the packed data Variable is promoted to float or double.
Missing data is indicated by the <b>_valid_min_</b>, <b>_valid_max_</b>, <b>_valid_range_</b>, <b>_missing_value_</b> or <b>__FillValue_</b> attributes. When a Variable has any of these attributes, the <b>_VariableDS.hasMissing()_</b> method returns true. You can test for missing values with the <b>_VariableDS.isMissing( value)_</b> method.

To open a NetcdfDataset in enhanced mode, toggle the enhance button {% include inline_image.html file="netcdf-java/tutorial/cdmdatasets/enhanceButton.jpg" alt="Enhance button" %} on the ToolsUI Viewer tab to ON.

#### Coordinate Systems

NetcdfDataset will try to identify the Coordinate Systems in the dataset by calling a CoordSystemBuilder class that knows how to interpret the Conventions for that dataset. The information is placed in Coordinate objects that follow this abstract model (see the javadoc for the specifics):

{% include image.html file="netcdf-java/tutorial/coordsystems/CoordSys.png" alt="Tools UI Coord Sys" caption="" %}

To write your Coordinate System Builder, see here. To see the list of CoordinateBuilder classes, look at the source code in the <b>_ucar.nc2.dataset.conv_</b> package.

When writing netCDF files, we recommend using the Climate and Forcast (CF) Convention if possible. When an IOSP reads in a non-netCDF file, it should choose a Convention to use to encode the Coordinate Systems.

#### Using ToolsUI

You can use ToolsUI <b>_CoordSys_</b> Tab to view the Coordinate Systems that have been constructued. This consists of 3 tables that show the data variables, the coordinate systems, and the coordinate axes.

{% include image.html file="netcdf-java/tutorial/cdmdatasets/TUIcoordSys.jpg" alt="Tools UI Coord Sys" caption="" %}

#### Advanced Use of NetcdfDataset (version 4.2+)

The following is applicable to version 4 of the Netcdf-Java library. Netcdf-Java version 2.2 effectively has only two enhance modes, All and None.

#### NetcdfDataset.Enhance

The kind of enhancements made when a NetcdfDataset is opened is described by <b>_NetcdfDataset.Enhance, an enumerated type with these possible values:

* <b>_ScaleMissing : process scale/offset/missing attributes, and convert data_</b>
* <b>_ScaleMissingDefer : calculate scale/offset/missing info, but dont automatically convert data._</b>
* <b>_CoordSystems : just add coordinate systems_</b>
* <b>_ConvertEnums: convert enums to Strings_</b>
 
When using the <b>_ScaleMissing_</b> enhance mode, scale/offset/missing attributes are processed when the dataset is opened, and the datatype of a Variable is promoted if necessary to match the unpacked data type. Data is automatically converted when read.

When using the <b>_ScaleMissingDefer_</b> enhance mode, scale/offset/missing attributes are processed when the dataset is opened, but the datatype of a Variable is NOT promoted, and data is not converted. After reading data, you can convert the entire Array with VariableEnhanced.convertArray(Array data), or convert single values with the convertScaleOffsetMissing methods, eg VariableEnhanced.convertScaleOffsetMissing(byte value).

When using </b>_CoordSystems_</b> enhance mode, CoordSysBuilder is called to populate the coordinate system objects in the NetcdfDataset when the dataset is opened.

When using </b>_ConvertEnums_</b> enhance mode, Variables of type <b>_enum_</b> are promoted to String types and data is automatically converted using the EnumTypedef objectss, which are maps of the stored integer values to String values.

The enhancement of a dataset can be controlled by passing in a Set of Enhance to </b>_NetcdfDataset_</b>.openDataset(). The default enhance mode is

 Set<Enhance> EnhanceAll = Collections.unmodifiableSet(EnumSet.of(Enhance.ScaleMissing, Enhance.CoordSystems, Enhance.ConvertEnums));
and can be changed through NetcdfDataset.setDefaultEnhanceMode(Set<Enhance> mode).

The simplest factory method, _NetcdfDataset.openDataset( location)_, uses the default enhance mode. Other factory methods with a boolean enhance parameter, such as NetcdfDataset.openDataset(String location, boolean enhance, CancelTask cancelTask) use the default enhance mode if enhance is true, and EnhanceMode.None if enhance is false. Other classes, such as GridDataset, also use the default enhance mode.

#### Advanced options when opening

The most general factory method for opening NetcdfDataset allows one to explicitly set the EnhanceMode:

NetcdfDataset openDataset(String location, Set<Enhance> enhanceMode, int buffer_size, CancelTask cancelTask, Object spiObject);
One can also set the buffer size used for reading data, pass in a CancelTask object to allow user cancelling, and pass an arbitrary object to the IOServiceProvider that handles the dataset. These last 3 parameters correspond to the ones in the similar factory method for NetcdfFile:

~~~
 NetcdfFile openFile(String location, int buffer_size, CancelTask cancelTask, Object spiObject);
~~~
  
#### Caching NetcdfDataset and NetcdfFile

Advanced applications like servers might want to enable the caching of NetcdfDataset and NetcdfFile objects in memory, for performance. Caching is safe to use in a multithreaded environment such as a servlet container like Tomcat. Caching keeps resources such as file handles open, and so cache sizes should be carefully considered.

To enable caching, you must first call

 NetcdfDataset.initNetcdfFileCache(int minElementsInMemory, int maxElementsInMemory, int period);
where minElementsInMemory are the number of objects to keep in the cache when cleaning up, maxElementsInMemory triggers a cleanup if the cache size goes over it, and period specifies the time in seconds to do periodic cleanups.

One then calls the <b>_acquireFile()_</b> or <b>_acquireDataset_</b> factory methods instead of openFile() and openDataset. For example:

~~~
  NetcdfDataset.initNetcdfFileCache(100,200,15*60); // on application startup
  ...

  NetcdfFile ncfile = null;
  try {
    ncfile = NetcdfDataset.acquireFile(location, cancelTask);
    ...
  } finally {
    if (ncfile != null) ncfile.close();
  }

  ...

  NetcdfDataset.shutdown();  // when terminating the application
~~~

Note that when done with the file, the close() method is called as usual. Instead of actually closing the file, it is left in the cache for subsequent acquiring.

Note also that calling <b>_NetcdfDataset.shutdown_</b> is crucial for terminating background threads that otherwise can prevent process termination.