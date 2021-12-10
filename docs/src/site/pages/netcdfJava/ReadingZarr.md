---
title: Reading the Zarr data model
last_updated: 2021-12-22
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: reading_zarr.html
---
## Zarr

As of version 5.5.1, the netCDF-Java library provides read-only support for the [Zarr v2 data model](https://zarr.readthedocs.io/en/stable/spec/v2.html#){:target="_blank"}. 
Any dataset that adheres to the v2 spec can be read into a `NetcdfFile` object, as long as the following is true:

* all filters and compressors used by the dataset must be known to the netCDF-Java library (see [Filters](reading_zarr.html#filters))
* the underlying storage of the dataset must be a directory store, zip store, or object store

### Enabling Zarr support

To use Zarr in the netCDF-Java library, you must include the `cdm-zarr` module in your netCDF-Java build. 
See [here](using_netcdf_java_artifacts.html) for more information on including optional modules.

### How to read a Zarr dataset

Reading a Zarr dataset is syntactically the same as reading a netCDF file:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/ZarrExamples.java&readZarrStores %}
{% endcapture %}
{{ rmd | markdownify }}

If the file is a legal Zarr dataset, the library will map it to a `NetcdfFile` object for reading.
See [reading CDM files](reading_cdm.html) for more examples on accessing data once the `NetcdfFile` object is returned.

## Filters

As of netCDF-Java version 5.5.1, a `ucar.nc2.filter` package is included, that provides a suite of implemented filters and compressors,
as well as a mechanism for user-supplied filters. This package is used by both the Zarr and HDF5 IOSPs, and is available for public use.

The current list of filters included natively in the netCDF-Java library is:

* Deflate (zlib)
* Shuffle
* 32-bit Checksum (CRC, Fletcher, and Adler)
* ScaleOffset

This list is still expanding, but if the filter you are looking for is not provided at this time, you are able to provide it yourself 
(See [Implementing a Filter](#implementing-a-filter)) for details.)

### Implementing a Filter

To add a user-supplied `Filter` to the netDF-Java library, you will have to provide two classes:

* A class that extends the `ucar.nc2.filter.Filter` abstract class
* A class that implements the `ucar.nc2.filter.FilterProvider` interface

Once implemented, you will need to include these classes as JAR files in your classpath. See [here](runtime_loading.html) for more information.

#### `Filter` implementation

To implement a user-supplied filter, you will need to extend the abstract `ucar.nc2.filter.Filter` class, and provide implementations for the following methods:
* `getName` returns a `String` identifier for the filter (see note below on filter names)
* `getId` returns an `int` identifier for the filter (see note below on filter ids)
* `encode` takes a `byte[]` of unfiltered data and returns a `byte[]` of filtered data
* `decode` takes a `byte[]` of filtered data and returns a `byte[]` of unfiltered data

Your `Filter` class should look something like this:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/ZarrExamples.java&implementFilter %}
{% endcapture %}
{{ rmd | markdownify }}

{%include note.html content="
`getName` and `getId`   
When reading data, [IOSPs](writing_iosp.html) can look up filters by either a `String` or `int` (name or id). Currently, the `ucar.nc2.filter`
package is shared by the Zarr and HDF5 IOSPs. The Zarr IOSP looks up filters by name; if you plan to use your third party filter with Zarr data,
the string returned by `getName` should match that specified by the [NumCodecs](https://numcodecs.readthedocs.io/en/stable/) library.
The HDF5 IOSP looks up filters by id; if you plan to use your third party filter with HDF5 data, the int returned by `getId` should adhere to the
[guidelines](https://portal.hdfgroup.org/display/support/Registered+Filter+Plugins) set by the HDF group.
If your filter is registered with the HDF group, your `getId` method should return the HDF id. If your filter is not registered with the HDF group,
" %}

#### `FilterProvider` implementation

For the netCDF-Java library to find your `Filter` implementation, you will need to provide a `FilterProvider` as well. 

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/ZarrExamples.java&implementFilterProvider %}
{% endcapture %}
{{ rmd | markdownify }}

There are two more methods in the `FilterProvider` interface: the `canProvide` methods. By default, these methods work as follows:
* `boolean canProvide(String name)` returns true if the string returned by `getName()` matches the string passed to the method
* `boolean canProvide(int id)` returns true if the int returned by `getId()` matches the int passed to the method

It is unlikely that you would want to override these methods, as the netCDF-Java [IOSPs](writing_iosp.html) look for the correct filter implementations 
for a dataset by either name or numeric id. However, it is possible to write your own implementation of `canProvide`; for example, the 
following `FilterProvider` returns an instance of a `DefaultFilter` regardless of the name or id provided.

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/ZarrExamples.java&implementFilterProvider2 %}
{% endcapture %}
{{ rmd | markdownify }}
