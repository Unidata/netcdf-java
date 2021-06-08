---
title: Coverage Datasets
last_updated: 2019-11-02
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: coverage_feature.html
---

{% include image.html file="netcdf-java/reference/FeatureDatasets/CoverageDataset.svg" alt="Coverage Dataset" caption="" %}

##  Overview

<b>Coverage Datasets</b> are collections of Coverage Features such as _Grid, Fmrc,_ and _Swath_.
_FeatureDatasetCoverage_ are containers for _CoverageCollections_:

~~~
public class ucar.nc2.ft.FeatureDatasetCoverage extends ucar.nc2.ft.FeatureDataset, Closeable {
  public List<CoverageCollection> getCoverageCollections();
  public CoverageCollection findCoverageDataset( FeatureType type);
  public ucar.nc2.constants.FeatureType getFeatureType();
}
~~~

_FeatureDatasetCoverage_ extends _FeatureDataset_, and contains _discovery metadata_ for search and discovery.
A _CoverageCollection_ can only have a single _HorizCoordSys, Calendar_, and _FeatureType_.
A single endpoint (eg GRIB collection) often has more than one HorizCoordSys or Type (eg Best and TwoD), and so has multiple collections.
When there are multiple types, _getFeatureType()_ returns the generic _FeatureType.COVERAGE_. Otherwise it will return the specific type.

* _Grid_: all coordinates are one dimensional, aka _seperable_.
* _Fmrc_: Both a _runtime_ and _time_ coordinate exist. The time coordinate may be 2D, or it may be a 1D _time offset_ coordinate.
* _Swath_: has 2D lat/lon & time exists but not independent
* _Curvilinear:_ has 2D lat/lon & time independent if it exists

A Coverage Collection contains coverages, coordinate systems, coordinate axes, and coordinate transforms.
All of the coverages in a collections are on the same horizontal grid, and have the same datetime Calendar.

~~~
public class CoverageCollection implements Closeable {
  public String getName();
  public FeatureType getCoverageType();

  public List<Attribute> getGlobalAttributes();
  public String findAttValueIgnoreCase(String attName, String defaultValue);
  public Attribute findAttribute(String attName);
  public Attribute findAttributeIgnoreCase(String attName);

  public LatLonRect getBoundingBox() ;
  public ProjectionRect getProjBoundingBox();
  public CalendarDateRange getCalendarDateRange();
  public ucar.nc2.time.Calendar getCalendar();

  public List<CoordSysSet> getCoverageSets();
  public List<CoverageCoordSys> getCoordSys();
  public List<CoverageTransform> getCoordTransforms();
  public List<CoverageCoordAxis> getCoordAxes();
  public CoverageCoordSys findCoordSys(String name);
  public CoverageCoordAxis findCoordAxis(String name);
  public CoverageTransform findCoordTransform(String name);
  public HorizCoordSys getHorizCoordSys();

  public Iterable<Coverage> getCoverages();
  public int getCoverageCount();
  public Coverage findCoverageByAttribute(String attName, String attValue);

  public void close()
}
~~~

### CoordSysSet

A _CoordSysSet_ simply groups all of the _Coverages_ that belong to the same _CoverageCoordSys_:

~~~
public class CoordSysSet {
  public CoverageCoordSys getCoordSys();
  public List<Coverage> getCoverages();
}
~~~

### CoverageCoordSys

{% include image.html file="netcdf-java/reference/FeatureDatasets/CoverageCoordSys.svg" alt="Coverage Coordinate Systems" caption="" %}

A CoverageCoordSys represents the _Coordinate System_ for Coverages.

~~~
public class CoverageCoordSys {
  public String getName();
  public FeatureType getCoverageType();

  public List<String> getAxisNames();
  public List<CoverageCoordAxis> getAxes();
  public CoverageCoordAxis getAxis(String axisName);
  public CoverageCoordAxis getAxis(AxisType type);
  public CoverageCoordAxis getXAxis();
  public CoverageCoordAxis getYAxis() ;
  public CoverageCoordAxis getZAxis();
  public CoverageCoordAxis getTimeAxis();

  public List<String> getTransformNames();
  public List<CoverageTransform> getTransforms();
  public CoverageTransform getHorizTransform();
  public HorizCoordSys getHorizCoordSys();

  public boolean isRegularSpatial();
  public ProjectionImpl getProjection();
}
~~~

### CoverageCoordAxis

~~~
public class CoverageCoordAxis {
  public String getName();
  public DataType getDataType();
  public AxisType getAxisType();
  public List<Attribute> getAttributes();
  public String getUnits();
  public String getDescription();

  public int getNcoords();
  public DependenceType getDependenceType();
  public Spacing getSpacing();
  public double getResolution();
  public double getStartValue();
  public double getEndValue();

  public boolean isRegular();
  public boolean isScalar();
}
~~~

~~~
public enum DependenceType {
  independent,             // has its own dimension, is a coordinate variable, eg x(x)
  dependent,               // aux coordinate, eg reftime(time) or time_bounds(time);
  scalar,                  // eg reftime
  twoD                     // eg time(reftime, time), lat(x,y)
}
~~~

~~~
public enum Spacing {
  regular,           // regularly spaced points or intervals (start, end, npts),
                     // edges halfway between coords
  irregularPoint,    // irregular spaced points (values, npts), edges halfway between coords
  contiguousInterval,// irregular contiguous spaced intervals (values, npts),
                     // values are the edges, and there are npts+1, coord halfway between edges
  discontiguousInterval // irregular discontiguous spaced intervals (values, npts),
                     // values are the edges, and there are 2_npts: low0, high0, low1, high1..
}
~~~

### CoverageTransform

A _CoverageTransform_ contains parameters for horizontal or vertical transforms.

~~~
public class CoverageTransform  {
  public String getName();
  public boolean isHoriz();
  public ProjectionImpl getProjection();

  public java.util.List<Attribute> getAttributes();
  public Attribute findAttribute(String name);
  public Attribute findAttributeIgnoreCase(String name);
  public String findAttValueIgnoreCase(String attName, String defaultValue);
}
~~~
## Classification

* must have lat/lon or have x,y and projection
* x and y both have rank <= 2
* x and y both have size > 1; eliminates some miscoded point data
* x and y have at least 2 dimensions between them (eliminates point data)
* A runtime axis must be scalar or one-dimensional
* Time may be 0, 1 or 2 dimensional
* If time is 2D and runtime exists, first time dimension must agree with runtime
* other coordinates, dependent or independent (ie has another dimension) are ok.

