---
title: Standard Vertical Coordinate Transforms
last_updated: 2019-07-23
sidebar: netcdfJavaTutorial_sidebar 
permalink: std_vertical_coord_transforms.html
toc: false
---


This page documents the vertical coordinate transforms that are standard in CDM. 
These follow the [CF-1.0 Convention](http://cfconventions.org/Data/cf-conventions/cf-conventions-1.6/build/cf-conventions.html#appendix-grid-mappings){:target="_blank"}, where they are called Dimensionless Vertical Coordinates.
The purpose of a vertical transform is to define a function to calculate the pressure or height coordinate of each grid point in a data variable.
This typically creates a 3D vertical coordinate, one which varies at each grid point.
The dimensionless vertical coordinate in contrast, is 1 dimensional, and thus easier to work with and smaller to store in the file.

To follow CF, typically one adds transform information to the vertical coordinate, for example:

~~~
double s_rho(s_rho=20);
  :long_name = "S-coordinate at RHO-points";
  :valid_min = -1.0; // double
  :valid_max = 0.0; // double

  :standard_name = "ocean_s_coordinate";
  :positive = "up";
  :formula_terms = "s: s_rho eta: zeta depth: h a: theta_s b: theta_b depth_c: hc";
~~~

This example is a CF `ocean_s_coordinate` vertical coordinate.
The formula terms, explained in the CF doc, point to variables in the same file, which the calculation needs:

~~~
Definition:
  z(n,k,j,i) = eta(n,j,i)*(1+s(k)) + depth_c*s(k) +  (depth(j,i)-depth_c)*C(k)  
~~~

So in this example, anywhere you see `s(k)` in the definition, one uses the variable `s_rho` in the calculation.
Similarly for the rest:

~~~
Definition term		actual variable name
---------------		-------------------
s 					s_rho 
eta 					zeta 
depth 				h 
a 					theta_s 
b 					theta_b 
depth_c 				hc
~~~

All of these variables must exist in your file, and be the proper dimension etc, as spelled out in the CF doc.
Note that the file writer must construct the `formula_terms` attribute with the correct variable names.
The CDM, as well as other software that implements this part of the CF spec, will use the above information to calculate the 3D vertical coordinate.

This 3D vertical coordinate can be obtained in the CDM library by opening the file as a `NetcdfDataset`, and examining the `CoordinateSystem` attached to each `VariableDS`.
Look through the transforms from `CoordinateSystem.getCoordinateTransforms()` for the vertical transform (class `ucar.nc2.dataset.VerticalCT`).
For performance, the actual work is not done until you call `VerticalTransform vy = VerticalCT.makeVerticalTransform()`, and then `VerticalTransform.getCoordinateArray()` to get the 3D coordinate array.

To summarize, in order for CF Vertical transforms to work in the CDM, you must:

* Add the `standard_name`, `positive`, and `formula_terms` attributes to the vertical ccoordinate.
* Add to your file all the variables required by the transform definition.
* If you are writing your own software that needs the 3D pressure/height values, follow the above steps to retrieve the 3D vertical coordinate.

### Resources

* Standard vertical transforms are documented on this page. You can also [implement your own](coord_transform.html#implementing-a-verticaltransform).
* You may also be interested in [Standard Horizontal Coordinate Transforms](std_horizonal_coord_transforms.html).
* The CDM [_Coordinate Conventions](coord_attr_conv.html)

## Standard Vertical Transforms

Attribute names follow the CF Conventions Appendix D ([Vertical Transforms](http://cfconventions.org/Data/cf-conventions/cf-conventions-1.6/build/cf-conventions.html#dimensionless-v-coord){:target="_blank"})) and Appendix F ([Grid Mappings](http://cfconventions.org/Data/cf-conventions/cf-conventions-1.6/build/cf-conventions.html#appendix-grid-mappings){:target="_blank"}).
See that document for details on the meanings of the formula terms.

These are examples of placing the Coordinate Transform parameters on the corresponding vertical coordinate (required by CF).
If you are using CF Conventions, you do not have to add the _Coordinate attributes, as they will be added automatically in the CoordSysBuilder.

### atmosphere_ln_pressure_coordinate

~~~
double levCoord(levCoord=26);
    :long_name = "log pressure levels";
    :units = "";
    :positive = "down";
    :standard_name = "atmosphere_ln_pressure_coordinate";
    :formula_terms = "p0: P0 lev: levCoord";
~~~
  
atmosphere_ln_pressure_coordinate transform only works in CF Conventions.
Required attributes: `standard_name`, `formula_terms`

### atmosphere_hybrid_height_coordinate

~~~
double lev(lev=26);
    :long_name = "hybrid hybrid height coordinate";
    :units = "m";
    :positive = "up";
    :standard_name = "atmosphere_hybrid_height_coordinate";
    :formula_terms = "a: varA b: varB orog: orography";
    :_CoordinateAxisType = "GeoZ";
    :_CoordinateZisPositive = "up;
    :_CoordinateTransformType = "Vertical";
    :_CoordinateAxes = "lev";
~~~

Required attributes: `standard_name`, `formula_terms`, `_CoordinateTransformType`, `_CoordinateAxes`

### atmosphere_hybrid_sigma_pressure_coordinate

~~~
double lev(lev=26);
    :long_name = "hybrid level at midpoints (1000*(A+B))";
    :units = "";
    :positive = "down";
    :standard_name = "atmosphere_hybrid_sigma_pressure_coordinate";
    :formula_terms = "a: hyam b: hybm p0: P0 ps: PS";
    :_CoordinateAxisType = "GeoZ";
    :_CoordinateZisPositive = "down;
    :_CoordinateTransformType = "Vertical";
    :_CoordinateAxes = "lev";
~~~

Required attributes: `standard_name`, `formula_terms`, `_CoordinateTransformType`, `_CoordinateAxes`

or

~~~
double lev(lev=26);
    :long_name = "hybrid level at midpoints (1000*(A+B))";
    :units = "";
    :positive = "down";
    :standard_name = "atmosphere_hybrid_sigma_pressure_coordinate";
    :formula_terms = "ap: hyam b: hybm p0: P0";
    :_CoordinateAxisType = "GeoZ";
    :_CoordinateZisPositive = "down;
    :_CoordinateTransformType = "Vertical";
    :_CoordinateAxes = "lev";
~~~

Required attributes: `standard_name`, `formula_terms`, `_CoordinateTransformType`, `_CoordinateAxes`

### atmosphere_sigma_coordinate

~~~
float level(level=2);
    :units = "";
    :long_name = "sigma at layer midpoints";
    :positive = "down";
    :standard_name = "atmosphere_sigma_coordinate";
    :formula_terms = "sigma: level ps: PS ptop: PTOP";
    :_CoordinateAxisType = "GeoZ";
    :_CoordinateZisPositive = "down";
    :_CoordinateTransformType = "Vertical";
    :_CoordinateAxes = "level";
~~~

Required attributes: `standard_name`, `formula_terms`, `_CoordinateTransformType`, `_CoordinateAxes`

### ocean_s_coordinate

~~~ 
double s_rho(s_rho=20);
    :long_name = "S-coordinate at RHO-points";
    :units = "";
    :positive = "up";
    :standard_name = "ocean_s_coordinate";
    :formula_terms = "s: s_rho eta: zeta depth: h a: theta_s b: theta_b depth_c: hc";
    :_CoordinateAxisType = "GeoZ";
    :_CoordinateZisPositive = "up"; 
    :_CoordinateTransformType = "Vertical";
    :_CoordinateAxes = "s_rho";
~~~

Required attributes: `standard_name`, `formula_terms`, `_CoordinateTransformType`, `_CoordinateAxes`

### ocean_s_coordinate_g1

~~~
char OceanSG1_Transform_s_rho;
    :standard_name = "ocean_s_coordinate_g1";
    :formula_terms = "s: s_rho C: Cs_r eta: zeta depth: h depth_c: hc";
    :height_formula = "height(x,y,z) =  depth_c*s(z) + (depth([n],x,y)-depth_c)*C(z) + eta(x,y)*(1+(depth_c*s(z) + (depth([n],x,y)-depth_c)*C(z))/depth([n],x,y))";
    :Eta_variableName = "zeta";
    :S_variableName = "s_rho";
    :Depth_variableName = "h";
    :Depth_c_variableName = "hc";
    :c_variableName = "Cs_r";
~~~

Required attributes: `standard_name`, `formula_terms`
The other are added for extra readability.

### ocean_s_coordinate_g2

~~~
char OceanSG2_Transform_s_rho;
    :standard_name = "ocean_s_coordinate_g2";
    :formula_terms = "s: s_rho C: Cs_r eta: zeta depth: h depth_c: hc";
    :height_formula = "height(x,y,z) = eta(x,y) + (eta(x,y) + depth([n],x,y)) * ((depth_c*s(z) + depth([n],x,y)*C(z))/(depth_c+depth([n],x,y)))";
    :Eta_variableName = "zeta";
    :S_variableName = "s_rho";
    :Depth_variableName = "h";
    :Depth_c_variableName = "hc";
    :c_variableName = "Cs_r";
~~~
Required attributes: `standard_name`, `formula_terms`.
The other are added for extra readability.

### ocean_sigma_coordinate

~~~
float zpos(zpos=22);
    :long_name = "Sigma Layer";
    :units = "";
    :positive = "up";
    :standard_name = "ocean_sigma_coordinate";
    :formula_terms = "sigma: zpos eta: elev depth: depth";
    :_CoordinateAxisType = "GeoZ";
    :_CoordinateZisPositive = "up";
    :_CoordinateTransformType = "Vertical";
    :_CoordinateAxes = "zpos";
~~~

Required attributes: `standard_name`, `formula_terms`, `_CoordinateTransformType`, `_CoordinateAxes`

### explicit_field

~~~
char ExplicitField;
    :standard_name = "explicit_field";  // canonical transform name
    :existingDataField = "ght_hybr";  // must be a 3 or 4D pressure / height / geopotential height field
    :_CoordinateTransformType = "vertical"; // unambiguouly identifies it as vertical transform
    :_CoordinateAxes = "hybr"; // attach transform to any coord sys with the "hbyr" axis.
~~~

Required attributes: `standard_name`, `existingDataField`,  `_CoordinateTransformType`, `_CoordinateAxes`
This is not part of CF, but a way to mark an existing 3D (4D if time dependent) field as the vertical coordinate.

## Using Vertical Transforms

~~~java
public void testAtmHybrid() throws java.io.IOException, InvalidRangeException {
  GridDataset gds = ucar.nc2.dt.grid.GridDataset.open( TestAll.cdmUnitTestDir + "conventions/cf/ccsm2.nc"); 
  GridDatatype grid = gds.findGridDatatype("T");
  GridCoordSystem gcs = grid.getCoordinateSystem();

  VerticalTransform vt = gcs.getVerticalTransform();
  CoordinateAxis1DTime taxis = gcs.getTimeAxis1D();
  for (int t=0; t<taxis.getSize(); t++) {
    System.out.printf("vert coord for time = %s%n", taxis.getTimeDate(t));
    ArrayDouble.D3 ca = vt.getCoordinateArray(t);
    doSomething(ca);
  }
}
~~~