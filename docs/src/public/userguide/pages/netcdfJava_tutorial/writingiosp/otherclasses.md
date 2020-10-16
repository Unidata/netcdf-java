---
title: Writing an IOSP - Other Classes
last_updated: 2019-07-23
sidebar: netcdfJavaTutorial_sidebar
permalink: other_classes.html
toc: false
---

## Other classes needed when writing an IOSP
To implement an IOSP, you will likely need to be familiar with the following classes:
* [`RandomAccessFile`](#randomaccessfile)
* [`Array`](#array)
* [`DataType`](#datatype)

### RandomAccessFile

The class `ucar.unidata.io.RandomAccessFile` is a cover for `java.io.RandomAccessFile`, which it (usually) uses underneath. 
It additionally implements user-settable buffer sizes, files with both big and little endianness, and several other methods to improve the API.

There are subclasses of `RandomAccessFile` such as `HTTPRandomAccessFile` and `InMemoryRandomAccessFile`, which deal with remote HTTP files 
and memory resident files. Use of these subclasses is transparent to an IOSP.

A summary of the public methods that may useful to an IOSP:

~~~java
public class ucar.unidata.io.RandomAccessFile {
    public static final int BIG_ENDIAN;
    public static final int LITTLE_ENDIAN;

    // Constructors
    public RandomAccessFile(String location, String mode) throws IOException;
    public RandomAccessFile(String location, String mode, int buffer_size) throws IOException;

    public void close() throws IOException;
    public String getLocation();
    public void order(int endian); // set to BIG_ENDIAN or LITTLE_ENDIAN

    // file position
    public long getFilePointer() throws IOException;
    public long length() throws IOException;
    public void seek(long filePos) throws IOException;
    public int skipBytes(int nbytes) throws IOException;

    // read
    public int read() throws IOException;
    public int read(byte[] arr) throws IOException;
    public int read(byte[] arr, int start, int n) throws IOException;
    public byte readByte() throws IOException;

    public final double readDouble() throws IOException;
    public final void readDouble(double[] arr, int start, int n) throws IOException;

    public final float readFloat() throws IOException;
    public final void readFloat(float[] arr, int start, int n) throws IOException;

    public final int readInt() throws IOException;
    public final void readInt(int[] arr, int start, int n) throws IOException;

    public final long readLong() throws IOException;
    public final void readLong(long[]
    arr, int start, int n) throws IOException;

    public final short readShort() throws IOException;
    public final void readShort(short[]
    arr, int start, int n) throws IOException;

    // read unsigned, promote to int
    public final int readUnsignedShort() throws IOException;
    public final int readUnsignedByte() throws IOException;

    // read Strings
    public final String readLine() throws IOException;
    public final String readUTF() throws IOException;
    public String readString(int nbytes) throws IOException;

}
~~~

### Array
A `ucar.ma2.Array` is a way to work with multidimensional arrays in a type and rank general way.

~~~java
public abstract class Array {

    public static Array factory(ucar.ma2.DataType type, int[] shape);
    public static Array factory(java.lang.Class class, int[] shape);
    public static Array factory(java.lang.Class class, int[] shape, java.lang.Object jarray);
    public static Array factory(java.lang.Object jarray);

    public int getRank();
    public int[] getShape();
    public long getSize();
    public abstract java.lang.Class getElementType();
    public abstract java.lang.Object getStorage();

    public ucar.ma2.Index getIndex();
    public ucar.ma2.IndexIterator getIndexIterator();

    public static void arraycopy(Array, int, Array, int, int);
    public Array copy();
    public java.lang.Object get1DJavaArray(java.lang.Class);
    public java.lang.Object copyTo1DJavaArray();
    public java.lang.Object copyToNDJavaArray();

    public Array flip(int);
    public Array transpose(int, int);
    public Array permute(int[]);
    public Array reshape(int[]);
    public Array reduce();
    public Array reduce(int);
    public Array section(java.util.List<Range> section) throws InvalidRangeException;
    public Array sectionNoReduce(java.util.List<Range> section) throws InvalidRangeException;
    public Array slice(int, int);

    public abstract double getDouble(ucar.ma2.Index);
    public abstract void setDouble(ucar.ma2.Index, double);
    public abstract float getFloat(ucar.ma2.Index);
    public abstract void setFloat(ucar.ma2.Index, float);
    public abstract long getLong(ucar.ma2.Index);
    public abstract void setLong(ucar.ma2.Index, long);
    public abstract int getInt(ucar.ma2.Index);
    public abstract void setInt(ucar.ma2.Index, int);
    public abstract short getShort(ucar.ma2.Index);
    public abstract void setShort(ucar.ma2.Index, short);
    public abstract byte getByte(ucar.ma2.Index);
    public abstract void setByte(ucar.ma2.Index, byte);
    public abstract char getChar(ucar.ma2.Index);
    public abstract void setChar(ucar.ma2.Index, char);
    public abstract boolean getBoolean(ucar.ma2.Index);
    public abstract void setBoolean(ucar.ma2.Index, boolean);
    public abstract java.lang.Object getObject(ucar.ma2.Index);
    public abstract void setObject(ucar.ma2.Index, java.lang.Object);
}
~~~
Typically an IOSP will create the underlying primitive Java array, then wrap it in an `Array` using `Array.factory`, for example:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/writingiosp/OtherClassesIospTutorial.java&makeArray %}
{% endcapture %}
{{ rmd | markdownify }}

A `Section` is a container for a `List` of `Range` objects:

~~~java
 public class ucar.ma2.Section {
   public List<Range> getRanges();
   public int[] getOrigin();
   public int[] getShape();
   public int[] getStride();
   ...
 }
~~~

When you do end up working with an `Array`, you will get an `Index` or `IndexIterator` from the `Array` to access individual elements of 
the `Array`. An `IndexIterator` iterates over each element of the `Array` in canonical order.

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/writingiosp/OtherClassesIospTutorial.java&arrayIndexIterator %}
{% endcapture %}
{{ rmd | markdownify }}

Or, using an `Index`:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/writingiosp/OtherClassesIospTutorial.java&arrayIndex %}
{% endcapture %}
{{ rmd | markdownify }}

If you know the rank and type of the `Array`, it is both convenient and more efficient to use the rank and type specific subclasses:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/writingiosp/OtherClassesIospTutorial.java&arrayRankAndType %}
{% endcapture %}
{{ rmd | markdownify }}

The type specific Arrays are:  
`ArrayBoolean`, `ArrayByte`, `ArrayChar`, `ArrayDouble`, `ArrayFloat`, `ArrayInt`, `ArrayLong`, `ArrayObject` and `ArrayShort`.  
`ArrayObject` is used for the `String` `DataType`.

Each of these have rank specific subtypes rank 0 through rank 7, so for example:  
`ArrayDouble.D0`, `ArrayDouble.D1`, `ArrayDouble.D2`, `ArrayDouble.D3`, `ArrayDouble.D4`, `ArrayDouble.D5`, `ArrayDouble.D6`, `ArrayDouble.D7`.

There is also `ArrayStructure`, but this is handled differently from the numeric and `String` types. 
See [ArrayStructures](arraystructures_ref.html).

### DataType

The class `ucar.ma2.DataType` is a type-safe enumeration of data types for the CDM and include the following types:

~~~java
public class ucar.ma2.DataType extends java.lang.Object{
    public static final ucar.ma2.DataType BOOLEAN;
    public static final ucar.ma2.DataType BYTE;
    public static final ucar.ma2.DataType CHAR;
    public static final ucar.ma2.DataType SHORT;
    public static final ucar.ma2.DataType INT;
    public static final ucar.ma2.DataType LONG;
    public static final ucar.ma2.DataType FLOAT;
    public static final ucar.ma2.DataType DOUBLE;

    public static final ucar.ma2.DataType SEQUENCE;
    public static final ucar.ma2.DataType STRING;
    public static final ucar.ma2.DataType STRUCTURE;

    public static final ucar.ma2.DataType ENUM1; // byte
    public static final ucar.ma2.DataType ENUM2; // short
    public static final ucar.ma2.DataType ENUM3; // int

    public static final ucar.ma2.DataType OPAQUE; // byte blobs
    public static final ucar.ma2.DataType OBJECT;

    public static final ucar.ma2.DataType UBYTE;
    public static final ucar.ma2.DataType USHORT;
    public static final ucar.ma2.DataType UINT;
    public static final ucar.ma2.DataType ULONG;
}
~~~