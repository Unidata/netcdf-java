/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.DataType;

/** Abstraction of using java arrays to store Array data in. Allows multiple java arrays. */
@Immutable
abstract class Storage<T> implements Iterable<T> {

  static <T> Storage<T> factoryArrays(DataType dataType, List<Array> dataArrays) {
    switch (dataType) {
      case DOUBLE:
        return (Storage<T>) new StorageDM(dataArrays);
      case FLOAT:
        return (Storage<T>) new StorageFM(dataArrays);
      default:
        throw new RuntimeException();
    }
  }

  static <T> Storage<T> factory(DataType dataType, Object dataArray) {
    switch (dataType) {
      case DOUBLE:
        return (Storage<T>) new StorageD((double[]) dataArray);
      case FLOAT:
        return (Storage<T>) new StorageF((float[]) dataArray);
      default:
        throw new RuntimeException();
    }
  }

  abstract long getLength();

  abstract T get(long elem);

  abstract Object getPrimitiveArray();

  private static class StorageD extends Storage<Double> {
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

  private static class StorageDM extends Storage<Double> {
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

  private static class StorageF extends Storage<Float> {
    private final float[] storage;

    StorageF(float[] storage) {
      this.storage = storage;
    }

    @Override
    long getLength() {
      return storage.length;
    }

    @Override
    public Float get(long elem) {
      return storage[(int) elem];
    }

    @Override
    public Iterator<Float> iterator() {
      return new StorageFIter();
    }

    Object getPrimitiveArray() {
      return storage;
    }

    private final class StorageFIter implements Iterator<Float> {
      int count = 0;

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

  private static class StorageFM extends Storage<Float> {
    private final ImmutableList<float[]> dataArrays;
    private final long[] arrayEdge;
    private final long totalLength;

    StorageFM(List<Array> dataArrays) {
      ImmutableList.Builder<float[]> builder = ImmutableList.builder();
      List<Long> edge = new ArrayList<>();
      edge.add(0L);
      long total = 0L;
      for (Array<?> dataArray : dataArrays) {
        builder.add((float[]) dataArray.getPrimitiveArray());
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
    public Float get(long elem) {
      int search = Arrays.binarySearch(arrayEdge, elem);
      int arrayIndex = (search < 0) ? -search - 1 : search;
      float[] array = dataArrays.get(arrayIndex);
      return array[(int) (elem - arrayEdge[arrayIndex])];
    }

    @Override
    public Iterator<Float> iterator() {
      return new StorageFMIter();
    }

    Object getPrimitiveArray() {
      return dataArrays;
    }

    private final class StorageFMIter implements Iterator<Float> {
      int count = 0;
      int arrayIndex = 0;
      float[] array = dataArrays.get(0);

      @Override
      public final boolean hasNext() {
        return count < totalLength;
      }

      @Override
      public final Float next() {
        if (count >= arrayEdge[arrayIndex + 1]) {
          arrayIndex++;
          array = dataArrays.get(arrayIndex);
        }
        float val = array[(int) (count - arrayEdge[arrayIndex])];
        count++;
        return val;
      }
    }
  }

}
