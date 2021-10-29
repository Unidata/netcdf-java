/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.util.Iterator;
import javax.annotation.concurrent.Immutable;

/** Concrete implementation of Array specialized for floats. */
@Immutable
final class ArrayFloat extends Array<Float> {
  private final Storage<Float> storageF;

  // LOOK whats the point if you cant change the storage?
  /** Create an empty Array of type float and the given shape. */
  ArrayFloat(int[] shape, float fillValue) {
    super(ArrayType.FLOAT, shape);
    float[] parray = new float[(int) indexFn.length()];
    java.util.Arrays.fill(parray, fillValue);
    storageF = new StorageF(parray);
  }

  /** Create an Array of type float and the given shape and storage. */
  ArrayFloat(int[] shape, Storage<Float> storageF) {
    super(ArrayType.FLOAT, shape);
    Preconditions.checkArgument(indexFn.length() <= storageF.length());
    this.storageF = storageF;
  }

  /** Create an Array of type float and the given indexFn and storage. */
  private ArrayFloat(IndexFn indexFn, Storage<Float> storageF) {
    super(ArrayType.FLOAT, indexFn);
    Preconditions.checkArgument(indexFn.length() <= storageF.length());
    this.storageF = storageF;
  }

  @Override
  Iterator<Float> fastIterator() {
    return storageF.iterator();
  }

  @Override
  public Iterator<Float> iterator() {
    return indexFn.isCanonicalOrder() ? fastIterator() : new CanonicalIterator();
  }

  @Override
  public Float get(int... index) {
    return storageF.get(indexFn.get(index));
  }

  @Override
  public Float get(Index index) {
    return get(index.getCurrentIndex());
  }

  @Override
  void arraycopy(int srcPos, Object dest, int destPos, long length) {
    if (indexFn.isCanonicalOrder()) {
      storageF.arraycopy(srcPos, dest, destPos, length);
    } else {
      float[] ddest = (float[]) dest;
      int destIndex = destPos;
      Iterator<Integer> iter = indexFn.iterator(srcPos, length);
      while (iter.hasNext()) {
        ddest[destIndex++] = storageF.get(iter.next());
      }
    }
  }

  @Override
  Storage<Float> storage() {
    return storageF;
  }

  /** create new Array with given IndexFn and the same backing store */
  @Override
  protected ArrayFloat createView(IndexFn view) {
    return new ArrayFloat(view, storageF);
  }

  private class CanonicalIterator implements Iterator<Float> {
    // used when the data is not in canonical order
    private final Iterator<Integer> iter = indexFn.iterator();

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public Float next() {
      return storageF.get(iter.next());
    }
  }

  @Immutable
  static final class StorageF implements Storage<Float> {
    private final float[] storage;

    StorageF(float[] storage) {
      this.storage = storage;
    }

    @Override
    public long length() {
      return storage.length;
    }

    @Override
    public Float get(long elem) {
      return storage[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(storage, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<Float> iterator() {
      return new StorageFIter();
    }

    private final class StorageFIter implements Iterator<Float> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < storage.length;
      }

      @Override
      public final Float next() {
        return storage[count++];
      }
    }
  }

}
