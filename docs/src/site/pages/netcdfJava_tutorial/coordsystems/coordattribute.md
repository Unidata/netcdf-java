---
title: Coordinate attribute examples
last_updated: 2021-06-29
sidebar: netcdfJavaTutorial_sidebar
permalink: coord_attribute_ex.html
toc: false
---
## Tutorial for coordinate attribute convention

### See Also:
* [CDM Object Model](common_data_model_overview.html)
* [Coordinate Attribute Reference](coord_attr_conv.html)

### Contents

1. [Example 1: Simple Coordinate Variables](#example-1-simple-coordinate-variables)

2. [Example 2: Coordinate System Variable](#example-2-coordinate-system-variable)

3. [Example 3: Multiple Coordinate Systems](#example-3-multiple-coordinate-systems)

4. [Example 4: Adding Coordinate Transforms](#example-4-adding-coordinate-transforms)

5. [Example 5: Reusing Variables for Coordinate Systems and/or Coordinate Transform Variables](#example-5-reusing-variables-for-coordinate-system-and-coordinate-transform-variables)

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
In this simple case, both data `Variables` explicitly name their coordinate axes with the `_CoordinateAxes` attribute. 
Each `CoordinateAxis` has a `_CoordinateAxisType` attribute. A single `CoordinateSystem` is defined which both data `Variables` share.

**Rule**: A `CoordinateAxis` is identified in one of 2 ways:

* having the attribute `_CoordinateAxisType`.
* being listed in a `_CoordinateAxes` attribute.

**Rule**: A `CoordinateSystem` is uniquely determined by its list of `CoordinateAxes`.

**Rule**: A `CoordinateSystem` may be implicitly defined by a data `Variable`'s `_CoordinateAxes` attribute.

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
 
In this case we create a `_CoordinateSystem` variable, a dummy variable whose purpose is to define a `CoordinateSystem`. The data `Variables` now point to it with a `_CoordinateSystem` attribute.

**Rule**: A `CoordinateSystem` may be explicitly defined by a `CoordinateSystem` variable, which always has a `_CoordinateAxes` attribute.

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
 
In this case, the data `Variables` have two coordinate systems, the `LatLonCoordinateSystem` and the `ProjectionCoordinateSystem`.

Note that for projection coordinates, use `AxisType =` `GeoX` and `GeoY`. We also introduce the `_CoordinateZisPositive` attribute, which is used only on vertical `CoordinateAxes` (`AxisType =` `Pressure`, `Height`, or `GeoZ`), to indicate in which direction increasing values of the coordinate go.

**Rule**: To indicate multiple `CoordinateSystems` for a single data `Variable`, you must use `CoordinateSystem` variables and list them from the data `Variable`'s `_CoordinateSystems` attribute.

**Rule**: Use the `GeoX` and `GeoY` `AxisTypes` for projection coordinate axes.

**Rule**: Use the `_CoordinateZisPositive` attribute on vertical `CoordinateAxes` to indicate in whether increasing values of the coordinate go up or down.

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
 
To create a `CoordinateTransform`, we define a `CoordinateTransform` variable (here called `LambertConformalProjection`), which is a dummy variable similar to a `CoordinateSystem` variable (here called `ProjectionCoordinateSystem`). 
The attributes on the `CoordinateTransform` variable are the parameters of the transform. The `CoordinateSystem` variable points to it with a `_CoordinateTransforms` attribute. 
You can have multiple `CoordinateTransforms` for a `CoordinateSystem`.

**Rule**: A `CoordinateTransform` must be explicitly defined by a `CoordinateTransform` variable. It is identified by having the `_CoordinateTransformType` attribute, or by being pointed to by a `CoordinateSystem` variable's `_CoordinateTransforms` attribute.

## Example 5: Reusing Variables for Coordinate System and Coordinate Transform Variables

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
     
Here we are using the `ProjectionCoordinateSystem` variable as both a `CoordinateSystem` variable and a `CoordinateTransform` variable. 
In this case, you must use a `_CoordinateTransformType` attribute to explicitly show that `ProjectionCoordinateSystem` is a `CoordinateTransform` variable.

You can use any `Variable` as the `CoordinateTransform` variable; here's an example using the vertical `CoordinateAxis` to hold a vertical transform:

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
     
Here again we are using the `ProjectionCoordinateSystem` variable as both a `CoordinateSystem` variable and a `CoordinateTransform` variable. 
In addition, there is a vertical transformation on the level `Variable`, pointed to by the `ProjectionCoordinateSystem:_CoordinateTransforms` attribute.

**Rule**: You can turn any `Variable` into a `CoordinateSystem` or `CoordinateTransform` variable.

## Implicit Coordinate Systems
The above attributes allow for explicitly specifying `CoordinateSystems` for data `Variables`. This section defines how `CoordinateSystems` may be implicitly defined.

### Goals
* Use the existing "Coordinate Variable" convention to infer `CoordinateSystems` for legacy files.
* allow minimum annotation to existing datasets to fully specify `CoordinateSystems` and `CoordinateTransforms`.

### Coordinate Variables and Aliases
A `Coordinate` variable is a one dimensional `Variable` with monotonic values that has the same name as its dimension.

A one-dimensional `Variable` with monotonic values can act as a `Coordinate` variable for its dimension, even when it doesn't have the same name as the dimension. 
To indicate this, add an attribute to the variable called `_CoordinateAliasForDimension`, whose value must be the name of its single dimension. 
A dimension may have multiple `Coordinate` variables in this way, which is useful, for example, if the data is a trajectory. `Coordinate` variables created this way are used for implicit `_CoordinateAxes` processing (next section). 

Example:

~~~
   double valtime(record);
     :long_name = "valid time";
     :units = "hours since 1992-1-1";

     :_CoordinateAliasForDimension = "record";
     :_CoordinateAxisType = "Time"; 
~~~

Note that it's very important to also identify the `CoordinateAxis` type.

This can also be used to fix existing files whose `Coordinate` variables were not named the same as their dimension. 
However, if you are writing the file, you should use correctly named `Coordinate` variables when possible.

We will call both regular `Coordinate` variables and ones that have been defined with the `_CoordinateAliasForDimension` attribute `Coordinate` variables.

### Implicit Coordinate System

When there is no `_CoordinateSystems` or `_CoordinateAxes` attribute on a data `Variable`, a list of coordinate axes is constructed from the list of `Coordinate` variables for the data `Variable`. 
If there are 2 or more axes, the `CoordinateSystem` for the variable is found by examining all `CoordinateSystems` and matching exactly its list of `CoordinateAxes`. 
If there is no existing `CoordinateSystem` that matches, one is added, and this is called an *implicit Coordinate System*.

### Assigning CoordinateTransforms

The only way to add a `CoordinateTransform` to an implicit `CoordinateSystem`, is to add a `_CoordinateAxes` or `_CoordinateAxisTypes` attribute to the `CoordinateTransform` variable listing the names or types of `CoordinateAxes`. 
The `CoordinateTransform` will be added to any `CoordinateSystem` that contains all named axes. 

For example, will apply this to any CoordinateSystem that has both GeoX and GeoY Coordinate axes:

~~~
   char ProjectionCoordinateSystem;
     ProjectionCoordinateSystem:_CoordinateTransformType = "Projection";
     ProjectionCoordinateSystem:_CoordinateAxisTypes = "GeoX GeoY";
~~~
For example, will apply to any CoordinateSystem that has the Coordinate Axis named "hybrid":

~~~
   char VerticalCoordinateSystem;
     VerticalCoordinateSystem:_CoordinateTransformType = "Vertical";
     VerticalCoordinateSystem:_CoordinateAxes = "hybrid";
~~~
### Coordinate Transform Variables

A `CoordinateTransform` variable is a container for information about a transformation function from a `CoordinateSystem` to a *reference* `CoordinateSystem`. 
At a minimum it must have a `transform_name` attribute. For CF compatibility, use `grid_mapping_name` or `standard_name`.

~~~
 char Lambert_Conformal_Projection;
 Lambert_Conformal_Projection:transform_name = "lambert_conformal_conic";

 Lambert_Conformal_Projection:standard_parallel = 25.0;
 Lambert_Conformal_Projection:longitude_of_central_meridian = 265.0;

 Lambert_Conformal_Projection:latitude_of_projection_origin = 25.0;
~~~
  
When a `CoordinateSystem` has only one `CoordinateTransform`, the information on the transform may be added directly to the `CoordinateSystem` variable. The variable acts as both a `CoordinateSystem` and a `CoordinateTransform`.

For example:

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
Generally the set of valid transforms are not specified by this `Coordinate`'s convention. 
The [transforms that the nj22 library recognizes](std_horizonal_coord_transforms.html) come from the CF grid mappings and vertical transforms, so these are recommended, when possible, for the actual transform content.

It is often convenient to define the `CoordinateTransform` variable and have it point to the `CoordinateSystems` that use it. 
For this purpose, you can use the `_CoordinateSystems` attribute on a `CoordinateTransform` variable. 
You also need to add the `_CoordinateTransformType` attribute to make sure it is interpreted as a `CoordinateTransform` variable instead of a data `Variable`.

~~~
 int Lambert_Conformal;
  Lambert_Conformal:grid_mapping_name = "lambert_conformal_conic";
  Lambert_Conformal:standard_parallel = 25.0;
  Lambert_Conformal:longitude_of_central_meridian = 265.0;
  Lambert_Conformal:latitude_of_projection_origin = 25.0;
  Lambert_Conformal:_CoordinateTransformType = "Projection";
  Lambert_Conformal:_CoordinateSystems = "ProjectionCoordinateSystem";
~~~
For dealing with implicitly defined `CoordinateSystems`, you can use the `_CoordinateAxes` attribute:
~~~
 int Lambert_Conformal;
  Lambert_Conformal:grid_mapping_name = "lambert_conformal_conic";
  Lambert_Conformal:standard_parallel = 25.0;
  Lambert_Conformal:longitude_of_central_meridian = 265.0;
  Lambert_Conformal:latitude_of_projection_origin = 25.0;
  Lambert_Conformal:_CoordinateTransformType = "Projection";
  Lambert_Conformal:_CoordinateAxes = "y x";
~~~   
This means to apply it to any `CoordinateSystem` that includes the x and y `CoordinateAxes`.
