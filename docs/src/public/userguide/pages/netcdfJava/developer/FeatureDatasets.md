---
title: CDM Feature Datasets
last_updated: 2019-07-22
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: feature_datasets.html
---
### CDM Feature Datasets

A <b>_FeatureDataset_</b> is a container for FeatureType objects. It is a generalization of a NetcdfDataset, and the common case is that it wraps a NetcdfDataset. The metadata in a <b>_FeatureDataset_</b> is intended to be search metadata, useful for quickly finding datasets of interest in a large catalog of data.

~~~
  public interface ucar.nc2.ft.FeatureDataset extends Closeable {
    ucar.nc2.constants.FeatureType getFeatureType();

    String getTitle();
    String getDescription();
    String getLocation();

    CalendarDateRange getCalendarDateRange();
    ucar.unidata.geoloc.LatLonRect getBoundingBox();

    List<Attribute> getGlobalAttributes();
    ucar.nc2.Attribute findGlobalAttributeIgnoreCase(String attName);

    List<VariableSimpleIF> getDataVariables();
    ucar.nc2.VariableSimpleIF getDataVariable(String name);

    void close() throws java.io.IOException;
  }
~~~

The data variables are simple versions of Variables, in particular they have no read methods:

~~~
  public interface ucar.nc2.VariableSimpleIF {
    String getFullName();
    String getShortName();
    String getDescription();
    String getUnitsString();

    int getRank();
    int[] getShape();
    List<Dimension> getDimensions();
    ucar.ma2.DataType getDataType();

    List<Attribute> getAttributes();
    ucar.nc2.Attribute findAttributeIgnoreCase(java.lang.String);
~~~

The information in the FeatureDataset interface constitutes a simple kind of discovery metadata for the dataset.


#### FeatureTypes

A featureType is specified with one of the following enum values:

~~~
public enum ucar.nc2.constants.FeatureType {
  ANY,        // No specific type

  COVERAGE,   // any of the coverage types: GRID, FMRC, SWATH, CURVILINEAR
  GRID,       // seperable coordinates
  FMRC,       // two time dimensions, runtime and forecast time
  SWATH,      // 2D latlon, dependent time, polar orbiting satellites
  CURVILINEAR,// 2D latlon, independent time

  ANY_POINT,  // Any of the point types
  POINT,      // unconnected points
  PROFILE,    // fixed x,y with data along z
  STATION,    // timeseries at named location
  STATION_PROFILE, // timeseries of profiles
  TRAJECTORY, // connected points in space and time
  TRAJECTORY_PROFILE, //  trajectory of profiles

  RADIAL,     // polar coordinates
  STATION_RADIAL, // time series of radial data

  // experimental
  IMAGE,    // pixels, may not be geolocatable
  UGRID;    // unstructured grids
}
~~~

#### Opening a FeatureDataset

The general way to open a <b>_FeatureDataset_</b> from a file or remote file is by calling FeatureDatasetFactoryManager.open() :

~~~
FeatureDataset FeatureDatasetFactoryManager.open( FeatureType want, String endpoint,
    ucar.nc2.util.CancelTask task, java.util.Formatter errlog);
~~~

where endpoint is a remote dataset represented as a URL (eg using <b>_cdmrFeature_</b>, <b>_cdmremote_</b>, or <b>_OPeNDAP_</b> protocols) or a local file pathname (see [here](dataset_urls.html) for details).

or if you already have an opened NetcdfDataset:

~~~
  FeatureDataset FeatureDatasetFactoryManager.wrap( FeatureType want, NetcdfDataset ncd, CancelTask task, Formatter errlog);
~~~

Specifying the FeatureType means that you only want a FeatureDataset of that FeatureType. If you dont know the feature type, leave the parameter _null_ or set to _FeatureType.ANY_. You may specify that you want one of the point types with _FeatureType.ANY_POINT_ or one of the coverage types (swath, grid, etc) with _FeatureType.COVERAGE_. The CancelTask allows the opener task to be cleanly cancelled, eg in a user interface, and may be null. The errlog is an instance of <b>_java.util.Formatter_</b>, and must not be null. If the open() or wrap() is not successful, a null <b>_FeatureDataset_</b> will be returned, and the errlog will usually have an explanatory message.

The returned object will be a subclass of <b>_FeatureDataset_</b>. To continue processing the <b>_FeatureDataset_</b>, you must cast it to its subclass, based on the featureType. For example:

~~~
    Formatter errlog = new Formatter();
    try (FeatureDataset fdataset = FeatureDatasetFactoryManager.open(wantType, location, null, errlog)) {
      if (fdataset == null) {
        logger.warn("**failed on %s %n --> %s %n", location, errlog);
        return;
      }

      FeatureType ftype = fdataset.getFeatureType();

      if (ftype.isCoverageFeatureType()) {
        FeatureDatasetCoverage covDataset = (FeatureDatasetCoverage) fdataset;
        ...

      } else if (ftype.isPointFeatureType()) {
        FeatureDatasetPoint pointDataset = (FeatureDatasetPoint) fdataset;
        ...

      } else if (ftype == FeatureType.RADIAL) {
        RadialDatasetSweep radialDataset = (RadialDatasetSweep) fdataset;
        ...
      }
    }
~~~

#### Note:
~~~
The above code fragment uses the Java 7 try-with-resources statement, which is highly recommended to eliminate file leaks. Note that the FeatureDataset is closed when the try block is exited, so this example assumes the processing is done inside the block. If thats not the case, you must not use a try-with-resources statement and be sure to close the FeatureDataset yourself.
~~~

### Opening a FeatureDataset from a THREDDS Catalog
Get a thredds.client.catalog.Dataset object from a THREDDS catalog and call the factory method in thredds.client.catalog.tools.DataFactory:

~~~
public thredds.client.catalog.Dataset findDatasetById(String catalogUri, String datasetId) {
  CatalogBuilder builder = new CatalogBuilder();
  try {
    Catalog cat = builder.buildFromLocation(catalogUri, null);
    if (builder.hasFatalError()) {
      log.warn("Error building catalog uri='"+catalogUri+"' error="+ builder.getErrorMessage());
      return null;
    }
    return cat.findDatasetByID(datasetId);

  } catch (IOException ioe) {
    log.warn("Error opening catalog uri='"+catalogUri+"' error="+ ioe.getMessage());
    return null;
  }
}

ucar.nc2.ft.FeatureDataset getFeatureDataset( Dataset invDataset, CancelTask task) {
  try {
    DataFactory dataFactory = new DataFactory();
    DataFactory.Result result = dataFactory.openFeatureDataset(invDataset, task);
    if (result.fatalError) {
      JOptionPane.showMessageDialog(this, "Cant open dataset=" + threddsData.errLog);
      return null;
    }
    return result.featureDataset;

  } catch (IOException ioe) {
    JOptionPane.showMessageDialog("Error opening dataset='"+invDataset+"' error="+ ioe.getMessage());
    return null;
  }
}
~~~

#### Note:

The catalog API is significantly changed in version 5.0.

### Resources
[Point Dataset](pointfeature_ref.html){:target="_blank"}: Discrete Sampling Geometry (DSG) datasets 

[Coverage Dataset](coverages.html): Data in a multidimensional grid, eg model output, satellite data

[Forecast Model Run Collection (FMRC)](runtime_loading.html): Gridded data with two time coordinates, Run Time (aka Reference Time) and Forecast Time (aka Valid Time)

[Radial Dataset](radial_datasets.html): uses polar coordinates (elevation, azimuth, distance), for example scanning radars, lidars.




