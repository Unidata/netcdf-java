---
title: CDM – OPeNDAP Interface
last_updated: 2019-07-22
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: cdm_opendap_interface.html
---

When the CDM reads an OPeNDAP dataset, it makes the following transformations:

| OPeNDAP primitive | CDM primitive |
|:------------------|:--------------|
| Boolean           | boolean       |
| Byte              | byte*         |
| Float32           | float         |
| Float64           | double        |
| Int16             | short         |
| Int32             | int           |
| UInt16            | short*        |
| UInt32            | int*          |

*\** the `_Unsigned` attribute is set

| Constructor Type | CDM DataType   |
|:-----------------|:---------------|
| Grid             | Variable       |
| List             | ignored        |
| Sequence         | Structure(*)   |
| String           | String or char |
| Structure        | Structure      |
| URL              | ignored        |
 

## OPeNDAP Attributes

An OPeNDAP Attribute may be a name/value pair, or may contain other attributes, in which case it is called an attribute container.
Containers may be nested.
The CDM first matches OPeNDAP attribute containers against `Variable` names.
On a match, the attributes become the Variable's attributes.
Anything that doesn't match is made into a global `Variable`.
The CDM does not support nested attributes, so these are flattened by using a "." to separate the container(s) from the name.

Certain OPeNDAP attribute containers are handled separately.
An attribute named `NC_GLOBAL` or `HDF_GLOBAL` is assumed to contain global attributes.
`Attribute` containers named `DODS_EXTRA` and `EXTRA_DIMENSION` are handled as described in the next section.

## OPeNDAP Dimensions

OPeNDAP (version 2) does not have explicit shared `Dimensions` in its object model, except for `Map` arrays inside of `Grids`.
NetCDF has only shared `Dimensions`, while the CDM data model allows for both shared and private (non-shared) `Dimensions`.
A shared CDM `Dimension` always has a name, while a private CDM `Dimension` may or may not be named.

Unnamed OPeNDAP `Dimensions` are mapped to anonymous (private, unnamed) CDM `Dimensions`.
Named OPeNDAP `Dimensions` are made into shared CDM `Dimensions`, in the order they are found in the `DDS`.
If a shared `Dimension` with the same name already exists, and it has a different length, a named OPeNDAP `Dimension` is made into a private, named CDM `Dimension`.
Because DAP 2 doesn't have `Groups`, all shared `Dimensions` will be global.
This effectively means that we assume that OPeNDAP `Dimensions` of the same name and length are the same object.

In order to get the full semantics of NetCDF files transported across the OPeNDAP protocol, we use 2 special global attributes.
If there is a global attribute called `DODS_EXTRA/Unlimited_Dimension` then the value of that attribute is assumed to be a `Dimension` name, which is then set to be an `Unlimited` `Dimension`.
If there is a global attribute called `EXTRA_DIMENSION`, then it contains one or more (name, length) attributes which are made into shared CDM `Dimensions`.
This allows a server to pass `Dimensions` which are not used in a `Variable`, but conceivably might be needed by a client.
Here are examples of these two cases:

~~~
DODS_EXTRA { String Unlimited_Dimension record; }  
~~~

`DODS_EXTRA` and `EXTRA_DIMENSION` are ad-hoc Conventions used only by the TDS as far as I know.

## OPeNDAP Grids

An OPeNDAP `Grid` object is decomposed into different `Variables` for the data array and for each map vector in the `Grid`.
The map vectors are placed at the global level, in the order they are found in the `DDS`.
If a map vector already exists, then it is assumed to be identical with the existing one.
`Dimensions` are currently not checked. 
or both maps and arrays, the containing grid name is discarded.
These assumptions preserve the semantics of netCDF datasets, but are a violation of the OPeNDAP specification in which `Grids` constitute a separate namespace.
Trying to preserve the namespace makes netCDF datasets incorrect.
This is one of the most important incompatibilities of the netCDF and OPeNDAP data models, since there is not workaround for DAP 2.
   
In order to preserve the semantics of the map vectors, the following CDM attribute is added to the data variable:

~~~   
_Coordinate.Axes = "map1 map2 map3";
~~~

The CDM Coordinate System layer uses this to associate the array and map Variables, as intended by the Grid specification

## OPeNDAP Strings

The default case is to map OPeNDAP Strings to CDM Strings.
If the `Variable` has an attribute named `DODS/strlen` , then the CDM instead uses `DataType.CHAR`.
The value of the `DODS/strlen` attribute is the length of the innermost dimension.
For example, an OPeNDAP String of dimension (5, 7) becomes a char `Variable` of dimension (5,7,strlen).
If the `Variable` has an attribute named `DODS/dimName`, then `dimName` becomes the dimension name of the inner `Dimension`, otherwise the `Dimension` is anonymous.

Some older OPeNDAP servers that read netCDF files map multidimensional char arrays to multidimensional String arrays, with each String being length 1.
As a workaround, the CDM reads the data for all String variables from an OPeNDAP dataset when the dataset is opened.
If it discovers that the data consists of Strings of length 1, it changes the `Variable` `dataType` to `DataType.CHAR`.
In any case, we cache the String data on the client.
This workaround is reasonable as long as there isn't huge amounts of String data in the dataset.

## OPeNDAP Sequences

An OPeNDAP `Sequence` is mapped to a one-dimensional CDM `Structure` of variable length (uses `Dimension.VLEN`).
The CDM now has a `Sequence` class, but we have not yet converted the CDM opendap code to use it.
The semantics of both are the same, the user is only allowed to read the entire sequence (no subsetting).
In the general API (ie through the `NetcdfFile` interface), constraint expressions are not supported .
However, a user could break encapsulation and call `DODSNetcdfFile.readWithCE()` to read data with a relational constraint expression on a `Sequence`.