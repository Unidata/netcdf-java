---
title: _Coordinate Attribute Convention Reference 
last_updated: 2019-06-27
sidebar: netcdfJavaTutorial_sidebar 
permalink: coord_attr_conv.html
toc: false
---
 
{% include image.html file="netcdf-java/tutorial/coordsystems/CoordSys.png" alt="Coordinate Systems UML" caption="Coordinate Systems UML" %}

For an overview, see: [CDM Object Model]( /common_data_model_overview.html){:target="_blank"}. The Coordinate Axes for a Variable must use a subset of the Variables's dimensions.

## Goals of _Coordinate Attribute Convention

1. Encode the above coordinate system information into both netcdf-3 and netcdf-4 files.
2. Easily retrofit existing files with coordinate info using NcML.
3. use the existing "Coordinate Variable" Convention to infer Coordinate Systems for legacy files
4. allow minimum annotation to existing datasets to fully specify Coordinate Systems and Transforms

### Proposal

1. Add the following "standard attributes" as an endorsed way of encoding coordinate system information.
2. Create an API in both C and Java libraries to make it easy for users to follow these standards. The APIs will be optional, and built on top of the core APIs.
3. Use the attributes and APIs in Unidata software when appropriate.

### See also :

_Coordinate [Attribute Examples](coord_attribute_ex.html){:target="_blank"}

### Contents

1. [Summary of the CDM _Coordinate Conventions](#summary-of-the-cdm-_coordinate-conventions)
2. [Coordinate Attributes Definition](#coordinate-attributes-definition)
3. [Summary of Rules for Processing Coordinate Attributes](#summary-of-rules-for-processing-coordinate-attributes)

### Summary of the CDM _Coordinate Conventions:

A Transform is defined by creating a Coordinate Transform Variable [Example](coord_attribute_ex.html#example-4-adding-coordinate-transforms) and [Reference](#_coordinate-attribute-convention-reference). The attributes of the Coordinate Transform Variable become the parameters of the transform. A variable is a Coordinate Transform Variable if one (or both) of these is true:

1. It has a <b>__CoordinateTransformType_</b>_ or </b>__CoordinateAxisTypes attribute_</b>.
2. It is listed in a <b>__CoordinateTransforms_</b> attribute from any variable in the file.

Any Variable can be a Coordinate Transform Variable, as it is just a container for attributes, i.e. the data values of the Variable are not used by the transform. It is common to use a vertical coordinate to be a Coordinate Transform Variable. Using a _dummy variable_ (with no useful data in it) is also common. The example here are done with dummy variables.

The CF CoordSysBuilder will add the <b>__CoordinateTransformType_</b> attribute upon recognizing a standard_name, so adding that is optional if you are using CF Conventions. CF also requires that you use the vertical coordinate as the Coordinate Transform Variable, so that the transform will be added to any Coordinate System that uses that vertical coordinate. The CF CoordSysBuilder will thus add the _CoordinateAxes = "vertCoordName" attribute to indicate this.

A Transform is defined by creating a Coordinate Transform Variable (see Example and Reference). The attributes of the Coordinate Transform Variable become the parameters of the transform. A variable is a Coordinate Transform Variable if one (or both) of these is true:

1. It has a <b>_CoordinateTransformType_</b> or <b>__CoordinateAxisTypes_</b> attribute.
2. It is listed in a <b>__CoordinateTransforms_</b> attribute from any variable in the file.

Any Variable can be a Coordinate Transform Variable, as it is just a container for attributes, i.e. the data values of the Variable are not used by the transform. It is common to use a vertical coordinate to be a Coordinate Transform Variable. Using a dummy variable (with no useful data in it) is also common. The example here are done with dummy variables.

The CF CoordSysBuilder will add the _CoordinateTransformType attribute upon recognizing a standard_name, so adding that is optional if you are using CF Conventions. CF also requires that you use the vertical coordinate as the Coordinate Transform Variable, so that the transform will be added to any Coordinate System that uses that vertical coordinate. The CF CoordSysBuilder will thus add the <b>__CoordinateAxes = "vertCoordName"_</b> attribute to indicate this.

### Coordinate Attributes Definition

#### _CoordinateAliasForDimension

A one-dimensional variable with monotonic values can act as a coordinate variable for its dimension, even when it doesnt have the same name as the dimension. To indicate this, add an attribute to the variable called _CoordinateAliasForDimension, whose value must be the name of its single dimension. A dimension may have multiple coordinate variables in this way, for example if the data is a trajectory.

~~~
   double valtime(record);
     :long_name = "valid time";
     :units = "hours since 1992-1-1";

  :_CoordinateAliasForDimension = "record";
     :_CoordinateAxisType = "Time";
~~~

#### _CoordinateAxes

This attribute lists (in any order) names of Coordinate Axis Variables. When a Variable is listed in a <b>__CoordinateAxes_</b> attribute, it is made into a CoordinateAxis.

The attribute value must be a space-separated list of names of Coordinate Axis Variables in the same dataset:

~~~
 _CoordinateAxes = "time lev lat lon";
~~~

#### _CoordinateAxisType

This attribute is used on a Coordinate Axis variable to specify that it is a space or time coordinate, such as lat, lon, altitude or time. Currently the valid values are Lat, Lon, Height, Pressure, Time, GeoX, GeoY, GeoZ, RadialElevation, RadialAzimuth, or RadialDistance. This is the preferred way to make a Variable into a CoordinateAxis (the other way is to list the variable in a _CoordinateAxes attribute).

The attribute value must be one of the valid Axis Types, for example:

~~~
 _CoordinateAxisType = "Lat";
~~~

#### _CoordinateAxisTypes

This attribute is used on a Coordinate Transform variable to specify that the Transform applies to any Coordinate System with the specified list of Axis types.

The attribute value must be a list of the valid Axis types (see <b>_ucar.nc2.dataset.AxisType_</b>):

~~~
 _CoordinateAxisTypes = "GeoZ Time";
~~~
 
#### _CoordinateSystems

When many data Variables use the same Coordinate System it is convenient to factor out the information into one place. We create a dummy Variable which holds all of the information, called the <b>_Coordinate System Variable_</b> .The <b>_CoordinateSystems_</b> attribute is used on a data Variable to point to its Coordinate System Variable(s). This is the only way to indicate multiple Coordinate Systems for the same data Variable.

The attribute value must be a space-separated list of names of Coordinate System Variables in the same dataset:

~~~
 _CoordinateSystems = "ProjectionCoordinateSystem LatLonCoordinateSystem";


  char ProjectionCoordinateSystem;
 ProjectionCoordinateSystem:_CoordinateAxes = "time depth_below_surface y x";
  ProjectionCoordinateSystem:_CoordinateTransforms = "Lambert_Conformal_Projection";
~~~

#### _CoordinateSystemFor

This is a way to assign explicit Coordinate System to a set of variables, without having to name each variable. The value of the attribute is a list of dimensions. A data variable that does not have an explicit _CoordinateSystem or CoordinateAxes attribute will be assigned this CoordinateSystem, if it contains exactly the listed dimensions.

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
The <b>__CoordinateTransforms_</b> attribute is used only on Coordinate System Variables and is used to indicate how to transform the Coordinate System to a <b>_Reference Coordinate System_</b>. A _Reference Coordinate System_ is one that uses _Latitude_, _Longitude_ for the horizontal axes, and _Height_ or _Pressure_ for the vertical axes. To hold the transform information, create a dummy Variable called the <b>_Coordinate Transform Variable_</b>. This Coordinate Transform variable always has a name that identifies the transform, and any attributes needed for the transformation.

The attribute value must be a space-separated list of names of Coordinate Transform Variables in the same dataset.

~~~
 _CoordinateTransforms = "LambertProjection HybridSigmaVerticalTransform";

  char LambertProjection;
 LambertProjection:transform_name = "lambert_conformal_conic";
 LambertProjection:standard_parallel = 25.0;

  LambertProjection:longitude_of_central_meridian = 265.0;
 LambertProjection:latitude_of_projection_origin = 25.0;
~~~

This is a general mechanism for any transformation a file writer wants to define. The nj22 library has a [set of transforms that it recognizes](std_horizonal_coord_transforms.html), mostly based on the <a href="http://www.cgd.ucar.edu/cms/eaton/cf-metadata/">CF-1 conventions</a>. Attributes should be String, integer, or double valued.


#### _CoordinateTransformType
This attribute is used to unambiguously indicate that a variable is a Coordinate Transform Variable (the other way is to list the variable in a _CoordinateTransforms attribute).

The attribute value must be one of the valid Transform types (see ucar.nc2.dataset.TransformType) Currently the valid values are Projection or Vertical.

~~~
 _CoordinateTransformType = "Projection";
~~~

#### _CoordinateZisPositive

Only used for vertical coordinate axes to disambiguate direction <b>_up_</b> or <b>_down_</b> of increasing coordinate values.

The attribute value must equal <b>_"up"_</b> or <b>_"down"_</b>.

~~~
 _CoordinateZisPositive = "down";
~~~
 
### Summary of Rules for Processing Coordinate Attributes

#### Coordinate Axis Variable

May have attributes:

* <b>__CoordinateAxisType_</b>
* <b>__CoordinateAliasForDimension_</b>
* <b>__CoordinateZisPositive_</b>

A Variable is made into a Coordinate Axis if one of these is true:

1. It has any of the <b>__CoordinateAxisType_</b>, <b>__CoordinateAliasForDimensionv, or <b>__CoordinateZisPositive_</b> attributes.
2. It is a coordinate variable
3. It is listed in a <b>__CoordinateAxes_</b> attribute from any variable in the file.

A Variable is a <b>_coordinate variable_</b> if it is one dimensional and one of these is true:

1. It has the same name as its dimension.
2. It has the _CoordinateAliasForDimension attribute.

#### Coordinate System Variable

May have attributes:

* <b>__CoordinateAxes (required, must be a complete list of axes, must have at least one axis)._</b>
* <b>__CoordinateSystemFor (list of dimensions) will be assigned to any Variable which contains exactly these dimensions._</b>
* <b>__CoordinateTransforms_</b>

A variable is a Coordinate System Variable if one of these is true:

It has a <b>__CoordinateTransforms_</b> attribute.
Its has a <b>__CoordinateSystemFor_</b> attribute.
It is listed in a <b>__CoordinateSystems_</b> attribute from any variable in the file.

#### Coordinate Transform Variable

May have attributes:

* <b>__CoordinateTransformType_</b>
* <b>__CoordinateSystems_</b> apply to these Coordinate Systems
* <b>__CoordinateAxes_</b> apply to any Coordinate Systems that contain all these axes
* <b>__CoordinateAxisTypes_</b> apply to any Coordinate Systems that contain all these types of axes

A variable is a Coordinate Transform Variable if one of these is true:

It has a <b>__CoordinateTransformType_</b> or <b>__CoordinateAxisTypes attribute_</b>.
It is listed in a <b>__CoordinateTransforms_</b> attribute from any variable in the file.
Data Variables
May have attributes:

1. <b>__CoordinateSystems_</b>
2. <b>__CoordinateAxes_</b>

You should use one or the other. If both are present, <b>__CoordinateSystems_</b> is used.

A Data Variable is assigned one or more Coordinate Systems in the following way:

If it has a <b>__CoordinateSystems_</b> attribute, it is assigned the listed Coordinate Systems, and no further processing is done.
If it has a <b>__CoordinateAxes_</b> attribute, it will have one Coordinate System consisting of the listed <b>__CoordinateAxes_</b> plus any Coordinate Variables that it uses which are not listed. Must have at least 2 axes.
Otherwise it will have one Coordinate System consisting of the Coordinate Variables that it uses.
If <b>_CoordSysBuilder.useMaximalCoordSys_</b> is true (default is true), and all the following conditions are true:
A Data Variable has none or one implicit Coordinate System.
Its implicit Coordinate System (if it exists) has fewer axes than the rank of the Variable.
Then all Coordinate Axes are examined, and a Coordinate System is made out of all that fit the Variable. If this Coordinate System has 2 or more axes, then it is assigned to the Variable. This is called the maximal algorithm.

### Notes

1. Data Variable <b>_CoordinateAxes_</b> may be partial listing, Coordinate variables will be added. ??
2. Variables of type Structure cannot be a coordinate axis.
3. A Coordinate System is defined by its list of Coordinate Axes, so two Coordinate System can't have same list of axes.
4. _Coordinate attributes will take precedence over Conventions in our own decoding. Other software may implement differently.
5. When Coordinate Systems have been added, to prevent adding again, NcML writing adds the global attribute <b>_:Conventions = "_Coordinates"_</b>. ??

#### Coordinate Axis Types

(see <b>_ucar.nc2.constants.AxisType_</b>)

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
