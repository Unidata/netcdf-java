# ucar.array

## Array\<T>

An **Array\<T>** is a container for data of Java class T, implementing Iterable\<T>.

The possible values for the Java class T are 

* Byte
* Short
* Integer
* Long
* Float
* Double
* StructureData
* Array

Although Iterable\<T> requires the use of an Object class for T, in practice the data is stored using primitive arrays, 
eg double[], not Double[]. When an Array is [variable length](#variable-length-arrays), a 2D primitive array is used, 
eg double[fixed][].
    
An Array\<T> is associated with a CDM type by having an **ArrayType** enum:

1. Numeric Types
    * BOOLEAN("boolean", 1, Byte.class, false), // zero or non zero
    * BYTE("byte", 1, Byte.class, false), // signed byte
    * CHAR("char", 1, Byte.class, false), // usually ascii
    * SHORT("short", 2, Short.class, false), // signed short
    * INT("int", 4, Integer.class, false), // signed int
    * LONG("long", 8, Long.class, false), // signed long
    * FLOAT("float", 4, Float.class, false), // IEEE float
    * DOUBLE("double", 8, Double.class, false), // IEEE double
    * UBYTE("ubyte", 1, Byte.class, true), // unsigned byte
    * USHORT("ushort", 2, Short.class, true), // unsigned short
    * UINT("uint", 4, Integer.class, true), // unsigned int
    * ULONG("ulong", 8, Long.class, true); // unsigned long
    * ENUM1("enum1", 1, Byte.class, false), // signed byte
    * ENUM2("enum2", 2, Short.class, false), // signed short
    * ENUM4("enum4", 4, Integer.class, false), // signed int

2. Object Types
    * STRING("String", 4, String.class, false), // Java String
    * STRUCTURE("Sequence", 4, StructureData.class, false), // compact storage of heterogeneous fields
    * SEQUENCE("Structure", 4, StructureData.class, false), // Iterator\<StructureData>
    * OPAQUE("opaque", 4, Array.class, false), // Array\<Array\<Byte>>, an array of variable length byte arrays

Reading a Variable with ArrayType _atype_ returns an Array\<T> where T = _atype.primitiveType_.

## Numeric ArrayTypes

The numeric ArrayTypes are mostly straightforward, representing multidimensional arrays of their primitive types.
Their backing store is a Java primitive array, and so are limited to 2^31 - 1 elements. The amount of memory used
= _(number of elements) * atype.size_.

The CHAR type is a legacy type that should not be used if possible. Use a STRING instead, even when the data is
of constant length.

## Object ArrayTypes

The object ArrayTypes also represent multidimensional arrays of their primitive types, but the individual
elements use variable length amounts of memory. 

### String

A String Variable is a multidimensional array of Strings. Each String has a variable length of characters.

A Java String is encoded with UTF-16 in memory, but that is a detail one rarely needs to know. For external storage, 
encoding in UTF-8 is strongly recommended in order to deal with legacy datasets that dont know their encoding. Its up
to the calling program to correctly encode external data into Java Strings.

### Structure

A Structure Variable is a multidimensional array of Structures. Each Structure has the same metadata, but may use a variable 
length of bytes when stored in memory.

A Structure contains nested fields of arrays of any type, including arrays of nested Structures. One can consider Structures to be 
_row-oriented_ storage (in DBMS jargon), whereas numeric types, like the classic Netcdf3 data model, are _column-oriented_
storage. All the fixed length data in a Structure is stored contiguously in memory. Variable length data is stored with
32-bit indices to an external _data heap_. Generally, one does not need to know these internal storage details.

### Sequence

A Sequence is an array of Structures where the length of the array is not known until one reads it from the external storage.
Thus, you cannot index into a Sequence, you can only read the data sequentially with an Iterator.

### Opaque

An Opaque Variable is a multidimensional array of Opaque data, where each element is represented by a variable length array 
of uninterpreted bytes.

## Multidimensional Arrays

The multidimensional shape of an Array is described by its _int[rank] shape_ array (or by an **Index** object). 
We use Netcdf CDL to describe a multidimension array, eg _double Temperature(12, 64, 1128)_. 
We use zero-based Fortran 90 notation to describe a **Section** of the array, eg _Temperature(:, 29, 100:1127)_.

The length of the shape array is the number of _dimensions_, or **rank**. The data is stored in a one dimensional array, 
and the mapping between the 1D index and the multidimensional index is done by the **IndexFn**. 

### Variable length Arrays

An Array may be variable length, which means the length of the last dimension is not known until the data is read
from external storage, eg _float BuoySounding(9, 11, *)_. The data is stored in a 2D primitive array, in this
example _float[99][]_, an array of pointers of length 99 (the length of the fixed part), each pointing to a float[] array
of variiable length. 

This 2D primitive array is wrapped by an ArrayVlen\<T>, which extends Array\<Array\<T>>, and so implements
Iterable\<Array\<T>>. When you iterate over an ArrayVlen, or get an element from it, you get an Array\<T>, which is 
an array of type T, whose length may be different for each element of ArrayVlen.

## Create an array

````
int m = 18, n = 11, p = 60;
int[] shape = new int[] {m, n, p};
double[] data = new double[m * n * p];
int count = 0;
for (int i = 0; i < m; i++) {
   for (int j = 0; j < n; j++) {
     for (int k = 0; k < p; k++) {
       data[count++] = (i * 100 + j * 10 + k);
     }
   }
}
Array<Double> A = Arrays.factory(ArrayType.DOUBLE, shape, data);
````

Discover an Array's rank and shape: 
````
int rank = A.getRank();
int[] shape = A.getShape();
Section section = A.getSection();
````

## Reading from Array

Iterate over all the elements of an array in canonical (row-major) order:

````
double sum(Array<Double> A) {
  double sum = 0;
  for (double val : A) {
    sum += val;
  }
  return sum;
}
````
Array\<T> implements Iterable\<T>, so you can use streams:
````
double sum = StreamSupport.stream(A.spliterator(), false).mapToDouble(Double::doubleValue).sum();
double sum = Streams.stream(A).mapToDouble(Double::doubleValue).sum();
````
Where the second form uses Guava.

To access individual elements, you can use either a list of indices, 
one for each dimension (rank):

````
double value341 = A.get(3,4,1);
````
or an _**Index**_, which wraps the integer index array:

````
Index idx = A.getIndex();
double val341 = A.get(idx.set(3,4,1));
double val331 = A.get(idx.set1(3));
````

The Index keeps state, and has various convenience methods. In the above example,
idx.set1(3) sets index 1 to 3, keeping the values of index 0 and index 2 the same.
Indices are numbered from 0, from slower to faster varying.

You implicitly need to know the Array's rank and shape to keep your request in bounds, else
you will get an _IllegalArgumentException_ runtime Exception.

## The Arrays class

The **_Arrays_** class has static members to perform various operations on **_Array_**.
Generally these create logical views of an Array, ie there is no data copying. 
Note that Array is _**Immutable**_.

### Working with sections of an Array

Use a _**Section**_ of the same rank for the general case:

````
 Section section = new Section("2:4,:,:");
 Array<Double> arraySection = Arrays.section(A, section);
````

A _**slice**_ always sets one of the dimensions to a single value, 
and reduces the rank by one:

````
 Array<Double> arraySlice = Arrays.slice(A, 0, 22);
````

One can remove all dimensions of length one from an Array, or just a particular
dimension:

````
 Array<Double> allReduced = Arrays.reduce(A);
 Array<Double> dim0Reduced = Arrays.reduce(A, 0);
````

### Changing the logical ordering of Array elements

You can **permute** the order of the dimensions. For example to switch the dimensions
from row-major to column-major ordering:

````
 Array<Double> columnOrder = Arrays.permute(A, new int[2,1,0]);
````

**Flip** index 1 so that it runs from shape[index]-1 to 0:

````
 Array<Double> flipped1 = Arrays.permute(A, 1);
````

**Transpose** two dimensions of an Array:

````
 Array<Double> transArray = Arrays.transpose(A, 1, 2);
````

**Reshape** the Array, so that it has a different shape. 

````
 Array<Double> columnOrder = Arrays.permute(A, new int[mp, np, pp]);
````

The total elements must be
the same, eg if Array has shape [m,n, p], and the new shape is [mp, np, pp], then
````
m * n * p = mp * np * pp
````

