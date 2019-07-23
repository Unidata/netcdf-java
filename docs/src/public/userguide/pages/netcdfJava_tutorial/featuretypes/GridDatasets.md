---
title: Grid Datasets
last_updated: 2019-07-22
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: grid_datasets.html
---
## Tutorial: The Grid Feature Type

### Scientific Feature Types
The Common Data Model (CDM) Scientific Feature Type layer adds another layer of functionality on top of NetcdfDataset, by specializing in various kinds of data that are common in earth science. The abstract concepts and concrete classes are continually evolving, and we have concentrated on, for obvious reasons, the types of datasets and data that Unidata is most familiar with, mainly from the atmospheric and ocean sciences.

All Scientific Feature Types have georeferencing coordinate systems, from which a location in real physical space and time can be found, usually with reference to the Earth. Each adds special data subsetting methods which cannot be done efficiently or at all in the general case of NetcdfDataset objects.

Also see overview of Scientific Feature Types.

The Grid Feature Type
A Grid Coordinate System at a minimum has a Lat and Lon coordinate axis, or a GeoX and GeoY coordinate axis plus a Projection that maps x, y to lat, lon. It usually has a time coordinate axis. It may optionally have a vertical coordinate axis, classified as Height, Pressure, or GeoZ. If it is a GeoZ axis, it may have a Vertical Transform that maps GeoZ to height or pressure. A Grid may also optionally have a Runtime and/or Ensemble coordinate axis.

A GridDatatype (aka GeoGrid or just Grid) has a Grid Coordinate System, whose dimensions are all connected, meaning that neighbors in index space are connected neighbors in coordinate space. This means that data values that are close to each other in the real world (coordinate space) are close to each other in the data array, and are usually stored close to each other on disk, making coordinate subsetting easy and efficient.

A Grid Dataset has Grids that are grouped into Gridsets based on common Grid Coordinate Systems. Here is the UML for the Grid interface classes, found in the ucar.nc2.dt package:

{% include image.html file="netcdf-java/tutorial/feature_types/Grid.png" alt="Grid" caption="Grid Interface" %}

### Opening a GridDataset

The most general way to open a <b>_GridDataset_</b> is to use the <b>_FeatureDatasetFactoryManager_</b> class. This allows third parties to plug-in alternative implementations of <b>_GridDataset_</b> at runtime. For example:
~~~
  Formatter errlog = new Formatter();
  FeatureDataset fdataset = FeatureDatasetFactoryManager.open(FeatureType.GRID, location, null, errlog);
  if (fdataset == null) {
    log.error("**failed on {} %n --> {} %n", location, errlog);
    return null;
  }

  FeatureType ftype = fdataset.getFeatureType();
  assert (ftype == FeatureType.GRID);
  GridDataset gds = (GridDataset) fdataset;
~~~
  
If you know that the file you are opening is a GridDataset, you can call directly:
~~~
       GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(location);
~~~

### Using a GridDataset

Once you have a GridDataset, you can get the grids and their associated coordinate systems:

~~~
  GridDatatype grid = gds.findGridDatatype( args[1]);
  GridCoordSystem gcs = grid.getCoordinateSystem();
  CoordinateAxis xAxis = gcs.getXHorizAxis();
  CoordinateAxis yAxis = gcs.getYHorizAxis();
  CoordinateAxis1D zAxis = gcs.getVerticalAxis(); // may be null

  if (gcs.hasTimeAxis1D()) {
    CoordinateAxis1DTime tAxis1D = gcs.getTimeAxis1D();
    java.util.Date[] dates = tAxis1D.getTimeDates();

  } else if (gcs.hasTimeAxis()) {
    CoordinateAxis tAxis = gcs.getTimeAxis();
  } 
  ... 
~~~
 
A <b>_GridCoordSystem_</b> wraps a georeferencing coordinate system. It always has 1D or 2D XHoriz and YHoriz axes, and optionally 1D vertical and 1D or 2D time axes. The XHoriz/YHoriz axes will be lat/lon if isLatLon() is true, otherwise they will be GeoX,GeoY with an appropriate Projection. The getBoundingBox() method returns a bounding box from the XHoriz/YHoriz corner points. The getLatLonBoundingBox() method returns the smallest lat/lon bounding box that contains getBoundingBox().

You can use the <b>_GridCoordSystem_</b> to find the value of a grid a a specific lat, lon point:

~~~
  // open the dataset, find the variable and its coordinate system
  GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(location);
  GridDatatype grid = gds.findGridDatatype( "myVariableName");
  GridCoordSystem gcs = grid.getCoordinateSystem();

  double lat = 8.0;
  double lon = 21.0;

  // find the x,y index for a specific lat/lon position
  int[] xy = gcs.findXYindexFromLatLon(lat, lon, null); // xy[0] = x, xy[1] = y

  // read the data at that lat, lon and the first time and z level (if any) 
  Array data  = grid.readDataSlice(0, 0, xy[1], xy[0]); // note order is t, z, y, x
  double val = data.getDouble(0); // we know its a scalar
  System.out.printf("Value at %f %f == %f%n", lat, lon, val);
  ... 
~~~
   
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

~~~
      NetcdfCFWriter writer = new NetcdfCFWriter();
      writer.makeFile(filename, gds, gridList, boundingBox, timeRange, addLatLon, horizStride, vertStride, timeStride);

  /**
   * Write a CF compliant Netcdf-3 file from any gridded dataset.
   *
   * @param location    write to this location on disk
   * @param gds         A gridded dataset
   * @param gridList    the list of grid names to be written, must not be empty. Full name (not short).
   * @param llbb        optional lat/lon bounding box
   * @param range       optional time range
   * @param addLatLon   should 2D lat/lon variables be added, if its a projection coordainte system?
   * @param horizStride x,y stride
   * @param stride_z    not implemented yet
   * @param stride_time not implemented yet
   * @throws IOException           if write or read error
   * @throws InvalidRangeException if subset is illegal
   */
  public void makeFile(String location, ucar.nc2.dt.GridDataset gds, List<String> gridList,
          LatLonRect llbb, DateRange range,
          boolean addLatLon,
          int horizStride, int stride_z, int stride_time)
          throws IOException, InvalidRangeException;
~~~
          
### Using ToolsUI to look at Grids

You can use ToolsUI <b>_FeatureTypes/Grids_</b> Tab to view Grid Datasets. This consists of 3 tables that show the Grid DataTypes, the Grid Coordinate systems, and the Coordinate Axes, eg:

{% include image.html file="netcdf-java/tutorial/toolsui/GridUI.png" alt="Grid UI" caption="ToolsUI Interface" %}

Use the {% include inline_image.html file="netcdf-java/tutorial/toolsui/redrawButton.jpg" alt="Redraw button" %}  button to display the grids in the grid viewer:

{% include image.html file="netcdf-java/tutorial/toolsui/GridView.png" alt="Grid View" caption="ToolsUI View" %}

