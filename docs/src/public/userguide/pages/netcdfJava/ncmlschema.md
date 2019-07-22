---
title: Annotated Schema for NcML
last_updated: 2019-07-11
sidebar: netcdfJavaTutorial_sidebar 
permalink: annotated_ncml_schema.html
toc: false
---

_for version 4.4+ of the Netcdf-Java/CDM library_

An _<b>NcML document_</b> is an XML document (aka an _instance_ document) whose contents are described and constrained by _<b>NcML Schema-2.2_</b>. NcML Schema-2.2 combines the earlier _NcML core schema_ which is an XML description of the netCDF-Java / CDM data model, with the earlier _NcML dataset schema_, which allows you to define, redefine, aggregate, and subset existing netCDF files.

An NcML document represents a generic netCDF dataset, i.e. a container for data conforming to the netCDF data model. For instance, it might represent an existing netCDF file, a netCDF file not yet written, a GRIB file read through the netCDF-Java library, a subset of a netCDF file, an aggregation of netCDF files, or a self-contained dataset (i.e. all the data is included in the NcML document and there is no seperate netCDF file holding the data). An NcML document therefore should not necessarily be thought of as a physical netCDF file, but rather the "public interface" to a set of data conforming to the netCDF data model.

<b>_NcML Schema-2.2_</b> is written in the <a href="http://www.w3.org/XML/Schema">W3C XML Schema</a> language, and essentially represents the netCDF-Java / CDM data model, which schematically looks like this in UML

{% include image.html file="netcdf-java/reference/uml/CDM-UML.png" alt="CDM UML" caption="" %}

### Annotated Schema

Aggregation specific elements are listed in <font color="darkred">red. The forecastModelRunCollection, forecastModelRunSingleCollection, joinExisting and joinNew aggregation types are called <b>outer aggregations</b> because they work on the outer (first) dimension.</font>

### schema Element

~~~
<?xml version="1.0" encoding="UTF-8"?>
<xsd:schema targetNamespace="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2"
  xmlns:xsd="http://www.w3.org/2001/XMLSchema"
  xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2"
  elementFormDefault="qualified">
~~~

### netcdf Element

The element <b>_netcdf_</b> is the root tag of the NcML instance document, and is said to define a <b>_NetCDF dataset_</b>.

~~~
<!-- XML encoding of Netcdf container object -->
<xsd:element name="netcdf">
  <xsd:complexType>
    <xsd:sequence>

 (1)  <xsd:choice minOccurs="0">
        <xsd:element name="readMetadata"/>
        <xsd:element name="explicit"/>
      </xsd:choice>

 (2)  <xsd:element name="iospParam" minOccurs="0" />

 (3)  <xsd:choice minOccurs="0" maxOccurs="unbounded">
        <xsd:element ref="group"/>
        <xsd:element ref="dimension"/>
        <xsd:element ref="variable"/>
        <xsd:element ref="attribute"/>
        <xsd:element ref="remove"/>
      </xsd:choice>

 (4)  <xsd:element ref="aggregation" minOccurs="0"/>
    </xsd:sequence>
 (5)<xsd:attribute name="location" type="xsd:anyURI"/>
 (6)<xsd:attribute name="id" type="xsd:string"/>
 (7)<xsd:attribute name="title" type="xsd:string"/>
 (8)<xsd:attribute name="enhance" type="xsd:string"/>
 (9)<xsd:attribute name="addRecords" type="xsd:boolean"/>

(10)<xsd:attribute name="iosp" type="xsd:string"/>
    <xsd:attribute name="iospParam" type="xsd:string"/>
    <xsd:attribute name="bufferSize" type="xsd:int"/>

  <!-- for netcdf elements nested inside of aggregation elements -->
(11)<xsd:attribute name="ncoords" type="xsd:string"/>
(12)<xsd:attribute name="coordValue" type="xsd:string"/>
(13)<xsd:attribute name="section" type="xsd:string"/>

  </xsd:complexType>
</xsd:element>
~~~

1. A <b>_readMetadata_</b> (default) or an <b>_explicit_</b> element comes first. The <b>_readMetadata_</b> element indicates that all the metadata from the referenced dataset will be read in. The <b>_explicit_</b> element indicates that only the metadata explictly declared in the NcML file will be used.
2. An optional <b>_iospParam_</b> element. The NcML inside this element is passed directly to the IOSP. If an iospParam attribute is used, the attribute is used instead.
3. The <b>_netcdf_</b> element may contain any number (including 0) of elements <b>_group, variable, dimension, or attribute_</b> that can appear in any order. If you use readMetadata, you can <b>_remove_</b> specific elements with the remove element.
4. <font color="darkred"> An aggregation element is used to logically join multiple netcdf datasets into a single dataset.</font>
5. The optional <b>_location_</b> attribute provides a reference to another netCDF dataset, called the <b>_referenced dataset_</b>. The location can be an absolute URL (eg <b>_http://server/myfile_</b>, or <b>_file:/usr/local/data/mine.nc_</b>) or a URL reletive to the NcML location (eg <b>_subdir/mydata.nc_</b>). The referenced dataset contains the variable data that is not explicitly specified in the NcML document itself. If the location is missing and the data is not defined in values elements, then an empty file is written similar to the way CDL files are written by ncgen.
6. The optional <b>_id_</b> attribute is meant to provide a way to uniquely identify (relative to the application context) the NetCDF dataset. It is important to understand that the id attribute refers to the NetCDF dataset defined by the XML instance document, NOT the referenced dataset if there is one.
7. The optional <b>_title_</b> attribute provides a way to add a human readable title to the netCDF dataset.
8. The optional <b>_enhance_</b> attribute indicates whether the referenced dataset is opened in enhanced mode, and can be set to _All_, _AllDefer_, _ScaleMissing_, _ScaleMissingDefer_, _CoordSystems_, or _None_ (case insensitive). For backwards compatibility, a value of _true_ means _All_. Default is _None_. See [NetcdfDataset.EnhanceMode](#netcdf_dataset.html#NetcdfDataset.Enhance).
9. The optional <b>_addRecords_</b> attribute is used only when the referenced datasets is a netCDF-3 file. If true (default false) then a Structure named <b>_record_</b> is added, containing the record (unlimited) variables. This allows one to read efficiently along the unlimited dimension.
10. These 3 parameters control how the referenced dataset is opened by the IOServiceProvider. If <b>_iosp_</b> is specified, its value must be a fully qualified class name of an [IOServiceProvider](writing_iosp.html) class that knows how to open the file specified by <b>_location_</b>. The optional <b>_iospParam_</b> is passed to the IOSP through the <b>_IOServiceProvider_</b>.setSpecial() method. The optional <b>_bufferSize_</b> tells the IOSP how many bytes to use for buffering the file data.
11. <font color="darkred">The optional <b>ncoords</b> attribute is used for <b>joinExisting</b> aggregation datasets to indicate the number of coordinates that come from the dataset. This is used to avoid having to open each dataset when starting.</font>
12. <font color="darkred">The <b>coordValue</b> attribute is used for <b>joinExisting</b> or <b>joinNew</b> aggregations to assign a coordinate value(s) to the dataset. A <b>joinNew</b> aggregation always has exactly one coordinate value. A joinExisting may have multiple values, in which case, blanks and/or commas are used to delineate them, so you cannot use those characters in your coordinate values.</font>
13. <font color="darkred">The <b>section</b> attribute is used only for tiled aggregations, and describes which section of the entire dataset this dataset represents. The section value follows the ucar.ma2.Section section spec (see javadocs), eg "(1:20,:,3)", parenthesis optional</font>

An example:

~~~
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" 
		  location="C:/dev/github/thredds/cdm/src/test/data/testWrite.nc">
  <dimension name="lat" length="64" />
  <dimension name="lon" length="128" />
  <dimension name="names_len" length="80" />
  <dimension name="names" length="3" />

  <variable name="names" type="char" shape="names names_len" />
 
  <variable name="temperature" shape="lat lon" type="double">
    <attribute name="units" value="K" />
    <attribute name="scale" type="int" value="1 2 3" />
  </variable>

</netcdf>
~~~

#### group Element

A <b>_group_</b> element represents a netCDF group, a container for <b>_variable, dimension, attribute_</b>, or other <b>_group_</b> elements.

~~~ 
<xsd:element name="group">
  <xsd:complexType>
(1)<xsd:choice minOccurs="0" maxOccurs="unbounded">
     <xsd:element ref="enumTypedef"/>
     <xsd:element ref="dimension"/>
     <xsd:element ref="variable"/>
     <xsd:element ref="attribute"/>
     <xsd:element ref="group"/>
     <xsd:element ref="remove"/>
   </xsd:choice>

(2)<xsd:attribute name="name" type="xsd:string" use="required"/>
(3)<xsd:attribute name="orgName" type="xsd:string"/>
  </xsd:complexType>
</xsd:element>


~~~

1. The <b>_group_</b> element may contain any number (including 0) of elements <b>_group, variable, dimension_</b>, or <b>_attribute_</b> that can appear in any order. You can also mix in remove elements to remove elements coming from the referenced dataset.
2. The mandatory name attribute must be unique among groups within its containing <b>_group_</b> or <b>_netcdf_</b> element.
3. The optional attribute <b>_orgName_</b> is used when renaming a group.

#### dimension Element

The <b>_dimension_</b> element represents a netCDF dimension, i.e. a named index of specified length.

~~~
  <!-- XML encoding of Dimension object -->
  <xsd:element name="dimension">
    <xsd:complexType>
(1)   <xsd:attribute name="name" type="xsd:token" use="required"/>
(2)   <xsd:attribute name="length" type="xsd:string"/>
(3)   <xsd:attribute name="isUnlimited" type="xsd:boolean" default="false"/>
(4)   <xsd:attribute name="isVariableLength" type="xsd:boolean" default="false"/>
(5)   <xsd:attribute name="isShared" type="xsd:boolean" default="true"/>
(6)   <xsd:attribute name="orgName" type="xsd:string"/>
    </xsd:complexType>
  </xsd:element>
~~~

1. The mandatory <b>_name_</b> attribute must be unique among dimensions within its containing <b>_group_</b> or <b>_netcdf_</b> element.
2. The mandatory attribute <b>_length_</b> expresses the cardinality (number of points) associated with the dimension. Its value can be any non negative integer including 0 (since the unlimited dimension in a netCDF file may have length 0, corresponding to 0 records). A variable length dimension should be given length="*".
3. The attribute <b>_isUnlimited_</b> is _true_ only if the dimension can grow (a.k.a the record dimension in NetCDF-3 files), and _false_ when the length is fixed at file creation.
4. The attribute <b>_isVariableLength_</b> is used for _variable length data types_, where the length is not part of the metadata..
5. The attribute <b>_isShared_</b> is _true_ for shared dimensions, and _false_ when the dimension is private to the variable.
6. The optional attribute <b>_orgName_</b> is used when renaming a dimension.

#### variable Element

A <b>_variable_</b> element represents a netCDF variable, i.e. a scalar or multidimensional array of specified type indexed by 0 or more dimensions.

~~~
  <xsd:element name="variable">
    <xsd:complexType>
      <xsd:sequence>
(1)     <xsd:element ref="attribute" minOccurs="0" maxOccurs="unbounded"/>
(2)     <xsd:element ref="values" minOccurs="0"/>
(3)     <xsd:element ref="variable" minOccurs="0" maxOccurs="unbounded"/>
(4)     <xsd:element ref="logicalSection" minOccurs="0"/>
(5)     <xsd:element ref="logicalSlice" minOccurs="0"/>
(6)     <xsd:element ref="remove" minOccurs="0" maxOccurs="unbounded" />
      </xsd:sequence>

(7)   <xsd:attribute name="name" type="xsd:token" use="required" />
(8)   <xsd:attribute name="type" type="DataType" use="required" />
(9)   <xsd:attribute name="typedef" type="xsd:string"/>
(10)  <xsd:attribute name="shape" type="xsd:token" />
(11)  <xsd:attribute name="orgName" type="xsd:string"/>
    </xsd:complexType>
  </xsd:element>
~~~

1. A <b>_variable_</b> element may contain 0 or more <b>_attribute_</b> elements,
2. The optional <b>_values_</b> element is used to specify the data values of the variable. The values must be listed compatibly with the size and shape of the variable (slowest varying dimension first). If not specified, the data values are taken from the variable of the same name in the referenced dataset. Values are the "raw values", and will have scale.offset/missing applied to them if those attributes are present.
3. A variable of data type <b>_structure_</b> may have nested variable elements within it.
4. Create a logical section of this variable.
5. Create a logical slice of this variable, where one of the dimensions is set to a constant.
6. You can remove attributes from the underlying variable.
7. The mandatory <b>_name_</b> attribute must be unique among variables within its containing <b>_group, variable_</b>, or <b>_netcdf_</b> element.
8. The <b>_type_</b> attribute is one of the enumerated DataTypes.
9. The typedef is the name of an enumerated Typedef. Can be used only for <b>_type=enum1, enum2 or enum4_</b>.
10. The <b>_shape_</b> attribute lists the names of the dimensions the variable depends on. For a scalar variable, the list will be empty. The dimension names must be ordered with the slowest varying dimension first (same as in the CDL description). Anonymous dimensions are specified with just the integer length. For backwards compatibility, scalar variables may omit this attribute, although this is deprecated.
11. The optional attribute <b>_orgName_</b> is used when renaming a variable. .

#### values Element

A values element specifies the data values of a variable, either by listing them for example: <values>-109.0 -107.0 -115.0 93.923230</values> or by specifying a start and increment, for example: <values start="-109.5" increment="2.0" />. For a multi-dimensional variable, the values must be listed compatibly with the size and shape of the variable (slowest varying dimension first).

~~~
  <xsd:element name="values">
    <xsd:complexType mixed="true">
 (1)  <xsd:attribute name="start" type="xsd:float"/>
      <xsd:attribute name="increment" type="xsd:float"/>
      <xsd:attribute name="npts" type="xsd:int"/>
 (2)  <xsd:attribute name="separator" type="xsd:string" />
 (3)  <xsd:attribute name="fromAttribute" type="xsd:string"/>
    </xsd:complexType>
  </xsd:element>
~~~

1. The values can be specified with a <b>_start_</b> and <b>_increment_</b> attributes, if they are numeric and evenly spaced. You can enter these as integers or floating point numbers, and they will be converted to the data type of the variable. The number of points will be taken from the shape of the variable. (For backwards compatibility, an <b>_npts_</b> attribute is allowed, although this is deprecated and ignored).
2. By default, the list of values are separated by whitespace but a different token can be specified using the <b>_separator_</b> attribute. This is useful if you are entering String values, e.g. <values separator="*">My dog*has*fleas</values> defines three Strings.
3. The values can be specified from a global or variable attribute. To specify a global attribute, use <b>_@gattname_</b>. For a variable attibute use <b>_varName@attName_</b>. The data type and the shape of the variable must agree with the attribute.

#### attribute Element

The attribute elements represents a netCDF attribute, i.e. a name-value pair of specified type. Its value may be specified in the value attribute or in the element content.

~~~
  <xsd:element name="attribute">
    <xsd:complexType mixed="true">
(1)   <xsd:attribute name="name" type="xsd:token" use="required"/>
(2)   <xsd:attribute name="type" type="DataType" default="String"/>
(3)   <xsd:attribute name="value" type="xsd:string" />
(4)   <xsd:attribute name="separator" type="xsd:string" />
(5)   <xsd:attribute name="orgName" type="xsd:string"/>
(6)   <xsd:attribute name="isUnsigned" type="xsd:boolean"/>
    </xsd:complexType>
  </xsd:element>
~~~

1. The mandatory <b>_name_</b> attribute must be unique among attributes within its containing <b>_group_</b>, <b>_variable_</b>, or <b>_netcdf_</b> element.
2. The <b>_type_</b> attribute may be <b>_String, byte, short, int, long, float, double_</b>. If not specified, it defaults to a String.
3. The value attribute contains the actual data of the _attribute_ element. In the most common case of single-valued attributes, a single number or string will be listed (as in value="3.0"), while in the less frequent case of multi-valued attributes, all the numbers will be listed and separated by a blank or optionally some other character (as in value="3.0 4.0 5.0"). Values can also be specified in the element content:

~~~
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2">
  <attribute name="actual_range" type="int" value="1 2" />
  <attribute name="factual_range" type="int">1 2</attribute>
</netcdf>
~~~

4. By default, if the attribute has type String, the entire value is taken as the attribute value, and if it has type other than String, then the list of values are separated by whitespace. A different token seperator can be specified using the <b>_separator_</b> attribute.
5. The optional attribute <b>_orgName_</b> is used when renaming an existing attribute.
6. The attribute's values may be unsigned (if _byte, short, int_ or _long_). By default, they are signed.

#### DataType Type

The DataType Type is an enumerated list of the data types allowed for NcML Variable objects.

~~~
 <xsd:simpleType name="DataType">
    <xsd:restriction base="xsd:token">
      <xsd:enumeration value="byte"/>
      <xsd:enumeration value="char"/>
      <xsd:enumeration value="short"/>
      <xsd:enumeration value="int"/>
      <xsd:enumeration value="long"/>
      <xsd:enumeration value="float"/>
      <xsd:enumeration value="double"/>
      <xsd:enumeration value="String"/>
      <xsd:enumeration value="string"/>
      <xsd:enumeration value="Structure"/>
      <xsd:enumeration value="Sequence"/>
      <xsd:enumeration value="opaque"/>
      <xsd:enumeration value="enum1"/>
      <xsd:enumeration value="enum2"/>
      <xsd:enumeration value="enum4"/>
    </xsd:restriction>
  </xsd:simpleType>
~~~

* Unsigned integer types (byte, short, int) are indicated with an _Unsigned = "true" attribute on the Variable.
* A Variable with type enum1. enum2 or enum4 will refer to a enumTypedef object. Call Variable.getEnumTypedef().

#### enumTypedef Element

The enumTypedef element defines an enumeration.

~~~
 <xsd:element name="enumTypedef">
   <xsd:complexType mixed="true">
      <xsd:sequence>
        <xsd:element name="map" minOccurs="1" maxOccurs="unbounded">
          <xsd:complexType mixed="true">
            <xsd:attribute name="value" type="xsd:string" use="required"/>
          </xsd:complexType>
        </xsd:element>
      </xsd:sequence>
      <xsd:attribute name="name" type="xsd:token" use="required"/>
      <xsd:attribute name="type" type="DataType" default="enum1"/>
    </xsd:complexType>
  </xsd:element>
~~~

Example:

~~~
<?xml version="1.0" encoding="UTF-8"?>
<netcdf xmlns="http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2" location="Q:/cdmUnitTest/formats/netcdf4/tst/test_enum_type.nc">
  <enumTypedef name="cloud_class_t" type="enum1">
    <enum key="0">Clear</enum>
    <enum key="1">Cumulonimbus</enum>
    <enum key="2">Stratus</enum>
    <enum key="3">Stratocumulus</enum>
    <enum key="4">Cumulus</enum>
    <enum key="5">Altostratus</enum>
    <enum key="6">Nimbostratus</enum>
    <enum key="7">Altocumulus</enum>
    <enum key="8">Cirrostratus</enum>
    <enum key="9">Cirrocumulus</enum>
    <enum key="10">Cirrus</enum>
    <enum key="255">Missing</enum>
  </enumTypedef>
  <dimension name="station" length="5" />
  <variable name="primary_cloud" shape="station" type="enum1">
    <attribute name="_FillValue" value="Missing" />
  </variable>
</netcdf>
~~~

#### remove Element

The remove element is used to remove attribute, dimension, variable or group objects that are in the referenced dataset. Place the remove element in the container of the object to be removed.

~~~
  <xsd:element name="remove">
    <xsd:complexType>
(1)   <xsd:attribute name="name" type="xsd:string" use="required"/>
(2)   <xsd:attribute name="type" type="ObjectType" use="required"/>
    </xsd:complexType>
  </xsd:element>
~~~

~~~
 <xsd:simpleType name="ObjectType">
   <xsd:restriction base="xsd:string">
     <xsd:enumeration value="attribute"/>
     <xsd:enumeration value="dimension"/>
     <xsd:enumeration value="variable"/>
     <xsd:enumeration value="group"/>
   </xsd:restriction>
 </xsd:simpleType>
~~~

1. The name of the object to remove
2. The type of the object to remove: attribute, dimension, variable or group.

#### logical view Elements

(since version 4.4)

These allow a variable to be a <b>_logical view_</b> of the original variable. Only one of the logical views can be used per variable.

~~~
 <!-- logical view: use only a section of original  -->
 <xsd:element name="logicalSection">
   <xsd:complexType>
     <xsd:attribute name="section" type="xsd:token" use="required"/>  <!-- creates anonymous dimensions -->
   </xsd:complexType>
 </xsd:element>
   
 <xsd:element name="logicalSlice">
   <xsd:complexType>
     <xsd:attribute name="dimName" type="xsd:token" use="required"/>
     <xsd:attribute name="index" type="xsd:int" use="required"/>
   </xsd:complexType>
 </xsd:element>

 <xsd:element name="logicalReduce">
   <xsd:complexType>
     <xsd:attribute name="dimNames" type="xsd:string" use="required"/>
   </xsd:complexType>
 </xsd:element>
~~~

#### logicalReduce example:

The original variable has dimensions of length=1 named "latitude" and "longitude" :

~~~
<dimension name="time" length="143" />
<dimension name="pressure" length="63" />
<dimension name="latitude" length="1" />
<dimension name="longitude" length="1" />

<variable name="temperature" shape="time pressure latitude longitude" type="float">
  <attribute name="long_name" value="Sea Temperature" />
  <attribute name="units" value="Celsius" />
</variable>
~~~

Here is the NcML to remove them:

~~~
<variable name="temperature">
  <logicalReduce dimNames="latitude longitude" />
</variable>
~~~

<b>_Everything following pertains to aggregation, and can be ignored if you are not using aggregation_</b>.

#### aggregation Element

The aggregation element allows multiple datasets to be combined into a single logical dataset. There can only be one aggregation element in a netcdf element.

~~~
<xsd:element name="aggregation">
  <xsd:complexType>
    <xsd:sequence>
(1)  <xsd:choice minOccurs="0" maxOccurs="unbounded">
      <xsd:element ref="group"/>
      <xsd:element ref="dimension"/>
      <xsd:element ref="variable"/>
      <xsd:element ref="attribute"/>
      <xsd:element ref="remove"/>
     </xsd:choice>

(2)  <xsd:element name="variableAgg" minOccurs="0" maxOccurs="unbounded">
      <xsd:complexType>
       <xsd:attribute name="name" type="xsd:string" use="required"/>
      </xsd:complexType>
     </xsd:element>
(3) <xsd:element ref="promoteGlobalAttribute" minOccurs="0" maxOccurs="unbounded"/>
(4)  <xsd:element ref="cacheVariable" minOccurs="0" maxOccurs="unbounded"/>
(5)  <xsd:element ref="netcdf" minOccurs="0" maxOccurs="unbounded"/>
(6)  <xsd:element name="scan" minOccurs="0" maxOccurs="unbounded">
      <xsd:complexType>
(7)    <xsd:attribute name="location" type="xsd:string" use="required"/>
(8)    <xsd:attribute name="regExp" type="xsd:string" />
(9)    <xsd:attribute name="suffix" type="xsd:string" />
(10)   <xsd:attribute name="subdirs" type="xsd:boolean" default="true"/>
(11)   <xsd:attribute name="olderThan" type="xsd:string" />
(12)   <xsd:attribute name="dateFormatMark" type="xsd:string" />
(13)   <xsd:attribute name="enhance" type="xsd:string"/>
      </xsd:complexType>
     </xsd:element>

(14) <xsd:element name="scanFmrc" minOccurs="0" maxOccurs="unbounded">
      <xsd:complexType>
(7)    <xsd:attribute name="location" type="xsd:string" 
(8)    <xsd:attribute name="regExp" type="xsd:string" />use="required"/>
(9)    <xsd:attribute name="suffix" type="xsd:string" />
(10)   <xsd:attribute name="subdirs" type="xsd:boolean" default="true"/>
(11)   <xsd:attribute name="olderThan" type="xsd:string" />

(15)   <xsd:attribute name="runDateMatcher" type="xsd:string" />
    <xsd:attribute name="forecastDateMatcher" type="xsd:string" />
    <xsd:attribute name="forecastOffsetMatcher" type="xsd:string" />
      </xsd:complexType>
     </xsd:element>
    </xsd:sequence>
    
(16) <xsd:attribute name="type" type="AggregationType" use="required"/>
(17) <xsd:attribute name="dimName" type="xsd:token" />
(18) <xsd:attribute name="recheckEvery" type="xsd:string" />
(19) <xsd:attribute name="timeUnitsChange" type="xsd:boolean"/>


      <!-- fmrc only  -->
(20) <xsd:attribute name="fmrcDefinition" type="xsd:string" />

</xsd:complexType>
</xsd:element>
~~~

1. Elements inside the <aggregation> get applied to each dataset in the aggregation, before it is aggregated. Elements outside the <aggregation> get applied to the aggregated dataset.
2. For joinNew aggregation types, each variable to be aggregated must be explicitly listed in a variableAgg element.
3. Optionally specify global attributes to promote to a variable (outer aggregations only) with a promoteGlobalAttribute element.
4. Specify which variables should be cached (outer aggregation only) with a cacheVariable element.
5. Nested netcdf datasets can be explicitly listed.
6. Nested netcdf datasets can be implictly specified with a scan element.
7. The scan directory location.
8. If you specify a regExp, only files with whose full pathnames match the regular expression will be included.
9. If you specify a suffix, only files with that ending will be included. A regExp attribute will override, that is, you cant specify both.
10. You can optionally specify if the scan should descend into subdirectories (default true).
11. If olderThan attribute is present, only files whose last modified date are older than this amount of time will be included. This is a way to exclude files that are still being written. This must be a udunit time such as "5 min" or "1 hour".
12. A dateFormatMark is used on joinNew types to create date coordinate values out of the filename. It consists of a section of text, a '#' marking character, then a java.text.SimpleDateFormat string. The number of characters before the # is skipped in the filename, then the next part of the filename must match the SimpleDateFormat string. You can ignore trailing text. For example:

        Filename: SUPER-NATIONAL_1km_SFC-T_20051206_2300.gini 
 DateFormatMark: SUPER-NATIONAL_1km_SFC-T_#yyyyMMdd_HHmm
 
<b>_Note that the dateFormatMark works on the name of the file, without the directories!!_</b>

A <b>_dateFormatMark_</b> can be used on a <b>_joinExisting_</b> type only if there is a single time in each file of the aggregation, in which case the coordinate values of the time can be created from the filename, instead of having to open each file and read it.

13. You can optionally specify that the files should be opened in enhanced mode (default is NetcdfDataset.EnhanceMode.None). Generally you should do this if the ncml needs to operate on the datset after the CoordSysBuilder has augmented it. Otherwise, you should not enhance.
14. A specialized scanFmrc element can be used for a forecastModelRunSingleCollection aggregation, where forecast model run data is stored in multiple files, with one forecast time per file.
15. For scanFmrc, the run date and the forecast date is extracted from the file pathname using a runDateMatcher and either a forecastDateMatcher or a forecastOffsetMatcher attribute. All of these require matching a specific string in the file's pathname and then matching a date or hour offset immediately before or after the match. The match is specified by placing it between '#' marking characters. The runDateMatcher and forecastDateMatcher has a java.text.SimpleDateFormat string before or after the match, while a forecastOffsetMatcher counts the number of 'H' characters, and extracts an hour offset from the run date. For example:
     
             Filename:  gfs_3_20060706_0300_006.grb 
       runDateMatcher: #gfs_3_#yyyyMMdd_HH
       
forecastOffsetMatcher:                     HHH#.grb#
will extract the run date 2006-07-06T03:00:00Z, and the forecast offset "6 hours".

16. You must specify an aggregation type.
17. For all types except joinUnion, you must specify the dimension name to join.
17. The recheckEvery attribute only applies when using a scan element. When you are using scan elements on a set of files that may change, and you are using caching, set recheckEvery to a valid udunit time value, like "10 min", "1 hour", "30 days", etc. Whenever the dataset is reacquired from the cache, the directories will be rescanned if recheckEvery amount of time has elapsed since the last time it was scanned. If you do not specify a recheckEvery attribute, the collection will be assumed to be non-changing.

 The recheckEvery attribute specifies how out-of-date you are willing to allow your changing datasets to be, not how often the data changes. If you want updates to be seen within 5 min, use 5 minutes here, regardless of the frequency of updating.

19. Only for joinExisting and forecastModelRunCollection types: if timeUnitsChange is set to true, the units of the joined coordinate variable may change, so examine them and do any appropriate conversion so that the aggregated coordinate values have consistent units.
20. Experimental, do not use.
 
#### AggregationType Type

~~~
 <!-- type of aggregation -->
 <xsd:simpleType name="AggregationType">
  <xsd:restriction base="xsd:string">
   <xsd:enumeration value="forecastModelRunCollection"/>
   <xsd:enumeration value="forecastModelRunSingleCollection"/>
   <xsd:enumeration value="joinExisting"/>
   <xsd:enumeration value="joinNew"/>
   <xsd:enumeration value="tiled"/>
   <xsd:enumeration value="union"/>
  </xsd:restriction>
 </xsd:simpleType>
~~~

The allowable aggregation types. The _forecastModelRunCollection_, _forecastModelRunSingleCollection_, _joinExisting_ and _joinNew_ aggregation types are called <b>_outer aggregations_</b> because they work on the outer (first) dimension.

#### promoteGlobalAttribute Element
~~~
  <!-- promote global attribute to variable -->
  <xsd:element name="promoteGlobalAttribute">
   <xsd:complexType>
(1)  <xsd:attribute name="name" type="xsd:token" use="required"/>
(2)  <xsd:attribute name="orgName" type="xsd:string"/>
   </xsd:complexType>
  </xsd:element>
~~~
  
1. The name of the variable to be created.
2. If the global attribute name is different from the variable name, specify it here.

This can be used on <b>_joinNew_</b>, <b>_joinExisting_</b>, and <b>_forecastModelRunCollection_</b>, aka the <b>_outer dimension aggregations_</b>. A new variable will be added using the aggregation dimension and its type will be taken from the attribute type. If theres more than one slice in the file (eg in a <b>_joinExisting_</b>), the attribute value will be repeated for each coordinate in that file.

#### cacheVariable Element

~~~
  <!-- cache a Variable for efficiency -->
  <xsd:element name="cacheVariable">
   <xsd:complexType>
    <xsd:attribute name="name" type="xsd:token" use="required"/>
   </xsd:complexType>
  </xsd:element>
~~~

Not ready to be used in a general way yet.

### Notes

* <font color="blue">Any attributes of type xsd:token, have trailing and ending spaces ignored, and all other spaces or new lines are collapsed to one single space.</font>
* <font color="blue">If any attribute or content has the characters ">", "<", """, or "&", they must be encoded using standard XML escape sequences &gt;, &lt;, &quot;, &amp; respectively.</font>

### The java.text.SimpleDateFormat

NcML uses the java.text.SimpleDateFormat class for date and time formatting.  Refer to the <a href="https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html">current javadoc</a> for details.

#### Examples

The following examples show how date and time patterns are interpreted in the U.S. locale. The given date and time are 2001-07-04 12:08:56 local time in the U.S. Pacific Time time zone.

|---
| Date and Time Pattern | Result
| :- | :-
| "yyyy.MM.dd G 'at' HH:mm:ss z"	| 2001.07.04 AD at 12:08:56 PDT
| "EEE, MMM d, ''yy"	| Wed, Jul 4, '01
| "h:mm a"	| 12:08 PM
| "hh 'o''clock' a, zzzz"	| 12 o'clock PM, Pacific Daylight Time
| "K:mm a, z"	| 0:08 PM, PDT
| "yyyyy.MMMMM.dd GGG hh:mm aaa"	| 02001.July.04 AD 12:08 PM
| "EEE, d MMM yyyy HH:mm:ss Z"	| Wed, 4 Jul 2001 12:08:56 -0700
| "yyMMddHHmmssZ"	| 010704120856-0700

### Regular Expressions : java.util.regexp

Regular expressions are used in </b>_scan_</b> elements to match filenames to be included in the aggregation. Note that the regexp pattern is matched against the <b>_full pathname_</b> of the file (_/dir/file.nc, not file.nc_).

When placing regular expressions in NcML, you dont need to use \\ for \, eg use

~~~
  <scan location="test" regExp=".*/AG.*\.nc$" />
~~~

instead of 

~~~
  <scan location="test" regExp=".*/AG.*\\.nc$" />
~~~

This may be confusing if you are used to having to double escape in Java Strings:

  Pattern.compile(".*/AG.*\\.nc$")
  
#### Examples

|---
| Pattern | File Pathname | match?
|:-|:-|:-
| .*/AG.*\.nc$ | C:/data/test/AG2006001_2006003_ssta.nc | true                
| .*/AG.*\.nc$ | C:/data/test/AG2006001_2006003_ssta.ncd | false
| .*/AG.*\.nc$ | C:/data/test/PS2006001_2006003_ssta.ncs | false

 	 	 
### Resources:

<a href="http://java.sun.com/docs/books/tutorial/essential/regex/">Java Tutorial on Regular Expressions</a>

<a href="http://en.wikipedia.org/wiki/Regular_expression">Regular Expressions wikipedia</a>

<a href="http://www.regular-expressions.info/">Regular-Expressions.info</a>
