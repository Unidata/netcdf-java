/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.util.Iterator;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.DataType;

/** Concrete implementation of Array specialized for Integer. */
@Immutable
public class ArrayInteger extends Array<Integer> {
  private final Storage<Integer> storage;

  /** Create an empty Array of type Integer and the given shape. */
  public ArrayInteger(DataType dtype, int[] shape) {
    super(dtype, shape);
    storage = new StorageS(new int[(int) indexFn.length()]);
  }

  /** Create an Array of type Integer and the given shape and storage. */
  public ArrayInteger(DataType dtype, int[] shape, Storage<Integer> storage) {
    super(dtype, shape);
    Preconditions.checkArgument(indexFn.length() <= storage.getLength());
    this.storage = storage;
  }

  /** Create an Array of type Integer and the given indexFn and storage. */
  private ArrayInteger(DataType dtype, IndexFn indexFn, Storage<Integer> storageD) {
    super(dtype, indexFn);
    Preconditions.checkArgument(indexFn.length() <= storageD.getLength());
    this.storage = storageD;
  }

  @Override
  public Iterator<Integer> fastIterator() {
    return storage.iterator();
  }

  @Override
  public Iterator<Integer> iterator() {
    return indexFn.isCanonicalOrder() ? fastIterator() : new CanonicalIterator();
  }

  @Override
  public Integer get(int... index) {
    return storage.get(indexFn.get(index));
  }

  @Override
  public Integer get(Index index) {
    return get(index.getCurrentIndex());
  }

  @Override
  void arraycopy(int srcPos, Object dest, int destPos, long length) {
    if (indexFn.isCanonicalOrder()) {
      storage.arraycopy(srcPos, dest, destPos, length);
    } else {
      int[] ddest = (int[]) dest;
      int destIndex = destPos;
      Iterator<Integer> iter = indexFn.iterator(srcPos, length);
      while (iter.hasNext()) {
        ddest[destIndex++] = storage.get(iter.next());
      }
    }
  }

  @Override
  Storage<Integer> storage() {
    return storage;
  }

  /** create new Array with given IndexFn and the same backing store */
  @Override
  protected ArrayInteger createView(IndexFn indexFn) {
    return new ArrayInteger(this.dataType, indexFn, this.storage);
  }

  // used when the data is not in canonical order
  private class CanonicalIterator implements Iterator<Integer> {
    private final Iterator<Integer> iter = indexFn.iterator();

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public Integer next() {
      return storage.get(iter.next());
    }
  }

  // standard storage using int[] primitive array
  @Immutable
  static class StorageS implements Storage<Integer> {
    private final int[] storage;

    StorageS(int[] storage) {
      this.storage = storage;
    }

    @Override
    public long getLength() {
      return storage.length;
    }

    @Override
    public Integer get(long elem) {
      return storage[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(storage, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<Integer> iterator() {
      return new StorageIter();
    }

    private final class StorageIter implements Iterator<Integer> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < storage.length;
      }

      @Override
      public final Integer next() {
        return storage[count++];
      }
    }
  }

}
