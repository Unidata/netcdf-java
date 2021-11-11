/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.util.Iterator;
import javax.annotation.concurrent.Immutable;

/** Concrete implementation of Array specialized for doubles. */
@Immutable
final class ArrayDouble extends ucar.array.Array<Double> {
  private final Storage<Double> storageD;

  /** Create an empty Array of type double and the given shape. */
  ArrayDouble(int[] shape, double fillValue) {
    super(ArrayType.DOUBLE, shape);
    double[] parray = new double[(int) indexFn.length()];
    java.util.Arrays.fill(parray, fillValue);
    storageD = new StorageD(parray);
  }

  /** Create an Array of type double and the given shape and storage. */
  ArrayDouble(int[] shape, Storage<Double> storageD) {
    super(ArrayType.DOUBLE, shape);
    Preconditions.checkArgument(indexFn.length() <= storageD.length());
    this.storageD = storageD;
  }

  /** Create an Array of type double and the given indexFn and storage. */
  private ArrayDouble(IndexFn indexFn, Storage<Double> storageD) {
    super(ArrayType.DOUBLE, indexFn);
    Preconditions.checkArgument(indexFn.length() <= storageD.length());
    this.storageD = storageD;
  }

  @Override
  Iterator<Double> fastIterator() {
    return storageD.iterator();
  }

  @Override
  public Iterator<Double> iterator() {
    return indexFn.isCanonicalOrder() ? fastIterator() : new CanonicalIterator();
  }

  @Override
  public Double get(int... index) {
    return storageD.get(indexFn.get(index));
  }

  @Override
  public Double get(Index index) {
    return get(index.getCurrentIndex());
  }

  @Override
  void arraycopy(int srcPos, Object dest, int destPos, long length) {
    if (indexFn.isCanonicalOrder()) {
      storageD.arraycopy(srcPos, dest, destPos, length);
    } else {
      double[] ddest = (double[]) dest;
      int destIndex = destPos;
      Iterator<Integer> iter = indexFn.iterator(srcPos, length);
      while (iter.hasNext()) {
        ddest[destIndex++] = storageD.get(iter.next());
      }
    }
  }

  @Override
  Storage<Double> storage() {
    return storageD;
  }

  /** create new Array with given IndexFn and the same backing store */
  @Override
  protected ArrayDouble createView(IndexFn view) {
    return new ArrayDouble(view, storageD);
  }

  // used when the data is not in canonical order
  private class CanonicalIterator implements Iterator<Double> {
    private final Iterator<Integer> iter = indexFn.iterator();

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public Double next() {
      return storageD.get(iter.next());
    }
  }

  // standard storage using double[] primitive array
  @Immutable
  static final class StorageD implements Storage<Double> {
    private final double[] storage;

    StorageD(double[] storage) {
      this.storage = storage;
    }

    @Override
    public long length() {
      return storage.length;
    }

    @Override
    public Double get(long elem) {
      return storage[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(storage, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<Double> iterator() {
      return new StorageDIter();
    }

    private final class StorageDIter implements Iterator<Double> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < storage.length;
      }

      @Override
      public final Double next() {
        return storage[count++];
      }
    }
  }

}
