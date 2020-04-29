---
title: Upgrading to netCDF-Java version 5.x
last_updated: 2019-12-31
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: upgrade_to_50.html
---

## Requirements

* Java 8 or later is required

## Overview

A number of API enhancements have been made to take advantage of evolution in the Java language, for example _try-with-resource_ and _foreach_ constructs.
The use of these make code simpler and more reliable.

Deprecated classes and methods have been removed, and the module structure and third-party jar use has been improved.

Java WebStart has been deprecated as of [Java 9](https://www.oracle.com/technetwork/java/javase/9-deprecated-features-3745636.html#JDK-8184998){:target="_blank"}.
As such, we no longer utilize WebStart.

## Quick Navigation
* [Summary of changes for v5.0.x](#netcdf-java-api-changes-50x)
* [Summary of changes for v5.1.x](#netcdf-java-api-changes-51x)
* [Summary of changes for v5.2.x](#netcdf-java-api-changes-52x)
* [Summary of changes for v5.3.x](#netcdf-java-api-changes-53x)

## netCDF-Java API Changes (5.0.x)

### Unsigned Types

* `DataType` now has unsigned types: `UBYTE`, `USHORT`, `UINT`, `ULONG`
* `Array`, `ArrayScalar`, `ArrayByte`, `ArrayInt`, `ArrayShort`, `ArrayLong` factory and constructor methods now require `isUnsigned` parameter.
* `Array.factory(class, shape)` &rarr; `Array.factory(DataType, shape)` or `Array.factory(dataType, class, shape)`
* `Array.get1DJavaArray(Class)` &rarr; `Array.get1DJavaArray(DataType)` or `Array.get1DJavaArray(Class, isUnsigned)`
* Remove `Array.setUnsigned()`, `Variable.setUnsigned()`
* `Variable.getUnsigned()` &rarr; `Variable.getDataType().getUnsigned()`
* `new Attribute(String name, List values)` &rarr; `new Attribute(String name, List values, boolean isUnsigned)`
* `StructureDataScalar.addMember(String name, String desc, String units, DataType dtype, boolean isUnsigned, Number val)` &rarr; `StructureDataScalar.addMember(String name, String desc, String units, DataType dtype, Number val)`

### Variable Length (vlen) Dimensions and Variables

* The CDM data model is clarified to allow _vlen_ dimensions only in the outermost (fastest changing) Dimension.
* Reading a Variable with only a single _vlen_ Dimension will result in a regular Array.
* Reading a Variable with a nested _vlen_ Dimension will result in an ArrayObject containing regular Arrays of independent lengths.
* In both cases the returned array's `DataType` will be the primitive type.
* Previously the exact Array class and DataType returned from a read on a vlen was not well-defined.
* Use `Array.isVlen()` to discover if an Array represents vlen data.
* `ArrayObject.factory(Class classType, Index index)` is now `ArrayObject.factory(DataType dtype, Class classType, boolean isVlen, Index index)`
* Use `Array.makeVlenArray(int[] shape, Array[] data)` to construct _vlen_ data.

### AutoCloseable

_AutoCloseable_ was introduced in Java 7, along with the _try-with-resources_ language feature.
Use of this feature makes code more readable and more reliable in ensuring that resources (like file handles) are released when done.
We strongly recommend that you modify your code to take advantage of it wherever possible.
For example:

~~~java
 try (NetcdfFile ncfile = NetcdfFile.open(location)) {
    ...
 } catch (IOException ioe) {
   // handle ioe here, or propagate by not using catch clause
 }
~~~

* The following now implement _AutoCloseable_, so can be the target of _try-with-resource_:
 * `NetcdfFile`, `NetcdfFileWriter`
 * `HTTPMethod`, `HTTPSession`
 * `FeatureDataset`, `CoverageDataset`, `CoverageDatasetCollection`
 * `CFPointWriter`, `Grib2NetcdfFile`
 * ArrayStructure (deprecate _finish()_)
 * `PointFeatureCollectionIterator` (`finish()` method now deprecated)
 * `StructureDataIterator`, `PointFeatureIterator`, `PointFeatureCollectionIterator`, `NestedPointFeatureCollectionIterator`
 * `thredds.client.catalog.tools.DataFactory.Result`

### Iterable

_Iterable_ was introduced in Java 7, along with the _foreach_ language feature, and makes code more readable with less boilerplate.
For example:

~~~java
for (StructureData sdata : myArrayStructure) {
    // ...
}
~~~

* The following now implement `Iterable<>`, and so can be the target of _foreach_:
 * `Range` implements `Iterable<Integer>` (replace `first()`, `last()`, `stride()` methods)
 * `ArrayStructure` implements `Iterable<StructureData>` (replace `getStructureDataIterator()` method)
 * `PointFeatureIterator` extends `Iterator<PointFeature>`  (`finish()` method deprecated)
  * In order for `PointFeatureIterator` to implement `Iterator<PointFeature>`, the `hasNext()` and `next()` methods cannot throw `IOException`.
    The interface is changed to remove `throws IOException`, which will now be wrapped in `RuntimeException`.
 * `PointFeatureCollection` implements `Iterable<PointFeature>` (replace `hasNext()`, `next()`, `finish()`, `resetIteration()` methods)
 * `StationTimeSeriesFeatureCollection` implements `Iterable<StationTimeSeriesFeature>` (replace `hasNext()`, `next()`, `finish()`, `resetIteration()` methods)
 * `ProfileFeatureCollection` implements `Iterable<ProfileFeature>` (replace `hasNext()`, `next()`, `finish()`, `resetIteration()` methods)
 * `TrajectoryFeatureCollection` implements `Iterable<TrajectoryFeature>` (replace `hasNext()`, `next()`, `finish()`, `resetIteration()` methods)
 * `StationProfileFeature` implements `Iterable<ProfileFeature>` (replace `hasNext()`, `next()`, `finish()`, `resetIteration()` methods)
 * `StationProfileFeatureCollection` implements `Iterable<StationProfileFeature>` (replace `hasNext()`, `next()`, `finish()`, `resetIteration()` methods)
 * `SectionFeature` implements `Iterable<ProfileFeature>` (replace `hasNext()`, `next()`, `finish()`, `resetIteration()` methods)
 * `SectionFeatureCollection` implements `Iterable<SectionFeature>` (replace `hasNext()`, `next()`, `finish()`, `resetIteration()` methods)

### ucar.nc2.util.DiskCache2

* All instances of `DiskCache2` now have one cleanup thread
* The `DiskCache2.exit()` method is now static and need only be called once when the application is exiting.
* `DiskCache2.setLogger()` is removed.
* `DiskCache2.cleanCache(File dir, StringBuffer sbuff, boolean isRoot)` is now `DiskCache2.cleanCache(File dir, Formatter sbuff, boolean isRoot)`
* deprecated methods are removed: `setCachePathPolicy(int cachePathPolicy, String cachePathPolicyParam)`, `setPolicy(int cachePathPolicy)`
* logging of routine cache cleanup is now at `DEBUG` level

### ucar.ma2.Range

 * `Range.copy(String name)` replaced by `Range.setName(String name)`
 * `Range.getIterator()` deprecated, use `Range.iterator()`

Currently a `Range` is specified by _start:end:stride_
In the future, may be extended with subclasses `RangeScatter` and `RangeComposite`
You should use the iterator now to ensure correct functionality.
To iterate over the values of the `Range`:

~~~java
for (int i=range.first(); i<=range.last(); i+= range.stride()) {    // REPLACE THIS
 // ...
}

for (int i : range) {  // USE THIS
 // ...
}
~~~

### ucar.nc2.util.cache

* `FileCache` and `FileFactory` take a `DatasetUrl` instead of a String `location`

### ucar.nc2.dataset

In order to disambiguate remote protocols, all using _http:_, the utility method `DatasetUrl.findDatasetUrl(location)` is used to determine the protocol and capture the result in a `DatasetUrl` object.
Because this can be expensive, the `DatasetUrl` should be calculated once and kept for the duration of the dataset access.
When the protocol is already known, the `DatasetUrl(ServiceType protocol, String location)` constructor may be used.
The API is changed to allow/require the application to compute these `DatasetUrls`.

* `NetcdfDataset.acquireDataset()` takes a `DatasetUrl` instead of a String location.
* the general method of `NetcdfDataset.openDataset()` takes a DatasetUrl instead of a String location.
Variants use a String location, and call `DatasetUrl.findDatasetUrl(location)`.

* `CoordinateAxis2D.getMidpoints()` was deprecated and now removed, use `getCoordValuesArray()`

### ucar.nc2.ft.PointFeature

* Added method `getTimeUnit()`. An implementation exists in `ucar.nc2.ft.point.PointFeatureImpl`, so if your
`PointFeature` extends it, you shouldn't need to do any work.
* Removed method `getObservationTimeAsDate()`. Instead, use `getObservationTimeAsCalendarDate().toDate()`.
* Removed method `getNominalTimeAsDate()`. Instead, use `getNominalTimeAsCalendarDate().toDate()`.
* Removed method `getData()`. Instead, use `getDataAll()`.

### ucar.ma2.MAMath

* Added method `equals(Array, Array)`. It is intended for use in `Object.equals()` implementations.
This means, among other things, that corresponding floating-point elements must be exactly equal, not merely within
some epsilon of each other.
* Added method `hashCode(Array array)`. It is intended for use in `Object.hashCode()` implementations and is
compatible with `equals(Array, Array)`.
* Renamed `isEqual(Array, Array)` to `nearlyEquals(Array, Array)`. This was done to avoid (some) confusion with the new
`equals(Array, Array)`, and to highlight that this method performs _approximate_ comparison of floating-point numbers,
instead of the exact comparison done by `equals(Array, Array)`.

### Coordinate Systems

* `ucar.nc2.dataset.CoordTransBuilderIF` is split into `ucar.nc2.dataset.builder.HorizTransformBuilderIF` and `ucar.nc2.dataset.builder.VertTransformBuilderIF`
* `HorizTransformBuilderIF` now uses `AttributeContainer` instead of `NetcdfDataset`, `Variable`
* `CoordinateTransform.makeCoordinateTransform(NetcdfDataset ds, Variable ctv)` is now `ProjectionCT makeCoordinateTransform(AttributeContainer ctv)`
* Previously, the optional _false_easting_ and _false_northing_ should match the units of the _x_ and _y_ projection coordinates in `ucar.nc2.dataset.CoordinateSystem`
 * `List<Dimension> getDomain()` &rarr; `Collection<Dimension> getDomain()`
 * boolean `isSubset(List<Dimension> subset, List<Dimension> set)` &rarr; `isSubset(Collection<Dimension> subset, Collection<Dimension> set)`

### Feature Datasets

* `ucar.nc2.dt.TypedDatasetFactory` has been removed. Use `ucar.nc2.ft.FeatureDatasetFactoryManager`
* `ucar.nc2.dt.grid` is deprecated (but not removed) and is replaced by `ucar.nc2.ft2.coverage`
* `ucar.nc2.dt.point` and `ucar.nc2.dt.trajectory` have been removed, replaced by `ucar.nc2.ft.\*`
* In `FeatureDataset`, deprecated methods `getDateRange()`, `getStartDate()`, `getStartDate()` have been removed
* In `FeatureDataset`, mutating method removed: `calcBounds()`

### Point Feature Datasets (`ucar.nc2.ft` and `ucar.nc2.ft.point`)

* `FeatureCollection` has been renamed to `ucar.nc2.ft.DsgFeatureCollection` for clarity.
* `SectionFeature` and `SectionFeatureCollection` have been renamed to `TrajectoryProfileFeature`, `TrajectoryProfileFeatureCollection` for clarity.
 * `FeatureType.SECTION` renamed to `FeatureType.TRAJECTORY_PROFILE` for clarity.
 * `NestedPointFeatureCollection` has been removed, use `PointFeatureCC` and `PointFeatureCCC` instead when working with `DsgFeatureCollection` in a general way.
* In all the Point Feature classes, `DateUnit`, `Date`, and `DateRange` have been replaced by `CalendarDateUnit`, `CalendarDate`, and `CalendarDateRange`:
 * In `PointFeature` and subclasses, deprecated methods `getObservationTimeAsDate()`, `getNominalTimeAsDate()` have been removed
 * In `ProfileFeature`, `getTime()` returns `CalendarDate` instead of `Date`
 * In `PointFeature` implementations and subclasses, all constructors use `CalendarDateUnit` instead of `DateUnit`, and all `subset()` and `flatten()` methods use `CalendarDateRange`, not `DateRange`
 * In `CFPointWriter` subclasses, all constructors use `CalendarDateUnit` instead of `DateUnit`
* In `PointFeature`, deprecated method `getData()` is removed; usually replace it with `getDataAll()`
* In `PointFeatureCollection`, mutating methods are removed: `setCalendarDateRange()`, `setBoundingBox()`, `setSize()`, `calcBounds()`
* The time and altitude units for the collection can be found in the `DsgFeatureCollection`, and you can get the collection object from `PointFeature.getFeatureCollection()`
* In `PointFeatureIterator` and subclasses, methods `setCalculateBounds()`, `getDateRange()`, `getCalendarDateRange()`, `getBoundingBox()`, `getSize()` have been removed. That information is obtained from the `DsgFeatureCollection`.
* In `PointFeatureIterator` and subclasses, `setBufferSize()` bas been removed.
* In `PointFeatureCollection` and subclasses, `getPointFeatureIterator()` no longer accepts a `bufferSize` argument.

### Coverage Feature Datasets (`ucar.nc2.ft2.coverage`)

* Completely new package `ucar.nc2.ft2.coverage` that replaces `ucar.nc2.dt.grid`
  The class `FeatureDatasetCoverage` replaces `GridDataset`.
* Uses of classes in `car.nc2.dt.grid` are deprecated, though the code is still in the core jar file for now.
* For new API see [Coverage Datasets](coverage_feature.html)
* `FeatureType.COVERAGE` is the general term for `GRID`, `FMRC`, `SWATH`, `CURVILINEAR` types.
  Previously, `GRID` was used as the general type, now it refers to a specific type of Coverage.
  Affects `FeatureDatasetFactoryManager.open(FeatureType wantFeatureType, ...)`

### Shared Dimensions

* `Group.addDimension` and `Group.addDimensionIfNotExists` methods now throw an `IllegalArgumentException` if the
dimension isn't shared.
* `NetcdfFileWriter.addDimension` methods no longer have an `isShared` parameter. Such dimensions should always be
shared and allowing them to be private is confusing and error-prone.

### Catalog API

* All uses of classes in `thredds.catalog` are deprecated. If you still need these, you must add `legacy.jar` to your path.
* TDS and CDM now use `thredds.server.catalog` and `thredds.client.catalog`. The APIs are different, but with equivalent functionality to thredds.catalog.
* `thredds.client.DatasetNode` now has `getDatasetsLogical()` and `getDatasetsLocal()` that does or does not dereference a `CatalogRef`, respectively.
  You can also use `getDatasets()` which includes a dereferenced catalog if it has already been read.

## netCDF-Java API Changes (5.1.x)

List of GitHub commits since 5.0.0 release ([link](https://github.com/Unidata/netcdf-java/compare/v5.0.0...v5.1.0){:target="_blank"})

### Many checked exceptions removed

Many unneeded checked Exceptions were removed in 5.1.
In some cases, you may need to change your code to remove Exception handling.
For example, the `buildFrom*` methods from `thredds.client.catalog.builder.CatalogBuilder` no longer throw `IOException`s.

### CFGridCoverageWriter2

* The `writeOrTestSize` method of `CFGridCoverageWriter2` has been refactored into two new methods:
  * Check size of file before writing:
    ~~~java
    ucar.nc2.util.Optional<Long> getSizeOfOutput(CoverageCollection gdsOrg, List<String> gridNames,
                                                 SubsetParams subset, boolean tryToAddLatLon2D)
    ~~~
  * Write file and return size:
    ~~~java
    ucar.nc2.util.Optional<Long> write(CoverageCollection gdsOrg, List<String> gridNames,
                                       SubsetParams subset, boolean tryToAddLatLon2D,
                                       NetcdfFileWriter writer)
    ~~~

### ucar.nc2.constants.CDM.utf8Charset Removed

`ucar.nc2.constants.CDM.utf8Charset` has been removed in favor of `java.nio.charset.StandardCharsets.UTF_8`

### Limit use of org.joda.time

Remove usages of `org.joda.time` outside of `ucar.nc2.time`.

## netCDF-Java API Changes (5.2.x)

List of GitHub commits since 5.1.0 release ([link](https://github.com/Unidata/netcdf-java/compare/v5.1.0...v5.2.0){:target="_blank"})

### netcdf-java artifact changes

The following artifacts have changed:
* The `cdm` artifact has been split into:
  * `cdm-core`
  * `cdm-radial`
  * `cdm-misc`
* The `visadCdm` artifact has been split into:
 * `cdm-mcidas`
 * `cdm-vis5d`
* The `clcommon` artifact has been renamed to `cdm-image`

None of these moves resulted in an API break.
This move is a stepping stone towards supporting the Java Platform Module System in future releases of netCDF-Java.
At this very least, this change will require updating your build scripts to use `cdm-core` over `cdm`.
For information on what is included in the new modules, please visit the [Using netCDF-Java Maven Artifacts](using_netcdf_java_artifacts.html) documentation.

### Allow HTTPRandomAccessFile maximum buffer size to be set by System Property

When reading a remote file over http using byte-range requests, the default buffer size [can result in the need for a large heap](https://www.unidata.ucar.edu/mailing_lists/archives/netcdf-java/2019/msg00016.html){:target="_blank"}.
Starting with v5.2.0, users can set the maximum buffer size in bytes using the Java System Property `ucar.unidata.io.http.maxHTTPBufferSize` (default is 10 MB).

## netCDF-Java API Changes (5.3.x)

List of GitHub commits since 5.2.0 release ([link](https://github.com/Unidata/netcdf-java/compare/v5.2.0...v5.3.2){:target="_blank"})

### Opening remote files on Object Stores that support the AWS S3 API

Object Store support has been extended to interface with any AWS S3 API compatible system, and has been tested against Google Cloud Storage, Azure Blob Storage, ActiveScale Object Store, and Ceph.
Note: AWS specific S3 support was added in `5.3`.
Objects were identified using a specific URI of the form `s3://bucket-name/key`.
Usage of that URI has been deprecated in favor of the more flexible `cdms3 ":" [ // [ userinfo "@" ] host [ ":" port ] / ]  [path] "/" bucket_name ? key`.
For more information, see the [DatasetUrl](dataset_urls.html#object-stores) documentation.