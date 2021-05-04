/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.annotation.concurrent.Immutable;

/** Concrete implementation of Array specialized for floats. */
@Immutable
public final class ArrayFloat extends Array<Float> {
  private final Storage<Float> storageF;

  // LOOK whats the point if you cant change the storage?
  /** Create an empty Array of type float and the given shape. */
  public ArrayFloat(int[] shape) {
    super(ArrayType.FLOAT, shape);
    storageF = new StorageF(new float[(int) indexFn.length()]);
  }

  /** Create an Array of type float and the given shape and storage. */
  public ArrayFloat(int[] shape, Storage<Float> storageF) {
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

  @Immutable
  static class StorageFM implements Storage<Float> {
    private final ImmutableList<Storage<Float>> dataArrays;
    private final long[] arrayEdge;
    private final long totalLength;

    StorageFM(List<Array<?>> dataArrays) {
      ImmutableList.Builder<Storage<Float>> builder = ImmutableList.builder();
      List<Long> edge = new ArrayList<>();
      edge.add(0L);
      long total = 0L;
      for (Array<?> dataArray : dataArrays) {
        builder.add(((Array<Float>) dataArray).storage());
        total += dataArray.length();
        edge.add(total);
      }
      this.dataArrays = builder.build();
      this.arrayEdge = edge.stream().mapToLong(i -> i).toArray();
      this.totalLength = total;
    }

    @Override
    public long length() {
      return totalLength;
    }

    @Override
    public Float get(long elem) {
      int search = Arrays.binarySearch(arrayEdge, elem);
      int arrayIndex = (search < 0) ? -search - 2 : search;
      Storage<Float> storage = dataArrays.get(arrayIndex);
      return storage.get((int) (elem - arrayEdge[arrayIndex]));
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      long needed = length;
      int startDst = destPos;

      int search = Arrays.binarySearch(arrayEdge, srcPos);
      int startIndex = (search < 0) ? -search - 2 : search;
      int startSrc = (int) (srcPos - arrayEdge[startIndex]);

      for (int index = startIndex; index < dataArrays.size(); index++) {
        Storage<Float> storage = dataArrays.get(index);
        int have = (int) Math.min(storage.length() - startSrc, needed);
        storage.arraycopy(startSrc, dest, startDst, have);
        needed -= have;
        startDst += have;
        startSrc = 0;
      }
    }

    @Override
    public Iterator<Float> iterator() {
      return new StorageFMIter();
    }

    final class StorageFMIter implements Iterator<Float> {
      private int count = 0;
      private int arrayIndex = 0;
      Storage<Float> storage = dataArrays.get(0);

      @Override
      public final boolean hasNext() {
        return count < totalLength;
      }

      @Override
      public final Float next() {
        if (count >= arrayEdge[arrayIndex + 1]) {
          arrayIndex++;
          storage = dataArrays.get(arrayIndex);
        }
        float val = storage.get((int) (count - arrayEdge[arrayIndex]));
        count++;
        return val;
      }
    }
  }

}
