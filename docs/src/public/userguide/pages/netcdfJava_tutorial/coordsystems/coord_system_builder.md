---
title: Coordinate System Builder
last_updated: 2019-07-22
sidebar: netcdfJavaTutorial_sidebar
permalink: coord_system_builder.html
toc: false
---
## Writing your own Java class to identify Coordinate Systems
### Overview

In order to use a dataset at the scientific datatype layer, the dataset's coordinate systems must first be identified. This is done by an implementation of <b>_ucar.nc2.dataset.CoordSysBuilderIF_</b> whose job is to examine the contents of the dataset and create coordinate system objects that follow this object model:

{% include image.html file="netcdf-java/tutorial/coordsystems/CoordSys.png" alt="Coord Sys Object Model" caption="" %}

For more details, see the <a href="common_data_model_overview.html">CDM Object Model</a>.

A CoordSysBuilderIF class must be created for each type of dataset that encodes their coordinate systems differently. This obviously is burdensome, and data providers are encouraged to use <a href="http://www.unidata.ucar.edu/software/netcdf/docs/conventions.html" target="_blank">existing Conventions</a> for writing their datasets. If those are inadequate, then the next best thing is to define and document a new Convention in collaboration with others with similar needs. If you do so, read <a href="http://www.unidata.ucar.edu/software/netcdf/docs/BestPractices.html" target="_blank">Writing NetCDF Files: Best Practices</a>, look at other Convention examples, and get feedback form others before committing to it. Send us a URL to your documentation, and we will add it to the <a href="http://www.unidata.ucar.edu/software/netcdf/docs/conventions.html" target="_blank">NetCDF Conventions page</a>.

The steps to using your CoordSysBuilderIF class in the Netcdf-Java library:

Write a class that implements <b>_ucar.nc2.dataset.CoordSysBuilderIF_</b>, such as by subclassing <b>_ucar.nc2.dataset.CoordSysBuilder_</b>.
1. Add the class to your classpath.
2. From your application, call ucar.nc2.dataset.CoordSysBuilder.registerConvention( String conventionName, Class c). This is called "plugging in" your code at runtime.
3. When you open the dataset in enhanced mode, eg by calling
~~~
       NetcdfDataset.openDataset(String location, boolean enhance, ucar.nc2.util.CancelTask cancelTask);
~~~
      
an instance of your class will be called to add coordinate system objects to the NetcdfDataset.

The Coordinate System objects are then available through the classes in the <b>_ucar.nc2.dataset_</b> package, for example:

~~~
ucar.nc2.dataset.VariableDS:
  public List getCoordinateSystems();

ucar.nc2.dataset.CoordinateSystem:
  public List getCoordinateAxes();
  public List getCoordinateTransforms();

ucar.nc2.dataset.CoordinateAxis:
  public List getAxisType();

ucar.nc2.dataset.CoordinateTransform:
  public List getParameters();
  public List getTransformType();
~~~
  
## Writing a CoordSysBuilderIF class

These are the steps taken by CoordSysBuilder to add Coordinate Systems:

1. Identify which subclass should be used.
2. Create a new object of that class.
3. Call <b>_augmentDataset( netcdfDataset, cancelTask)_</b> to make any changes to the dataset (add attributes, variables, etc).
4. Call <b>_buildCoordinateSystems( netcdfDataset)_<b/> to add the coordinate system objects.

Your class must implement this interface:

~~~
public interface CoordSysBuilderIF {
  public void setConventionUsed( String convName);
  public void augmentDataset( NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException;
  public void buildCoordinateSystems( NetcdfDataset ncDataset);
  public void addUserAdvice( String advice);
}
~~~

You can override the <b>_buildCoordinateSystems()_</b> method and completely build the coordinate system objects yourself. However, its usually easier to take advantage of the code in the <b>_CoordSysBuilder_</b> superclass, which translates standard <b>__Coordinate attributes_</b> into coordinate system objects. The job of the subclass may then reduce to adding these _Coordinate attributes to the file in the <b>_augmentDataset()_</b> method. The subclass may also need to create and add new Coordinate Variables to the file, and/or to create Coordinate Transforms. Examples of existing <b>_CoordSysBuilder_</b> subclasses are in the <b>_ucar.nc2.dataset.conv_<b> package.

The <b>_ucar.nc2.dataset.CoordSysBuilder_</b> class uses the " <b>__Coordinate attributes_</b>" ("underscore Coordinate attributes", described fully <a href="coord_attr_conv.html">here</a>) to create Coordinate System objects. An attribute that starts with an underscore is a "system attribute", which usually implies some special processing or behavior within the NetCDF library (both C and Java).

If you are subclassing ucar.nc2.dataset.CoordSysBuilder, you can ignore the <b>_setConventionUsed_</b> and <b>_addUserAdvice_</b> methods and let the superclass handle them. If not, you can just implement dummy methods.

The ToolsUI application has a <b>_CoordSys_</b> tab that is designed to help with the process of building coordinate systems. Open up your dataset in that tab, and 3 tables are presented: The data variables, the cooordinate systems, and the coordinate axes. The <b>_Info button_<b> (top right) will show various information from the <b>_CoordSysBuilder_</b> class that was used for the dataset.

### Identifying which datasets your class should operate on

If your datasets use the global attribute <b>_Convention_</b>, then you only need to pass in the value of that attribute into <b>_CoordSysBuilder.registerConvention(String conventionName, Class c)_</b>, and you do not need to implement the isMine() method.

Otherwise, your class must implement a static method <b>_isMine()_</b> that returns true when it is given a dataset that it knows how to handle. For example:

~~~
  public static boolean isMine( NetcdfFile ncfile) {
    String s =  ncfile.findAttValueIgnoreCase(null, "sensor_name", "none");
    return s.equalsIgnoreCase("CRAFT/NEXRAD");
  }
~~~
  
looks to see if the global attribute <b>_sensor_name_</b> has the value <b>_CRAFT/NEXRAD_</b>. <b>_Its important that the isMine() method be efficient_</b>, ideally using only the dataset metadata (attributes, variable names, etc) rather than having to do any data reading.

### Adding Attributes to the Dataset

For the simple case where you only need to add attributes to the file, you might do it as in this example:

~~~
  protected void augmentDataset( NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException {
    this.conventionName = "ATDRadar";

    Variable time = ds.findVariable("time");
    time.addAttribute( new Attribute("_CoordinateAxisType", "Time"));

    // etc
  }
~~~
  
You may find it easier to do the same thing using an <a href="ncml_overview.html">NcML</a> file, for example:

~~~
  protected void augmentDataset( NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException {
    this.conventionName = "ATDRadar";
    NcMLReader.wrapNcML(
  ncDataset, "file:/MyResource/ATDRadar.ncml", cancelTask);
  }
~~~
  
The </b>_NcMLReader.wrapNcML()_</b> method wraps a NetcdfDataset in an NcML file, making whatever modifications are specified in the NcML file. You pass in the URL location of the NcML to use, typically a local file as above, but it may also be a remote access over <b>_http_</b>. Alternatively, you could add the <b>_/MyResource_</b> directory to your classpath, and call this variation:

~~~
 NcMLReader.wrapNcMLresource( ncDataset, "ATDRadar.ncml", cancelTask);
~~~
 
The <b>_NcMLReader.wrapNcMLresource()_</b> looks for the NcML document by calling <b>_Class.getResource()_</b>. The example NcML file might look like:

~~~
<?xml version='1.0' encoding='UTF-8'?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
 <attribute name="Conventions" value="ATDRadar"/>
  <variable name="latitude">
  <attribute name="_CoordinateAxisType" value="Lat" />
 </variable>
  <variable name="longitude">
  <attribute name="_CoordinateAxisType" value="Lon" />
 </variable>
  <variable name="altitude">
  <attribute name="_CoordinateAxisType" value="Height" />
  <attribute name="_CoordinateZisPositive" value="up" />
 </variable>
  <variable name="time">
  <attribute name="_CoordinateAxisType" value="Time" />
 </variable>
</netcdf>
~~~

The NcML adds the appropriate <b>__CoordinateAxisType_</b> attribute to existing Coordinate Axes. Because the data variables all use coordinate variables, implicit Coordinate System objects are created and assigned. There is no need for Coordinate Transforms because all the coordinates are reference coordinates (lat, lon, height). <a href="http://www.unidata.ucar.edu/software/netcdf/ncml/" target="_blank">Here</a> is complete info on NcML.

If all you need to do is wrap the dataset in NcML, and the dataset already has a Convention attribute in it (before it is wrapped), then you can simply register the NcML directly, without having to write any code. For this, you use:

~~~
 CoordSysBuilder.registerNcML( String conventionName, String ncmlLocation); 
~~~ 

### Adding Coordinate Axes to the Dataset

When a Coordinate Axis is missing, you must add it. You can do this programatically or through an NcML file, for example:

~~~
  <variable name="latitude" shape="row" type="double">
    <attribute name="long_name" value="latitide coordinate" />
    <attribute name="units" value="degrees_north" />
    <attribute name="_CoordinateAxisType" value="Lat" />
    <values start="90.0" incr="5.0" />
  </variable>
~~~
  
creates a new coordinate axis variable, and gives it evenly spaced values. You can also enumerate the values:

~~~
  <values>90.0 88.3 72.6 66.9</values>
~~~
  
When the values must be computed, then you need to do this programatically, for example:

~~~
 protected void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) throws IOException {
    this.conventionName = "Zebra";
(1) NcMLReader.wrapNcMLresource( ds, CoordSysBuilder.resourcesDir+"Zebra.ncml", cancelTask);

    // the time coord var is created in the NcML
    // set its values = base_time + time_offset(time)
    Dimension timeDim = ds.findDimension("time");
    Variable base_time = ds.findVariable("base_time");
    Variable time_offset = ds.findVariable("time_offset");
(2) Variable time = ds.findVariable("time");
    Attribute att = base_time.findAttribute("units");
    String units = (att != null) ? att.getStringValue() : "seconds since 1970-01-01 00:00 UTC";
(3) time.addAttribute( new Attribute("units", units));

    Array data;
    try {
(4)   double baseValue = base_time.readScalarDouble();
(5)   data = time_offset.read();
      IndexIterator iter = data.getIndexIterator();
      while (iter.hasNext()) {
(6)     iter.setDoubleCurrent( iter.getDoubleNext() + baseValue);
(7)     if ((cancelTask != null) && cancelTask.isCancel()) return;
      }
     } catch (java.io.IOException ioe) {
(8)  parseInfo.append("ZebraConvention failed to create time Coord Axis for "+ ds.getLocation()+"\n"+ioe+"\n");
     return;
    }
(9) time.setCachedData( data, true);
(10)ds.finish();
}
~~~

1. Its convenient to wrap the dataset in NcML, even when you also have to do some programming. For one thing, you can change the NcML file without recompiling.
2. The time coordinate is created in the NcML file, and we will set its values here, based on other data in the file
3. Set time coordinate units are set to be the same as the units on the base_time variable.
4. Read in the (scalar) base_time.
5. Read in the time_offset array.
6. Add the baseValue to each value of the time_offset.
7. For potentially long running calculations, you should check to see if the user has cancelled, and return ASAP.
8. Error message if theres an excception.
9. Set the data values of the time coordinate to the computed values.
10. When adding new variables to a dataset, you must call finish() when all done.

### Identifying Coordinate Axis Types

Another simple case to handle is when you are using Coordinate Variables for all data variables. Coordinate Variables are 1D variables with the same name as their dimension, which encode the coordinate values for that dimension. In that case, you only need to identify the Coordinate Axes types, which you do by overriding the <b>_getAxisType()_</b> method. This will pass in all variables that have been identified as coordinate axes, and your job is to return theier AxisType, if they have one:

~~~
protected AxisType getAxisType( NetcdfDataset ncDataset, VariableEnhanced v) {
  String unit = v.getUnitsString();
  if (unit == null)
    return null;
  if ( unit.equalsIgnoreCase("degrees_east") ||
   unit.equalsIgnoreCase("degrees_E") ||
   unit.equalsIgnoreCase("degreesE") ||
   unit.equalsIgnoreCase("degree_east") ||
   unit.equalsIgnoreCase("degree_E") ||
   unit.equalsIgnoreCase("degreeE"))
     return AxisType.Lon;
  
  if ( unit.equalsIgnoreCase("degrees_north") ||
    unit.equalsIgnoreCase("degrees_N") ||
    unit.equalsIgnoreCase("degreesN") ||
    unit.equalsIgnoreCase("degree_north") ||
    unit.equalsIgnoreCase("degree_N") ||
    unit.equalsIgnoreCase("degreeN"))
      return AxisType.Lat;
      
  if (SimpleUnit.isDateUnit(unit) || SimpleUnit.isTimeUnit(unit))
    return AxisType.Time;
 
    // look for other z coordinate
  if (SimpleUnit.isCompatible("m", unit))
    return AxisType.Height;
  if (SimpleUnit.isCompatible("mbar", unit))
    return AxisType.Pressure;
  if (unit.equalsIgnoreCase("level") || unit.equalsIgnoreCase("layer") || unit.equalsIgnoreCase("sigma_level"))
    return AxisType.GeoZ;
   
  String positive = ncDataset.findAttValueIgnoreCase((Variable) v, "positive", null);
  if (positive != null) {
    if (SimpleUnit.isCompatible("m", unit))
      return AxisType.Height;
    else
      return AxisType.GeoZ;
  }
  return null;
}
~~~

### Creating Coordinate Transformations

A more complex task is to create Coordinate Transforms, which map your coordinates to reference coordinates, such as lat/lon. A Coordinate Transform is typically represented by a <b>_Coordinate Transform Variable_</b>, which may be a dummy variable (ie has no data in it), and whose attributes document the meaning and specify any needed parameters for it. You can create arbitrary transforms by creating <b>_ucar.nc2.dataset.CoordinateTransform_</b> objects, which your code will have access to when it opens a NetcdfDataset.

However, for your Transform to be used by the Netcdf Java library and standard applications built on top of it, the <b>_CoordinateTransform_</b> must have a reference to a <b>_ucar.unidata.geoloc.Projection_</b> or a <b>_ucar.unidata.geoloc.vertical.VerticalTransform_</b> object which knows how to do the actual mathematical transformation. The Netcdf-Java library has a number of these, mostly following the CF-1.0 specification (Appendix F for projections, Appendix D for vertical transforms). You can also <a href="coord_transform.html">write your own implementation</a> and add them at run time.

For this lesson, we will concentrate on what your CoordSysBuilder needs to do to use an existing standard or user written Projection or VerticalTransform class.

You can create the Coordinate Transform objects yourself, by overriding the <b>_makeCoordinateTransforms()_</b> and <b>_assignCoordinateTransforms()_</b> methods in CoordSysBuilder. Much easier is to use the existing machinery and create a <b>_Coordinate Transform Variable_</b> which represents the parameters of the transform in a way recognized by a <b>_CoordTransBuilder_</b> class.

Here is an example of one way to do that:

~~~
 public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) throws IOException {
   // read global parameters
1) double lat_origin = findAttributeDouble( ds, "LAT0");
   double lon_origin = findAttributeDouble( ds, "LON0");
   double scale = findAttributeDouble( ds, "SCALE");
   if (Double.isNaN(scale)) scale = 1.0; 

2) VariableDS v = new VariableDS( ds, null, null, "ProjectionPS", DataType.CHAR, "", null, null);
   v.addAttribute( new Attribute("grid_mapping_name", "polar_stereographic"));
   v.addAttribute( new Attribute("straight_vertical_longitude_from_pole", lon_origin));
   v.addAttribute( new Attribute("latitude_of_projection_origin", lat_origin));
   v.addAttribute( new Attribute("scale_factor_at_projection_origin", scale)); 
   
3) v.addAttribute( new Attribute(_Coordinate.TransformType, TransformType.Projection.toString());
4) v.addAttribute( new Attribute(_Coordinate.AxisTypes, "GeoX GeoY");
   // fake data
5) Array data = Array.factory(DataType.CHAR.getPrimitiveClassType(), new int[] {}, new char[] {' '});
   v.setCachedData(data, true);
6) ds.addVariable(v);
   ds.finish();
}
~~~

1. Read the projection values that happen to be stored as non-standard global attributes in your dataset.
2. A Coordinate Transform Variable is created, and the parameters are renamed according to the CF-1.0
3. The <b>__CoordinateTransformType_</b> identifies this variable unambiguously as a Coordinate Transform Variable.
4. The <b>__CoordinateAxisTypes_</b> attribute indicates that the transform is to be used for all Coordinate Systems that have a <b>_GeoX_</b> and <b>_GeoY_</b> coordinate axis. To be CF compliant, you would have to identify all data variables and add the attribute <b>_grid_mapping="ProjectionPS"_</b> to each.
5. Fake data is added, in case someone accidently tries to read it.
6. The Coordinate Transform Variable is added to the dataset. When adding new variables to a dataset, you must call finish() when all done.

This creates a <b>_Coordinate Transform Variable_</b> in your dataset that looks like this:

~~~
 char Projection;
   :grid_mapping_name = "polar_stereographic";
   :straight_vertical_longitude_from_pole = "-150.0";
   :latitude_of_projection_origin = "90.0";
   :scale_factor_at_projection_origin = "0.996";
   :_CoordinateTransformType = "Projection";
   :_CoordinateAxisTypes = "GeoX GeoY";
~~~
   
A similar way to do this, which creates the same result, creates <b>_ProjectionImpl_</b> and <b>_ProjectionCT_</b> objects, and calls the <b>_makeCoordinateTransformVariable_</b> utility method in CoordSysBuilder to handle the details:

~~~
 public void augmentDataset( NetcdfDataset ds, CancelTask cancelTask) throws IOException {

   // read global parameters
1) double lat_origin = findAttributeDouble( ds, "LAT0");
   double lon_origin = findAttributeDouble( ds, "LON0");
   double scale = findAttributeDouble( ds, "SCALE");
   if (Double.isNaN(scale)) scale = 1.0; 

2) ProjectionImpl proj = new ucar.unidata.geoloc.projection.Stereographic( lat_origin, lon_origin, scale);
3) ProjectionCT projCT = new ProjectionCT("ProjectionPS", "FGDC", proj);

4) VariableDS v = makeCoordinateTransformVariable(ds, projCT);
5) v.addAttribute( new Attribute(_Coordinate.AxisTypes, "GeoX GeoY"));
6) ds.addVariable(v);
   ds.finish();
}
~~~

1. Read the projection values that happen to be stored as non-standard global attributes in your dataset.
2. A <b>_ProjectionImpl_</b> is created out of those parameters.
3. A <b>_ProjectionCT_</b> wraps the <b>_ProjectionImpl_</b>
4. The <b>_makeCoordinateTransformVariable_</b> method handles the details of creating the Coordinate Transform Variable. The ProjectionImpl knows what the standard names of its parameters are.
5. The <b>__CoordinateAxisTypes_</b> attribute indicates that the transform is to be used for all Coordinate Systems that have a <b>_GeoX_</b> and <b>_GeoY_</b> coordinate axis.
6. The Coordinate Transform Variable is added to the dataset.

### CoordSysBuilder Reference

These are the steps taken by NetcdfDataset to add Coordinate Systems:

1. Identify which subclass should be used
2. Create a new object of that class
3. Call <b>_augmentDataset( ds, cancelTask)_</b>
4. Call <b>_buildCoordinateSystems( ds)_</b>

The <b>_augmentDataset()_</b> method is where subclasses should modify the underlying dataset.

The <b>_buildCoordinateSystems()_</b> method is where CoordSysBuilder constructs the Coordinate Systems and adds them to the dataset. In some special cases, the subclass may need to override some of the methods that are called by <b>_buildCoordinateSystems_</b>.

~~~
protected void buildCoordinateSystems( NetcdfDataset ncDataset) {
  // put status info into parseInfo to be shown to someone trying to debug this process
  parseInfo.append("Parsing with Convention = "+conventionName+"\n");
  // Bookeeping info for each variable is kept in the VarProcess inner class
  List vars = ncDataset.getVariables();
  for (int i = 0; i < vars.size(); i++) {
    VariableEnhanced v = (VariableEnhanced) vars.get(i);
    varList.add( new VarProcess(ncDataset, v));
  }
    // identify which variables are coordinate axes
  findCoordinateAxes( ncDataset);
  
  // identify which variables are used to describe coordinate system
  findCoordinateSystems( ncDataset);

  // identify which variables are used to describe coordinate transforms
  findCoordinateTransforms( ncDataset);

  // turn Variables into CoordinateAxis objects
  makeCoordinateAxes( ncDataset);

  // make Coordinate Systems for all Coordinate Systems Variables
  makeCoordinateSystems( ncDataset);
    
  // assign explicit CoordinateSystem objects to variables
  assignExplicitCoordinateSystems( ncDataset);
    
  // assign implicit CoordinateSystem objects to variables
  makeCoordinateSystemsImplicit( ncDataset);
    
  // optionally assign implicit CoordinateSystem objects to variables that dont have one yet
  if (useMaximalCoordSys)
    makeCoordinateSystemsMaximal( ncDataset);
     
   // make Coordinate Transforms
  makeCoordinateTransforms( ncDataset);
   
  // assign Coordinate Transforms
  assignCoordinateTransforms( ncDataset);
}
~~~
To work at this level, you will need to study the source code of <b>_CoordSysBuilder_</b>, and existing subclasses in the <b>_ucar.nc2.dataset.conv_</b> package. As a subclass, you will have access to the list of VarProcess objects, which wrap each variable in the Dataset, and keep track of various information about them.
