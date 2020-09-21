/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
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
import ucar.ma2.DataType;

/** Concrete implementation of Array specialized for doubles. */
@Immutable
public class ArrayDouble extends ucar.array.Array<Double> {

  private final Storage<Double> storageD;

  /** Create an empty Array of type double and the given shape. */
  public ArrayDouble(int[] shape) {
    super(DataType.DOUBLE, shape);
    storageD = new StorageD(new double[(int) indexCalc.getSize()]);
  }

  /** Create an Array of type double and the given shape and storage. */
  public ArrayDouble(int[] shape, Storage<Double> storageD) {
    super(DataType.DOUBLE, shape);
    Preconditions.checkArgument(indexCalc.getSize() <= storageD.getLength());
    this.storageD = storageD;
  }

  /** Create an Array of type double and the given shape and storage. */
  private ArrayDouble(Strides shape, Storage<Double> storageD) {
    super(DataType.DOUBLE, shape);
    Preconditions.checkArgument(indexCalc.getSize() <= storageD.getLength());
    this.storageD = storageD;
  }

  @Override
  public Iterator<Double> fastIterator() {
    return storageD.iterator();
  }

  @Override
  public Iterator<Double> iterator() {
    return indexCalc.isCanonicalOrder() ? fastIterator() : new CanonicalIterator();
  }

  @Override
  public Double get(int... index) {
    return storageD.get(indexCalc.get(index));
  }

  @Override
  public Double get(Index index) {
    return get(index.getCurrentIndex());
  }

  @Override
  void arraycopy(int srcPos, Object dest, int destPos, long length) {
    if (indexCalc.isCanonicalOrder()) {
      storageD.arraycopy(srcPos, dest, destPos, length);
    } else {
      double[] ddest = (double[]) dest;
      int destIndex = destPos;
      Iterator<Integer> iter = indexCalc.iterator(srcPos, length);
      while (iter.hasNext()) {
        ddest[destIndex++] = storageD.get(iter.next());
      }
    }
  }

  @Override
  Storage<Double> storage() {
    return storageD;
  }

  /** create new Array with given indexImpl and the same backing store */
  @Override
  protected ArrayDouble createView(Strides index) {
    return new ArrayDouble(index, storageD);
  }

  // used when the data is not in canonical order
  private class CanonicalIterator implements Iterator<Double> {
    private final Iterator<Integer> iter = indexCalc.iterator();

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public Double next() {
      return storageD.get(iter.next());
    }
  }

  // standard storage using double[] primitive array
  @Immutable
  static class StorageD implements Storage<Double> {
    private final double[] storage;

    StorageD(double[] storage) {
      this.storage = storage;
    }

    @Override
    public long getLength() {
      return storage.length;
    }

    @Override
    public Double get(long elem) {
      return storage[(int) elem];
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      System.arraycopy(storage, srcPos, dest, destPos, (int) length);
    }

    @Override
    public Iterator<Double> iterator() {
      return new StorageDIter();
    }

    private final class StorageDIter implements Iterator<Double> {
      private int count = 0;

      @Override
      public final boolean hasNext() {
        return count < storage.length;
      }

      @Override
      public final Double next() {
        return storage[count++];
      }
    }
  }

  // experimental storage using List of Storage<Double>
  @Immutable
  static class StorageDM implements Storage<Double> {
    private final ImmutableList<Storage<Double>> dataArrays;
    private final long[] arrayEdge;
    private final long totalLength;

    StorageDM(List<Array<?>> dataArrays) {
      ImmutableList.Builder<Storage<Double>> builder = ImmutableList.builder();
      List<Long> edge = new ArrayList<>();
      edge.add(0L);
      long total = 0L;
      for (Array<?> dataArray : dataArrays) {
        builder.add(((Array<Double>) dataArray).storage());
        total += dataArray.getSize();
        edge.add(total);
      }
      this.dataArrays = builder.build();
      this.arrayEdge = edge.stream().mapToLong(i -> i).toArray();
      this.totalLength = total;
    }

    @Override
    public long getLength() {
      return totalLength;
    }

    @Override
    public Double get(long elem) {
      int search = Arrays.binarySearch(arrayEdge, elem);
      int arrayIndex = (search < 0) ? -search - 2 : search;
      Storage<Double> array = dataArrays.get(arrayIndex);
      return array.get((int) (elem - arrayEdge[arrayIndex]));
    }

    @Override
    public void arraycopy(int srcPos, Object dest, int destPos, long length) {
      long needed = length;
      int startDst = destPos;

      int search = Arrays.binarySearch(arrayEdge, srcPos);
      int startIndex = (search < 0) ? -search - 2 : search;
      int startSrc = (int) (srcPos - arrayEdge[startIndex]);

      for (int index = startIndex; index < dataArrays.size(); index++) {
        Storage<Double> storage = dataArrays.get(index);
        int have = (int) Math.min(storage.getLength() - startSrc, needed);
        storage.arraycopy(startSrc, dest, startDst, have);
        needed -= have;
        startDst += have;
        startSrc = 0;
      }
    }

    @Override
    public Iterator<Double> iterator() {
      return new StorageDMIter();
    }

    private final class StorageDMIter implements Iterator<Double> {
      private int count = 0;
      private int arrayIndex = 0;
      private Storage<Double> array = dataArrays.get(0);

      @Override
      public final boolean hasNext() {
        return count < totalLength;
      }

      @Override
      public final Double next() {
        if (count >= arrayEdge[arrayIndex + 1]) {
          arrayIndex++;
          array = dataArrays.get(arrayIndex);
        }
        double val = array.get((int) (count - arrayEdge[arrayIndex]));
        count++;
        return val;
      }
    }
  }

}
