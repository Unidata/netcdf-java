/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.util.Iterator;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.DataType;

/** Concrete implementation of Array specialized for Byte. */
@Immutable
public final class ArrayByte extends Array<Byte> {
  private final Storage<Byte> storage;

  /** Create an empty Array of type Byte and the given shape. */
  public ArrayByte(DataType dtype, int[] shape) {
    super(dtype, shape);
    storage = new StorageS(new byte[(byte) indexFn.length()]);
  }

  /** Create an Array of type Byte and the given shape and storage. */
  public ArrayByte(DataType dtype, int[] shape, Storage<Byte> storage) {
    super(dtype, shape);
    Preconditions.checkArgument(indexFn.length() <= storage.getLength());
    this.storage = storage;
  }

  /** Create an Array of type Byte and the given indexFn and storage. */
  private ArrayByte(DataType dtype, IndexFn indexFn, Storage<Byte> storageD) {
    super(dtype, indexFn);
    Preconditions.checkArgument(indexFn.length() <= storageD.getLength());
    this.storage = storageD;
  }

  @Override
  public Iterator<Byte> fastIterator() {
    return storage.iterator();
  }

  @Override
  public Iterator<Byte> iterator() {
    return indexFn.isCanonicalOrder() ? fastIterator() : new CanonicalIterator();
  }

  @Override
  public Byte get(int... index) {
    return storage.get(indexFn.get(index));
  }

  @Override
  public Byte get(Index index) {
    return get(index.getCurrentIndex());
  }

  @Override
  void arraycopy(int srcPos, Object dest, int destPos, long length) {
    if (indexFn.isCanonicalOrder()) {
      storage.arraycopy(srcPos, dest, destPos, length);
    } else {
      byte[] ddest = (byte[]) dest;
      int destIndex = destPos;
      Iterator<Integer> iter = indexFn.iterator(srcPos, length);
      while (iter.hasNext()) {
        ddest[destIndex++] = storage.get(iter.next());
      }
    }
  }

  @Override
  Storage<Byte> storage() {
    return storage;
  }

  /** create new Array with given IndexFn and the same backing store */
  @Override
  protected ArrayByte createView(IndexFn indexFn) {
    return new ArrayByte(this.dataType, indexFn, this.storage);
  }

  // used when the data is not in canonical order
  private class CanonicalIterator implements Iterator<Byte> {
    private final Iterator<Integer> iter = indexFn.iterator();

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public Byte next() {
      return storage.get(iter.next());
    }
  }

  // standard storage using byte[] primitive array
  @Immutable
  static final class StorageS implements Storage<Byte> {
    private final byte[] storage;

    StorageS(byte[] storage) {
      this.storage = storage;
    }

    @Override
    public long getLength() {
      return storage.length;
    }

    @Override
    public Byte get(long elem) {
      return storage[(byte) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(storage, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<Byte> iterator() {
      return new StorageIter();
    }

    private final class StorageIter implements Iterator<Byte> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < storage.length;
      }

      @Override
      public final Byte next() {
        return storage[count++];
      }
    }
  }

}
