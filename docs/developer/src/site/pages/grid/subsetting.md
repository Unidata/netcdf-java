---
title: Grid Dataset subsetting
last_updated: 2021-07-08
sidebar: developer_sidebar 
permalink: grid_subsetting.html
toc: false
---

# Grid Dataset subsetting

For NetcdfDataset, index-based subsetting is a familiar extension of Fortran-style array handling. 
When associated with coordinate variables, the same indexes can reliably be used to geolocate the subsetted
array.

The essence of the Grid feature type is the ability to subset by coordinate value, rather than index. This
breaks the strict association with coordinate variable indices, and allows GridDatasets to be used across 
much larger data collection. When the subset of data is read, georeferencing coordinate variables (called
GridAxes) can still be accurately constructed. However, these may be different than when constructing index-based 
subsets directly on the coordinate variables of the GridDataset. Allowing this _contract_ to be broken makes the
implementation of GridDatset across large data collections feasible.

In this document, we describe the semantics of coordinate-based subsetting.

## Grid coordinate system

A Grid has a GridCoordinateSystem composed of 4 orthogonal pieces:

`GridTimeCoordinateSystem X EnsembleAxis X VerticalAxis X GridHorizCoordinateSystem`

where `X` means orthogonal, meaning they can be treated independently. Only the GridHorizCoordinateSystem
is required.

The GridHorizCoordinateSystem deals with horizontal projection coordinates and lat/lon coordinates. A
GridHorizCoordinateSystem always has two independent 1D coordinate variables, either GeoX/GeoY or Lat/Lon.
For _curvilinear coordinates_ where the Lat/Lon coordinates are given as 2D arrays and the projection is not
specified, the 1D coordinate variables will be nominal index coordinates using GeoX/GeoY.

The GridTimeCoordinateSystem manages the possibility that there is both a runtime coordinate, and a time offset
coordinate that depends on the runtime, also know as Fmrc (Forcast model run coordinate). See below for the ways
to work with Fmrc datasets, and how the subsetting parameter can be used. 

## Example subsetting

## GridSubset

````
  public static final String runtime = "runtime"; // value = CalendarDate
  public static final String runtimeLatest = "runtimeLatest"; // value = Boolean
  public static final String runtimeAll = "runtimeAll"; // value = Boolean

  public static final String time = "time"; // value = CalendarDate
  public static final String timeRange = "timeRange"; // value = CalendarDateRange
  public static final String timeStride = "timeStride"; // value = Integer
  public static final String timePresent = "timePresent"; // value = Boolean
  public static final String timeAll = "timeAll"; // value = Boolean LOOK whats diff with timeOffsetAll?

  public static final String timeOffset = "timeOffset"; // value = Double
  public static final String timeOffsetIntv = "timeOffsetIntv"; // value = CoordInterval
  public static final String timeOffsetFirst = "timeOffsetFirst"; // value = Boolean
  public static final String timeOffsetAll = "timeOffsetAll"; // value = Boolean

  public static final String vertPoint = "vertPoint"; // value = Double
  public static final String vertIntv = "vertIntv"; // value = CoordInterval
  public static final String ensCoord = "ensCoord"; // value = Double

  public static final String latlonBB = "latlonBB"; // value = LatLonRect
  public static final String projBB = "projBB"; // value = ProjectionRect
  public static final String horizStride = "horizStride"; // value = Integer
  public static final String latlonPoint = "latlonPoint"; // value = LatLonPoint

````

Choose 0 or 1 from each category. Default = all.
If runtime doesnt exist, ok to choose runtimeAll or runtimeLatest?

### Fmrc subsetting

If you set time but not runtime, find the latest offset that matches. (Best)

### Runtime subsetting

1. **runtime**

    The value is the CalendarDate of the requested runtime.

2. **runtimeLatest**

    Requests the most recent runtime.

3. **runtimeAll**

    Request all runtimes. Limit?
    
The Runtime coordinate may be missing, a scalar or have a single value.

runtimeClosest? runtimeInInterval? runtimesInInterval?
    
### TimeOffset subsetting

1. **timeOffset**

    The value is the offset in the units of the GridAxisPoint. Must match exactly? using closest match.

2. **timeOffsetIntv**

    The value is the offset in the units of the GridAxisInterval. Must match exactly? using closest match.
    
timeOffsetClosest? timeOffsetInInterval?  timeOffsetLatest? timeOffsetPresent?

FMRC best: Add a utility class that calculates the best, and iterates ?? or timeOffsetBest?

### Time subsetting - non FMRC only: Observation or SingleRuntime ??

1. **time**

    The value is the CalendarDate of the requested time. Must match exactly.
    
2. **timeRange**

    All times within the given CalendarDateRange. Intervals are included if they overlap. Note the results here might be a RangeScatter ??

3. **timeLatest**

    Request the latest time.

4. **timeAll**

    Request all times.

5. **timePresent**

    Request the time closest to the present time.
    
Not used:
    
5. **timeStride**

    Request every nth time value. Use with time to request where to start. why needed ??
    
timeClosest? timeInInterval? 
       
### Vertical and Ensemble subsetting

1. **vertPoint**

    The value is the offset in the units of the GridAxisPoint.

2. **vertInterval**

    The value is the offset in the units of the GridAxisInterval. 

3. **ensCoord**

    The value is the coordinate of the GridAxisPoint. It is always a nominal integer value.
    
### Horizontal subsetting

If both projBB and latlonBB are given, only the projBB is used. 

1. **projBB**

   The value is a ProjectionRect. 

2. **latlonBB**

    The value is a LatLonRect.

3. **horizStride**

    Request every nth x and every nth y value.
    
4. **latlonPoint**

    Request only the point that contains the given LatLonPoint.
