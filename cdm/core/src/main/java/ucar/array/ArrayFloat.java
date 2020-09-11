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
public class ArrayFloat extends Array<Float> {
  private final Storage<Float> storageF;

  /** Create an empty Array of type double and the given shape. */
  public ArrayFloat(int[] shape) {
    super(DataType.FLOAT, shape);
    storageF = Storage.factory(DataType.FLOAT, new float[(int) indexCalc.getSize()]);
  }

  /** Create an Array of type double and the given shape and storage. */
  public ArrayFloat(int[] shape, Storage<Float> storageF) {
    super(DataType.DOUBLE, shape);
    Preconditions.checkArgument(indexCalc.getSize() == storageF.getLength());
    this.storageF = storageF;
  }

  /** Create an Array of type double and the given shape and storage. */
  private ArrayFloat(Strides shape, Storage<Float> storageF) {
    super(DataType.FLOAT, shape);
    this.storageF = storageF;
  }

  @Override
  public Iterator<Float> fastIterator() {
    return storageF.iterator();
  }

  @Override
  public Iterator<Float> iterator() {
    return indexCalc.isCanonicalOrder() ? fastIterator() : new CanonicalIterator();
  }

  public Float sum() {
    return (float) Streams.stream(() -> fastIterator()).mapToDouble(d -> d).sum();
  }

  @Override
  public Float get(int... index) {
    Preconditions.checkArgument(this.rank == index.length);
    return storageF.get(indexCalc.get(index));
  }

  @Override
  public Float get(Index index) {
    return get(index.getCurrentIndex());
  }

  Object getPrimitiveArray() {
    return storageF.getPrimitiveArray();
  }

  /** create new Array with given indexImpl and the same backing store */
  @Override
  protected ArrayFloat createView(Strides index) {
    return new ArrayFloat(index, storageF);
  }

  private class CanonicalIterator implements Iterator<Float> {
    // used when the data is not in canonical order
    private final Iterator<Integer> iter = indexCalc.iterator();

    @Override
    public boolean hasNext() {
      return iter.hasNext();
    }

    @Override
    public Float next() {
      return storageF.get(iter.next());
    }
  }

  static class StorageF extends Storage<Float> {
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

    @Override
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

  static class StorageFM extends Storage<Float> {
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

    @Override
    Object getPrimitiveArray() {
      return dataArrays;
    }

    final class StorageFMIter implements Iterator<Float> {
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
