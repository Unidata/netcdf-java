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
* [`ArrayType`](#arraytype)

### RandomAccessFile

The class `ucar.unidata.io.RandomAccessFile` is a cover for `java.io.RandomAccessFile`, which it (usually) uses underneath. 

It additionally implements user-settable buffer sizes, files with both big and little endianness, reading multiple `Charsets`, and several other methods to improve the API.

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
    public final void readShort(short[] arr, int start, int n) throws IOException;

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
A `ucar.array.Array` is an abstract, immutable object which allows working with multidimensional arrays in a type and rank general way.  
It replaces the deprecated `ucar.ma2.array` package.    
It is implemented by `ArrayByte`, `ArrayChar`, `ArrayDouble`, `ArrayFloat`, `ArrayInteger`, `ArrayLong`, `ArrayShort`, and `ArrayString`.  
Some useful `Array` methods include:

~~~java
public abstract class Array<T> implements Iterable<T> {
    public abstract T get(int... index); // Get the element indicated by the list of multidimensional indices.
    public abstract T get(Index index); // Get the element indicated by Index    
    public T getScalar(); // Get the first element of the Array

    public ArrayType getArrayType(); // Get the datatype for this array
    public boolean isVlen();
    public Index getIndex(); // Get an Index that can be used instead of an int[]
    
    public int getRank();
    public int[] getShape();
    public Section getSection(); // Returns a list of Ranges, one for each dimenstion
    public long length(); // Get the total number of elements in the array. Excludes vlen dimensions.
}
~~~

The `ucar.array.Arrays` class contains an number of static function for creating and using `Array` objects:

~~~java
    public static <T> Array<T> factory(ArrayType dataType, int[] shape, Storage<T> storage);
    public static <T> Array<T> factory(ArrayType dataType, int[] shape, Object dataArray);
    public static <T> Array<T> factory(ArrayType dataType, int[] shape);
    public static <T> Array<T> factoryCopy(ArrayType dataType, int[] shape, List<Array<t>> dataArrays);
    public static <T> Object combine(ArrayType dataType, int[] shape, Object dataArray);
    public static <T> Array<T> factoryArrays(ArrayType dataType, int[] shape, List<Array<?>> dataArrays);

    public static <T> Array<T> flip(Array<t> org, int dim);
    public static Array transpose(<T> Array<T> org, int dim1, int dim2);
    public static Array <T> permute(<T> Array<T> org, int[]) dims;
    public static <T> Array<T> reshape(<T> Array<T> org, int[] shape);
    public static Array <T> reduce(<T> Array<T> org, );
    public static Array <T> reduce<T>(<T> Array<T> org, int dim);
    public static Array <T> section(<T> Array<T> org, Section section) throws InvalidRangeException;
    public static Array <T> slice(<T> Array<T> org, int dim, int value);

    public static long computerSize(int[] shape);
    public static int[] removeVlen(int[] shape);
    public static Array<Double> toDouble(Array<?> array);
    public static MinMax getMinMaxSkipMissingData(Array<? extends Number> a, @Nullable IsMissingEvaluator eval);
    public static Array<Double> makeArray(int[] shape, int npts, double start, double incr);
}
~~~
Typically an IOSP will create the underlying primitive Java array, then wrap it in an `Array` using `Arrays.factory`, for example:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/writingiosp/OtherClassesIospTutorial.java&makeArray %}
{% endcapture %}
{{ rmd | markdownify }}

A `ucar.array.Section` is an immutable container for a `List` of `Range` objects, replacing the deprecated `ucar.ma2.Section` package. 
A section can be used to read only wanted data, using `Arrays.section(Array<T> org, Section section)`.


Once you have an `Array`, you can iterate over each element using the built-in `iterator`:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/writingiosp/OtherClassesIospTutorial.java&arrayIterator %}
{% endcapture %}
{{ rmd | markdownify }}

You can also access `Array` values by index:

{% capture rmd %}
{% includecodeblock netcdf-java&docs/src/test/java/examples/writingiosp/OtherClassesIospTutorial.java&arrayIndices %}
{% endcapture %}
{{ rmd | markdownify }}

There is also `StructureDataArray`, but this is handled differently from the numeric and `String` types. 
See [StructureDataArrays](structureDataArrays_ref.html).

### ArrayType

The class `ucar.array.ArrayType` is a type-safe enumeration of data types for the CDM and include the following types:

~~~java
public enum ArrayType {
  BOOLEAN("boolean", 1, boolean.class, false), //
  BYTE("byte", 1, byte.class, false), //
  CHAR("char", 1, char.class, false), //
  SHORT("short", 2, short.class, false), //
  INT("int", 4, int.class, false), //
  LONG("long", 8, long.class, false), //
  FLOAT("float", 4, float.class, false), //
  DOUBLE("double", 8, double.class, false), //

  // object types
  SEQUENCE("Sequence", 4, Iterator.class, false), // 32-bit index
  STRING("String", 4, String.class, false), // 32-bit index
  STRUCTURE("Structure", 0, StructureData.class, false), // size unknown

  ENUM1("enum1", 1, byte.class, false), // byte
  ENUM2("enum2", 2, short.class, false), // short
  ENUM4("enum4", 4, int.class, false), // int

  OPAQUE("opaque", 1, ByteBuffer.class, false), // size unknown, byte blobs;
  OBJECT("object", 1, Object.class, false), // size unknown, use with ucar.ma2.Array

  UBYTE("ubyte", 1, byte.class, true), // unsigned byte
  USHORT("ushort", 2, short.class, true), // unsigned short
  UINT("uint", 4, int.class, true), // unsigned int
  ULONG("ulong", 8, long.class, true); // unsigned long
}
~~~