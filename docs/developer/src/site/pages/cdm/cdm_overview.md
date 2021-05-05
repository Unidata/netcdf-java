---
title: The Common Data Model
last_updated: 2020-04-18
sidebar: developer_sidebar
permalink: cdm_overview.html
toc: false
---
## Unidiata's Common Data Model (Version 4)

Unidata’s Common Data Model (CDM) is an abstract <a href="https://en.wikipedia.org/wiki/Data_model" target="_blank">data model</a> for scientific datasets. 
It merges the <a href="https://www.unidata.ucar.edu/software/netcdf/" target="_blank">netCDF</a>, 
<a href="http://www.opendap.org" target="_blank">OPeNDAP</a>, and <a href="https://portal.hdfgroup.org/display/support/" target="_blank">HDF5</a> 
data models to create a common API for many types of scientific data. 
The NetCDF Java library is an implementation of the CDM which can read many [file formats](../developer/file_types.html){:target="_blank"} besides netCDF. 
We call these CDM files, a shorthand for files that can be read by the NetCDF Java library and accessed through the CDM data model.

The Common Data Model has three layers, which build on top of each other to add successively richer semantics:

1. The [Data Access Layer ](#data-access-layer-object-model), also known as the <b>_syntactic layer_</b>, handles data reading and writing.
2. The [Coordinate System ](#coordinate-system-object-model) layer identifies the coordinates of the data arrays. Coordinates are a completely general concept for scientific data; we also identify specialized <b>_georeferencing coordinate systems_</b>, which are important to the Earth Science community.
3. The [Scientific Feature Types](#scientific-feature-types) layer identifies specific types of data, such as grids, radial, and point data, adding specialized methods for each kind of data.


## Data Access Layer Object Model

{% include image.html file="overview/CDM-UML.png" alt="CDM UML" caption="" %}

A <b>_Dataset_</b> may be a netCDF, HDF5, GRIB, etc. file, an OPeNDAP dataset, a collection of files, or anything else which can be accessed through the netCDF API. We sometimes use the term <b>_CDM dataset_</b> to mean any of these possibilities, and to emphasize that a dataset does not have to be a file in netCDF format.

A <b>_Group_</b> is a container for Attributes, Dimensions, EnumTypedefs, Variables, and nested Groups. The Groups in a Dataset form a hierarchical tree, like directories on a disk.There is always at least one Group in a Dataset, the <b>_root Group_</b>, whose name is the empty string.

A <b>_Variable_</b> is a container for data. It has a DataType, a set of Dimensions that define its array shape, and optionally a set of Attributes. Any shared Dimension it uses must be in the same Group or a parent Group.

A <b>_Dimension_</b> is used to define the array shape of a Variable. It may be shared among Variables, which provides a simple yet powerful way of associating Variables. When a Dimension is shared, it has a unique name within the Group. If unlimited, a Dimension's length may increase. If variableLength, then the actual length is data dependent, and can only be found by reading the data. A variableLength Dimension cannot be shared or unlimited.

An <b>_Attribute_</b> has a name and a value, and associates arbitrary metadata with a Variable or a Group. The value is a scalar or one dimensional array of Strings or numeric values, so the possible data types are (<b>_String, byte, short, int, long, float, double_</b>). The integer types (<b>_byte, short, int, long_</b>) may be signed or unsigned.

A <b>_Structure_</b> is a type of Variable that contains other Variables, analogous to a <b>_struct_</b> in C, or a </b>_row_</b> in a relational database. In general, the data in a Structure are physically stored close together on disk, so that it is efficient to retrieve all of the data in a Structure at the same time. A Variable contained in a Structure is a member Variable, and can only be read in the context of its containing Structure.

A <b>_Sequence_</b> is a one dimensional Structure whose length is not known until you actually read the data. To access the data in a Sequence, you can only iterate through the Sequence, getting the data from one Structure instance at a time.

An <b>_EnumTypedef_</b> is an enumeration of Strings, used by Variables of type <b>_enum_</b>.

An <b>_Array_</b> contains the actual data for a Variable after it is read from the disk or network. You get an Array from a Variable by calling <b>_read()_</b> or its variants. An Array is rectangular in shape (like Fortran arrays). There is a specialized Array type for each of the DataTypes.

An <b>_ArrayStructure_</b> is a subclass of Array that holds the data for Structure Variables. Essentially it is an array of <b>_StructureData_</b> objects.

[CDM Datatypes](cdm_datatypes.html) describes in detail the possible types of data:

The primitive numeric types are byte, short, int, long, float and double. The integer types (8-bit byte, 16-bit short, 32-bit int, 64-bit long) may be signed or unsigned. Variable and Array objects have isUnsigned() methods to indicate, and conversion to wider types is correctly done.

* A String is a variable length array of <a href="http://unicode.org/" target="_blank">Unicode characters</a>. A string is stored in a netCDF file as UTF-8 encoded Unicode (note that ASCII is a subset of UTF-8). Libraries may use different internal representations, for example the Java library uses UTF-16 encoding.

* A char is an 8-bit byte that contains uninterpreted character data. Typically a char contains a 7-bit ASCII character, but the character encoding is application-specific. Because of this, one should avoid using the char type for data. A legitimate use of char is with netCDF classic files, to store string data or attributes. The CDM will interpret these as UTF-8 encoded Unicode, but 7-bit ASCII encoding is probably the only portable encoding.

* An enum type is a list of distinct (integer, String) pairs. A Variable with enum type stores integer values, which can be converted to the String enum value. There are 3 enum types: enum1, enum2, and enum4, corresponding to storing the integer as a byte, short, or int.

* An opaque type stores uninterpreted blobs of bytes. The length of the blob is not known until it is read. An array of opaque objects may have different lengths for each of the blobs.

An Object name refers to the name of a Group, Dimension, Variable, Attribute, or EnumTypedef. An object name is a String, a variable length array of Unicode characters. The [set of allowed characters](cdm_objectnames.html) is still being considered.

### Comparision to netCDF-4

The CDM data model is close to, but not identical with the <a href="https://www.unidata.ucar.edu/software/netcdf/docs/netcdf_data_model.html" target="_blank">netCDF-4 data model</a> <a href="https://www.unidata.ucar.edu/software/netcdf/workshops/2008/netcdf4/Nc4DataModel.html" target="_blank">(informal UML)</a>. However there is a complete 2-way mapping between the two models. In the CDM:

Non-shared dimensions are allowed, but netCDF-4 does not support these.
* An attribute type may only be a scalar or 1D array of byte, short, int, long, float, double, or String.
* Variables that are members of Structures may have attributes attached directly to them.
* The opaque type does not include the length of the opaque object, so an array of opaque may have different lengths for each object in the array.
* There are not separate unsigned integer types. The Variable and Array objects have isUnsigned() methods, and conversion to wider types is correctly done. Since Java does not have unsigned types, the alternative is to automatically widen unsigned data arrays, which would double the memory used.
* NetCDF-4 user-defined types are not kept independently from the variables that use them, except for EnumTypedef. The NetCDF-4 user-defined types are mapped to these CDM constructs:
    * A NetCDF-4 Compound is a CDM Structure.
    * A NetCDF-4 Enum is a CDM enum1, enum2, or enum4 and references a EnumTypedef which holds the (enum, String) map.
    * A NetCDF-4 Vlen is mapped to a CDM variable length Dimension.
    * A NetCDF-4 Opaque type is a CDM opaque type, but the length of the data cannot be found until you read the variable.
    
See [CDM to NetCDF mapping](cdm_netcdf_mapping.html) for detailed mapping between the NetCDF-4 and CDM data models.

### Comparision to OPeNDAP (DAP 2)

* OPeNDAP allows nested attributes, but in the CDM, attributes may only be a scalar or 1D array of byte, short, int, long, float, double, or String.
* OPeNDAP does not have these data types: signed byte, char, long, opaque, enum.
* OPeNDAP does not have shared dimensions. These can be approximated by map vectors in Grid datatypes, but one cannot share dimensions across Grids, or between Arrays.
* OPeNDAP does not support Groups.
* OPeNDAP Sequences map to CDM Sequences, but CDM does not support relational constraints at this layer.
See <a href="cdm_opendap_interface.html">here</a> for more details on OPeNDAP processing.
file formats

### Comparision to HDF-5

As of version 4.1, the CDM can read all versions of HDF5 through version 1.8.4, except for the following HDF5 features:

* SZIP compression. The <a href="http://www.hdfgroup.org/doc_resource/SZIP/" target="_blank">SZIP library</a> is proprietary and does not have a Java implementation. Its not clear if we can even legally write one if we wanted to.
* Dataset region references. <a href="http://www.google.com/search?q=HDF5+region+reference" target="_blank">These</a> are used in NPOESS, but their purpose is unclear. Since they point to other datasets that are accessible through the CDM, all the data in the file can still be read by the CDM. However, whatever information the region reference represents is not currently accessible.
* Since HDF5 does not support shared dimensions, however, reading HDF5 files into the higher levels of the CDM (Coordinate Systems, Grids, etc) may not work like you want. For this reason we recommend using the <a href="https://www.unidata.ucar.edu/software/netcdf/" target="_blank">netCDF-4 C library</a> for writing HDF5 files. <a href="https://www.unidata.ucar.edu/blogs/developer/en/entry/dimensions_scales" target="_blank">Here</a>is why.
* Hard links that cause cycles in the group structure are ignored. These break the CDM and netCDF-4 data model, in which groups comprise a tree. All info in the HDF-5 file is still available to CDM users, but certain paths that one could call from the HDF-5 library are not available.
* Please send file examples if you find a problem with the CDM reading HDF5 files, other than the ones listed above.

## Coordinate System Object Model

{% include image.html file="overview/CoordSys.png" alt="Coord Sys Object Model" caption="" %}

A <b>_Variable_</b> can have zero or more Coordinate Systems containing one or more CoordinateAxis. A CoordinateAxis can only be part of a Variable's CoordinateSystem if the CoordinateAxis' set of Dimensions is a subset of the Variable's set of Dimensions. This ensures that every data point in the Variable has a corresponding coordinate value for each of the CoordinateAxis in the CoordinateSystem.

A <b>_Coordinate System_</b> has one or more CoordinateAxis, and zero or more CoordinateTransforms.

A <b>_CoordinateAxis_</b> is a subtype of Variable, and is optionally classified according to the types in AxisType.

A <b>_CoordinateTransform_</b> abstractly represents a transformation between CoordinateSystems, and currently is either a Projection or a Vertical Transform.

The <b>_AxisType enumerations_</b> are specific to the case of georeferencing coordinate systems. Time refers to the real date/time of the dataset. Latitude and Longitude identify standard horizontal coordinates. Height and Pressure identify the vertical coordinate. GeoX and GeoY are used in transfomations (eg projections) to Latitude, Longitude. GeoZ is used in vertical transformations to vertical Height or Pressure. RadialAzimuth, RadialElevation and RadialDistance designate polar coordinates and are used for Radial DataTypes. RunTime and Ensemble are used in forecast model output data. Often much more detailed information is required (geoid reference, projection parameters, etc), so these enumerations are quite minimal.

### Restrictions on CoordinateAxes

These are the rules which restrict which Variables can be used as Coordinate Axes:

1. <b>_Shared Dimensions_</b>: All dimensions used by a Coordinate Axis must be shared with the data variable. When a variable is part of a Structure, the dimensions used by the parent Structure(s) are considered to be part of the nested Variable. Exceptions to this rule:
   1. String valued Coordinate Axes may be represented by variables of type char with a non-shared dimension representing the string length.
   2. DSG joins.
2. <b>_Structures and nested Structures_</b>: When a variable is part of a Structure, the dimensions used by the parent Structure(s) are considered to be part of the nested Variable. Therefore, all dimensions used by the parent Structure(s) of a Coordinate Axis must be shared with the data variable.
3. <b>_Variable length dimensions and Sequences_</b>: A variable length dimension is always a private (non-shared) dimension, and therefore cannot be used by a Coordinate Axis, except when the data variable and coordinate variable are in the same Structure. For example, a Sequence is a variable length array of Structures, and the following examples are legitimate uses of coordinate axes.

~~~java
Structure {
  float lat;
  float lon;
  float data;
    data:coordinates = "lat lon";
} sample(*)

~~~

~~~
Structure {
  float lat;
  float lon; 
  
  Structure {
    float altitude;
    float data;
      data:coordinates = "lat lon altitude";
  } profile(*)

} station(*)
~~~

Formally, a Variable is thought of as a sampled function whose domain is an index range; each CoordinateAxis is a scalar valued function on the same range; each CoordinateSystem is therefore a vector-valued function on the same range consisting of its CoordinateAxis functions. To take it one step further, when the CoordinateSystem function is invertible, the Variable can be thought of as a sampled function whose domain is the range of the Coordinate System, that is on Rn (the product space of real numbers). To be invertible, each CoordinateAxis should be invertible. For a 1-dimensional CoordinateAxis this simply means the coordinate values are strictly monotonic. For a 2 dimensional CoordinateAxis, it means that the lines connecting adjacent coordinates do not cross each other. For > 2 dimensional CoordinateAxis, it means that the surfaces connecting the adjacent coordinates do not intersect each other.

### Current Encodings

Neither NetCDF, HDF5, or OPeNDAP have Coordinate Systems as part of their APIs and data models, so their specification in a file is left to to higher level libraries (like HDF-EOS) and to conventions. If you are writing netCDF files, we strongly recommend using CF Conventions.

NetCDF has long had the convention of specifying a 1-dimensional CoordinateAxis with a coordinate variable, which is a Variable with the same name as its single dimension. This is a natural and elegant way to specify a 1-dimensional CoordinateAxis, since there is an automatic association of the Coordinate Variable with any Varaible that uses its dimension. Unfortunately there are not similarly elegant ways to specify a multidimensional CoordinateAxis, and so various attribute conventions have sprung up, that typically list the CoordinateAxis variables, for example the CF Conventions has:

~~~java
  float lat(y,x);
  float lon(y,x);
  float temperature(y,x);
    temperature:coordinates="lat lon";
~~~
    
Note that in this example, there is no CoordinateSystem object, so the same list has to be added to each Variable, and any CoordinateTransform specifications also have to be added to each Variable. However, the common case is that all the Variables in a dataset use the same Coordinate System.

The <b>_ucar.nc2.dataset_</b> layer reads various Conventions and extracts the Coordinate Systems using the CoordSysBuilder framework. We often use a set of internal attributes called the [Underscore Coordinate](coord_attr_conv.html) attributes as a way to standardize the Coordinate Systems information. Although these may work when working with Unidata software, we do not recommend them as a substitute for conventions such as CF.

## Scientific Feature Types

Scientific Feature Types are a way to categorize scientific data. The CDM Feature Type layer turns CDM datasets into collections of Feature Type objects, and allows a user to extract subsets of the Feature Types "in coordinate space" i.e. using spatial and temporal bounding boxes. In contrast, the CDM Data Access layer provides array index space subsetting, and the client application must know how to map array indices into coordinate values.

With these Feature Types objects, mapping into other data models like ISO/OGC becomes possible.