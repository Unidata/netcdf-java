---
title: NcStream Grammar
last_updated: 2019-07-10
sidebar: netcdfJavaTutorial_sidebar 
permalink: ncstream_grammar.html
toc: false
---
## NcStream Grammar Version2 (DRAFT)

An <b>_ncstream_</b> is an ordered sequence of one or more messages:

~~~
   ncstream = MAGIC_START, {message}*, MAGIC_END
   message = headerMessage | dataMessage | errorMessage
   headerMessage = MAGIC_HEADER, vlenb, NcStreamProto.Header
   dataMessage = MAGIC_DATA, vlenb, NcStreamProto.Data, regData | vlenData | seqData | structData
   errorMessage = MAGIC_ERR, vlenb, NcStreamProto.Error

   regData = vlenb, (byte)*vlenb
   vlenData= vlenn, {vlenb, (byte)*vlenb}*vlenn
   seqData = {MAGIC_VDATA, vlenb, NcStreamProto.StructureData}*, MAGIC_VEND
   structData = vlenb, NcStreamProto.StructureData

   vlenb = variable length encoded positive integer == length of the following object in bytes
   vlenn = variable length encoded positive integer == number of objects that follow
   NcStreamProto.Header = Header message encoded by protobuf
   NcStreamProto.Data = Data message encoded by protobuf
   byte = actual bytes of data, encoding described by the NcStreamProto.Data message

primitives:

   MAGIC_START = 0x43, 0x44, 0x46, 0x53 
   MAGIC_HEADER= 0xad, 0xec, 0xce, 0xda 
   MAGIC_DATA =  0xab, 0xec, 0xce, 0xba 
   MAGIC_VDATA = 0xab, 0xef, 0xfe, 0xba 
   MAGIC_VEND  = 0xed, 0xef, 0xfe, 0xda 
   MAGIC_ERR   = 0xab, 0xad, 0xba, 0xda 
   MAGIC_END =   0xed, 0xed, 0xde, 0xde
 ~~~
 
The protobuf messages are defined by

* <b>_thredds\cdm\src\main\java\ucar\nc2\stream\ncStream.proto_</b>
* <b>_thredds\cdm\src\main\java\ucar\nc2\ft\point\remote\pointStream.proto_</b>

(these are files on Unidata's GitHub repository). These are compiled by the protobuf compiler into Java and C code that does the actual encoding/decoding from the stream.

#### Rules

* Messages are ordered, and the resulting dataset may depend on the order.
* A shared dimension must be defined in the same or an earlier header message than a variable that uses it.
* A variable must be defined first in a header message before it can be used in a data message.
* A variable may have 0, 1, or many data messages. These are logically combined, with later data messages taking precedent. Missing data values are taken from the variable's _FillValue attribute if it exists, else the default missing value for the dataType, following netCDF conventions.
* Primitive types are fixed length, following Java; StructureData has no padding.
* If StructureData member list is empty, then all members are present.

### Data encoding

There is just enough information in the stream to break the stream into messages and to know what kind of message it is. To interpret the data message correctly, one must have the definition of the variable.

~~~
message Data {
  required string varName = 1; // full escaped name. change to hash or index to save space ??
  required DataType dataType = 2;
  optional Section section = 3; // not required for Sequence
  optional bool bigend = 4 [default = true];
  optional uint32 version = 5 [default = 0];
  optional Compress compress = 6 [default = NONE];
  optional fixed32 crc32 = 7;
}
~~~

1. full name of variable (should this be index or hash in order to save space ?)
2. data type
3. section
4. stored in big or small end. reader makes right.
5. version
6. compress (deflate)
7. crc32 (not used yet)

<b>_Primitive types (byte, char, short, int, long, float, double)_<b/>: arrays of primitives are stored in row-major order. The endian-ness is specified in the NcStreamProto.Data message when needed.

* <b>_char_</b> is a legacy data type contains uninterpreted characters, one character per byte. Typically these contain 7-bit ASCII characters.
* <b>_byte, short, int, long_</b> may be interpreted as signed or unsigned. This is specified in the variable's header information.

<b>_Variable length types (String, Opaque)_</b>: First the number of objects is written, then each object, preceded by its length in bytes as a vlen. Strings are encoded as UTF-8 bytes. Opaque is just a bag of bytes.

<b>_Variable length arrays_</b>: First the number of objects is written, then each object, preceded by its length in bytes as a vlen.

<b>_Structure types (Structure, Sequence)_</b>: An array of StructureData. Can be encoded in row or col (?).

### Data Encoding

#### Vlen data example

~~~
int levels(ninst= 23, acqtime=100, *);
~~~

encoded as

1. 2300 as a vlen
2. then 2300 objects, for each:
   1. length in bytes
   2. nelems
   3. nelems integers
 
#### Compound Type

Should be able to pop this in and out of a ByteBuffer (java) or void * (C), then use pointer manipulation to decode on the fly. Maybe good candidate for encodeing with protobuf

1. n
2. n structs
3. nheap
4. nheap objects

in this case, you have to read everything. if buffer has no vlens or strings, could use fixed size offsets. otherwise record the offsets.

1. n
2. n structs
    1.  nheap
    2.  nheap objects
    
(each struct contains its own heap)

1. n
2. n lengths
3. n structs
    1. nheap
    2. nheap objects
    
(each struct contains its own heap)

this indicates maybe we should rewrite ArrayStructureBB to have seperate heaps for each struct.

#### Nested Vlen

A nested variable length field, goes on the heap

~~~
netcdf Q:/cdmUnitTest/formats/netcdf4/vlen/cdm_sea_soundings.nc4 {
 dimensions:
   Sounding = 3;

 variables:
 
  Structure {
    int sounding_no;
    float temp_vl(*);
  } fun_soundings(Sounding=3);
}
~~~

#### Notes and Questions
Should have a way to efficiently encode sparse data. Look at Bigtable/hBase.

Should we store ints using vlen?

Forces on the design:

* Allow data to be streamed.
* Allow compression
* Append only writing to disk
* Efficient encoding of variable length (ragged) arrays
* Efficient return of results from high level query.

#### Vlen Language

We already have Fortran 90 syntax, and * indicating a variable length dimension. Do we really want to support arbitrary vlen dimension ??

* array(outer, *)
* array(*, inner)
* array(outer, *, inner)

An obvious thing to do is to use java/C "array of arrays". rather than Fortran / netCDF rectangular arrays:

* array[outer][*]
* array[*][inner]
* array[outer][*][inner]

what does numPy do ??

java/C assumes in memory. Is this useful for very large, ie out of memory, data?

Nested Tables has taken approach that its better to use Structures rather than arrays, since there are usually multiple fields. Fortran programmers prefer arrays, but they are thinking of in memory.

What is the notation that allows a high level specification (eg SQL), that can be efficiently executed by a machine ?

Extending the array model to very large datasets may not be appropriate. Row vs column store.

What about a transform language on the netcdf4 / CDM data model, to allow efficient rewriting of data ? Then it also becomes an extraction language ??

