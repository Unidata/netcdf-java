---
title: CDM Data Types
last_updated: 2019-07-17
sidebar: netcdfJavaTutorial_sidebar 
permalink: cdm_datatypes.html
toc: false
---

This document explains how CDM data types are mapped into Netcdf-Java objects.

## Overview

An `Array` contains the actual data for a `Variable` after it is read from the disk or network. Data access can be done in a general way through the method

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/CdmDataTypesExamples.java&readToArrayExample %}
{% endcapture %}
{{ rmd | markdownify }}
 
The one exception to this is `Sequences`, which must be accessed through `Sequence.getStructureIterator()` which returns `StructureData` objects.

For `Variables` that are members of a `Structure`, data is accessed generally through the method

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/CdmDataTypesExamples.java&readStructureDataExample %}
{% endcapture %}
{{ rmd | markdownify }}

When a `Variable` has a variable length dimension, `variableInstance.isVariableLength()` is `true` and an `Array` object with the appropriate element type is returned. 
You cannot subset on the variable length dimension, all of it is always read. For example:

~~~CDL
  short levels(acqtime=10, *);
~~~

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/CdmDataTypesExamples.java&readVariableLengthDimensionExample %}
{% endcapture %}
{{ rmd | markdownify }}

You cannot use the older style `read(origin, shape)` interface:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/CdmDataTypesExamples.java&badReadExample %}
{% endcapture %}
{{ rmd | markdownify }}

Otherwise the `Array` is rectangular, and the following is returned from `variableInstance.read() `or `structureDataInstance.getArray()`:

|---
| DataType | Array subclass | Array.getElementType
|:-|:-|:-
| BYTE | ArrayByte | byte.class
| SHORT | ArrayShort | short.class
| INT | ArrayInt | int.class
| LONG | ArrayLong | long.class
| FLOAT | ArrayFloat | float.class
| DOUBLE | ArrayDouble | double.class
| CHAR | ArrayChar | char.class
| STRING | ArrayObject | String.class
| STRUCTURE | ArrayStructure | StructureData.class
| SEQUENCE | ArraySequence | StructureData.class
| ENUM1 | ArrayByte | byte.class
| ENUM2 | ArrayShort | short.class
| ENUM4 | ArrayInt | int.class
| OPAQUE | ArrayObject | ByteBuffer.class
 
## CDM Data Types

### Integer types

The CDM integer types are `byte`, `short`, `int`, and `long`. Each are mapped to the corresponding Java primitive type, which are 1,2,4, and 8 bytes wide, respectively.

When the underlying file format stores data as unsigned integers, an IOSP may decide to widen the type to a signed type, eg

* `unsigned byte` -> `signed short`
* `unsigned short` -> `signed int`
* `unsigned int` -> `signed long`

Otherwise, the variable is kept as an unsigned integer type and the attribute `_Unsigned = "true" `is added to the variable. 
Since Java does not have unsigned integer types, handling unsigned values requires some attention. 
Unsigned integer data are stored in the corresponding signed primitive types. You can detect this by calling `Array.isUnsigned()` or `Variable.isUnsigned()`.

You may use static methods in `ucar.ma2.DataType` to convert one value at a time:

~~~java
static public long unsignedIntToLong(int i);
static public int unsignedShortToInt(short s);
static public short unsignedByteToShort(byte b);
~~~

You may use this static method in `ucar.ma2.MAMath` to widen all the values in an `Array`:

~~~java
public static Array convertUnsigned( Array unsigned);
~~~

Theres not much to do in a general way with `unsigned longs`, as there is no primitive type that can hold 64 bits of precision.

`ArrayByte`,`ArrayShort`, and `ArrayInt` will widen an unsigned value when casting to wider type like `float` or `double`. For example, calling

~~~java
data.getDouble()
~~~

on an unsigned integer type will return the widened value cast to a `double`.

### Floating point types

The CDM floating point types are `float` and `double`. Each are mapped to the corresponding Java primitive type.

### Char and String types

A `String` is a variable length array of [Unicode](http://unicode.org/){:target="_blank"} characters. When reading/writing a `String` to a file or other external 
representation, the characters are by default UTF-8 encoded (note that ASCII is a subset of UTF-8). Libraries may use different internal representations, 
for example the Java library uses UTF-16 encoding.

The `char` type contains uninterpreted characters, one character per byte. Typically these contain 7-bit ASCII characters.

### `Structure`

A `Structure` is a type of `Variable` that contains other `Variables`, analogous to a `struct` in C, or a `row` in a relational database. 
In general, the data in a `Structure` are physically stored close together on disk, so that it is efficient to retrieve all of the data in a `Structure` at the same time. 
A Var`iable contained in a `Structure` is a member `Variable`, and can only be read in the context of its containing `Structure`.

The member data of a `Structure` is returned in a `StructureData` object. Since a `Structure` may be multidimensional, `Structure.read()` returns an `ArrayStructure`, 
a subclass of `Array` which contains an array of `StructureData` objects. Alternatively, one can call `Structure.getStructureIterator()` and iterate through the 
`StructureData` in canonical order. This potentially is a very efficient way to access the data, since the data does not have to all be memory resident at the same time.

For type specific access, use methods `getScalarXXX` and `getJavaArrayXXX`:


{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/CdmDataTypesExamples.java&readStructureDataTypesExample %}
{% endcapture %}
{{ rmd | markdownify }}

For nested `Structure` and `Sequences` (that is, `Structure` members that are themselves `Structures` or `Sequences`), use


{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/CdmDataTypesExamples.java&readNestedStructureDataExample %}
{% endcapture %}
{{ rmd | markdownify }}quence data = StructureData.getArraySequence( memberName);
   
### Sequence

A `Sequence` is a variable length, one dimensional `Structure` whose length is not known until you actually read the data. 
To access the data in a `Sequence`, you must use `Structure.getStructureIterator()`, i.e. you cannot call `Sequence.read()`. 
`Sequences` make the most sense as members of a `Structure`.

### Enumeration

An `enum` type is a mapping of integer values to `Strings`. The mapping itself is stored in an `EnumTypedef` object in the `Group`, and so is shared across all `Variables` that use that enumeration.

A enumeration `Variable` will have `DataType` `ENUM1`, `ENUM2`, or `ENUM4`, depending on whether the the enum value is stored in 1, 2, or 4 bytes. 
The raw values are returned in a `byte`, `short`, or `int` array. 
One can convert these raw values to the corresponding `String` enumeration values in a way that does not depend on their internal representation, for example:


{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/CdmDataTypesExamples.java&readEnumValuesExample %}
{% endcapture %}
{{ rmd | markdownify }}
 
When wrapping a `NetcdfFile` with a `NetcdfDataset`, by default enumerations will be converted to their `String` values, and the type of the `Variable` will be changed to `String`. 
This conversion is controlled by the `enhanceMode` parameter on `NetcdfDataset.open`:

~~~java
 static public NetcdfDataset openDataset(String location, EnumSet<Enhance> enhanceMode, int buffer_size, CancelTask cancelTask, Object
  spiObject);
~~~

or by setting the default `enhanceMode`:

~~~java
 static public void setDefaultEnhanceMode(EnumSet<Enhance> mode);
~~~

If you want to turn enum conversion off, create your own `EnumSet` `enhanceMode`, that leaves the other default enhancements on:

~~~java
 EnumSet<Enhance> myEnhanceMode = EnumSet.of(Enhance.ScaleMissing, Enhance.CoordSystems);
~~~

## Opaque

An `opaque` type stores uninterpreted blobs of bytes. The length of the blob is not known until it is read, and an array of opaque objects 
may have different lengths for each of the objects. Opaque data is returned as `java.nio.ByteBuffer` objects wrapped by an `ArrayObject`.


 