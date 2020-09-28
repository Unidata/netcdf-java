/*
 * Copyright (c) 1998-2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.util.Iterator;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.DataType;

/**
 * Array of variable length primitive arrays of T, eg double[length][];
 * Cast resulting Object, eg to double[].
 * Find out type from getPrimitiveArrayType() (not getDataType, which is VLEN).
 * This is mutable, to assist users in constructing. See set(index, value).
 */
public class ArrayVlen<T> extends Array<Object> {

  /**
   * Creates a Vlen of type dataType, and the appropriate primitive array.
   * The shape of the resulting array has vlen dimension removed, if present.
   */
  public static <T> ArrayVlen<T> factory(DataType dataType, int[] shape) {
    // find leftmost vlen, doesnt have to exist.
    // TODO this implies that vlen doesnt have to be rightmost dimension. For now, we flatten into 1D.
    int prefixrank = shape.length;
    for (int i = 0; i < shape.length; i++) {
      if (shape[i] < 0) {
        prefixrank = i;
        break;
      }
    }
    int[] newshape = new int[prefixrank];
    System.arraycopy(shape, 0, newshape, 0, prefixrank);

    return new ArrayVlen<>(dataType, newshape);
  }

  /** Creates a Vlen of type dataType, and the appropriate primitive array. */
  public static <T> ArrayVlen<T> factory(DataType dataType, int[] shape, Object storage) {
    int prefixrank = shape.length;
    for (int i = 0; i < shape.length; i++) {
      if (shape[i] < 0) {
        prefixrank = i;
        break;
      }
    }
    int[] newshape = new int[prefixrank];
    System.arraycopy(shape, 0, newshape, 0, prefixrank);

    return new ArrayVlen<>(dataType, newshape, storage);
  }

  public static StorageMutable<Object> createStorage(DataType dataType, int length, Object dataArray) {
    if (dataArray == null) {
      dataArray = createVlenArray(dataType, length);
    }
    Object result; // LOOK cant figure out correct generics syntax
    switch (dataType) {
      case BOOLEAN:
      case BYTE:
      case ENUM1:
      case OPAQUE:
      case UBYTE:
        result = new StorageVByte((byte[][]) dataArray);
        break;
      case CHAR:
        result = new StorageVChar((char[][]) dataArray);
        break;
      case DOUBLE:
        result = new StorageVDouble((double[][]) dataArray);
        break;
      case FLOAT:
        result = new StorageVFloat((float[][]) dataArray);
        break;
      case INT:
      case ENUM4:
      case UINT:
        result = new StorageVInt((int[][]) dataArray);
        break;
      case LONG:
      case ULONG:
        result = new StorageVLong((long[][]) dataArray);
        break;
      case SHORT:
      case ENUM2:
      case USHORT:
        result = new StorageVShort((short[][]) dataArray);
        break;
      case STRING:
        result = new StorageVString((String[][]) dataArray);
        break;
      default:
        throw new RuntimeException("Unimplemented DataType " + dataType);
    }
    return (StorageMutable<Object>) result;
  }

  public static Object createVlenArray(DataType dataType, int length) {
    switch (dataType) {
      case BOOLEAN:
      case BYTE:
      case ENUM1:
      case OPAQUE:
      case UBYTE:
        return new byte[length][];
      case CHAR:
        return new char[length][];
      case DOUBLE:
        return new double[length][];
      case FLOAT:
        return new float[length][];
      case INT:
      case ENUM4:
      case UINT:
        return new int[length][];
      case LONG:
      case ULONG:
        return new long[length][];
      case SHORT:
      case ENUM2:
      case USHORT:
        return new short[length][];
      case STRING:
        return new String[length][];
      default:
        throw new RuntimeException("Unimplemented DataType " + dataType);
    }
  }

  /////////////////////////////////////////////////////////////////////////
  private final StorageMutable<Object> storage;
  private final DataType primitiveArrayType;

  /** Create an empty Vlen of type primitiveArrayType and the given shape. */
  private ArrayVlen(DataType primitiveArrayType, int[] shape) {
    super(DataType.VLEN, shape);
    this.storage = createStorage(primitiveArrayType, (int) IndexFn.computeSize(shape), null);
    this.primitiveArrayType = primitiveArrayType;
  }

  /** Create an empty Vlen of type primitiveArrayType and data array T[][]. */
  private ArrayVlen(DataType primitiveArrayType, int[] shape, Object dataArray) {
    super(DataType.VLEN, shape);
    this.storage = createStorage(primitiveArrayType, (int) IndexFn.computeSize(shape), dataArray);
    this.primitiveArrayType = primitiveArrayType;
  }

  /** Create an Array of type Array<T> and the given indexFn and storage. */
  private ArrayVlen(DataType primitiveArrayType, IndexFn indexFn, StorageMutable<Object> storage) {
    super(DataType.VLEN, indexFn);
    Preconditions.checkArgument(indexFn.length() <= storage.getLength());
    this.storage = storage;
    this.primitiveArrayType = primitiveArrayType;
  }

  @Override
  public Iterator<Object> fastIterator() {
    return storage.iterator();
  }

  @Override
  public Iterator<Object> iterator() {
    return indexFn.isCanonicalOrder() ? fastIterator() : new CanonicalIterator();
  }

  @Override
  public Object get(int... index) {
    return storage.get(indexFn.get(index));
  }

  @Override
  public Object get(Index index) {
    return get(index.getCurrentIndex());
  }

  /** The resulting object will be a primitive array of this type, eg double[], or any length. */
  public DataType getPrimitiveArrayType() {
    return this.primitiveArrayType;
  }

  /**
   * Set the ith value
   * 
   * @param index 1d index
   * @param value must be primitive array of T, eg double[] of any length.
   */
  public void set(int index, Object value) {
    storage.set(index, value);
  }

  @Override
  void arraycopy(int srcPos, Object dest, int destPos, long length) {
    if (indexFn.isCanonicalOrder()) {
      storage.arraycopy(srcPos, dest, destPos, length);
    } else {
      Object[] ddest = (Object[]) dest;
      int destIndex = destPos;
      Iterator<Integer> iter = indexFn.iterator(srcPos, length);
      while (iter.hasNext()) {
        ddest[destIndex++] = storage.get(iter.next());
      }
    }
  }

  @Override
  Storage<Object> storage() {
    return storage;
  }

  /** create new Array with given IndexFn and the same backing store */
  @Override
  protected ArrayVlen<T> createView(IndexFn indexFn) {
    return new ArrayVlen<>(this.dataType, indexFn, this.storage);
  }

  // used when the data is not in canonical order
  private class CanonicalIterator implements Iterator<Object> {
    private final Iterator<Integer> iter = indexFn.iterator();

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public Object next() {
      return storage.get(iter.next());
    }
  }

  // standard storage using ragged array byte[fixed][]
  @Immutable
  static class StorageVByte implements StorageMutable<byte[]> {
    private final byte[][] primitiveArray;

    StorageVByte(byte[][] primitiveArray) {
      this.primitiveArray = primitiveArray;
    }

    @Override
    public long getLength() {
      return primitiveArray.length;
    }

    @Override
    public byte[] get(long elem) {
      return primitiveArray[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(primitiveArray, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<byte[]> iterator() {
      return new StorageIter();
    }

    @Override
    public void set(int index, byte[] value) {
      primitiveArray[index] = value;
    }

    private final class StorageIter implements Iterator<byte[]> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < primitiveArray.length;
      }

      @Override
      public final byte[] next() {
        return primitiveArray[count++];
      }
    }
  }

  // standard storage using ragged array char[fixed][]
  @Immutable
  static class StorageVChar implements StorageMutable<char[]> {
    private final char[][] primitiveArray;

    StorageVChar(char[][] primitiveArray) {
      this.primitiveArray = primitiveArray;
    }

    @Override
    public long getLength() {
      return primitiveArray.length;
    }

    @Override
    public char[] get(long elem) {
      return primitiveArray[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(primitiveArray, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<char[]> iterator() {
      return new StorageIter();
    }

    @Override
    public void set(int index, char[] value) {
      primitiveArray[index] = value;
    }

    private final class StorageIter implements Iterator<char[]> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < primitiveArray.length;
      }

      @Override
      public final char[] next() {
        return primitiveArray[count++];
      }
    }
  }

  // standard storage using ragged array double[fixed][]
  @Immutable
  static class StorageVDouble implements StorageMutable<double[]> {
    private final double[][] primitiveArray;

    StorageVDouble(double[][] primitiveArray) {
      this.primitiveArray = primitiveArray;
    }

    @Override
    public long getLength() {
      return primitiveArray.length;
    }

    @Override
    public double[] get(long elem) {
      return primitiveArray[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(primitiveArray, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<double[]> iterator() {
      return new StorageIter();
    }

    @Override
    public void set(int index, double[] value) {
      primitiveArray[index] = value;
    }

    private final class StorageIter implements Iterator<double[]> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < primitiveArray.length;
      }

      @Override
      public final double[] next() {
        return primitiveArray[count++];
      }
    }
  }

  // standard storage using ragged array float[fixed][]
  @Immutable
  static class StorageVFloat implements StorageMutable<float[]> {
    private final float[][] primitiveArray;

    StorageVFloat(float[][] primitiveArray) {
      this.primitiveArray = primitiveArray;
    }

    @Override
    public long getLength() {
      return primitiveArray.length;
    }

    @Override
    public float[] get(long elem) {
      return primitiveArray[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(primitiveArray, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<float[]> iterator() {
      return new StorageIter();
    }

    @Override
    public void set(int index, float[] value) {
      primitiveArray[index] = value;
    }

    private final class StorageIter implements Iterator<float[]> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < primitiveArray.length;
      }

      @Override
      public final float[] next() {
        return primitiveArray[count++];
      }
    }
  }

  // standard storage using ragged array int[fixed][]
  @Immutable
  static class StorageVInt implements StorageMutable<int[]> {
    private final int[][] primitiveArray;

    StorageVInt(int[][] primitiveArray) {
      this.primitiveArray = primitiveArray;
    }

    @Override
    public long getLength() {
      return primitiveArray.length;
    }

    @Override
    public int[] get(long elem) {
      return primitiveArray[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(primitiveArray, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<int[]> iterator() {
      return new StorageIter();
    }

    @Override
    public void set(int index, int[] value) {
      primitiveArray[index] = value;
    }

    private final class StorageIter implements Iterator<int[]> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < primitiveArray.length;
      }

      @Override
      public final int[] next() {
        return primitiveArray[count++];
      }
    }
  }

  // standard storage using ragged array long[fixed][]
  @Immutable
  static class StorageVLong implements StorageMutable<long[]> {
    private final long[][] primitiveArray;

    StorageVLong(long[][] primitiveArray) {
      this.primitiveArray = primitiveArray;
    }

    @Override
    public long getLength() {
      return primitiveArray.length;
    }

    @Override
    public long[] get(long elem) {
      return primitiveArray[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(primitiveArray, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<long[]> iterator() {
      return new StorageIter();
    }

    @Override
    public void set(int index, long[] value) {
      primitiveArray[index] = value;
    }

    private final class StorageIter implements Iterator<long[]> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < primitiveArray.length;
      }

      @Override
      public final long[] next() {
        return primitiveArray[count++];
      }
    }
  }

  // standard storage using ragged array short[fixed][]
  @Immutable
  static class StorageVShort implements StorageMutable<short[]> {
    private final short[][] primitiveArray;

    StorageVShort(short[][] primitiveArray) {
      this.primitiveArray = primitiveArray;
    }

    @Override
    public long getLength() {
      return primitiveArray.length;
    }

    @Override
    public short[] get(long elem) {
      return primitiveArray[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(primitiveArray, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<short[]> iterator() {
      return new StorageIter();
    }

    @Override
    public void set(int index, short[] value) {
      primitiveArray[index] = value;
    }

    private final class StorageIter implements Iterator<short[]> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < primitiveArray.length;
      }

      @Override
      public final short[] next() {
        return primitiveArray[count++];
      }
    }
  }

  // standard storage using ragged array String[fixed][]
  @Immutable
  static class StorageVString implements StorageMutable<String[]> {
    private final String[][] primitiveArray;

    StorageVString(String[][] primitiveArray) {
      this.primitiveArray = primitiveArray;
    }

    @Override
    public long getLength() {
      return primitiveArray.length;
    }

    @Override
    public String[] get(long elem) {
      return primitiveArray[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(primitiveArray, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<String[]> iterator() {
      return new StorageIter();
    }

    @Override
    public void set(int index, String[] value) {
      primitiveArray[index] = value;
    }

    private final class StorageIter implements Iterator<String[]> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < primitiveArray.length;
      }

      @Override
      public final String[] next() {
        return primitiveArray[count++];
      }
    }
  }

}
