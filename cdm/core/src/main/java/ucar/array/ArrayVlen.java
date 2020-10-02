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
 * Cast resulting Array<T>, eg to Array<Double>.
 * Find out type from getPrimitiveArrayType() (not getDataType(), which is VLEN).
 * This is mutable, to assist users in constructing. See set(index, value).
 */
public final class ArrayVlen<T> extends Array<Array<T>> {

  /**
   * Creates a Vlen of type dataType, and the given shape.
   * The shape of the resulting array has vlen dimension removed, if present.
   */
  public static <T> ArrayVlen<T> factory(DataType dataType, int[] shape) {
    return new ArrayVlen<>(dataType, Arrays.removeVlen(shape));
  }

  /**
   * Creates a Vlen of type dataType, and the given shape and primitive array like double[][].
   * The shape of the resulting array has vlen dimension removed, if present.
   */
  public static <T> ArrayVlen<T> factory(DataType dataType, int[] shape, Object storage) {
    return new ArrayVlen<>(dataType, Arrays.removeVlen(shape), storage);
  }

  /** Creates storage for a Vlen of type dataType, and the given length and primitive array like double[][]. */
  public static <T> StorageMutable<Array<T>> createStorage(DataType dataType, int length, Object dataArray) {
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
        result = new StorageVByte(dataType, (byte[][]) dataArray);
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
        result = new StorageVInt(dataType, (int[][]) dataArray);
        break;
      case LONG:
      case ULONG:
        result = new StorageVLong(dataType, (long[][]) dataArray);
        break;
      case SHORT:
      case ENUM2:
      case USHORT:
        result = new StorageVShort(dataType, (short[][]) dataArray);
        break;
      case STRING:
        result = new StorageVString((String[][]) dataArray);
        break;
      default:
        throw new RuntimeException("Unimplemented DataType " + dataType);
    }
    return (StorageMutable<Array<T>>) result;
  }

  /** Creates primitive array like double[length][] for a Vlen of type dataType, and the given length. */
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
  private final StorageMutable<Array<T>> storage;
  private final DataType primitiveArrayType;

  /** Create an empty Vlen of type primitiveArrayType and the given shape. */
  private ArrayVlen(DataType primitiveArrayType, int[] shape) {
    super(DataType.VLEN, shape);
    this.storage = createStorage(primitiveArrayType, (int) Arrays.computeSize(shape), null);
    this.primitiveArrayType = primitiveArrayType;
  }

  /** Create an empty Vlen of type primitiveArrayType and data array T[][]. */
  private ArrayVlen(DataType primitiveArrayType, int[] shape, Object dataArray) {
    super(DataType.VLEN, shape);
    this.storage = createStorage(primitiveArrayType, (int) Arrays.computeSize(shape), dataArray);
    this.primitiveArrayType = primitiveArrayType;
  }

  /** Create an Array of type Array<T> and the given indexFn and storage. */
  private ArrayVlen(DataType primitiveArrayType, IndexFn indexFn, StorageMutable<Array<T>> storage) {
    super(DataType.VLEN, indexFn);
    Preconditions.checkArgument(indexFn.length() <= storage.getLength());
    this.storage = storage;
    this.primitiveArrayType = primitiveArrayType;
  }

  @Override
  public Iterator<Array<T>> fastIterator() {
    return storage.iterator();
  }

  @Override
  public Iterator<Array<T>> iterator() {
    return indexFn.isCanonicalOrder() ? fastIterator() : new CanonicalIterator();
  }

  @Override
  public Array<T> get(int... index) {
    return storage.get(indexFn.get(index));
  }

  @Override
  public Array<T> get(Index index) {
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
  Storage<Array<T>> storage() {
    return storage;
  }

  /** create new Array with given IndexFn and the same backing store */
  @Override
  protected ArrayVlen<T> createView(IndexFn view) {
    return new ArrayVlen<>(this.dataType, view, this.storage);
  }

  // used when the data is not in canonical order
  private class CanonicalIterator implements Iterator<Array<T>> {
    private final Iterator<Integer> iter = indexFn.iterator();

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public Array<T> next() {
      return storage.get(iter.next());
    }
  }

  // standard storage using ragged array byte[fixed][]
  @Immutable
  static final class StorageVByte implements StorageMutable<Array<Byte>> {
    private final DataType primitiveArrayType;
    private final byte[][] primitiveArray;

    StorageVByte(DataType primitiveArrayType, byte[][] primitiveArray) {
      this.primitiveArrayType = primitiveArrayType;
      this.primitiveArray = primitiveArray;
    }

    @Override
    public long getLength() {
      return primitiveArray.length;
    }

    @Override
    public Array<Byte> get(long elem) {
      byte[] p = primitiveArray[(int) elem];
      return Arrays.factory(primitiveArrayType, new int[] {p.length}, p);
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(primitiveArray, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<Array<Byte>> iterator() {
      return new StorageIter();
    }

    @Override
    public void set(int index, Object value) {
      primitiveArray[index] = (byte[]) value;
    }

    private final class StorageIter implements Iterator<Array<Byte>> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < primitiveArray.length;
      }

      @Override
      public final Array<Byte> next() {
        byte[] p = primitiveArray[count++];
        return (p == null) ? null : Arrays.factory(primitiveArrayType, new int[] {p.length}, p);
      }
    }
  }

  // standard storage using ragged array char[fixed][]
  @Immutable
  static class StorageVChar implements StorageMutable<Array<Character>> {
    private final char[][] primitiveArray;

    StorageVChar(char[][] primitiveArray) {
      this.primitiveArray = primitiveArray;
    }

    @Override
    public long getLength() {
      return primitiveArray.length;
    }

    @Override
    public Array<Character> get(long elem) {
      char[] p = primitiveArray[(int) elem];
      return Arrays.factory(DataType.CHAR, new int[] {p.length}, p);
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(primitiveArray, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<Array<Character>> iterator() {
      return new StorageIter();
    }

    @Override
    public void set(int index, Object value) {
      primitiveArray[index] = (char[]) value;
    }

    private final class StorageIter implements Iterator<Array<Character>> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < primitiveArray.length;
      }

      @Override
      public final Array<Character> next() {
        char[] p = primitiveArray[count++];
        return (p == null) ? null : Arrays.factory(DataType.CHAR, new int[] {p.length}, p);
      }
    }
  }

  // standard storage using ragged array double[fixed][]
  @Immutable
  static class StorageVDouble implements StorageMutable<Array<Double>> {
    private final double[][] primitiveArray;

    StorageVDouble(double[][] primitiveArray) {
      this.primitiveArray = primitiveArray;
    }

    @Override
    public long getLength() {
      return primitiveArray.length;
    }

    @Override
    public Array<Double> get(long elem) {
      double[] p = primitiveArray[(int) elem];
      return Arrays.factory(DataType.DOUBLE, new int[] {p.length}, p);
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(primitiveArray, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<Array<Double>> iterator() {
      return new StorageIter();
    }

    @Override
    public void set(int index, Object value) {
      primitiveArray[index] = (double[]) value;
    }

    private final class StorageIter implements Iterator<Array<Double>> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < primitiveArray.length;
      }

      @Override
      public final Array<Double> next() {
        double[] p = primitiveArray[count++];
        return (p == null) ? null : Arrays.factory(DataType.DOUBLE, new int[] {p.length}, p);
      }
    }
  }

  // standard storage using ragged array float[fixed][]
  @Immutable
  static class StorageVFloat implements StorageMutable<Array<Float>> {
    private final float[][] primitiveArray;

    StorageVFloat(float[][] primitiveArray) {
      this.primitiveArray = primitiveArray;
    }

    @Override
    public long getLength() {
      return primitiveArray.length;
    }

    @Override
    public Array<Float> get(long elem) {
      float[] p = primitiveArray[(int) elem];
      return Arrays.factory(DataType.FLOAT, new int[] {p.length}, p);
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(primitiveArray, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<Array<Float>> iterator() {
      return new StorageIter();
    }

    @Override
    public void set(int index, Object value) {
      primitiveArray[index] = (float[]) value;
    }

    private final class StorageIter implements Iterator<Array<Float>> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < primitiveArray.length;
      }

      @Override
      public final Array<Float> next() {
        float[] p = primitiveArray[count++];
        return (p == null) ? null : Arrays.factory(DataType.FLOAT, new int[] {p.length}, p);
      }
    }
  }

  // standard storage using ragged array int[fixed][]
  @Immutable
  static class StorageVInt implements StorageMutable<Array<Integer>> {
    private final DataType primitiveArrayType;
    private final int[][] primitiveArray;

    StorageVInt(DataType primitiveArrayType, int[][] primitiveArray) {
      this.primitiveArrayType = primitiveArrayType;
      this.primitiveArray = primitiveArray;
    }

    @Override
    public long getLength() {
      return primitiveArray.length;
    }

    @Override
    public Array<Integer> get(long elem) {
      int[] p = primitiveArray[(int) elem];
      return Arrays.factory(primitiveArrayType, new int[] {p.length}, p);
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(primitiveArray, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<Array<Integer>> iterator() {
      return new StorageIter();
    }

    @Override
    public void set(int index, Object value) {
      primitiveArray[index] = (int[]) value;
    }

    private final class StorageIter implements Iterator<Array<Integer>> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < primitiveArray.length;
      }

      @Override
      public final Array<Integer> next() {
        int[] p = primitiveArray[count++];
        return (p == null) ? null : Arrays.factory(primitiveArrayType, new int[] {p.length}, p);
      }
    }
  }

  // standard storage using ragged array long[fixed][]
  @Immutable
  static class StorageVLong implements StorageMutable<Array<Long>> {
    private final DataType primitiveArrayType;
    private final long[][] primitiveArray;

    StorageVLong(DataType primitiveArrayType, long[][] primitiveArray) {
      this.primitiveArrayType = primitiveArrayType;
      this.primitiveArray = primitiveArray;
    }

    @Override
    public long getLength() {
      return primitiveArray.length;
    }

    @Override
    public Array<Long> get(long elem) {
      long[] p = primitiveArray[(int) elem];
      return Arrays.factory(primitiveArrayType, new int[] {p.length}, p);
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(primitiveArray, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<Array<Long>> iterator() {
      return new StorageIter();
    }

    @Override
    public void set(int index, Object value) {
      primitiveArray[index] = (long[]) value;
    }

    private final class StorageIter implements Iterator<Array<Long>> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < primitiveArray.length;
      }

      @Override
      public final Array<Long> next() {
        long[] p = primitiveArray[count++];
        return (p == null) ? null : Arrays.factory(primitiveArrayType, new int[] {p.length}, p);
      }
    }
  }

  // standard storage using ragged array short[fixed][]
  @Immutable
  static class StorageVShort implements StorageMutable<Array<Short>> {
    private final DataType primitiveArrayType;
    private final short[][] primitiveArray;

    StorageVShort(DataType primitiveArrayType, short[][] primitiveArray) {
      this.primitiveArrayType = primitiveArrayType;
      this.primitiveArray = primitiveArray;
    }

    @Override
    public long getLength() {
      return primitiveArray.length;
    }

    @Override
    public Array<Short> get(long elem) {
      short[] p = primitiveArray[(int) elem];
      return Arrays.factory(primitiveArrayType, new int[] {p.length}, p);
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(primitiveArray, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<Array<Short>> iterator() {
      return new StorageIter();
    }

    @Override
    public void set(int index, Object value) {
      primitiveArray[index] = (short[]) value;
    }

    private final class StorageIter implements Iterator<Array<Short>> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < primitiveArray.length;
      }

      @Override
      public final Array<Short> next() {
        short[] p = primitiveArray[count++];
        return (p == null) ? null : Arrays.factory(primitiveArrayType, new int[] {p.length}, p);
      }
    }
  }

  // standard storage using ragged array String[fixed][]
  @Immutable
  static class StorageVString implements StorageMutable<Array<String>> {
    private final String[][] primitiveArray;

    StorageVString(String[][] primitiveArray) {
      this.primitiveArray = primitiveArray;
    }

    @Override
    public long getLength() {
      return primitiveArray.length;
    }

    @Override
    public Array<String> get(long elem) {
      String[] p = primitiveArray[(int) elem];
      return Arrays.factory(DataType.STRING, new int[] {p.length}, p);
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(primitiveArray, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<Array<String>> iterator() {
      return new StorageIter();
    }

    @Override
    public void set(int index, Object value) {
      primitiveArray[index] = (String[]) value;
    }

    private final class StorageIter implements Iterator<Array<String>> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < primitiveArray.length;
      }

      @Override
      public final Array<String> next() {
        String[] p = primitiveArray[count++];
        return (p == null) ? null : Arrays.factory(DataType.STRING, new int[] {p.length}, p);
      }
    }
  }

}
