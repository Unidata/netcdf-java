---
title: Coordinate attribute convention
last_updated: 2021-06-10
sidebar: userguide_sidebar 
permalink: coord_attr_conv.html
toc: false
---
## Overview of coordinate attribute convention

For a brief overview, see: [CDM Object Model](../developer/cdm_overview.html). The `CoordinateAxis` for a `Variable` must use a subset of the `Variable`'s dimensions.

{% include image.html file="coordsystems/CoordSys.png" alt="Coordinate Systems UML" caption="Coordinate Systems UML" %}

### Goals of `_Coordinate` Attribute Convention

1. Encode coordinate system information into both netcdf-3 and netcdf-4 files.
2. Easily retrofit existing files with coordinate info using NcML.
3. Use the existing "Coordinate variable" convention to infer `CoordinateSystems` for legacy files
4. Allow minimum annotation to existing datasets to fully specify `CoordinateSystems` and `CoordinateTransforms`

### Proposal

1. Add the following "standard attributes" as an endorsed way of encoding coordinate system information.
2. Create an API in both C and Java libraries to make it easy for users to follow these standards. The APIs will be optional, and built on top of the core APIs.
3. Use the attributes and APIs in Unidata software when appropriate.

### See also :

`_Coordinate` Attribute [Examples](coord_attribute_ex.html)

### Contents

1. [Summary of the CDM _Coordinate Conventions](#summary-of-the-cdm-_coordinate-conventions)
2. [Coordinate Attributes Definition](#coordinate-attributes-definition)
3. [Summary of Rules for Processing Coordinate Attributes](#summary-of-rules-for-processing-coordinate-attributes)

### Summary of the CDM _Coordinate Conventions:

A `CoordinateTransform` is defined by creating a `CoordinateTransform` variable [example](coord_attribute_ex.html#example-4-adding-coordinate-transforms) and reference. 
The attributes of the `CoordinateTransform` variable become the parameters of the transform. A variable is a `CoordinateTransform` variable if one (or both) of these is true:

1. It has a `_CoordinateTransformType` or `_CoordinateAxisTypes` attribute.
2. It is listed in a `_CoordinateTransforms` attribute from any variable in the file.

Any `Variable` can be a `CoordinateTransform` variable, as it is just a container for attributes, i.e. the data values of the `Variable` are not used by the transform.
It is common to use a vertical coordinate to be a `CoordinateTransform` variable. Using a _dummy_ variable (with no useful data in it) is also common. The examples here are done with dummy variables.

The CF `CoordSystemBuilder` will add the `_CoordinateTransformType` attribute upon recognizing a standard name, so adding that is optional if you are using CF conventions. 
CF also requires that you use the vertical coordinate as the `CoordinateTransform` variable, so that the transform will be added to any `CoordinateSystem` that uses that vertical coordinate. 
The CF `CoordSystemBuilder` will thus add the `_CoordinateAxes = "vertCoordName"` attribute to indicate this.

### Coordinate Attributes Definition

#### _CoordinateAliasForDimension

A one-dimensional variable with monotonic values can act as a `Coordinate` variable for its dimension, even when it doesn't have the same name as the dimension. 
To indicate this, add an attribute to the variable called `_CoordinateAliasForDimension`, whose value must be the name of its single dimension. 
A dimension may have multiple `Coordinate` variables in this way, for example if the data is a trajectory.

~~~
   double valtime(record);
     :long_name = "valid time";
     :units = "hours since 1992-1-1";

  :_CoordinateAliasForDimension = "record";
     :_CoordinateAxisType = "Time";
~~~

#### _CoordinateAxes

This attribute lists (in any order) names of `CoordinateAxis` variables. When a `Variable` is listed in a `_CoordinateAxes` attribute, it is made into a `CoordinateAxis`.

The attribute value must be a space-separated list of names of `CoordinateAxis` variables in the same dataset:

~~~
 _CoordinateAxes = "time lev lat lon";
~~~

#### _CoordinateAxisType

This attribute is used on a `CoordinateAxis` variable to specify that it is a space or time coordinate, such as lat, lon, altitude or time. 
Currently the valid values are `Lat`, `Lon`, `Height`, `Pressure`, `Time`, `GeoX`, `GeoY`, `GeoZ`, `RadialElevation`, `RadialAzimuth`, or `RadialDistance`. 
This is the preferred way to make a `Variable` into a `CoordinateAxis` (the other way is to list the variable in a `_CoordinateAxes` attribute).

The attribute value must be one of the valid `AxisTypes`, for example:

~~~
 _CoordinateAxisType = "Lat";
~~~

#### _CoordinateAxisTypes

This attribute is used on a `CoordinateTransform` variable to specify that the transform applies to any `CoordinateSystem` with the specified list of `AxisTypes`.

The attribute value must be a list of the valid `AxisTypes` (see `ucar.nc2.constants.AxisType`):

~~~
 _CoordinateAxisTypes = "GeoZ Time";
~~~
 
#### _CoordinateSystems

When many data variables use the same `CoordinateSystem` it is convenient to factor out the information into one place. 
We create a dummy variable which holds all of the information, called the `CoordinateSystem` variable. 
The `CoordinateSystems` attribute is used on a data variable to point to its `CoordinateSystem` variable(s). 
This is the only way to indicate multiple `CoordinateSystems` for the same data variable.

The attribute value must be a space-separated list of names of `CoordinateSystem` variables in the same dataset:

~~~
 _CoordinateSystems = "ProjectionCoordinateSystem LatLonCoordinateSystem";

  char ProjectionCoordinateSystem;
  ProjectionCoordinateSystem:_CoordinateAxes = "time depth_below_surface y x";
  ProjectionCoordinateSystem:_CoordinateTransforms = "Lambert_Conformal_Projection";
~~~

#### _CoordinateSystemFor

This is a way to assign explicit `CoordinateSystems` to a set of variables, without having to name each variable. 
The value of the attribute is a list of dimensions. 
A data variable that does not have an explicit `_CoordinateSystem` or `CoordinateAxes` attribute will be assigned this `CoordinateSystem`, if it contains exactly the listed dimensions.

~~~
  <variable name="coordSysVar4D" type="int" shape="">
  <attribute name="_CoordinateAxes" value="x y zpos time"/>
  <attribute name="_CoordinateTransforms" value="zpos"/>
  <attribute name="_CoordinateSystemFor" value="xpos ypos zpos time"/>
  </variable>
~~~

~~~
  <variable name="coordSysVar3D" type="int" shape="">
  <attribute name="_CoordinateAxes" value="x y time"/>
  <attribute name="_CoordinateSystemFor" value="xpos ypos time"/>
  </variable>
~~~

#### _CoordinateTransforms
The `_CoordinateTransforms` attribute is used only on `CoordinateSystem` variables and is used to indicate how to transform the `CoordinateSystem` to a reference `CoordinateSystem`.
A reference `CoordinateSystem` is one that uses `Latitude`, `Longitude` for the horizontal axes, and `Height` or `Pressure` for the vertical axes. 
To hold the transform information, create a dummy variable called the `CoordinateTransform` variable. 
This `CoordinateTransform` variable always has a name that identifies the transform, and any attributes needed for the transformation.

The attribute value must be a space-separated list of names of `CoordinateTransform` variables in the same dataset.

~~~
 _CoordinateTransforms = "LambertProjection HybridSigmaVerticalTransform";

 char LambertProjection;
 LambertProjection:transform_name = "lambert_conformal_conic";
 LambertProjection:standard_parallel = 25.0;

 LambertProjection:longitude_of_central_meridian = 265.0;
 LambertProjection:latitude_of_projection_origin = 25.0;
~~~

This is a general mechanism for any transformation a file writer wants to define. The nj22 library has a [set of transforms that it recognizes](std_horizonal_coord_transforms.html), mostly based on the [CF-1 conventions](http://www.cgd.ucar.edu/cms/eaton/cf-metadata){:target="_blank"}. Attributes should be String, integer, or double valued.


#### _CoordinateTransformType
This attribute is used to unambiguously indicate that a variable is a `CoordinateTransform` variable (the other way is to list the variable in a `_CoordinateTransforms` attribute).

The attribute value must be one of the valid `Transform` types (see `ucar.nc2.dataset.TransformType`). Currently the valid values are `Projection` or `Vertical`.

~~~
 _CoordinateTransformType = "Projection";
~~~

#### _CoordinateZisPositive

Only used for vertical coordinate axes to disambiguate direction *up* or *down* of increasing coordinate values.

The attribute value must equal **"up"** or **"down"**.

~~~
 _CoordinateZisPositive = "down";
~~~
 
### Summary of Rules for Processing Coordinate Attributes

#### CoordinateAxis Variable

May have attributes:

* `_CoordinateAxisType`
* `_CoordinateAliasForDimension`
* `_CoordinateZisPositive`

A `Variable` is made into a `CoordinateAxis` if one of these is true:

1. It has any of the `_CoordinateAxisType`, `_CoordinateAliasForDimension`, or `_CoordinateZisPositive` attributes.
2. It is a `Coordinate` variable
3. It is listed in a `_CoordinateAxes` attribute from any variable in the file.

A `Variable` is a `Coordinate` variable if it is one dimensional and one of these is true:

1. It has the same name as its dimension.
2. It has the `_CoordinateAliasForDimension` attribute.

#### CoordinateSystem Variable

May have attributes:

* `_CoordinateAxes` (required, must be a complete list of axes, must have at least one axis).
* `_CoordinateSystemFor` (list of dimensions) will be assigned to any variable which contains exactly these dimensions.
* `_CoordinateTransforms`

A variable is a `CoordinateSystem` variable if one of these is true:

1. It has a `_CoordinateTransforms` attribute.
2. Its has a `_CoordinateSystemFor` attribute.
3. It is listed in a `_CoordinateSystems` attribute from any variable in the file.

#### CoordinateTransform Variable

May have attributes:

* `_CoordinateTransformType`
* `_CoordinateSystems` apply to these `CoordinateSystems`
* `_CoordinateAxes` apply to any `CoordinateSystems` that contain all these axes
* `_CoordinateAxisTypes` apply to any `CoordinateSystems` that contain all these types of axes

A variable is a `CoordinateTransform` variable if one of these is true:

1. It has a `_CoordinateTransformType` or `_CoordinateAxisTypes` attribute.
2. It is listed in a `_CoordinateTransforms` attribute from any variable in the file.

#### Data Variables
May have attributes:

* `_CoordinateSystems`
* `_CoordinateAxes`

You should use one or the other. If both are present, `_CoordinateSystems` is used.

A data variable is assigned one or more `CoordinateSystems` in the following way:

1. If it has a `_CoordinateSystems` attribute, it is assigned the listed `CoordinateSystems`, and no further processing is done.
2. If it has a `_CoordinateAxes` attribute, it will have one `CoordinateSystem` consisting of the listed `_CoordinateAxes` plus any `Coordinate` variables that it uses which are not listed. 
It must have at least 2 axes, otherwise it will have one `CoordinateSystem` consisting of the `Coordinate` variables that it uses.

If `CoordSysBuilder.useMaximalCoordSys` is true (default is true), and all the following conditions are true:
1.  A Data Variable has none or one implicit `CoordinateSystem`.
2.  Its implicit `CoordinateSystem` (if it exists) has fewer axes than the rank of the `Variable`.
Then all `CoordinateAxes` are examined, and a `CoordinateSystem` is made out of all that fit the `Variable`. 
3. If this `CoordinateSystem` has 2 or more axes, then it is assigned to the `Variable`. This is called the maximal algorithm.

### Notes

1. Data Variable `CoordinateAxes` may be partial listing, thus `Coordinate` variables will be added.
2. Variables of type `Structure` cannot be a `CoordinateAxes`.
3. A `CoordinateSystem` is defined by its list of `CoordinateAxes`, so two `CoordinateSystems` can't have the same list of axes.
4. `_Coordinate` attributes will take precedence over conventions in our own decoding. Other software may implement differently.
5. When `CoordinateSystems` have been added, to prevent adding them again, NcML writing adds the global attribute `:Conventions = "_Coordinates"`.

#### Coordinate Axis Types

(see `ucar.nc2.constants.AxisType`)

|---
| AxisType | description | order
|:-|:-|:-
| RunTime | model run time | 0
| Ensemble | model ensemble | 1
| Time |valid time | 2
| GeoZ | vertical coordinate | 3
| Height | vertical height, convertible to meters | 3
| Pressure | vertical pressure, converrtible to hPa |3
| GeoY | projection y coordinate | 4
| Lat | geodesic latitude | 4
| GeoX | projection x coordinate | 5
| Lon | geodesic longitude | 5
| RadialAzimuth | polar azimuth | 6
| RadialDistance | polar distance | 7
| RadialElevation | polar elevation | 8
