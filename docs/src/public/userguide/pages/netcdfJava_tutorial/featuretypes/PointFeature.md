---
title: Point Feature Datasets
last_updated: 2019-06-28
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: point_feature_datasets.html
---
## Point Feature Datasets

Point Feature Datasets (also known as Discrete Sampling Geometry (DSG) datasets) are collections of Point Features. Point Feature Datasets contain one or more FeatureCollections:

~~~
  public interface ucar.nc2.ft.FeatureDatasetPoint extends ucar.nc2.ft.FeatureDataset {
    List<FeatureCollection> getPointFeatureCollectionList();
  }
~~~

Point Feature Datasets will contain <b>_PointFeatureCollections_</b> or <b>_NestedPointFeatureCollections_</b>, described below. We take the approach that all point feature types are arrangements of collections of PointFeatures (a set of measurements at the same point in space and time), distinguished by the geometry and topology of the collections. The <b>_Point Feature Types_</b> that we implement are:

1. <b>Point feature</b> : one or more parameters measured at one point in time and space.
2. <b>Station time series feature</b> : a time-series of data points all at the same location, with varying time.
3. <b>Profile feature</b> : a set of data points along a vertical line.
4. <b>Station Profile feature</b> : a time-series of profile features at a named location.
5. <b>Trajectory feature</b> : a set of data points along a 1D curve in time and space.
6. <b>Section feature</b> : a collection of profile features which originate along a trajectory.

### Related documents:

* CF 1.6 Discrete Sampling Geometries Conventions
* CDM Feature Types draft doc
* CDM Point Feature Types draft doc
* Complete Point Feature UML

### Point Features

{% include image.html file="netcdf-java/tutorial/feature_types/PointFeature.png" alt="Point Feature UML" caption="" %}

A PointFeature is a collection of data (usually observational) taken at a single time and a single place:

~~~
  public interface ucar.nc2.ft.PointFeature {
    ucar.unidata.geoloc.EarthLocation getLocation();

    double getObservationTime();
    Date getObservationTimeAsDate();
    double getNominalTime();
    Date getNominalTimeAsDate();
    DateUnit getTimeUnit();

    ucar.ma2.StructureData getData() throws java.io.IOException;
  }
~~~
  
The time can be retrieved as a Date or as a double. The actual time of the data sample is the observation time. It is common in some observational systems to bin data into standard intervals, in which case there is also a nominal time. When the nominal time is not given in the data, it is usually set to the observational time, which must always be present.

The location is represented by:

~~~
  public interface ucar.unidata.geoloc.EarthLocation {
    double getLatitude();
    double getLongitude();
    double getAltitude();
    ucar.unidata.geoloc.LatLonPoint getLatLon();
  }
~~~
  
The latitude and longitude are required, while the altitude may be missing and if so, is set to Double.NaN. altitude units and datum ??

The actual data of the observation is contained in a ucar.ma2.StructureData, which has a collection of StructureMembers which describe the individual data members, along with many convenience routines for extracting the data.

### PointFeatureIterator

~~~
The only way to access data in point feature collections is to iterate over the data with a PointFeatureIterator.

 public interface ucar.nc2.ft.PointFeatureIterator {     
   boolean hasNext();
   ucar.nc2.ft.PointFeature next();
   void finish(); 
 }
~~~
 
When the iteration is complete, any system resources used by it are released. If the iteration is not completed, you must explicitly call finish(). Therefore best practice is to put your iteration in a try/finally block like:

~~~
  PointFeatureIterator pfi = collection.getPointFeatureIterator();
  try {

   while (iter.hasNext()) {
     ucar.nc2.ft.PointFeature pf = iter.next();
     ...
   }

 } finally {
   iter.finish();
 }
~~~
  
Note that calling hasNext() is required before calling next(), and the order in which the PointFeatures are returned is arbitrary, if not otherwise specified. 
PointFeatureCollection
A PointFeatureCollection is a collection of PointFeatures:

~~~
  public interface ucar.nc2.ft.PointFeatureCollection extends ucar.nc2.ft.FeatureCollection {
    String getName();
    int size();
    ucar.nc2.units.DateRange getDateRange();
    ucar.unidata.geoloc.LatLonRect getBoundingBox();
    void calcBounds();
    
    PointFeatureIterator getPointFeatureIterator(int buffersize);
    PointFeatureCollection subset(ucar.unidata.geoloc.LatLonRect, ucar.nc2.units.DateRange);

    // internal iterator
    void resetIteration();
    boolean hasNext();
    PointFeature next();
    void finish();  
  }
~~~  
  
All FeatureCollections have a name that must be unique within its containing collection (if any). The size of its collection is the number of PointFeatures that will be returned by the iterator. The size, boundingBox, and dateRange may not be known until after iterating through the collection, that is, actually reading the data. You can force the discovery of these by calling calcBounds(), but that may cost a complete iteration through the data.

When you are working in a single threaded context, you can use the internal iterator as a convenience:

~~~
  try {
   pointFeatureCollection.resetIterator();
   while(pointFeatureCollection.hasNext()) {
     PointFeature pf = pointFeatureCollection.next()
     ...
   }
  } finally {
    pointFeatureCollection.finish();
  }
~~~

Since an iterator always runs through the data exactly once, its a good idea to call resetIteration() first, to make sure that the iterator is in the correct state.
The internal iterator is not thread-safe. In a multithreaded application, you must explictly get a PointFeatureIterator to iterate over the PointFeatures:

~~~
  PointFeatureIterator iter = pointFeatureCollection.getPointFeatureIterator(100 * 1000); // 100Kb buffer
  try {
    while(iter.hasNext()) {
      ucar.nc2.ft.PointFeature pf = iter.next()
      ...
    }
  } finally {
    iter.finish();
  }
~~~

The buffersize parameter allows you to specify guidance on how many bytes can be used to buffer data, which may increase performance. The implementation is free to ignore this, however. Setting buffersize to -1 tells the implementation to choose its own buffer size.

You may subset a PointFeatureCollection with a lat/lon bounding box, and/or a dateRange:

~~~
  ucar.nc2.units.DateFormatter dformat = new ucar.nc2.units.DateFormatter()
  PointFeatureCollection subset = pointFeatureCollection.subset(new LatLonRect("40,-105,2,2"), 
		new DateRange( dformat.getISODate("1999-09-31T12:00:00"), null, new TimeDuration("3 days"), null); 

  // get all the points in that subset
  while(subset.hasNext()) {
    ucar.nc2.ft.PointFeature pf = subset.next()
    ...
  } 
~~~
### Profile Feature Collection

{% include image.html file="netcdf-java/tutorial/feature_types/ProfileFeature.png" alt="Profile Feature UML" caption="" %}

A ProfileFeature is a set of PointFeatures along a vertical line.

  public interface ucar.nc2.ft.ProfileFeature extends ucar.nc2.ft.PointFeatureCollection {
    String getName();
    ucar.unidata.geoloc.LatLonPoint getLatLon();

    int size();
    boolean hasNext();
    ucar.nc2.ft.PointFeature next();
    void resetIteration();

    ucar.nc2.ft.PointFeatureIterator getPointFeatureIterator(int buffersize);
    ucar.nc2.ft.PointFeatureCollection subset(ucar.unidata.geoloc.LatLonRect, ucar.nc2.units.DateRange);
  }
Note that a ProfileFeature is a collection of PointFeatures, extending PointFeatureCollection. In this case, the iteration will return PointFeatures that all belong to the same profile, with the same lat/lon point and varying heights. The number of points in the collection may be available through the size() method. When that number can only be determined by reading through the data, a -1 is returned.

Note that the subset method, inherited from the PointFeatureCollection interface, is not normally useful here, since the lat/lon values are identical. The time values are also often identical, although that is not required.

### ProfileFeatureCollection

A ProfileFeature is a PointFeatureCollection, and a collection of ProfileFeatures is a ProfileFeatureCollection, which extends NestedPointFeatureCollection:

~~~
  public interface ucar.nc2.ft.ProfileFeatureCollection extends FeatureCollection, NestedPointFeatureCollection {
    String getName();

    int size();
    boolean hasNext();
    ucar.nc2.ft.ProfileFeature next();
    void resetIteration();

    ucar.nc2.ft.PointFeatureCollectionIterator getPointFeatureCollectionIterator(int buffersize);
    ucar.nc2.ft.ProfileFeatureCollection subset(ucar.unidata.geoloc.LatLonRect);
    ucar.nc2.ft.PointFeatureCollection flatten(ucar.unidata.geoloc.LatLonRect, ucar.nc2.units.DateRange);
  }
~~~
  
To read all the data, iterate through each ProfileFeature in the collection, then through each PointFeature of the ProfileFeature:

~~~
  profileFeatureCollection.resetIteration();
  while(profileFeatureCollection.hasNext()) { 
    ucar.nc2.ft.ProfileFeature profile = profileFeatureCollection.next();

    profile.resetIteration();
    while(profile.hasNext()) {
      ucar.nc2.ft.PointFeature pointFeature = profile.next();
      ...
    }
  }
~~~
  
You may subset a ProfileFeatureCollection with a lat/lon bounding box, getting back another ProfileFeatureCollection:

~~~
  ucar.nc2.units.DateFormatter dformat = new ucar.nc2.units.DateFormatter()
  ProfileFeatureCollection subset = profileFeatureCollection.subset(new LatLonRect("-60,120,12,20"));

  // get all the profiles in the specified bounding box
  subset.resetIteration();
  while(subset.hasNext() { 
    ucar.nc2.ft.ProfileFeature profile = subset.next()

    // get all the points
    profile.resetIteration();
    while(profile.hasNext()) {
      ucar.nc2.ft.PointFeature pointFeature = profile.next()
      ...
    }
  }
~~~
  
You may flatten a ProfileFeatureCollection with a lat/lon bounding box, and/or a dateRange, which throws away all the connectedness information of the profile, and treats the data as a collection of points. In this case, you get back a PointFeatureCollection:

~~~
  ucar.nc2.units.DateFormatter dformat = new ucar.nc2.units.DateFormatter()
  PointFeatureCollection subset = profileFeatureCollection.flatten(new LatLonRect("-60,120,12,20"), 
		new DateRange( dformat.getISODate("1999-09-30T00:00:00"), dformat.getISODate("1999-09-31T00:00:00"));
  // get all the points in that subset
  subset.resetIteration();
  while(subset.hasNext()) {
    ucar.nc2.ft.PointFeature pf = subset.next()
    ...
  }  
~~~

Equivalent to the internal iterator, you can explictly get a PointFeatureCollectionIterator to iterate over the ProfileFeatures. TThe PointFeatureCollectionIterator is identical to a PointFeatureIterator, except that it returns PointFeatureCollections instead of PointFeatures. The main reason to use this is probably to explicitly set the buffer size.

~~~
  public interface ucar.nc2.ft.PointFeatureCollectionIterator {
    boolean hasNext();
    ucar.nc2.ft.PointFeatureCollection nextFeature();
    void setBufferSize(int bufferSize);
  }
~~~

### Trajectory Feature Collection
  
{% include image.html file="netcdf-java/tutorial/feature_types/TrajectoryFeature.png" alt="Trajectory Feature UML" caption="" %}  


### Station Time Series Features

{% include image.html file="netcdf-java/tutorial/feature_types/StationTimeSeries.png" alt="Station Time Series UML" caption="" %}  

A StationTimeSeriesFeature is a time series of PointFeatures at a single, named location called a Station:

  public interface ucar.nc2.ft.StationTimeSeriesFeature extends Station, PointFeatureCollection {
    String getName();
    String getDescription();
    String getWmoId();
    double getLatitude();
    double getLongitude();
    double getAltitude();
    ucar.unidata.geoloc.LatLonPoint getLatLon();

    
    ucar.nc2.ft.PointFeatureIterator getPointFeatureIterator(int buffersize);
    ucar.nc2.ft.StationTimeSeriesFeature subset(ucar.nc2.units.DateRange);
    ucar.nc2.ft.PointFeatureCollection subset(ucar.unidata.geoloc.LatLonRect, ucar.nc2.units.DateRange); // not useful
    
    // internal iterator
    int size();
    boolean hasNext();
    ucar.nc2.ft.PointFeature next();
    void resetIteration();
  }
Note that a StationTimeSeriesFeature is a collection of PointFeatures, extending PointFeatureCollection. In this case, the iteration will return PointFeatures that all belong to the same station. These may or may not be time-ordered.

Note that the subset(LatLonRect, DateRange) method, inherited from the PointFeatureCollection interface, is not normally useful here, since the lat/lon values at all points are identical. Subsetting on just the DateRange is useful, however, and returns another StationTimeSeriesFeature whose PointFeatures lie within the specified range of dates.

StationTimeSeriesFeatureCollection
A StationTimeSeriesFeatureCollection is a collection of StationTimeSeriesFeatures:

  public interface ucar.nc2.ft.StationTimeSeriesFeatureCollection extends StationCollection, NestedPointFeatureCollection {
    String getName();

    List<Station> getStations();
    List<Station> getStations(ucar.unidata.geoloc.LatLonRect subset);
    ucar.nc2.ft.Station getStation(String stationName);
    ucar.unidata.geoloc.LatLonRect getBoundingBox();

    int size();
    boolean hasNext();
    ucar.nc2.ft.StationTimeSeriesFeature next();
    void resetIteration();

    ucar.nc2.ft.PointFeatureCollectionIterator getPointFeatureCollectionIterator(int bufferSize);
    ucar.nc2.ft.PointFeatureCollection flatten(ucar.unidata.geoloc.LatLonRect, ucar.nc2.units.DateRange);

    ucar.nc2.ft.StationTimeSeriesFeatureCollection subset(List<Station> stns);
    ucar.nc2.ft.StationTimeSeriesFeature getStationFeature(ucar.nc2.ft.Station);
  }
A StationTimeSeriesFeatureCollection is a collection of Stations, extending StationCollection, from which you can get the list of available Stations, a bounding box, etc. You may subset the StationTimeSeriesFeatureCollection by passing in a list of Stations. You may also flatten the NestedPointFeatureCollection, throwing away the station information, and making it into a collection of PointFeatures. The flattening may include subsetting by lat/lon bounding box, and/or a dateRange.

To access the data, you may get a StationTimeSeriesFeature for a specified Station, or you can iterate over all StationTimeSeriesFeatures in the collection.

  stationTimeSeriesFeatureCollection.resetIteration();
  while(stationTimeSeriesFeatureCollection.hasNext() { 
    ucar.nc2.ft.StationTimeSeriesFeature timeSeries = stationTimeSeriesFeatureCollection.next()

    timeSeries.resetIteration();
    while(timeSeries.hasNext()) {
      ucar.nc2.ft.PointFeature pointFeature = timeSeries.next()
      ...
    }
  }
To get a time series at a particular station:

  Station stn = stationTimeSeriesCollection.getStation("FXOW");
  StationTimeSeriesFeature timeSeries = stationTimeSeriesCollection.getStationFeature(stn);
  timeSeries.resetIteration();
  while(timeSeries.hasNext()) {
    ucar.nc2.ft.PointFeature pointFeature = timeSeries.nextData()
    ...
  }
To get all PointFeatures in a specific area and time range:

  LatLonRect bb = new LatLonRect( new LatLonPointImpl(40.0, -105.0), new LatLonPointImpl(42.0, -100.0));
  ucar.nc2.ft.PointFeatureCollection points = stationTimeSeriesCollection.flatten(bb, new DateRange(start, end))
  points.resetIteration();
  while(points.hasNext()) {
    ucar.nc2.ft.PointFeature pointFeature = points.next()
    ...
  }
### Station Profile Features


StationProfileFeature

{% include image.html file="netcdf-java/tutorial/feature_types/StationTimeSeries.png" alt="Station Time Series UML" caption="" %} 

A StationProfileFeature is a time series of ProfileFeatures at a single, named location.

~~~
  public interface ucar.nc2.ft.StationProfileFeature extends ucar.nc2.ft.Station, ucar.nc2.ft.NestedPointFeatureCollection {

    String getName();
    String getDescription();
    String getWmoId();

    double getLatitude();
    double getLongitude();
    double getAltitude();
    ucar.unidata.geoloc.LatLonPoint getLatLon();

    int size();
    boolean hasNext();
    ucar.nc2.ft.ProfileFeature next();
    void resetIteration();

    ucar.nc2.ft.PointFeatureCollection flatten(ucar.unidata.geoloc.LatLonRect, ucar.nc2.units.DateRange);
    ucar.nc2.ft.StationProfileFeature subset(ucar.nc2.units.DateRange);
  }
~~~

A StationProfileFeature is a collection of ProfileFeatures, extending NestedPointFeatureCollection.. To access the data, you can iterate over all ProfileFeatures in the collection, then through all PointFeatures of the ProfileFeature:

~~~
  stationProfileFeature.resetIteration();
  while(stationProfileFeature.hasNext() { 
    ucar.nc2.ft.ProfileFeature profile = stationProfileFeature.next();

    profile.resetIteration();
    while(profile.hasNext()) {
      ucar.nc2.ft.PointFeature pointFeature = profile.next();
      ...
    }
  }
~~~

Note that the flatten(LatLonRect, DateRange) method, inherited from the NestedPointFeatureCollection interface, is not normally useful here, since the lat/lon values are identical. Subsetting on just the DateRange is useful, however, and returns anotherStationProfileFeature whose ProfileFeatures lie within the specified range of dates.

### StationProfileFeatureCollection

A StationProfileFeatureCollection is a collection of StationProfileFeature, ie. a collection of time series of ProfileFeatures.

~~~
  public interface ucar.nc2.ft.StationProfileFeatureCollection extends StationCollection, NestedPointFeatureCollection {
    String getName();

    List<Station> getStations();
    List<Station> getStations(ucar.unidata.geoloc.LatLonRect subset);
    ucar.nc2.ft.Station getStation(String stationName);
    ucar.unidata.geoloc.LatLonRect getBoundingBox();

    int size();
    boolean hasNext();
    ucar.nc2.ft.StationProfileFeature next();
    void resetIteration();

    ucar.nc2.ft.PointFeatureCollection flatten(ucar.unidata.geoloc.LatLonRect, ucar.nc2.units.DateRange);

    ucar.nc2.ft.StationProfileFeatureCollection subset(List<Station> stns);
    ucar.nc2.ft.StationProfileFeature getStationProfileFeature(Station stn);
  }
~~~
  
A StationProfileFeatureCollection extends StationCollection, from which you can get the list of available Stations, a bounding box, etc. Note how the StationCollection interface makes handling StationProfileFeatureCollection identical to StationTimeSeriesFeatureCollection. You may subset the collection by passing in a list of Stations, or get a StationProfileFeature from a specific Station.

To run through all the data, iterate through each StationProfileFeature in the collection, then through each ProfileFeature in the StationProfileFeature, then through each PointFeature of the ProfileFeatures:

~~~
    stationProfileFeatureCollection.resetIteration();
    while (stationProfileFeatureCollection.hasNext()) {
      ucar.nc2.ft.StationProfileFeature stationProfile = stationProfileFeatureCollection.next();

      stationProfile.resetIteration();
      while (stationProfile.hasNext()) {
        ucar.nc2.ft.ProfileFeature profile = stationProfile.next();

        profile.resetIteration();
        while (profile.hasNext()) {
          ucar.nc2.ft.PointFeature pointFeature = profile.next();
          StructureData sdata = pointFeature.getData();
          ...
        }
      }
    }
~~~
As usual, you can <b>_flatten_</b> the collection, throwing away the station and profile information, and making it into a collection of PointFeatures. The flattening may include subsetting by lat/lon bounding box, and/or a dateRange.



