---
title: CDM Data Types
last_updated: 2019-07-17
sidebar: cdm_sidebar 
permalink: cdm_datatypes.html
toc: false
---

## CDM Data Types

This document explains how CDM data types are mapped into Netcdf-Java objects.

### Overview

An <b>_Array_</b> contains the actual data for a Variable after it is read from the disk or network. Data access can be done in a general way through the method

~~~java
 Array data = Variable.read();
~~~
 
The one exception to this is <b>_Sequences_</b>, which must be accessed through <b>_Sequence.getStructureIterator()_</b> which returns <b>_StructureData_</b> objects.

For Variables that are members of a Structure, data is accessed generally through the method

~~~java
    Array data = StructureData.getArray( memberName);
~~~

When a Variable has a variable length dimension, <b>_Variable.isVariableLength()_</b> is true and a <b>_ArrayObject_</b> with the appropriate element type is returned. You cannot subset on the variable length dimension, all of it is always read. For example:

~~~java
CDL:
  short levels(acqtime=10, *);

Java:
  Variable v = ncfile.findVariable("levels");
  Array data = v.read();
  NCdumpW.printArray(data, "read()",  new PrintWriter( System.out), null);
 
  // loop over outer dimension
  while (data.hasNext()) {
    Array as = (Array) data.next(); // inner variable length array of short
    NCdumpW.printArray(as, "",  new PrintWriter( System.out), null);
  }
  
  // subset ok on outer dimension
  data = v.read("0:9:2,:");
  NCdumpW.printArray(data, "read(0:9:2,:)",  new PrintWriter( System.out), null); // ok
    
  data = v.read(new Section().appendRange(0,9,2).appendRange(null));
  NCdumpW.printArray(data, "read(Section)",  new PrintWriter( System.out), null); // ok
~~~

You cannot use the older style <b>_read(origin, shape)_</b> interface:

~~~java
  // fail
  int[] origin = new int[] {0, 0};
  int[] size = new int[] {3, -1};
  data = v.read(origin, size);
~~~

Otherwise the Array is rectangular, and the following is returned from <b>_Variable.read()_</b> or <b>_StructureData.getArray()_</b>:

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
 

### Integer Types

The CDM integer types are <b>_byte_</b>, <b>_short_</b>, <b>_int_</b>, and <b>_long_</b>. Each are mapped to the corresponding Java primitive type, which are 1,2,4, and 8 bytes wide, respectively.

When the underlying file format stores data as unsigned integers, an IOSP may decide to widen the type to a signed type, eg

* unsigned byte > signed short
* unsigned short > signed int
* unsigned int > signed long

Otherwise, the the variable is kept as an unsigned integer type and the attribute <b>__Unsigned = "true"_</b> is added to the variable. Since Java does not have unsigned integer types, handling unsigned values requires some attention. Unsigned integer data are stored in the corresponding signed primitive types. You can detect this by calling <b>_Array.isUnsigned()_</b> or <b>_Variable.isUnsigned()_</b>.

You may use static methods in <b>_ucar.ma2.DataType_</b> to convert one value at a time:

* static public long unsignedIntToLong(int i);
* static public int unsignedShortToInt(short s);
* static public short unsignedByteToShort(byte b);

You may use this static method in <b>_ucar.ma2.MAMath_</b> to widen all the values in an Array:

~~~java
 public static Array convertUnsigned( Array unsigned);
~~~

Theres not much to do in a general way with unsigned longs, as there is no primitive type that can hold 64 bits of precision.

<b>_ArrayByte_</b>, <b>_ArrayShort_</b> and <b>_ArrayInt_</b> will widen an unsigned value when casting to wider type like float or double. For example, calling

~~~java
  data.getDouble()
~~~

on an unsigned integer type will return the widened value cast to a double.

### Floating Point Types

The CDM integer types are <b>_float_</b> and <b>_double_</b>. Each are mapped to the corresponding Java primitive type.

### Char and String Types

A <b>_String_</b> is a variable length array of [Unicode](http://unicode.org/){:target="_blank"} characters. When reading/writing a String to a file or other external representation, the characters are by default UTF-8 encoded (note that ASCII is a subset of UTF-8). Libraries may use different internal representations, for example the Java library uses UTF-16 encoding.

The char type contains uninterpreted characters, one character per byte. Typically these contain 7-bit ASCII characters.

### Structure

A Structure is a type of Variable that contains other Variables, analogous to a struct in C, or a row in a relational database. In general, the data in a Structure are physically stored close together on disk, so that it is efficient to retrieve all of the data in a Structure at the same time. A Variable contained in a Structure is a member Variable, and can only be read in the context of its containing Structure.

The member data of a Structure is returned in a StructureData object. Since a Structure may be multidimensional, Structure.read() returns an ArrayStructure, a subclass of Array which contains an array of StructureData objects. Alternatively, one can call Structure.getStructureIterator() and iterate through the StructureData in canonical order. This potentially is a very efficient way to access the data, since the data does not have to all be memory resident at the same time.

or type specific access:

~~~java
    StructureData.getScalarXXX( memberName);
    StructureData.getJavaArrayXXX( memberName);
~~~

For nested Structure and Sequences (that is, Structure members that are themselves Structures or Sequences, use

StructureData data = StructureData.getScalarStructure( memberName);

~~~java
  ArrayStructure data = StructureData.getArrayStructure( memberName);
  ArraySequence data = StructureData.getArraySequence( memberName);
~~~
   
### Sequence

A <b>_Sequence_</b> is a variable length, one dimensional Structure whose length is not known until you actually read the data. To access the data in a Sequence, you must use <b>_Structure.getStructureIterator()_</b>, ie you cannot call Sequence.read(). Sequences make the most sense as members of a Structure.

### Enumeration

An enum type is an mapping of integer values to Strings. The mapping itself is stored in an EnumTypedef object in the Group, and so is shared across all Variables that use that enumeration.

A enumeration Variable will have DataType <b>_ENUM1_</b>, <b>_ENUM2_</b>, or <b>_ENUM4_</b>, depending on whether the the enum value is stored in 1, 2, or 4 bytes. The raw values are returned in a byte, short, or integer array. One can convert these raw values to the corresponding String _enumeration values_ in a way that does not depend on their internal representation, for example:

~~~java
 if (var.getDataType().isEnum()) {
    Array rawValues = var.read();
    Array enumValues = Array.factory(DataType.STRING, rawValues.getShape());
    IndexIterator ii = enumValues.getIndexIterator();


    // use implicit Array iterator

    while (rawValues.hasNext()) {
      String sval = var.lookupEnumString(rawValues.nextInt());
      ii.setObjectNext(sval);
    }
  }
~~~
 
When wrapping a NetcdfFile with a NetcdfDataset, by default enumerations will be converted to their String values, and the type of the Variable will be changed to String. This conversion is controlled by the enhanceMode parameter on NetcdfDataset.open:

 static public NetcdfDataset openDataset(String location, EnumSet<Enhance> enhanceMode, int buffer_size, CancelTask cancelTask, Object
  spiObject);
or by setting the default enhanceMode:

 static public void setDefaultEnhanceMode(EnumSet<Enhance> mode);
If you want to turn enum conversion off, create your own EnumSet enhanceMode, for example this leaves the other default enhancements on:

 EnumSet<Enhance> myEnhanceMode = EnumSet.of(Enhance.ScaleMissing, Enhance.CoordSystems);

### Opaque

An opaque type stores uninterpreted blobs of bytes. The length of the blob is not known until it is read, and an array of opaque objects may have different lengths for each of the objects. Opaque data is returned as java.nio.ByteBuffer objects wrapped by an ArrayObject.


 