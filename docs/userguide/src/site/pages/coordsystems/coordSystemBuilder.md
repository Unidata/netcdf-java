---
title: Coordinate system builder
last_updated: 2021-06-11
sidebar: userguide_sidebar
permalink: coord_system_builder.html
toc: false
---
## Writing your own Java class to identify Coordinate Systems
### Overview

In order to use a dataset at the scientific datatype layer, the dataset's coordinate systems must first be identified. 
This is done by an implementation of `ucar.nc2.dataset.CoordSysBuilderIF` whose job is to examine the contents of the dataset and create coordinate system objects that follow this object model:

For more details, see the [CDM Object Model](../developer/cdm_overview.html){:target="_blank"}.

A `CoordSysBuilderIF` class must be created for each type of dataset that encodes their coordinate systems differently. 
This obviously is burdensome, and data providers are encouraged to use [existing Conventions](https://www.unidata.ucar.edu/software/netcdf/conventions.html){:target="_blank"} for writing their datasets. 
If those are inadequate, then the next best thing is to define and document a new Convention in collaboration with others with similar needs. 
If you do so, read [Writing NetCDF Files: Best Practices](https://www.unidata.ucar.edu/software/netcdf/documentation/NUG/best_practices.html){:target="_blank"}, look at other Convention examples, and get feedback form others before committing to it. 
Send us a URL to your documentation, and we will add it to the [NetCDF Conventions page](https://www.unidata.ucar.edu/software/netcdf/conventions.html){:target="_blank"}.

The steps to use your `CoordSysBuilderIF` class in the Netcdf-Java library:

1. Write a class that implements `ucar.nc2.dataset.CoordSysBuilderIF`, such as by subclassing `ucar.nc2.dataset.CoordSysBuilder`.
2. Add the class to your classpath.
3. From your application, call `ucar.nc2.dataset.CoordSysBuilder.registerConvention( String conventionName, Class c)`. This is called "plugging in" your code at runtime.
4. When you open the dataset in enhanced mode, e.g. by calling

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/coordsystems/coordSystemBuilderTutorial.java&openDataset %}
{% endcapture %}
{{ rmd | markdownify }}
      
In this call, an instance of your class will be called to add coordinate system objects to the `NetcdfDataset`.

The `CoordinateSystem` objects are then available through the classes in the `ucar.nc2.dataset` package, for example:

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

These are the steps taken by `CoordSystemBuilder` to add `CoordinateSystems`:

1. Identify which subclass should be used.
2. Create a new object of that class.
3. Call `augmentDataset( netcdfDataset, cancelTask)` to make any changes to the dataset (add attributes, variables, etc).
4. Call `buildCoordinateSystems( netcdfDataset)` to add the coordinate system objects.

Your class must implement this interface:

~~~java
public interface CoordSysBuilderIF {
  public void setConventionUsed( String convName);
  public void augmentDataset( NetcdfDataset ncDataset, CancelTask cancelTask) throws IOException;
  public void buildCoordinateSystems( NetcdfDataset ncDataset);
  public void addUserAdvice( String advice);
}
~~~

You can override the `buildCoordinateSystems()` method and completely build the coordinate system objects yourself. 
However, it's usually easier to take advantage of the code in the `CoordSystemBuilder` superclass, which translates standard `_Coordinate` attributes into coordinate system objects. 
The job of the subclass may then reduce to adding these `_Coordinate` attributes to the file in the `augmentDataset()` method. 
The subclass may also need to create and add new `Coordinate` variables to the file, and/or to create `CoordinateTransforms`. 
Examples of existing `CoordSystemBuilder` subclasses are in the `ucar.nc2.internal.dataset` package.

The `ucar.nc2.internal.dataset.CoordSystemBuilder` class uses the  `_Coordinate` attributes ("underscore Coordinate attributes", described fully [here](coord_attr_conv.html) to create `CoordinateSystem` objects.
An attribute that starts with an underscore is a "system attribute", which usually implies some special processing or behavior within the NetCDF library (both C and Java).

If you are subclassing `ucar.nc2.internal.dataset.CoordSystemBuilder`, you can ignore the `setConventionUsed` and `addUserAdvice` methods and let the superclass handle them. 
If not, you can just implement dummy methods.

The ToolsUI application has a **CoordSys** tab that is designed to help with the process of building coordinate systems. 
Open up your dataset in that tab, and 3 tables are presented: The data variables, the coordinate systems, and the coordinate axes. The **Info button** (top right) will show various information from the `CoordSystemBuilder` class that was used for the dataset.

### Identifying which datasets your class should operate on

If your datasets use the global attribute convention, then you only need to pass in the value of that attribute into `ucar.nc2.internal.dataset.CoordSystemBuilder.registerConvention(String conventionName, String className)`, and you do not need to implement the `isMine()` method.

Otherwise, your class must implement a static method `isMine()` that returns true when it is given a dataset that it knows how to handle. 

For example:`

~~~java
  public static boolean isMine( NetcdfFile ncfile) {
        String stringValue =  ncfile.findAttribute( "full_name").getStringValue(); //may be null
        return stringValue.equalsIgnoreCase("CRAFT/NEXRAD");
    }
~~~
  
look to see if the global attribute `sensor_name` has the value `CRAFT/NEXRAD`.
It is important that the `isMine()` method be efficient, ideally using only the dataset metadata (attributes, variable names, etc) rather than having to do any data reading.

### Adding Attributes to the Dataset

For the simple case where you only need to add attributes to the file, you might implement the `augmentDataset()` method as follows.

Example implementation:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/coordsystems/coordSystemBuilderTutorial.java&augmentDataset1 %}
{% endcapture %}
{{ rmd | markdownify }}


You may find it easier to do the same thing using an [NcML](../developer/index.html) file, for example:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/coordsystems/coordSystemBuilderTutorial.java&augmentDataset2 %}
{% endcapture %}
{{ rmd | markdownify }}
  
The `NcMLReader.wrapNcML()` method wraps a `NetcdfDataset` in an NcML file, making whatever modifications are specified in the NcML file. 
You pass in the URL location of the NcML to use, typically a local file as above, but it may also be a remote access over *http*. 
Alternatively, you could add the `/MyResource` directory to your classpath, and call this variation:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/coordsystems/coordSystemBuilderTutorial.java&wrapNcmlExample %}
{% endcapture %}
{{ rmd | markdownify }}
 
The `NcMLReader.wrapNcMLresource()` looks for the NcML document by calling `Class.getResource()`. The example NcML file might look like:

~~~xml
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

The NcML adds the appropriate `_CoordinateAxisType` attribute to existing `CoordinateAxes`. Because the data variables all use coordinate variables, implicit `CoordinateSystem` objects are created and assigned. 
There is no need for `CoordinateTransforms` because all the coordinates are reference coordinates (lat, lon, height). [Here](https://www.unidata.ucar.edu/software/netcdf-java/v4.6/ncml/Tutorial.html){:target="_blank"}  is complete info on NcML.

If all you need to do is wrap the dataset in NcML, and the dataset already has a Convention attribute in it (before it is wrapped), then you can simply register the NcML directly, without having to write any code. For this, you use:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/coordsystems/coordSystemBuilderTutorial.java&registerNcml %}
{% endcapture %}
{{ rmd | markdownify }}

### Adding Coordinate Axes to the Dataset

When a `CoordinateAxis` is missing, you must add it. You can do this programmatically or through an NcML file, for example:

~~~xml
  <variable name="latitude" shape="row" type="double">
    <attribute name="long_name" value="latitide coordinate" />
    <attribute name="units" value="degrees_north" />
    <attribute name="_CoordinateAxisType" value="Lat" />
    <values start="90.0" incr="5.0" />
  </variable>
~~~
  
This file creates a new coordinate axis variable, and gives it evenly spaced values. You can also enumerate the values:

~~~xml
  <values>90.0 88.3 72.6 66.9</values>
~~~
  
When the values must be computed, then you need to do this programmatically.
It's convenient to wrap the dataset in NcML, even when you also have to do some programming. For one thing, you can change the NcML file without recompiling.

Example implementation of `argumentDataset()`:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/coordsystems/coordSystemBuilderTutorial.java&augmentDataset3 %}
{% endcapture %}
{{ rmd | markdownify }}

### Identifying Coordinate Axis Types

Another simple case to handle is when you are using `Coordinate` variables for all data variables. `Coordinate` variables are 1D variables with the same name as their dimension, which encode the coordinate values for that dimension. 
In that case, you only need to identify the `CoordinateAxes` types, which you do by overriding the `getAxisType()` method.

Below is an example implementation to override `getAxisType()`. This will pass in all variables that have been identified as coordinate axes, and your job is to return their AxisType, if they have one:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/coordsystems/coordSystemBuilderTutorial.java&getAxisType %}
{% endcapture %}
{{ rmd | markdownify }}

### Creating Coordinate Transformations

A more complex task is to create `CoordinateTransforms`, which map your coordinates to reference coordinates, such as lat/lon. 
A `CoordinateTransform` is typically represented by a `CoordinateTransform` variable, which may be a dummy variable (ie has no data in it), and whose attributes document the meaning and specify any needed parameters for it. 
You can create arbitrary transforms by creating `ucar.nc2.dataset.CoordinateTransform` objects, which your code will have access to when it opens a `NetcdfDataset`.

However, for your `Transform` to be used by the netCDF-Java library and standard applications built on top of it, the `CoordinateTransform` must have a reference to a `ucar.unidata.geoloc.Projection` or a `ucar.unidata.geoloc.vertical.VerticalTransform` object which knows how to do the actual mathematical transformation. 
The netCDF-Java library has a number of these, mostly following the CF-1.0 specification (Appendix F for projections, Appendix D for vertical transforms). 
You can also [write your own implementation](coord_transform.html) and add them at run time.

For this lesson, we will concentrate on what your `CoordSystemBuilder` needs to do to use an existing standard or user written `Projection` or `VerticalTransform` class.

You can create the `CoordinateTransform` objects yourself, by overriding the `makeCoordinateTransforms()` and `assignCoordinateTransforms()` methods in `CoordSystemBuilder`. 
Much easier is to use the existing machinery and create a `CoordinateTransform` variable which represents the parameters of the transform in a way recognized by a `CoordTransformFactory` class.

One way to do that is by overriding the `augmentDataset()` method:

For example:
{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/coordsystems/coordSystemBuilderTutorial.java&argumentDataset4 %}
{% endcapture %}
{{ rmd | markdownify }}


NOTE: The `_CoordinateAxisTypes` attribute indicates that the transform is to be used for all `CoordinateSystems` that have a `GeoX` and `GeoY` coordinate axis. 
To be CF compliant, you would have to identify all data variables and add the attribute `grid_mapping="ProjectionPS"` to each.

This creates a `CoordinateTransform` variable in your dataset that looks like this:

~~~
 char Projection;
   :grid_mapping_name = "polar_stereographic";
   :straight_vertical_longitude_from_pole = "-150.0";
   :latitude_of_projection_origin = "90.0";
   :scale_factor_at_projection_origin = "0.996";
   :_CoordinateTransformType = "Projection";
   :_CoordinateAxisTypes = "GeoX GeoY";
~~~
   
A similar way to do this, which creates the same result, creates `ProjectionImpl` and `ProjectionCT` objects, and calls the `makeCoordinateTransformVariable()` utility method in `CoordSystemBuilder` to handle the details:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/coordsystems/coordSystemBuilderTutorial.java&argumentDataset5 %}
{% endcapture %}
{{ rmd | markdownify }}

### CoordSystemBuilder Reference

These are the steps taken by `NetcdfDataset` to add `CoordinateSystems`:

1. Identify which subclass should be used
2. Create a new object of that class
3. Call `augmentDataset( ncDataset, cancelTask)`
4. Call `buildCoordinateSystems( ncDataset)`

The `augmentDataset()` method is where subclasses should modify the underlying dataset.

The `buildCoordinateSystems()` method is where `CoordSystemBuilder` constructs the `CoordinateSystems` and adds them to the dataset. 
In some special cases, the subclass may need to override some methods that are called by `buildCoordinateSystems()`.
See `ucar.nc2.internal.dataset.CoordSystemBuilder.buildCoordinateSystems()`  method implementation below.

{% capture rmd %}
{% includecodeblock netcdf-java&cdm-core/src/main/java/ucar/nc2/internal/dataset/CoordSystemBuilder.java&buildCoordinateSystems %}
{% endcapture %}
{{ rmd | markdownify }}

To work at this level, you will need to study the source code of `CoordSystemBuilder`, and existing subclasses in the `ucar.nc2.dataset.conv` package. 
As a subclass, you will have access to the list of `VarProcess` objects, which wrap each variable in the dataset, and keep track of various information about them.
