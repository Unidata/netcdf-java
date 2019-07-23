---
title: Coordinate Attribute Examples
last_updated: 2019-06-27
sidebar: netcdfJavaTutorial_sidebar
permalink: feature_types.html
toc: false
---

## Scientific Feature Types
A <b>_FeatureDataset_</b> is a container for FeatureType objects. It is a generalization of a NetcdfDataset, and the common case is that it wraps a NetcdfDataset. The metadata in a <b>_FeatureDataset_</b> is intended to be search metadata, useful for quickly finding datasets of interest in a large catalog of data.

~~~
  public interface ucar.nc2.ft.FeatureDataset {
    ucar.nc2.constants.FeatureType getFeatureType();

    String getTitle();
    String getDescription();
    String getLocationURI();

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
  }
~~~
  
The information in the FeatureDataset interface constitutes a simple kind of discovery metadata for the dataset.

### FeatureTypes

A featureType is specified with one of the following enum values:
~~~
public enum ucar.nc2.constants.FeatureType {
  ANY, // no specific type

  GRID,
  RADIAL,
  SWATH,
  IMAGE,

  ANY_POINT, // any of the following
  POINT,
  PROFILE,
  SECTION,
  STATION,
  STATION_PROFILE,
  TRAJECTORY;
}
~~~

### Opening a FeatureDataset from a URL or file path

The general way to open a FeatureDataset is by calling <b>_FeatureDatasetFactoryManager.open()_</b> :

~~~
  FeatureDataset FeatureDatasetFactoryManager.open( FeatureType type, String location, ucar.nc2.util.CancelTask task, java.util.Formatter errlog);
~~~

where location is a URL (eg OPeNDAP or cdmremote) or a local file pathname (see <a href="dataset_urls.html#ucarnc2ftfeaturedatasetfactorymanageropen">here</a> for details).


or if you already have an opened NetcdfDataset:

  FeatureDataset FeatureDatasetFactoryManager.wrap( FeatureType type, NetcdfDataset ncd, ucar.nc2.util.CancelTask task, java.util.Formatter errlog);
Specifying the FeatureType means that you only want a FeatureDataset of that FeatureType. If you want the dataset opened as any FeatureType, leave the type null or set to FeatureType.<b>_ANY_</b>. You may specify that you want one of the point types with FeatureType.<b>_ANY_POINT_</b>. You may leave the task null if you do not need to allow user cancelling. The errlog is an instance of java.util.Formatter, and must not be null. If the open() or wrap() is not successful, a null FeatureDataset will be returned, and the errlog will usually have an explanatory message.

The returned object will be a subclass of FeatureDataset, depending on the FeatureType. To get at the data in a FeatureDataset, you must cast it to its subclass, based on the FeatureType. For example:

~~~
    Formatter errlog = new Formatter();
    try (FeatureDataset fdataset = FeatureDatasetFactoryManager.open(wantType, location, null, errlog)) {
      if (fdataset == null) {
        System.out.printf("**failed on %s %n --> %s %n", location, errlog);
        return;
      }

      FeatureType ftype = fdataset.getFeatureType();
      assert FeatureDatasetFactoryManager.featureTypeOk(wantType, ftype);

      if (ftype == FeatureType.GRID) {
        assert (fdataset instanceof GridDataset);
        GridDataset griddedDataset = (GridDataset) fdataset;
        ...

      } else if (ftype == FeatureType.RADIAL) {
        assert (fdataset instanceof RadialDatasetSweep);
        RadialDatasetSweep radialDataset = (RadialDatasetSweep) fdataset;
        ...

      } else if (ftype.isPointFeatureType()) {
        assert fdataset instanceof FeatureDatasetPoint;
        FeatureDatasetPoint pointDataset = (FeatureDatasetPoint) fdataset;
        ...
      }
    }
~~~
     
Note that the above code fragment uses the Java 7 <a href="http://docs.oracle.com/javase/tutorial/essential/exceptions/tryResourceClose.html" target="_blank">try-with-resources statement</a>, which is highly recommended to eliminate file leaks.

### Opening a FeatureDataset from a THREDDS Catalog

The most general thing you can do is to get an InvDataset object from a THREDDS catalog and call the factory method in <b>_ucar.nc2.thredds.ThreddsDataFactory_</b>:

~~~
ucar.nc2.ft.FeatureDataset getFeatureDataset( InvDataset invDataset) {
  ThreddsDataFactory dataFactory = new ThreddsDataFactory();
  ThreddsDataFactory.Result result = dataFactory.openFeatureDataset(invDataset, null);
  
  if (result.fatalError) {
    JOptionPane.showMessageDialog(this, "Cant open dataset=" + threddsData.errLog);
    return null; 
  }

  return result.featureDataset
}
~~~

where do you get an InvDataset object? You get it from a THREDDS catalog, eg:

~~~
public InvDataset open(String catalogName, String datasetId) {
  InvCatalogFactory catFactory = InvCatalogFactory.getDefaultFactory( false);
  InvCatalogImpl cat = catFactory.readXML(catalogPath);
  
  StringBuilder errlog = new StringBuilder();
  boolean isValid = cat.check( errlog, false);
  if (!isValid) {
    System.out.printf("Validate failed on %s errs=%s%n", catalogName, errlog.toString());
    return null;
  } 

  return cat.cat.findDatasetByID(datasetId);
}
~~~

### Point FeatureDatasets
A <b>_FeatureDatasetPoint_</b> contains a list of <b>_FeatureCollections_</b>:

~~~
  public interface ucar.nc2.ft.FeatureDatasetPoint extends FeatureDataset {
     List<FeatureCollection> getPointFeatureCollectionList();
  }
~~~
  
All of the specialization is in the subclass of <b>_FeatureCollection_</b>, and you typically cast to process the data:

~~~
void process(FeatureDatasetPoint fdpoint) {
  FeatureType ftype = fdpoint.getFeatureType();
  assert (ftype == fc.getCollectionFeatureType());

  for (FeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {

    if (ftype == FeatureType.POINT) {
  	assert (fc instanceof PointFeatureCollection);
      PointFeatureCollection pointCollection = (PointFeatureCollection) fc;
      ...
    } else if (ftype == FeatureType.STATION) {
  	assert (fc instanceof StationTimeSeriesFeatureCollection);
      StationTimeSeriesFeatureCollection stationCollection = (StationTimeSeriesFeatureCollection) fc;
      ...
    } else if (ftype == FeatureType.PROFILE) {
  	assert (fc instanceof ProfileFeatureCollection);
      ProfileFeatureCollection profileCollection = (ProfileFeatureCollection) fc;
      ...
    } else if (ftype == FeatureType.STATION_PROFILE) {
     assert (fc instanceof StationProfileFeatureCollection);
      StationProfileFeatureCollection stationProfileCollection = (StationProfileFeatureCollection) fc;
 		...
    } else if (ftype == FeatureType.TRAJECTORY) {
  	assert (fc instanceof TrajectoryFeatureCollection);
      TrajectoryFeatureCollection trajectoryCollection = (TrajectoryFeatureCollection) fc;
      ...
    } else if (ftype == FeatureType.SECTION) {
  	assert (fc instanceof SectionFeatureCollection);
      SectionFeatureCollection sectiontCollection = (SectionFeatureCollection) fc;
      ...      
    }
  }
}
~~~

### Processing Feature Datasets

* [Grid Dataset](grid_datasets.html): data is in a multidimensional grid with seperable coordinates, eg model output, geosynchronous satellite data.
* [Radial Dataset](radial_datasets.html): uses polar coordinates (elevation, azimuth, distance), for example scanning radars, lidars.
* [Point Dataset](point_feature_datasets.html): Points, Station Time Series, Profiles, Trajectories and Sections.