---
title: Coordinate transforms
last_updated: 2021-07-01
sidebar: netcdfJavaTutorial_sidebar
permalink: coord_transform.html
toc: false
---

## Writing a Coordinate Transform: Projections and Vertical Transforms

### Overview

A `CoordinateTransform` represents a mathematical function that transforms a dataset's coordinates into coordinates in a reference `CoordinateSystem`. 
Currently, the CDM has two kinds of transforms: `Projections` and `VerticalTransforms`. 
A `Projection` maps between cartesian x and y coordinates (called `GeoX` and `GeoY`) and latitude, longitude coordinates, by implementing the `ucar.unidata.geoloc.Projection` interface. 
A `VerticalTransform` takes a `GeoZ` coordinate and usually other data fields such as surface pressure, and produces a 3D height or pressure vertical coordinate field.

A `CoordinateSystem` may have 0 or more `CoordinateTransforms`, each of which is either a `ProjectionCT` containing a `ucar.unidata.geoloc.Projection` or a `VerticalCT` containing a `ucar.unidata.geoloc.vertical.VerticalTransform`:

{% include image.html file="netcdf-java/tutorial/coordsystems/CoordSys.png" alt="Coord Sys Object Model" caption="" %}

The netCDF-Java library implements a standard set of `ucar.unidata.geoloc.Projection` and `ucar.unidata.geoloc.vertical.VerticalTransform` classes, following the specifications of the [CF-1.0 Conventions](http://cfconventions.org/){:target="_blank"}.

## Implementing a Coordinate Transform
The steps to using your own `CoordinateTransform` in the netCDF-Java library:

Write a class that implements `ucar.nc2.dataset.CoordTransBuilderIF`, by subclassing `ucar.nc2.dataset.transform.AbstractTransformBuilder`.
Add these classes to your classpath.
From your application, call `ucar.nc2.dataset.CoordTransBuilder.registerTransform( String transformName, Class c)`.
The [Coordinate System Builder](coord_system_builder.html#creating-coordinate-transformations) for your dataset must recognize the transform and add it to the coordinate system. 
If you use the CF-1.0 or the `_Coordinate` conventions, this means that the dataset must contain a `CoordinateTransform` variable that contains the parameters of the transform.

The classes that you will use are shown in the following diagram, which has an example of both a `Projection` ( LambertConformal) and a `VerticalTransform` (CFOceanSigma).

{% include image.html file="netcdf-java/tutorial/CoordTransforms.png" alt="Coordinate Transforms" caption="" %}

### Implementing a Projection

You should implement the `ucar.unidata.geoloc.Projection` interface by subclassing the abstract class `ucar.unidata.geoloc.projection.ProjectionImpl`. The methods you need to implement are:

~~~java
  public ProjectionPoint latLonToProj(LatLonPoint latlon, ProjectionPointImpl destPoint);
  public LatLonPoint projToLatLon(ProjectionPoint ppt, LatLonPointImpl destPoint);
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2);
  public ProjectionImpl constructCopy();
  public boolean equals(Object proj);
  public int hashCode()
  public String paramsToString();
~~~
  
The `latLonToProj()` and `projToLatLon()` methods are inverses of each other, and map between `lat`, `lon` (in units of decimal degrees) to cartesian x,y, the coordinates that your dataset uses, usually in units of "km on the projection plane". 
The `crossSeam()` method returns true when a line between two points in projection coordinates would cross a seam in the projection plane, such as for a cylindrical or conic projections. 
This helps drawing routines to eliminate spurious lines. The `constructCopy()` method constructs a new, equivalent `Projection` object, which avoids the problems with clone (see Bloch, [Effective Java](http://java.sun.com/developer/Books/effectivejava/Chapter3.pdf){:target="_blank"}, item 10). 
The `equals()` method should be overridden to make `Projections` equal that have the same parameters. You should also override `hashCode()` to make it consistent with equals (see Bloch, [Effective Java](http://java.sun.com/developer/Books/effectivejava/Chapter3.pdf){:target="_blank"}, item 8). 
The `paramsToString()` returns a String representation of the `Projection` parameters. Examine the classes in `ucar.unidata.geoloc.projection` for implementation examples.

### Implementing a VerticalTransform

You should implement the `ucar.unidata.geoloc.vertical.VerticalTransform` interface by subclassing the abstract class `ucar.unidata.geoloc.vertical.VerticalTransformImpl`. The methods you need to implement are:
~~~
  public ucar.ma2.ArrayDouble.D3 getCoordinateArray(int timeIndex);
  public String getUnitString();
~~~
  
The `getCoordinateArray()` method returns a 3D vertical coordinate for the given time step (the time step is ignored if `isTimeDependent()` is false). 
The returned array must use dimensions in the order of z, y, x. The `getUnitString()` method returns the unit of the transformed vertical coordinate, which should be `udunits` compatible with `height` or `pressure`. 
Examine the classes in `ucar.unidata.geoloc.vertical` for implementation examples.

### Implementing and registering CoordTransBuilderIF

The `Projection` and `VerticalTransform` implement the mathematical transformation itself. 
Now we need to add the glue classes that allow runtime discovery and object instantiation. 
To do so, you must add a class that implements the `ucar.nc2.dataset.CoordTransBuilderIF` interface. 
You should subclass the abstract class `ucar.nc2.dataset.transform.AbstractTransformBuilder`, and implement the following methods:

~~~java
  public String getTransformName();
  public TransformType getTransformType();
  public CoordinateTransform makeCoordinateTransform (NetcdfDataset ds, Variable ctv);
  public ucar.unidata.geoloc.vertical.VerticalTransform makeMathTransform(NetcdfDataset ds, Dimension timeDim, VerticalCT vCT);
~~~
  
Give your transform a unique name, which is returned by the `getTransformName()` method. 
The `getTransformType()` method should return either `ucar.nc2.dataset.TransformType.Projection` or `TransformType.Vertical`. 
The `makeCoordinateTransform()` method is the guts of the class, it takes as parameters the `NetcdfDataset` and the `CoordinateTransform` variable that contains the transformation parameters. 
The `makeMathTransform()` is used only for `VerticalTransforms` to defer the creation of the `VerticalTransform` until the `CoordinateSystem` has been fully constructed and, for example, the time dimension has been identified.

You then need to tell the netCDF-Java library about your transform class :

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/coordsystems/coordTransformTutorial.java&registerTransform %}
{% endcapture %}
{{ rmd | markdownify }}

The name is the same as `getTransformType()` returns, and must be referenced in your dataset by the `CoordinateTransform` variable.

### Projection Example

Following is an example from the standard implementation classes in `ucar.nc2.internal.dataset.transform`.

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/coordsystems/coordTransformTutorial.java&projectionEx %}
{% endcapture %}
{{ rmd | markdownify }}

#### Implementation of makeCoordinateTransform()

Below is an example implementation of the `makeCoordinateTransform()` method.

{% capture rmd %}
{% includecodeblock netcdf-java&cdm/core/src/main/java/ucar/nc2/dataset/transform/LambertConformalConic.java&makeCoordinateTransform %}
{% endcapture %}
{{ rmd | markdownify }}

Explanation of `makeCoordinateTransform()`
* Various parameters are read from the attributes of the Coordinate Transform Variable.
* A Projection is created from the parameters.
* A ProjectionCT wraps the Projection and is returned.

### Vertical Transform Example

Following is an example from the standard implementation classes in `ucar.nc2.internal.dataset.transform.vertical`.

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/coordsystems/coordTransformTutorial.java&vertTransEx %}
{% endcapture %}
{{ rmd | markdownify }}

#### Implementation of makeCoordinateTransform()

Below is an example implementation of the `makeCoordinateTransform()` method.

{% capture rmd %}
{% includecodeblock netcdf-java&cdm/core/src/main/java/ucar/nc2/dataset/transform/CFOceanSigma.java&makeCoordinateTransform %}
{% endcapture %}
{{ rmd | markdownify }}

### Corresponding Vertical Transform Example

Following is an example from the standard implementation classes in `ucar.unidata.geoloc.vertical`.

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/coordsystems/coordTransformTutorial.java&vertTransClass %}
{% endcapture %}
{{ rmd | markdownify }}

#### Implementation of the constructor

Below is an example implementation of the `AtmosSigma()` constructor.

{% capture rmd %}
{% includecodeblock netcdf-java&cdm/core/src/main/java/ucar/unidata/geoloc/vertical/AtmosSigma.java&AtmosSigma %}
{% endcapture %}
{{ rmd | markdownify }}

Explanation of AtmosSigma class constructor:
* The `psVar` variable holding the surface pressure 
* The `ptopVar` variable is the value of the `ptop` scalar variable
* The `sigmaVar` variable is the value of the `sigma[z]` coordinate
* The returned converted coordinates will be in the units of the surface pressure
#### Implementation of getCoordinateArray()

Below is an example implementation of the `getCoordinateArray()` method.

{% capture rmd %}
{% includecodeblock netcdf-java&cdm/core/src/main/java/ucar/unidata/geoloc/vertical/AtmosSigma.java&getCoordinateArray %}
{% endcapture %}
{{ rmd | markdownify }}

Explanation of `getCoordinateArray()`:
* Reads the surface pressure variable at the given time step through a utility method in the superclass
* Creates the result array
* Extracts the surface pressure at the given x,y point
* Loops over z, the converted coordinate = ptop + sigma(z)*(surfacePressure(x,y)-ptop)