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
 * Used for vlens. Each Object is a primitive array of dataType.
 * TODO: problem is that the type is the primitive type. Then you have to check if its a vlen, and process diffeently.
 * LOOK not used, will likely delete after the dust settles.
 */
@Immutable
public class ArrayObject extends Array<Object> {
  private final Storage<Object> storage;

  /** Create an empty Array of type String and the given shape. */
  public ArrayObject(DataType dataType, int[] shape) {
    super(dataType, shape);
    storage = new StorageO(new Object[(int) indexFn.length()]);
  }

  /** Create an Array of type double and the given shape and storage. */
  public ArrayObject(DataType dataType, int[] shape, Storage<Object> storage) {
    super(dataType, shape);
    Preconditions.checkArgument(indexFn.length() <= storage.getLength());
    this.storage = storage;
  }

  /** Create an Array of type double and the given indexFn and storage. */
  private ArrayObject(DataType dataType, IndexFn indexFn, Storage<Object> storageD) {
    super(dataType, indexFn);
    Preconditions.checkArgument(indexFn.length() <= storageD.getLength());
    this.storage = storageD;
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
  protected ArrayObject createView(IndexFn indexFn) {
    return new ArrayObject(this.dataType, indexFn, this.storage);
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

  // standard storage using String[] primitive array
  @Immutable
  static class StorageO implements Storage<Object> {
    private final Object[] storage;

    StorageO(Object[] storage) {
      this.storage = storage;
    }

    @Override
    public long getLength() {
      return storage.length;
    }

    @Override
    public Object get(long elem) {
      return storage[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(storage, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<Object> iterator() {
      return new StorageIter();
    }

    private final class StorageIter implements Iterator<Object> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < storage.length;
      }

      @Override
      public final Object next() {
        return storage[count++];
      }
    }
  }

}
