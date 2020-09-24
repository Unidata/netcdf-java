/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.util.Iterator;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.DataType;

/** Concrete implementation of Array specialized for Char. */
@Immutable
public class ArrayChar extends Array<Character> {
  private final Storage<Character> storage;

  /** Create an empty Array of type Char and the given shape. */
  public ArrayChar(int[] shape) {
    super(DataType.CHAR, shape);
    storage = new StorageS(new char[(int) indexFn.length()]);
  }

  /** Create an Array of type Char and the given shape and storage. */
  public ArrayChar(int[] shape, Storage<Character> storage) {
    super(DataType.CHAR, shape);
    Preconditions.checkArgument(indexFn.length() <= storage.getLength());
    this.storage = storage;
  }

  /** Create an Array of type Char and the given indexFn and storage. */
  private ArrayChar(IndexFn indexFn, Storage<Character> storageD) {
    super(DataType.CHAR, indexFn);
    Preconditions.checkArgument(indexFn.length() <= storageD.getLength());
    this.storage = storageD;
  }

  @Override
  public Iterator<Character> fastIterator() {
    return storage.iterator();
  }

  @Override
  public Iterator<Character> iterator() {
    return indexFn.isCanonicalOrder() ? fastIterator() : new CanonicalIterator();
  }

  @Override
  public Character get(int... index) {
    return storage.get(indexFn.get(index));
  }

  @Override
  public Character get(Index index) {
    return get(index.getCurrentIndex());
  }

  @Override
  void arraycopy(int srcPos, Object dest, int destPos, long length) {
    if (indexFn.isCanonicalOrder()) {
      storage.arraycopy(srcPos, dest, destPos, length);
    } else {
      char[] ddest = (char[]) dest;
      int destIndex = destPos;
      Iterator<Integer> iter = indexFn.iterator(srcPos, length);
      while (iter.hasNext()) {
        ddest[destIndex++] = storage.get(iter.next());
      }
    }
  }

  @Override
  Storage<Character> storage() {
    return storage;
  }

  /** create new Array with given IndexFn and the same backing store */
  @Override
  protected ArrayChar createView(IndexFn indexFn) {
    return new ArrayChar(indexFn, this.storage);
  }

  // used when the data is not in canonical order
  private class CanonicalIterator implements Iterator<Character> {
    private final Iterator<Integer> iter = indexFn.iterator();

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public Character next() {
      return storage.get(iter.next());
    }
  }

  // standard storage using short[] primitive array
  @Immutable
  static class StorageS implements Storage<Character> {
    private final char[] storage;

    StorageS(char[] storage) {
      this.storage = storage;
    }

    @Override
    public long getLength() {
      return storage.length;
    }

    @Override
    public Character get(long elem) {
      return storage[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(storage, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<Character> iterator() {
      return new StorageIter();
    }

    private final class StorageIter implements Iterator<Character> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < storage.length;
      }

      @Override
      public final Character next() {
        return storage[count++];
      }
    }
  }

}
