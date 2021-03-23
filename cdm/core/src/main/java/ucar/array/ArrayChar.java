/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import java.util.Iterator;
import javax.annotation.concurrent.Immutable;

/** Concrete implementation of Array specialized for Char. */
@Immutable
public final class ArrayChar extends Array<Character> {
  private final Storage<Character> storage;

  /** Create an empty Array of type Char and the given shape. */
  public ArrayChar(int[] shape) {
    super(ArrayType.CHAR, shape);
    storage = new StorageS(new char[(int) indexFn.length()]);
  }

  /** Create an Array of type Char and the given shape and storage. */
  public ArrayChar(int[] shape, Storage<Character> storage) {
    super(ArrayType.CHAR, shape);
    Preconditions.checkArgument(indexFn.length() <= storage.length());
    this.storage = storage;
  }

  /** Create an Array of type Char and the given indexFn and storage. */
  private ArrayChar(IndexFn indexFn, Storage<Character> storageD) {
    super(ArrayType.CHAR, indexFn);
    Preconditions.checkArgument(indexFn.length() <= storageD.length());
    this.storage = storageD;
  }

  @Override
  Iterator<Character> fastIterator() {
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

  /**
   * Create a String out of this rank zero or one ArrayChar.
   * If there is a null (zero) value in the ArrayChar array, the String will end there.
   * The null is not returned as part of the String.
   */
  public String makeStringFromChar() {
    Preconditions.checkArgument(getRank() < 2);
    int count = 0;
    for (char c : this) {
      if (c == 0) {
        break;
      }
      count++;
    }
    char[] carr = new char[count];
    int idx = 0;
    for (char c : this) {
      if (c == 0) {
        break;
      }
      carr[idx++] = c;
    }
    return String.valueOf(carr);
  }

  /**
   * Create an Array of Strings out of this ArrayChar of any rank.
   * If there is a null (zero) value in the ArrayChar array, the String will end there.
   * The null is not returned as part of the String.
   *
   * @return Array of Strings of rank - 1.
   */
  public Array<String> makeStringsFromChar() {
    if (getRank() < 2) {
      return Arrays.factory(ArrayType.STRING, new int[] {1}, new String[] {makeStringFromChar()});
    }
    int innerLength = this.indexFn.getShape(this.rank - 1);
    int outerLength = (int) this.length() / innerLength;
    int[] outerShape = new int[this.rank - 1];
    System.arraycopy(this.getShape(), 0, outerShape, 0, this.rank - 1);

    String[] result = new String[outerLength];
    char[] carr = new char[innerLength];
    int sidx = 0;
    int cidx = 0;
    int idx = 0;
    for (char c : this) {
      carr[cidx++] = c;
      idx++;
      if (idx % innerLength == 0) {
        result[sidx++] = String.valueOf(carr);
        cidx = 0;
      }
    }
    return Arrays.factory(ArrayType.STRING, outerShape, result);
  }

  @Override
  Storage<Character> storage() {
    return storage;
  }

  /** create new Array with given IndexFn and the same backing store */
  @Override
  protected ArrayChar createView(IndexFn view) {
    return new ArrayChar(view, this.storage);
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

  // standard storage using char[] primitive array
  @Immutable
  static final class StorageS implements Storage<Character> {
    private final char[] storage;

    StorageS(char[] storage) {
      this.storage = storage;
    }

    @Override
    public long length() {
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
