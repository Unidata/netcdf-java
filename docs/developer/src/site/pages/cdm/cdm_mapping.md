---
title: CDM NetCDF mapping
last_updated: 2018-10-23
sidebar: developer_sidebar
permalink: cdm_netcdf_mapping.html
toc: false
---

## Mapping between the CDM and NetCDF-4 Data Models

last modified: June 2019

The CDM data model is close to, but not identical to the <a href="https://www.unidata.ucar.edu/software/netcdf/workshops/2008/netcdf4/Nc4DataModel.html"> NetCDF-4 data model</a>. When reading netCDF-4 files, one is interested in the mapping from netCDF-4 to CDM. This mapping is relatively stable. As of version 4.3, the CDM can write to netCDF-4 files, and one is interested in the mapping from CDM to netCDF-4. This mapping is still being developed, eg to give users some control where needed.

NetCDF-4 intentionally supports a simpler data model than HDF5, which means there are HDF5 files that cannot be converted to netCDF-4. See: <a href="https://www.unidata.ucar.edu/software/netcdf/docs/faq.html#fv15"> NetCDF</a>

### Data Model Differences

#### DataTypes

##### From netCDF-4 to CDM

* A netCDF-4 _Compound is a CDM Structure_. Both can be arbitrarily nested. The components of the Compound type are called _fields_ in NetCDF-4 and _member variables_ in the CDM.
* A netCDF-4 _Enum_ is a CDM _enum1_, _enum2_, or _enum4_ and references a _EnumTypedef_ which holds the (enum, String) map.
* A netCDF-4 _Vlen_ is mapped to a CDM _variable length Dimension_.
* A netCDF-4 _Opaque_ type is a CDM _opaque_ type, but the length of the data cannot be found until you read the data.
* NetCDF-4 signed and unsigned _byte_, _short_, _int_, _long_ are mapped to CDM _byte_, _short_, _int_, _long_. If unsigned, the attribute <b>__Unsigned = "true"_</b> is added to the CDM Variable, and <b>_Variable.isUnsigned()_</b> is true, as well as <b>_Array.isUnsigned()_</b>, from any data that is read from the variable.

##### From CDM to netCDF-4

* A CDM array of Opaque may have a different length for each Opaque object. May have to read to find maximum length.
* A CDM Structure may have member Variables that use shared dimensions. NetCDF4 / HDF5 does not support shared dimensions in Compound fields. If writing CDM to netCDF4, currently we just anonymize the shared dimensions.
* A CDM Structure member Variables may have attributes. NetCDF4 / HDF5 does not support attributes on fields in Compound fields. The CDM uses [these conventions](../developer/netcdf_compound_attrs.html){:target="_blank"} for specifying attributes on compound fields.

#### Type Definitions

##### From netCDF-4 to CDM

* A netCDF-4 _Enumeration Type_ becomes a CDM _EnumTypedef_.
* All other netCDF-4 type definitions are repeated for each CDM variable that uses them. The attribute <b>__Typedef = "typename"_</b> is added to the CDM Variable, where typename is the name of the netCDF-4 type.

##### From CDM to netCDF-4

* A CDM EnumTypedef becomes a netCDF-4 Enumeration Type.
* If a CDM Variable has an attribute <b>__Typedef = "typename"_</b>, then the Variables' definition is made into a netCDF-4 type.

### Attributes

In CDM, an attribute type may only be a scalar or 1D array of signed _byte_, _short_, _int_, _long_, _float_, _double_, or _String_. A _char_ type is mapped to a _String_.

##### From netCDF-4 to CDM

* An attribute of _compound_ type in netCDF-4 is _flattened_, by making each field a separate attribute, with name _attName.fieldName_ in the CDM.
* If the compound attribute is for a compound variable, and the field name of the attribute matches a field name of the variable, the attribute is added to that field instead of being flattened.
* An attribute of _enum type_ in netCDF-4 becomes a _String_ type in the CDM. ???
* An attribute of _opaque type_ in netCDF-4 becomes a _byte_ type in the CDM.
* An attribute of _vlen of type_ in netCDF-4 becomes an array of _type_ in the CDM.
* An attribute of an unsigned _byte_, _short_, _int_ in netCDF-4 is promoted to a signed _short_, _int_, or _long_ in the CDM.

##### From CDM to netCDF-4

* Attributes on member variables of Structures are made into a compound attribute on the parent Structure.

### Differences between netCDF-4 C and Java libraries for netCDF4 files

#### Unsigned types

* The C library uses unsigned integer types: NC_UBYTE, NC_USHORT, NC_UINT, NC_UINT64.
* The Java library does not have separate types for unsigned integers, but adds the reserved attribute <b>__Unsigned = "true"_</b> when the variable is unsigned. One can check this with _Variable.isUnsigned()_, _Attribute.isUnsigned()_, and _Array.isUnsigned()_. Conversions done by the library are aware of this convention. Java does not have unsigned types, and we dont want to double the internal memory requirements by widening the data.

#### Enum Typedefs

* If there is a enum typedef that is not used by a variable, it will not show up in the enum typedefs.

#### Attributes

* When a variable is chunked, an integer array attribute named __ChunkSize_ is added to the variable, whose values are the chunk size for each dimension.

#### Creation Order

* The C library preserves the creation order of the Dimensions, Variables, Groups and Attributes, while the Java library does not.

#### Compound field Types

* in a netCDF4 file, fields in a Compound type may not have shared dimension.

### Differences between netCDF-4 C and Java libraries for HDF5 files

#### Fixed length Strings with anonymous dimension

* HDF5 object: type = 3 (String) with a dimension.
* C library: turns these into variable length Strings
* Java library: turns these into char arrays, with an anonymous dimension

#### Anonymous dimensions

* Java library: retains anonymous dimensions
* C library: turn into shared dimensions, by matching on the dimension length

#### Time datatype (HDF type 2)

* Java library: turn into a short, int ot long, depending on the precision
* C library: ignores this type
 

### Internal Notes

1) char arrays are interpreted as UTF-8 bytes array (Strings) when they are attributes. but data arrays are not, they are run through unsignedToShort() and cast to char. this seems like trouble.

2) nc4 allows arbitrary composition of vlen. cdm tries to map these to a variable length dimension, to get a ragged array, not part of the data type. But Arrays are rectangular, so its a difficult fit.

could define ArrayRagged which maps to C multidim arrays.

It is natural to map:

~~~java
 int data(x,y,*) -> int(*) data(x,y)
~~~

but it doesn't generalize well to nested vlens. nc4 solution is to declare each type separately and chain them:

~~~java
 int(*) type1;
 type1(*) type2;
 type2 data(x,y);
~~~

Array.isVariableLength(). IOSP might return ArrayInteger from int data(*). Needs to return ArrayObject for int data(3,*), with Array.isVariableLength() true.

~~~java
int(*)     returns ArrayInt
int(3,*)   returns ArrayObject(3) with ArrayInt(*) inside
int(*,3)  returns Array(n,3), whatever n happens to be.
int(3,*,*) returns ArrayObject(3) with ArrayObject(*) inside with ArrayInt(*) inside.
int(*,3,*) returns ArrayObject(n) with ArrayObject(3) inside with ArrayInt(*) inside.
int(*,*,3) returns ArrayObject(n) with ArrayInt(*,3) inside. OR  ArrayObject(n) with ArrayObject(*) with ArrayInt(3) inside.

struct {
  int i1;
  float vf(*);
} s(3);

is like float(3,*) -> ArrayObject(3) with ArrayFloat(*), inside the ArrayStructure.
this is getting out of control
~~~

3) attributes : n4 can be user defined types, cdm: 1 dim array of primitive or String.

~~~java
netcdf tst_enums {
  types:
    ubyte enum Bradys {Mike = 8, Carol = 7, Greg = 6, Marsha = 5, Peter = 4, Jan = 3, Bobby = 2, Whats-her-face = 1, Alice = 0} ;

// global attributes:
  Bradys :brady_attribute = Alice, Peter, Mike ;
}

netcdf R:/testdata/netcdf4/nc4/tst_enums.nc {
 types:
  enum Bradys { 'Alice' = 0, 'Whats-her-face' = 1, 'Bobby' = 2, 'Jan' = 3, 'Peter' = 4, 'Marsha' = 5, 'Greg' = 6, 'Carol' = 7, 'Mike' = 8};

 :brady_attribute = "Alice", "Peter", "Mike";
}
~~~

