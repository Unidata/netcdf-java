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
 * Find out type from getPrimitiveArrayType().
 * This is mutable, to assist users in constructing. See set(index, value).
 */
public class ArrayVlen<T> extends Array<Object> {

  public static <T> ArrayVlen<T> factory(DataType dataType, int[] shape) {
    // find leftmost vlen
    // TODO this implies that vlen doesnt have to be rightmost dimension. For now, we flatten into 1D.
    int prefixrank = 0;
    for (int i = 0; i < shape.length; i++) {
      if (shape[i] < 0) {
        prefixrank = i;
        break;
      }
    }
    int[] newshape = new int[prefixrank];
    System.arraycopy(shape, 0, newshape, 0, prefixrank);

    return new ArrayVlen<>(dataType, newshape, null);
  }

  public static StorageMutable<Object> createStorage(DataType dataType, int length, Object dataArray) {
    if (dataArray == null) {
      dataArray = createVlenArray(dataType, length);
    }
    Object result; // nasty
    switch (dataType) {
      case DOUBLE:
        result = new StorageVDouble((double[][]) dataArray);
        break;
      case FLOAT:
        result = new StorageVFloat((float[][]) dataArray);
        break;

      default:
        return null;
    }
    return (StorageMutable<Object>) result;
  }

  public static Object createVlenArray(DataType dataType, int length) {
    switch (dataType) {
      case DOUBLE:
        return new double[length][];
      case FLOAT:
        return new float[length][];
      default:
        return null;
    }
  }

  /////////////////////////////////////////////////////////////////////////
  private final StorageMutable<Object> storage;
  private final DataType primitiveArrayType;

  /** Create an empty Array of type Array<T> and the given shape. */
  public ArrayVlen(DataType primitiveArrayType, int[] shape) {
    super(DataType.VLEN, shape);
    this.storage = createStorage(dataType, (int) IndexFn.computeSize(shape), null);
    this.primitiveArrayType = primitiveArrayType;
  }

  /** Create an Array of type Array<T> and the given shape and data array T[][]. */
  public ArrayVlen(DataType primitiveArrayType, int[] shape, Object dataArray) {
    super(DataType.VLEN, shape);
    this.storage = createStorage(dataType, (int) IndexFn.computeSize(shape), dataArray);
    this.primitiveArrayType = primitiveArrayType;
  }

  /** Create an Array of type Array<T> and the given shape and storage. */
  public ArrayVlen(DataType primitiveArrayType, int[] shape, StorageMutable<Object> storage) {
    super(DataType.VLEN, shape);
    Preconditions.checkArgument(indexFn.length() <= storage.getLength());
    this.storage = storage;
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

  // standard storage using ragged array double[fixed][]
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


}
