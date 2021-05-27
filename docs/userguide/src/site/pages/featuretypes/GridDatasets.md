---
title: Grid datasets
last_updated: 2020-04-06
sidebar: userguide_sidebar
toc: false
permalink: grid_datasets.html
---
## Tutorial: The Grid Feature Type

### Scientific Feature Types
The Common Data Model (CDM) Scientific Feature Type layer adds another layer of functionality on top of NetcdfDataset, by specializing in various kinds of data that are common in earth science. The abstract concepts and concrete classes are continually evolving, and we have concentrated on, for obvious reasons, the types of datasets and data that Unidata is most familiar with, mainly from the atmospheric and ocean sciences.

All Scientific Feature Types have georeferencing coordinate systems, from which a location in real physical space and time can be found, usually with reference to the Earth. Each adds special data subsetting methods which cannot be done efficiently or at all in the general case of NetcdfDataset objects.

For more information, see the overview of 
[Scientific Feature Types.](https://www.unidata.ucar.edu/software/netcdf-java/v4.6/reference/FeatureDatasets/Overview.html){:target="_blank"}

The Grid Feature Type
A Grid Coordinate System at a minimum has a Lat and Lon coordinate axis, or a GeoX and GeoY coordinate axis plus a Projection that maps x, y to lat, lon. It usually has a time coordinate axis. It may optionally have a vertical coordinate axis, classified as Height, Pressure, or GeoZ. If it is a GeoZ axis, it may have a Vertical Transform that maps GeoZ to height or pressure. A Grid may also optionally have a Runtime and/or Ensemble coordinate axis.

A `GridDataset` (aka Grid) has a `GridCoordinateSystem`, whose dimensions are all connected, meaning that neighbors in index space are connected neighbors in coordinate space. This means that data values that are close to each other in the real world (coordinate space) are close to each other in the data array, and are usually stored close to each other on disk, making coordinate subsetting easy and efficient.

A `GridDataset` has `Grids` that can be grouped based on common attributes and `GridCoordinateSystems`. Below is the structure for the Grid interface classes, found in the ucar.nc2.grid and ucar.nc2.dataset package:

{% include image.html file="featuretypes/GridStructure.png" alt="Grid" caption="Grid Interface/Class Structure" %}

### Opening a GridDataset


The most general way to open a `GridDataset` is to use the `FeatureDatasetFactoryManager` class. This allows third parties to plug-in alternative implementations of `GridDataset` at runtime. The `wantFeatureType` parameter is checked, logging an error if incompatible. For example:
{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/featuretypes/GridDatasetsTutorial.java&gridDatasetFormat %}
{% endcapture %}
{{ rmd | markdownify }}

  
If you know that the file you are opening is a `GridDataset`, you can call directly:
{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/featuretypes/GridDatasetsTutorial.java&gridFormat %}
{% endcapture %}
{{ rmd | markdownify }}

### Using a GridDataset

Once you have a GridDataset, you can get the grids and their associated coordinate systems:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/featuretypes/GridDatasetsTutorial.java&usingGridDataset %}
{% endcapture %}
{{ rmd | markdownify }}
 
A <b>_GridCoordSystem_</b> wraps a georeferencing coordinate system. It always has 1D or 2D XHoriz and YHoriz axes, and optionally 1D vertical and 1D or 2D time axes. The XHoriz/YHoriz axes will be lat/lon if isLatLon() is true, otherwise they will be GeoX,GeoY with an appropriate Projection. The getBoundingBox() method returns a bounding box from the XHoriz/YHoriz corner points. The getLatLonBoundingBox() method returns the smallest lat/lon bounding box that contains getBoundingBox().

You can use the <b>_GridCoordSystem_</b> to find the value of a grid at a specific lat, lon point:

You can use the `GridCoordinateSystem` to find the indices and coordinates of the 2D grid from the (x,y) projection point:
{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/featuretypes/GridDatasetsTutorial.java&findLatLonVal %}
{% endcapture %}
{{ rmd | markdownify }}
  
Most GridCoordSystems have a CoordinateAxis1DTime time coordinate. If so, you can get the list of dates from it.

A <b>_GridDatatype_</b> is like a specialized Variable that explicitly handles X,Y,Z,T dimensions, which are put into canonical order: (t, z, y, x). It has various convenience routines that expose methods from the GridCoordSystem and VariableDS objects. The main data access method is readDataSlice,  where you can fix an index on any Dimension, or use a -1 to get all the data in that Dimension.

~~~
// get 2D data at timeIndex, levelIndex
Array data = grid.readDataSlice(timeIndex, levelIndex, -1, -1);
~~~

The subset method allows you to create a logical subset of a GeoGrid using index Ranges.
~~~
GridDatatype subset = grid.makeSubset(rt_range, ens_range, null, t_range, z_range, y_range, x_range);
~~~

### Writing a GridDataset to a Netcdf-3 file using CF-1.0 Conventions

Once you have a GridDataset, you can write it as a Netcdf-3 file using the <a href="http://cfconventions.org/" target="_blank">CF Conventions</a>, using <b>_ucar.nc2.dt.grid.NetcdfCFWriter_</b>.

{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/featuretypes/GridDatasetsTutorial.java&readingData %}
{% endcapture %}
{{ rmd | markdownify }}

The `GridSubset` class allows you to create a coordinate value-based subset of a `Grid`.
{% capture rmd %}
{% includecodeblock netcdf-java&docs/userguide/src/test/java/examples/featuretypes/GridDatasetsTutorial.java&CallMakeSubset %}
{% endcapture %}
{{ rmd | markdownify }}

### Writing a GridDataset to a Netcdf-3 file using CF-1.0 Conventions

Once you have a `GridDataset`, you can write it as a Netcdf-3 file using the <a href="http://cfconventions.org/" target="_blank">CF Conventions</a>, using `ucar.nc2.write.NetcdfFormatWriter`.
See the [Writing CDM](https://www.unidata.ucar.edu/software/netcdf-java/v4.6/reference/FeatureDatasets/Overview.html){:target="_blank"} page for a detailed explanation of writing netCDF-3 and netCDF-4 files.
      
### Using ToolsUI to look at Grids

You can use ToolsUI **FeatureTypes/Grids** Tab to view Grid Datasets. This consists of 3 tables that show the Grid DataTypes, the Grid Coordinate systems, and the Coordinate Axes:

{% include image.html file="featuretypes/toolsUIGrid.png" alt="Grid UI" caption="ToolsUI Interface" %}

Use the {% include inline_image.html file="featuretypes/redrawButton.jpg" alt="Redraw button" %}  button to display the grids in the grid viewer window:

{% include image.html file="featuretypes/toolsUIGridView.png" alt="Grid View" caption="ToolsUI Grid Viewer" %}
