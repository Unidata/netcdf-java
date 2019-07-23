---
title: Coordinate Attribute Examples
last_updated: 2019-07-22
sidebar: netcdfJavaTutorial_sidebar
permalink: coord_attribute_ex.html
toc: false
---
## Coordinate Attribute Convention (Tutorial)

{% include image.html file="netcdf-java/tutorial/CoordSys.png" alt="Coord Sys Object Model" caption="Coordinate Systems UML" %}

### See Also:
* <a href="common_data_model_overview.html">CDM Object Model</a>
* _Coordinate Attribute <a href="coord_attr_conv.html">Reference</a>

### Contents

1. [Example 1: Simple Coordinate Variables](#example-1-simple-coordinate-variables)

2. [Example 2: Coordinate System Variable](#example-2-coordinate-system-variable)

3. [Example 3: Multiple Coordinate Systems](#example-3-multiple-coordinate-systems)

4. [Example 4: Adding Coordinate Transforms](#example-4-adding-coordinate-transforms)

5. [Example 5: Reusing Variables for Coordinate Systems and/or Coordinate Transform Variables](#example-5-reusing-variables-for-coordiate-system-andor-coordinate-transform-variables)

## Example 1: Simple Coordinate Variables

~~~
 dimensions:
   lon = 144;
   lat = 73;
   level = 17;
   time = UNLIMITED;   // (13 currently)

 variables:
   float earth(time, level, lat, lon);
     earth:units = "K";
     earth:long_name = "mean Daily Soil temperature";
     earth:_CoordinateAxes="time
    level lat lon";

   float air(time, level, lat, lon);
     air:units = "K";
     air:long_name = "mean Daily Air temperature";
     air:_CoordinateAxes="time
    level lat lon";

   float lat(lat);
     lat:long_name = "Latitude";
     lat:units = "degrees_north";
 lat:_CoordinateAxisType="Lat";

   float lon(lon);
     lon:long_name = "Longitude";
     lon:units = "degrees_east";

    lon:_CoordinateAxisType="Lon";

    float level(level);
     level:units = "millibar";
     level:long_name = "Level";
     level:positive = "down";

    level:_CoordinateAxisType="Pressure";
  
   double time(time);
     time:units = "days since 1970-01-01 00:00:0.0";
     time:long_name = "Time";

    time:_CoordinateAxisType="Time";
~~~    
In this simple case, both data Variables explicitly name their CoordinateAxes with the <b>__CoordinateAxes_</b> attribute. Each CoordinateAxis has a <b>__CoordinateAxisType_</b> attribute. A single CoordinateSystem is defined which both data Variables share.

<b>_Rule_</b>: A CoordinateAxis is identified in one of 2 ways:

* having the attribute __CoordinateAxisType_.
* being listed in a __CoordinateAxes attribute_.

<b>_Rule_</b>: A CoordinateSystem is uniquely determined by its list of CoordinateAxes.

<b>_Rule_</b>: A CoordinateSystem may be implicitly defined by a data Variable's <b>__CoordinateAxes_</b> attribute.

## Example 2: Coordinate System Variable

~~~
 dimensions:
   lon = 144;
   lat = 73;
   level = 17;
   time = UNLIMITED;   // (13 currently)

 variables:
   float earth(time, level, lat, lon);
     earth:units = "K";
     earth:long_name = "mean Daily Soil temperature";
     earth:_CoordinateSystems="LatLonCoordinateSystem";

   float air(time, level, lat, lon);
     air:units = "K";
     air:long_name = "mean Daily Air temperature";
     air:_CoordinateSystems="LatLonCoordinateSystem";

   float lat(lat);
     lat:long_name = "Latitude";
     lat:units = "degrees_north";
     lat:_CoordinateAxisType="Lat";

   float lon(lon);
     lon:long_name = "Longitude";
     lon:units = "degrees_east";
     lon:_CoordinateAxisType="Lon";

  float level(level);
     level:units = "millibar";
     level:long_name = "Level";
     level:positive = "down";
     level:_CoordinateAxisType="Pressure";

   double time(time);
     time:units = "days since 1970-01-01 00:00:0.0";
     time:long_name = "Time";
     time:_CoordinateAxisType="Time";

    char LatLonCoordinateSystem;
 LatLonCoordinateSystem:_CoordinateAxes = "time level lat lon";
~~~
 
In this case we create a <b>_Coordinate System Variable_</b>, a dummy variable whose purpose is to define a CoordinateSystem. The data Variables now point to it with a <b>__CoordinateSystem attribute_</b>.

<b>_Rule_</b>: A CoordinateSystem may be explicitly defined by a Coordinate System Variable, which always has a <b>__CoordinateAxes_</b> attribute.

## Example 3: Multiple Coordinate Systems

~~~
dimensions:
   y = 428;
   x = 614;
   time = 2;
   depth_below_surface = 4;

 variables:
  float Soil_temperature(time, depth_below_surface, y, x);
     Soil_temperature:units = "K";
     Soil_temperature:_CoordinateSystems = "ProjectionCoordinateSystem
  LatLonCoordinateSystem";

  float Volumetric_Soil_Moisture_Content(time, depth_below_surface, y, x);
     Volumetric_Soil_Moisture_Content:units = "fraction";
 Volumetric_Soil_Moisture_Content:_CoordinateSystems = "ProjectionCoordinateSystem LatLonCoordinateSystem";

  double y(y);
     y:units = "km";
     y:long_name = "y coordinate of projection";
 y:_CoordinateAxisType = "GeoY";

   double x(x);
     x:units = "km";
     x:long_name = "x coordinate of projection";
 x:_CoordinateAxisType =
  "GeoX";

   int time(time);
     time:long_name = "forecast time";
     time:units = "hours since 2003-09-03T00:00:00Z";
     time:_CoordinateAxisType = "Time";

   double depth_below_surface(depth_below_surface);
     depth_below_surface:long_name = "Depth below land surface";
     depth_below_surface:units = "m";
     depth_below_surface:_CoordinateAxisType
  = "Height";
     depth_below_surface:_CoordinateZisPositive = "down";

   double lat(y, x);
     lat:units = "degrees_north";
     lat:long_name = "latitude coordinate";
     lat:_CoordinateAxisType = "Lat";

   double lon(y, x);
     lon:units = "degrees_east";
     lon:long_name = "longitude coordinate";
     lon:_CoordinateAxisType = "Lon";

 char LatLonCoordinateSystem;
 LatLonCoordinateSystem:_CoordinateAxes = "time depth_below_surface lat lon";

  char ProjectionCoordinateSystem;
 ProjectionCoordinateSystem:_CoordinateAxes = "time depth_below_surface y x";
~~~
 
In this case, the data Variables have two coordinate systems, the <b>_LatLonCoordinateSystem_</b> and the <b>_ProjectionCoordinateSystem_</b>.

Note that for projection coordinates, use <b>_AxisType_</b> = GeoX and GeoY. We also introduce the <b>__CoordinateZisPositive_</b> attribute, which is used only on vertical Coordinate Axes (AxisType = Pressure, Height, or GeoZ), to indicate in which direction increasing values of the coordinate go.

<b>_Rule_</b>: To indicate multiple Coordinate Systems for a single data variable, you must use Coordinate System Variables and list them from the data Variable's _CoordinateSystems attribute.

<b>_Rule_</b>: Use AxisType GeoX and GeoY for projection coordinate axes.

<b>_Rule_</b>: Use the _CoordinateZisPositive attribute on vertical Coordinate Axes to indicate in whether increasing values of the coordinate go up or down.

## Example 4: Adding Coordinate Transforms

~~~
dimensions:
   y = 428;
   x = 614;
   time = 2;
   depth_below_surface = 4;

 variables:
  float Soil_temperature(time, depth_below_surface, y, x);
     Soil_temperature:units = "K";
     Soil_temperature:_CoordinateSystems = "ProjectionCoordinateSystem";

  double y(y);
     y:units = "km";
     y:long_name = "y coordinate of projection";
     y:_CoordinateAxisType = "GeoY";

   double x(x);
     x:units = "km";
     x:long_name = "x coordinate of projection";
     x:_CoordinateAxisType = "GeoX";

   int time(time);
     time:long_name = "forecast time";
     time:units = "hours since 2003-09-03T00:00:00Z";
     time:_CoordinateAxisType = "Time";

   double depth_below_surface(depth_below_surface);
     depth_below_surface:long_name = "Depth below land surface";
     depth_below_surface:units = "m";
     depth_below_surface:_CoordinateAxisType = "Height";
     depth_below_surface:_CoordinateZisPositive = "down";

  char ProjectionCoordinateSystem;
     ProjectionCoordinateSystem:_CoordinateAxes = "time depth_below_surface y x";
 ProjectionCoordinateSystem:_CoordinateTransforms = "LambertConformalProjection";

 char LambertConformalProjection;
 LambertConformalProjection:_CoordinateTransformType = "Projection";
 LambertConformalProjection:transform_name = "lambert_conformal_conic";
 LambertConformalProjection:standard_parallel = 25.0;

  LambertConformalProjection:longitude_of_central_meridian = 265.0;
 LambertConformalProjection:latitude_of_projection_origin = 25.0;
 ~~~
 
To create a CoordinateTransform, we define a Coordinate Transform Variable (here called LambertConformalProjection), which is a dummy variable similar to a Coordinate System Variable (here called ProjectionCoordinateSystem). The attributes on the Coordinate Transform Variable are the parameters of the transform. The Coordinate System Variable points to it with a _CoordinateTransforms attribute. You can have multiple CoordinateTransforms for a CoordinateSystem.

<b>_Rule_</b>: A CoordinateTransform must be explicitly defined by a Coordinate Transform Variable. It is identified by having the <b>__CoordinateTransformType_</b> attribute, or by being pointed to by a Coordinate System Variable's <b>__CoordinateTransforms_</b> attribute.

## Example 5: Reusing Variables for Coordiate System and/or Coordinate Transform Variables

~~~
dimensions:
   y = 428;
   x = 614;
   time = 2;
   depth_below_surface = 4;

 variables:
  float Soil_temperature(time, depth_below_surface, y, x);
     Soil_temperature:units = "K";
     Soil_temperature:_CoordinateSystems = "ProjectionCoordinateSystem";

  double y(y);
     y:units = "km";
     y:long_name = "y coordinate of projection";
     y:_CoordinateAxisType = "GeoY";

   double x(x);
     x:units = "km";
     x:long_name = "x coordinate of projection";
     x:_CoordinateAxisType = "GeoX";

   int time(time);
     time:long_name = "forecast time";
     time:units = "hours since 2003-09-03T00:00:00Z";
     time:_CoordinateAxisType = "Time";

   double depth_below_surface(depth_below_surface);
     depth_below_surface:long_name = "Depth below land surface";
     depth_below_surface:units = "m";
     depth_below_surface:_CoordinateAxisType = "Height";
     depth_below_surface:_CoordinateZisPositive = "down";

  char ProjectionCoordinateSystem;
     ProjectionCoordinateSystem:_CoordinateAxes = "time depth_below_surface y x";

  ProjectionCoordinateSystem:_CoordinateTransformType = "Projection";
     ProjectionCoordinateSystem:transform_name = "lambert_conformal_conic";
     ProjectionCoordinateSystem:standard_parallel = 25.0;
     ProjectionCoordinateSystem:longitude_of_central_meridian = 265.0;
     ProjectionCoordinateSystem:latitude_of_projection_origin = 25.0;
~~~
     
Here we are using the <b>_ProjectionCoordinateSystem_</b> Variable as both a Coordinate System Variable and a Coordinate Transform Variable. In this case, you must use a <b>__CoordinateTransformType_</b> attribute to explicitly show that <b>_ProjectionCoordinateSystem_</b> is a Coordinate Transform Variable.

You can use any Variable as the Coordinate Transform Variable; here's an example using the vertical Coordinate Axis to hold a vertical transform:

~~~
dimensions:
   y = 428;
   x = 614;
   level = 44;

 variables:
  float Soil_temperature(level, y, x);
	   Soil_temperature:units = "K";
 Soil_temperature:_CoordinateSystems = "ProjectionCoordinateSystem";
  
  double y(y);
     y:units = "km";
     y:long_name = "y coordinate of projection";
     y:_CoordinateAxisType = "GeoY";

   double x(x);
     x:units = "km";
     x:long_name = "x coordinate of projection";
     x:_CoordinateAxisType = "GeoX";

   double level(level);
     :long_name = "hybrid level at midpoints (1000*(A+B))";
     :units = "level";
     :positive = "down";

    :standard_name = "atmosphere_hybrid_sigma_pressure_coordinate";
 :formula_terms = "a: hyam b: hybm p0: P0 ps: PS";

    :_CoordinateTransformType = "Vertical";
     :_CoordinateAxisType = "GeoZ";
     :_CoordinateZisPositive = "down";

   double P0;
     :long_name = "reference pressure";
     :units = "Pa";
   double hyam(lev);
     :long_name = "hybrid A coefficient at layer midpoints";
   double hybm(lev);
     :long_name = "hybrid B coefficient at layer midpoints";
   float PS(time, y, x);
     :units = "Pa";
     :long_name = "surface pressure";

  char ProjectionCoordinateSystem;
     ProjectionCoordinateSystem:_CoordinateAxes = "level y x";

    ProjectionCoordinateSystem:_CoordinateTransforms = "level";
       ProjectionCoordinateSystem:_CoordinateTransformType = "Projection";
     ProjectionCoordinateSystem:transform_name = "lambert_conformal_conic";
     ProjectionCoordinateSystem:standard_parallel = 25.0;
     ProjectionCoordinateSystem:longitude_of_central_meridian = 265.0;
     ProjectionCoordinateSystem:latitude_of_projection_origin = 25.0;
~~~
     
Here again we are using the ProjectionCoordinateSystem Variable as both a Coordinate System Variable and a Coordinate Transform Variable. In addition, there is a vertical transformation on the level Variable, pointed to by the ProjectionCoordinateSystem:_CoordinateTransforms attribute.

Rule: You can turn any Variable into a Coordinate System or Coordinate Transform Variable.

## Implicit Coordinate Systems
The above attributes allow for explicitly specifying Coordinate Systems for data variables. This section defines how Coordinate Systems may be implicitly defined.

### Goals
Use the existing "Coordinate Variable" Convention to infer Coordinate Systems for legacy files
allow minimum annotation to existing datasets to fully specify Coordinate Systems and Transforms

### Coordinate Variables and Aliases
A coordinate Variable is a one dimensional Variable with monotonic values that has the same name as its dimension.

A one-dimensional Variable with monotonic values can act as a Coordinate Variable for its dimension, even when it doesnt have the same name as the dimension. To indicate this, add an attribute to the variable called _CoordinateAliasForDimension, whose value must be the name of its single dimension. A dimension may have multiple coordinate variables in this way, which is useful, for example, if the data is a trajectory. Coordinate variables created this way are used for implicit _CoordinateAxes processing (next section). Example:

~~~
   double valtime(record);
     :long_name = "valid time";
     :units = "hours since 1992-1-1";

  :_CoordinateAliasForDimension = "record";
 :_CoordinateAxisType = "Time"; 
~~~

Note that its very important to also identify the Coordinate Axis type.

This can also be used to fix existing files whose coordinate Variables were not named the same as their dimension. (However, if you are writing the file, you should use correctly named coordinate Variables when possible).

We will call both regular Coordinate Variables and ones that have been defined with the _CoordinateAliasForDimension attribute Coordinate Variables.

### Implicit Coordinate System

When there is no <b>__CoordinateSystems_</b> or <b>__CoordinateAxes_</b> attribute on a data Variable, a list of coordinate axes is constructed from the list of Coordinate Variables for the data Variable. If there are 2 or more axes, the Coordinate System for the variable is found by examining all Coordinate Systems and matching exactly its list of Coordinate Axes. If there is no existing Coordinate System that matches, one is added, and this is called an <b>_implicit Coordinate System_<b/>.

### Assigning CoordinateTransforms

The only way to add a Coordinate Transform to an implicit Coordinate System, is to add a _CoordinateAxes or _CoordinateAxisTypes attribute to the Coordinate Transform Variable listing the names or types of Coordinate Axes. The Coordinate Transform will be addded to any Coordinate System that contains all named axes. For example:

~~~
   char ProjectionCoordinateSystem;
     ProjectionCoordinateSystem:_CoordinateTransformType = "Projection";
 ProjectionCoordinateSystem:_CoordinateAxisTypes = "GeoX GeoY";
will apply to any CoordinateSystem that has both GeoX and GeoY Coordinate axes.
~~~
~~~
   char VerticalCoordinateSystem;
     VerticalCoordinateSystem:_CoordinateTransformType = "Vertical";
 VerticalCoordinateSystem:_CoordinateAxes = "hybrid";
will apply to any CoordinateSystem that has the Coordinate Axis named "hybrid"..
~~~
### Coordinate Transform Variables

A Coordinate Transform Variable is a container for information about a transformation function from a Coordinate System to a Reference Coordinate System. At a minimum it must have a <b>_transform_name-</b> attribute. (alias grid_mapping_name or <b>_standard_name_</b> for CF compatability).

~~~
 char Lambert_Conformal_Projection;
 Lambert_Conformal_Projection:transform_name = "lambert_conformal_conic";

  Lambert_Conformal_Projection:standard_parallel = 25.0;
 Lambert_Conformal_Projection:longitude_of_central_meridian = 265.0;

  Lambert_Conformal_Projection:latitude_of_projection_origin = 25.0;
~~~
  
When a Coordinate System has only one Coordinate Transform, the information on the transform may be added directly to the Coordinate System Variable. The Variable acts as both a Coordinate System and a Coordinate Transform. Example:

~~~
 char ProjectionCoordinateSystem;
  ProjectionCoordinateSystem:_CoordinateAxes = "time depth_below_surface y x";
  ProjectionCoordinateSystem:transform_name = "lambert_conformal_conic";
 ProjectionCoordinateSystem:standard_parallel = 25.0;

  ProjectionCoordinateSystem:longitude_of_central_meridian = 265.0;
 ProjectionCoordinateSystem:latitude_of_projection_origin = 25.0;
 ~~~
 
You can use a CF grid mapping or dimensionless vertical coordinate as a transform:

~~~
 char ProjectionCoordinateSystem;
  ProjectionCoordinateSystem:_CoordinateAxes = "time depth_below_surface y x";
  ProjectionCoordinateSystem:_CoordinateTransforms = "Lambert_Conformal lev";
~~~
  
~~~
 int Lambert_Conformal;
  Lambert_Conformal:grid_mapping_name = "lambert_conformal_conic";
  Lambert_Conformal:standard_parallel = 25.0;
  Lambert_Conformal:longitude_of_central_meridian = 265.0;
  Lambert_Conformal:latitude_of_projection_origin = 25.0;
~~~

~~~  
 float lev(lev) ;
  lev:long_name = "sigma at layer midpoints" ;
  lev:positive = "down" ;
  lev:standard_name = "atmosphere_sigma_coordinate" ;
  lev:formula_terms = "sigma: lev ps: PS ptop: PTOP" ;
~~~
Generally the set of valid transforms are not specified by this <b>__Coordinates_</b> Convention. The <a href="std_horizonal_coord_transforms.html">transforms that the nj22 library recognizes</a> come from the CF grid mappings and vertical transforms, so these are recommended, when possible, for the actual transform content.

It is often convenient to define the Coordinate Transform Variable and have it point to the Coordinate Systems that use it. For this purpose, you can use the <b>__CoordinateSystems</b> attribute on a Coordinate Transform Variable. You also need to add the <b>__CoordinateTransformType_</b> attribute to make sure it is interpreted as a Coordinate Transform variable instead of a data variable.
~~~
 int Lambert_Conformal;
  Lambert_Conformal:grid_mapping_name = "lambert_conformal_conic";
  Lambert_Conformal:standard_parallel = 25.0;
  Lambert_Conformal:longitude_of_central_meridian = 265.0;
  Lambert_Conformal:latitude_of_projection_origin = 25.0;
   Lambert_Conformal:_CoordinateTransformType = "Projection";
   Lambert_Conformal:_CoordinateSystems = "ProjectionCoordinateSystem";
~~~
For dealing with implicitly defined Coordinate Systems, you can use the <b>__CoordinateAxes_</b> attribute:
~~~
 int Lambert_Conformal;
  Lambert_Conformal:grid_mapping_name = "lambert_conformal_conic";
  Lambert_Conformal:standard_parallel = 25.0;
  Lambert_Conformal:longitude_of_central_meridian = 265.0;
  Lambert_Conformal:latitude_of_projection_origin = 25.0;
   Lambert_Conformal:_CoordinateTransformType = "Projection";
   Lambert_Conformal:_CoordinateAxes = "y x";
~~~   
This means to apply it to any Coordinate System that includes the x and y Coordinate Axes.

