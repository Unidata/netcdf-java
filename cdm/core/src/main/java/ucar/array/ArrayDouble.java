/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import ucar.ma2.DataType;

/**
 * Concrete implementation of Array specialized for doubles.
 * Data storage is with 1D java array of doubles.
 */
public class ArrayDouble extends ucar.array.Array<Double> {

  private final Storage<Double> storageD;

  /** Create an empty Array of type double and the given shape. */
  public ArrayDouble(int[] shape) {
    super(DataType.DOUBLE, shape);
    storageD = Storage.factory(DataType.DOUBLE, new double[(int) indexCalc.getSize()]);
  }

  /** Create an Array of type double and the given shape and storage. */
  public ArrayDouble(int[] shape, Storage<Double> storageD) {
    super(DataType.DOUBLE, shape);
    Preconditions.checkArgument(indexCalc.getSize() == storageD.getLength());
    this.storageD = storageD;
  }

  /** Create an Array of type double and the given shape and storage. */
  private ArrayDouble(Strides shape, Storage<Double> storageD) {
    super(DataType.DOUBLE, shape);
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

  public Double sum() {
    return Streams.stream(() -> fastIterator()).mapToDouble(d -> d).sum();
  }

  @Override
  public Double get(int... index) {
    Preconditions.checkArgument(this.rank == index.length);
    return storageD.get(indexCalc.get(index));
  }

  @Override
  public Double get(Index index) {
    return get(index.getCurrentIndex());
  }

  @Override
  Object getPrimitiveArray() {
    return storageD.getPrimitiveArray();
  }

  /** create new Array with given indexImpl and the same backing store */
  @Override
  protected ArrayDouble createView(Strides index) {
    return new ArrayDouble(index, storageD);
  }

  private class CanonicalIterator implements Iterator<Double> {
    // used when the data is not in canonical order
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

  static class StorageD extends Storage<Double> {
    private final double[] storage;

    StorageD(double[] storage) {
      this.storage = storage;
    }

    @Override
    long getLength() {
      return storage.length;
    }

    @Override
    public Double get(long elem) {
      return storage[(int) elem];
    }

    @Override
    public Iterator<Double> iterator() {
      return new StorageDIter();
    }

    @Override
    Object getPrimitiveArray() {
      return storage;
    }

    private final class StorageDIter implements Iterator<Double> {
      int count = 0;

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

  static class StorageDM extends Storage<Double> {
    private final ImmutableList<double[]> dataArrays;
    private final long[] arrayEdge;
    private final long totalLength;

    StorageDM(List<Array> dataArrays) {
      ImmutableList.Builder<double[]> builder = ImmutableList.builder();
      List<Long> edge = new ArrayList<>();
      edge.add(0L);
      long total = 0L;
      for (Array<?> dataArray : dataArrays) {
        builder.add((double[]) dataArray.getPrimitiveArray());
        total += dataArray.getSize();
        edge.add(total);
      }
      this.dataArrays = builder.build();
      this.arrayEdge = edge.stream().mapToLong(i -> i).toArray();
      this.totalLength = total;
    }

    @Override
    long getLength() {
      return totalLength;
    }

    @Override
    public Double get(long elem) {
      int search = Arrays.binarySearch(arrayEdge, elem);
      int arrayIndex = (search < 0) ? -search - 1 : search;
      double[] array = dataArrays.get(arrayIndex);
      return array[(int) (elem - arrayEdge[arrayIndex])];
    }

    @Override
    public Iterator<Double> iterator() {
      return new StorageDMIter();
    }

    Object getPrimitiveArray() {
      return dataArrays;
    }

    private final class StorageDMIter implements Iterator<Double> {
      int count = 0;
      int arrayIndex = 0;
      double[] array = dataArrays.get(0);

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
        double val = array[(int) (count - arrayEdge[arrayIndex])];
        count++;
        return val;
      }
    }
  }

}
