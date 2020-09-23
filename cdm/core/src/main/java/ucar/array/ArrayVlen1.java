/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.util.Iterator;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.DataType;

/**
 * Array of variable length Arrays of T.
 * LOOK Could try ragged primitive arrays, but we'd have to have a seperate class for each type.
 * LOOK Probably vlens are not used enough for that, consider it an optimization that can be added later.
 */
@Immutable
public class ArrayVlen1<T> extends Array<Array<T>> {
  private final Storage<Array<T>> storage;

  /** Create an empty Array of type Array<T> and the given shape. */
  public ArrayVlen1(DataType dataType, int[] shape) {
    super(dataType, shape);
    // LOOK this has to be type specific
    this.storage = new StorageV<T>(new Array[(int) indexFn.length()]);
  }

  /** Create an Array of type Array<T> and the given shape and storage. */
  public ArrayVlen1(DataType dataType, int[] shape, Storage<Array<T>> storage) {
    super(dataType, shape);
    Preconditions.checkArgument(indexFn.length() <= storage.getLength());
    this.storage = storage;
  }

  /** Create an Array of type Array<T> and the given indexFn and storage. */
  private ArrayVlen1(DataType dataType, IndexFn indexFn, Storage<Array<T>> storageD) {
    super(dataType, indexFn);
    Preconditions.checkArgument(indexFn.length() <= storageD.getLength());
    this.storage = storageD;
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

  @Override
  void arraycopy(int srcPos, Object dest, int destPos, long length) {
    if (indexFn.isCanonicalOrder()) {
      storage.arraycopy(srcPos, dest, destPos, length);
    } else {
      Array<T>[] ddest = (Array<T>[]) dest;
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
  protected ArrayVlen1<T> createView(IndexFn indexFn) {
    return new ArrayVlen1(this.dataType, indexFn, this.storage);
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

  // standard storage using Array<T>[] primitive array
  @Immutable
  static class StorageV<T> implements Storage<Array<T>> {
    private final Array<T>[] storage;

    StorageV(Array<T>[] storage) {
      this.storage = storage;
    }

    @Override
    public long getLength() {
      return storage.length;
    }

    @Override
    public Array<T> get(long elem) {
      return storage[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(storage, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<Array<T>> iterator() {
      return new StorageIter();
    }

    private final class StorageIter implements Iterator<Array<T>> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < storage.length;
      }

      @Override
      public final Array<T> next() {
        return storage[count++];
      }
    }
  }

}
