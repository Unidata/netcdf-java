/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import java.nio.ByteBuffer;
import java.util.Iterator;
import javax.annotation.concurrent.Immutable;

/** Concrete implementation of Array specialized for Byte. */
@Immutable
public final class ArrayByte extends Array<Byte> {
  private final Storage<Byte> storage;

  /** Create an empty Array of type Byte and the given shape. */
  public ArrayByte(ArrayType dtype, int[] shape) {
    super(dtype, shape);
    storage = new StorageS(new byte[(byte) indexFn.length()]);
  }

  /** Create an Array of type Byte and the given shape and storage. */
  public ArrayByte(ArrayType dtype, int[] shape, Storage<Byte> storage) {
    super(dtype, shape);
    Preconditions.checkArgument(indexFn.length() <= storage.length());
    this.storage = storage;
  }

  /** Create an Array of type Byte and the given indexFn and storage. */
  private ArrayByte(ArrayType dtype, IndexFn indexFn, Storage<Byte> storageD) {
    super(dtype, indexFn);
    Preconditions.checkArgument(indexFn.length() <= storageD.length());
    this.storage = storageD;
  }

  @Override
  Iterator<Byte> fastIterator() {
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

  /** Convert the Array into a ByteString. */
  public ByteString getByteString() {
    if (indexFn.isCanonicalOrder()) {
      ByteString.copyFrom(((StorageS) storage).storage);
    }

    byte[] raw = new byte[(int) length()];
    int idx = 0;
    for (byte bval : this) {
      raw[idx++] = bval;
    }
    return ByteString.copyFrom(raw);
  }

  /** Convert the Array into a ByteBuffer. */
  public ByteBuffer getByteBuffer() {
    if (indexFn.isCanonicalOrder()) {
      return ByteBuffer.wrap(((StorageS) storage).storage);
    }
    ByteBuffer result = ByteBuffer.allocate((int) this.length());
    for (byte bval : this) {
      result.put(bval);
    }
    return result;
  }

  @Override
  Storage<Byte> storage() {
    return storage;
  }

  /** create new Array with given IndexFn and the same backing store */
  @Override
  protected ArrayByte createView(IndexFn view) {
    return new ArrayByte(this.arrayType, view, this.storage);
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
    public long length() {
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
