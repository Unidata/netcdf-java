---
title: ArrayStructures
last_updated: 2018-10-22
sidebar: netcdfJavaTutorial_sidebar
toc: false
permalink: arraystructures_ref.html
---
### Array Structures

In the netCDF-3 data model, all data is represented by multidimensional arrays, stored on disk or in memory in one-dimensional arrays, linearized in row-order. The netCDF-4/CDM data model adds a data type called a <b>_Structure_</b>, akin to a struct in C or a row in a relational database, which contains constituent data variables that are stored together on disk. A Structure can be multidimensional, like any other data type, and in the CDM the data is represented as <b>_ArrayStructure_</b> objects, which are collections of <b>_StructureData_</b> objects, where a StructureData contains the data for a single instance of the Structure.

Because Structures can be nested inside of each other, the data representation and proccesing of ArrayStructures can be complex. This document explains the API, it's use, and how to create ArrayStructure objects.

### ArrayStructure API

The public API of ArrayStructure objects is in the abstract class <b>_ucar.ma2.ArrayStructure_</b>:

~~~
public abstract class ucar.ma2.ArrayStructure extends ucar.ma2.Array {
  List<StructureMembers.Member> getMembers();
  List<String> getStructureMemberNames();
  StructureMembers.Member findMember(String memberName);
~~~

One can find an individual <b>_StructureMembers.Member_</b> from its name, get the list of member names, or get the list of StructureMembers.Member objects. These describe the individual data components of the ArrayStructure:

~~~
  public class ucar.ma2.StructureMembers.Member {
    String getName();
    DataType getDataType();
    int[] getShape();
    boolean isScalar();

    int getSize();
    int getTotalSize();

    String getUnitsString();
    String getDescription();
  }
~~~

Note: You may notice that you can also get a <b>_StructureMembers_</b> object by calling _Structure.makeStructureMembers()_. You should in general never do that, and use _Structure.getVariables()_ to get the list of Variables in a Structure. Always get <b>_StructureMembers_</b> directly from the ArrayStructure.

#### Data Access

The most general method for accessing the data in a StructureArray is to iterate over the collection of StructureData with a StructureDataIterator:

~~~
    public ucar.ma2.StructureDataIterator getStructureDataIterator();
~~~

Another general way to access data in an ArrayStructure is to use

~~~
    StructureData getStructureData(Index index).
~~~

One can also obtain the StructureData using the record number of the StructureData. For the common case of one-dimensional Structures, the record number is the same as the index:

~~~
    public ucar.ma2.StructureData getStructureData(int recno);
~~~~

Make sure that in all these calls, the StructureMembers.Member object is obtained directly from the ArrayStructure, eg using <b>_findMember_</b>(String memberName).
Often you are not actually interested in the StructureData object, only in the member data. The following method may avoid the overhead of creating the StructureData object:

~~~
   public ucar.ma2.Array getArray(int recno, StructureMembers.Member m);
~~~
 
An Array handles data of all data types and ranks in a uniform way. However, you may want to avoid the overhead of of creating an Array object when you simply want to extract the data as a Java primitive or primitive array, and you are willing to handle the data type and rank explicitly: The following methods are likely (but not guarenteed) to be more efficient, by avoiding unneeded Object creation. These methods must match the member type exactly:

~~~
    public double getScalarDouble(int recno, StructureMembers.Member m);
    public double[] getJavaArrayDouble(int recno, StructureMembers.Member m);

    public float getScalarFloat(int recno, StructureMembers.Member m);
    public float[] getJavaArrayFloat(int recno, StructureMembers.Member m);

    public byte getScalarByte(int recno, StructureMembers.Member m));
    public byte[] getJavaArrayByte(int recno, StructureMembers.Member m);

    public short getScalarShort(int recno, StructureMembers.Member m);
    public short[] getJavaArrayShort(int recno, StructureMembers.Member m);

    public int getScalarInt(int recno, StructureMembers.Member m);
    public int[] getJavaArrayInt(int recno, StructureMembers.Member m);

    public long getScalarLong(int recno, StructureMembers.Member m);
    public long[] getJavaArrayLong(int recno, StructureMembers.Member m);

    public char getScalarChar(int recno, StructureMembers.Member m);
    public char[] getJavaArrayChar(int recno, StructureMembers.Member m);

    public String getScalarString(int recno, StructureMembers.Member m);
    public String[] getJavaArrayString(int recno, StructureMembers.Member m);

    public ucar.ma2.StructureData getScalarStructure(int recno, StructureMembers.Member m);
    public ucar.ma2.ArrayStructure getArrayStructure(int recno, StructureMembers.Member m);

    public ucar.ma2.ArraySequence getArraySequence(int recno, StructureMembers.Member m);
~~~

This method can be used on any type. It converts primitives to their corresponding object types (eg float to Float):

~~~
    public java.lang.Object getScalarObject(int recno, StructureMembers.Member m);
~~~

These two methods convert any numeric type to a float or double:

~~~
    public float convertScalarFloat(int recno, StructureMembers.Member m);
    public double convertScalarDouble(int recno, StructureMembers.Member m);
~~~
#### Unsupported methods

Because ArrayStructure is a subclass of Array, there are getter/setter methods for each primitive type. These are not usable, since primitive types cannot be cast to StructureData:

~~~
    public double getDouble(ucar.ma2.Index);
    public void setDouble(ucar.ma2.Index, double);
    public float getFloat(ucar.ma2.Index);
    public void setFloat(ucar.ma2.Index, float);
    public long getLong(ucar.ma2.Index);
    public void setLong(ucar.ma2.Index, long);
    public int getInt(ucar.ma2.Index);
    public void setInt(ucar.ma2.Index, int);
    public short getShort(ucar.ma2.Index);
    public void setShort(ucar.ma2.Index, short);
    public byte getByte(ucar.ma2.Index);
    public void setByte(ucar.ma2.Index, byte);
    public boolean getBoolean(ucar.ma2.Index);
    public void setBoolean(ucar.ma2.Index, boolean);
    public char getChar(ucar.ma2.Index);
    public void setChar(ucar.ma2.Index, char);
~~~

For certain technical reasons these methods also cannot be used:

~~~
    public ucar.ma2.Array createView(ucar.ma2.Index);
    public ucar.ma2.Array copy();
~~~

These methods can be used, but the Object must be of type StructureData:

~~~
    public java.lang.Object getObject(ucar.ma2.Index);
    public void setObject(ucar.ma2.Index, java.lang.Object);
~~~

### StructureData API

StructureData encapsulates all the data in a single record. It is normally contained within an ArrayStructure, and its methods closely parellel the methods of its parent ArrayStructure.

~~~
public abstract class ucar.ma2.StructureData {
  List<StructureMembers.Member> getMembers();

  StructureMembers.Member findMember(String memberName);
  
~~~
In the following data access method, each method takes either a member name or a Member object. A common mistake is to assume that the Member object from the ArrayStructure is the same as the one from the StructureData objects that are contained in the ArrayStructure, which is not necessarily the case. Its slightly more efficient to use the Member object directly, as it avoids a hashMap lookup, but if using the Member directly, you must obtain it from the StructureData. Using the member name is always safe.

The most general ways to access data in a StructureData are:

~~~
    public ucar.ma2.Array getArray(String memberName);
    public ucar.ma2.Array getArray(StructureMembers.Member m);
~~~

The following method will handle a scalar object of any type, by converting primitives to their Object type (eg int to Integer):

~~~
    public java.lang.Object getScalarObject(String memberName);
    public java.lang.Object getScalarObject(StructureMembers.Member m);
~~~

The following routines may be able to avoid extra Object creation, and so are recommended when efficiency is paramount. These require that you know the data types of the member data:

~~~
    public double getScalarDouble(String memberName);
    public double getScalarDouble(StructureMembers.Member);
    public double[] getJavaArrayDouble(String memberName);
    public double[] getJavaArrayDouble(StructureMembers.Member);

    public float getScalarFloat(String memberName);
    public float getScalarFloat(StructureMembers.Member);
    public float[] getJavaArrayFloat(String memberName);
    public float[] getJavaArrayFloat(StructureMembers.Member);

    public byte getScalarByte(String memberName);
    public byte getScalarByte(StructureMembers.Member);
    public byte[] getJavaArrayByte(String memberName);
    public byte[] getJavaArrayByte(StructureMembers.Member);

    public int getScalarInt(String memberName);
    public int getScalarInt(StructureMembers.Member);
    public int[] getJavaArrayInt(String memberName);
    public int[] getJavaArrayInt(StructureMembers.Member);

    public short getScalarShort(String memberName);
    public short getScalarShort(StructureMembers.Member);
    public short[] getJavaArrayShort(String memberName);
    public short[] getJavaArrayShort(StructureMembers.Member);

    public long getScalarLong(String memberName);
    public long getScalarLong(StructureMembers.Member);
    public long[] getJavaArrayLong(String memberName);
    public long[] getJavaArrayLong(StructureMembers.Member);

    public char getScalarChar(String memberName);
    public char getScalarChar(StructureMembers.Member);
    public char[] getJavaArrayChar(String memberName);
    public char[] getJavaArrayChar(StructureMembers.Member);

    public String getScalarString(String memberName);
    public String getScalarString(StructureMembers.Member);
    public String[] getJavaArrayString(String memberName);
    public String[] getJavaArrayString(StructureMembers.Member);
~~~

For members that are themselves Structures, the equivalent is:

~~~
    public ucar.ma2.StructureData getScalarStructure(String memberName);
    public ucar.ma2.StructureData getScalarStructure(StructureMembers.Member);

    public ucar.ma2.ArrayStructure getArrayStructure(String memberName);
    public ucar.ma2.ArrayStructure getArrayStructure(StructureMembers.Member);

    public ucar.ma2.ArraySequence getArraySequence(String memberName); 
    public ucar.ma2.ArraySequence getArraySequence(StructureMembers.Member);
~~~
 
The following will return any compatible type as a double or float, but will have extra overhead when the types dont match:

~~~
    public float convertScalarFloat(String memberName);
    public float convertScalarFloat(StructureMembers.Member);
    public double convertScalarDouble(String memberName);
    public double convertScalarDouble(StructureMembers.Member);
~~~

### Creating ArrayStructures

IOSP writers need to create ArrayStructure objects for any Structure variables in their files.

ArrayStructure is an abstract class in which the only abstract method is:

~~~
  abstract protected StructureData makeStructureData( ArrayStructure as, int recno);
~~~

However, ArrayStructure has a number of default method implementations that may need to be overriden.

An ArrayStructure uses one of two strategies for StructureData implementations. It either uses a <b>_StructureDataW_</b>, in which each StructureData contains its own data, or it uses a <b>_StructureDataA_</b>, which defers data access back to the ArrayStructure itself.

This method puts the data in column store format

~~~
  public ucar.ma2.Array getMemberArray(ucar.ma2.StructureMembers.Member);
~~~

{% include image.html file="netcdf-java/reference/uml/ArrayStructure.png" alt="Array Structure UML" caption="Array Structure UML" %}

### ArrayStructureBB

ArrayStructureBB uses a <b>_java.nio.ByteBuffer_</b> for data storage and converts member data only on demand. The member data must be at constant offsets from the start of each record. This offset is stored into each StructureMembers.Member using <b>_StructureMembers.Member.setDataParam()_</b>.

Typically the data can be read from disk directly into a ByteBuffer, for example:

~~~
    // create the ArrayStructure
    StructureMembers members = s.makeStructureMembers();
    for (StructureMembers.Member m : members.getMembers()) {
      Variable v2 = s.findVariable(m.getName());
      N3header.Vinfo vinfo = (N3header.Vinfo) v2.getSPobject();
      m.setDataParam((int) (vinfo.begin - recStart)); // the offset from the start of the record
    }
    members.setStructureSize(recsize);  // the size of each record is constant

    // create the ArrayStructureBB
    ArrayStructureBB structureArray = new ArrayStructureBB(members, new int[]{recordRange.length()});
    byte[] result = structureArray.getByteBuffer().array();

    // read the data one record at a time into the ByteBuffer
    int count = 0;
    for (int recnum = recordRange.first(); recnum <= recordRange.last(); recnum += recordRange.stride()) {
      raf.seek(recStart + recnum * recsize); // where the record starts
      raf.readFully(result, count * recsize, recsize);
      count++;
    }
~~~

ArrayStructureBB calculates member offsets on demand. By default it assumes that each record is the same size. <b>_ucar.ma2.ArrayStructureBBpos_</b> relaxes this assumption by allowing you to pass in the starting positions in the ByteBuffer of each record.

Member offsets must be the same for each record. However, more complex objects can be stored as an index into a object heap list. For example, the object heap is used to store Strings, which are variable length arrays of UTF-16 charactors. The index of the String in the list is stored (as a 4-byte integer) in the ByteBuffer instead of the String. The String itself is added using <b>_ArrayStructureBB.addObjectToHeap()_</b>, as in the following code:

~~~
  int heapIndex = arrayStructureBB.addObjectToHeap(stringData);   // add object into the Heap
  arrayStructureBB.setInt(bbPos, heapIndex);                    // store the index
~~~

or

~~~
  arrayStructureBB.addObjectToHeap(recnum, member, stringData);   // add object for this recnum and member into the Heap
~~~

When storing data in an ArrayStructureBB, the heap must be used for both Strings and Sequences. Here is the Object type that must be used when adding to the heap in the _ArrayStructureBB.addObjectToHeap()_ call:

* scalar String: <b>_String_</b>
* array of Strings: <b>_String[]_</b>
* sequence: <b>_ArraySequence_</b>

#### ArrayStructureBB Nested Structures

You can accomodate arbitrary nesting of Structures by using a recursive method to set the offsets. The following is a static convenience method in ArrayStructureBB:

~~~
  public int ArrayStructureBB.setOffsets(StructureMembers members) {
    int offset = 0;
    for (StructureMembers.Member m : members.getMembers()) {
      m.setDataParam(offset);
      offset += m.getSize();

      // set inner offsets
      if (m.getDataType() == DataType.STRUCTURE) 
        setOffsets(m.getStructureMembers());
    }
    return offset;
  }
~~~

This only works when the nested structures are all of the same, known length. For variable length nested Structures, use ArraySequence.

#### Member data overridding

NetcdfDataset may widen the type of a Variable when implementing scale/offset attributes. Typically this will cause a byte or short to become a float or double. A StructureDS will post-process the data it gets from the IOSP to implement this. When the IOSP returns an ArrayStructureBB, it is convenient to rewrite just the member data that needs to be widened. This can be done by calculating the new data and calling ArrayStructure.setMemberData(Array).

~~~
  public void setMemberArray(ucar.ma2.StructureMembers.Member, ucar.ma2.Array memberArray);
~~~
  
Requests for data will be satisfied from the memberArray instead of the ByteBuffer. In order to make this work, the methods in ArrayStructureBB typically check if the member data array exists, and if so defers to the superclass. For example:

~~~
  public double getScalarDouble(int recnum, StructureMembers.Member m) {
    if (m.getDataArray() != null) return super.getScalarDouble(recnum, m);
    ...
  }
~~~
 
#### ArrayStructureMA

ArrayStructureMA stores its member data in _column-store form_, where each member's data is stored in a single Array across all rows. The member Arrays are stored with <b>_StructureMembers.Member.setDataArray()_</b>, for example:

~~~
    StructureMembers members = structure.makeStructureMembers();
    ArrayStructureMA ama = new ArrayStructureMA(members, shape);
    ArrayInt.D1 timeArray = new ArrayInt.D1(shape[0]);
    ArrayObject.D1 nameArray = new ArrayObject.D1(String.class, shape[0]);

    for (StructureMembers.Member m : members.getMembers()) {
      if (m.getName().equals("time"))
        m.setDataArray(timeArray);
      else
        m.setDataArray(nameArray);
    }
~~~

#### ArrayStructureMA Nested Structures
A nested Structure inside of an ArrayStructureMA would be represented by another ArrayStructureMA, when the nested structures are all of the same, known length. This inner ArrayStructureMA would represent all of the inner Structures across all rows of the outer Structure.

#### ArrayStructureW

ArrayStructureW defers data reading to the StructureData objects. To use it, one constructs all of the StructureData objects and passes them to the ArrayStructureW, for example:

~~~
  public ArrayStructureW(StructureMembers members, int[] shape, StructureData[] sdata);
~~~

All of the work is in constructing the StructureData objects.
 
### ArraySequence

To create an empty sequence, one needs an empty StructureDataIterator, for example the following can be used:

~~~
class EmptyStructureDataIterator implements StructureDataIterator {

   @Override
   public boolean hasNext() throws IOException {
     return false;
   }

   @Override
   public StructureData next() throws IOException {
     return null;
   }

   @Override
   public void setBufferSize(int bytes) { }

   @Override
   public StructureDataIterator reset() {  }

   @Override
   public int getCurrentRecno() {
     return -1;
   }
 }
~~~

### Variable Length Member Data

A nested variable length field, for example:

~~~
netcdf Q:/cdmUnitTest/formats/netcdf4/vlen/cdm_sea_soundings.nc4 {
 dimensions:
   Sounding = 3;

 variables:
 
  Structure {
    int sounding_no;
    float temp(*);
  } soundings(Sounding=3);

}

~~~

Can be read like this:

~~~
 Variable v = ncfile.findVariable("soundings");
 ArrayStructure data = (ArrayStructure) v.read();       // read all of it
 StructureData sdata = data.getStructureData(index);    // pick out one
 String memberName = "temp";
 Array tempData = sdata.getArray(memberName);           // get the data for this member
 assert tempData instanceof ArrayFloat;                 // it will be a float array
 
 System.out.printf("the %d th record has %d elements for vlen member %s%n", index, tempData.getSize(), memberName);
~~~

Or like this:

~~~
 int count = 0;
 Structure s = (Structure) v;
 StructureDataIterator siter = s.getStructureIterator();
 while (siter.hasNext()) {
   StructureData sdata2 = siter.next();
   Array vdata2 = sdata2.getArray(memberName);
   System.out.printf("iter %d  has %d elements for vlen member %s%n", count++, vdata2.getSize(), memberName);
 }
 ~~~