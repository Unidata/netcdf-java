/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.util.Iterator;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.DataType;

/** Concrete implementation of Array specialized for Long. */
@Immutable
public class ArrayLong extends Array<Long> {
  private final Storage<Long> storage;

  /** Create an empty Array of type Long and the given shape. */
  public ArrayLong(DataType dtype, int[] shape) {
    super(dtype, shape);
    storage = new StorageS(new long[(int) indexFn.length()]);
  }

  /** Create an Array of type Long and the given shape and storage. */
  public ArrayLong(DataType dtype, int[] shape, Storage<Long> storage) {
    super(dtype, shape);
    Preconditions.checkArgument(indexFn.length() <= storage.getLength());
    this.storage = storage;
  }

  /** Create an Array of type Long and the given indexFn and storage. */
  private ArrayLong(DataType dtype, IndexFn indexFn, Storage<Long> storageD) {
    super(dtype, indexFn);
    Preconditions.checkArgument(indexFn.length() <= storageD.getLength());
    this.storage = storageD;
  }

  @Override
  public Iterator<Long> fastIterator() {
    return storage.iterator();
  }

  @Override
  public Iterator<Long> iterator() {
    return indexFn.isCanonicalOrder() ? fastIterator() : new CanonicalIterator();
  }

  @Override
  public Long get(int... index) {
    return storage.get(indexFn.get(index));
  }

  @Override
  public Long get(Index index) {
    return get(index.getCurrentIndex());
  }

  @Override
  void arraycopy(int srcPos, Object dest, int destPos, long length) {
    if (indexFn.isCanonicalOrder()) {
      storage.arraycopy(srcPos, dest, destPos, length);
    } else {
      long[] ddest = (long[]) dest;
      int destIndex = destPos;
      Iterator<Integer> iter = indexFn.iterator(srcPos, length);
      while (iter.hasNext()) {
        ddest[destIndex++] = storage.get(iter.next());
      }
    }
  }

  @Override
  Storage<Long> storage() {
    return storage;
  }

  /** create new Array with given IndexFn and the same backing store */
  @Override
  protected ArrayLong createView(IndexFn indexFn) {
    return new ArrayLong(this.dataType, indexFn, this.storage);
  }

  // used when the data is not in canonical order
  private class CanonicalIterator implements Iterator<Long> {
    private final Iterator<Integer> iter = indexFn.iterator();

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public Long next() {
      return storage.get(iter.next());
    }
  }

  // standard storage using long[] primitive array
  @Immutable
  static class StorageS implements Storage<Long> {
    private final long[] storage;

    StorageS(long[] storage) {
      this.storage = storage;
    }

    @Override
    public long getLength() {
      return storage.length;
    }

    @Override
    public Long get(long elem) {
      return storage[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(storage, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<Long> iterator() {
      return new StorageIter();
    }

    private final class StorageIter implements Iterator<Long> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < storage.length;
      }

      @Override
      public final Long next() {
        return storage[count++];
      }
    }
  }

}
