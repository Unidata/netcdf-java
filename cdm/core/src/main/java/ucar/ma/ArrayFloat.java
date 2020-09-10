/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma;

import com.google.common.base.Preconditions;
import com.google.common.collect.Streams;
import java.util.Iterator;
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
  public Float get() {
    Preconditions.checkArgument(this.rank == 0);
    return storageF.get(0);
  }

  @Override
  public Float get(int v0) {
    Preconditions.checkArgument(this.rank == 1);
    return storageF.get(indexCalc.get(v0));
  }

  @Override
  public Float get(int v0, int v1) {
    Preconditions.checkArgument(this.rank == 2);
    return storageF.get(indexCalc.get(v0, v1));
  }

  @Override
  public Float get(int v0, int v1, int v2) {
    Preconditions.checkArgument(this.rank == 3);
    return storageF.get(indexCalc.get(v0, v1, v2));
  }

  @Override
  public Float get(int v0, int v1, int v2, int v3) {
    Preconditions.checkArgument(this.rank == 4);
    return storageF.get(indexCalc.get(v0, v1, v2, v3));
  }

  @Override
  public Float get(int v0, int v1, int v2, int v3, int v4) {
    Preconditions.checkArgument(this.rank == 5);
    return storageF.get(indexCalc.get(v0, v1, v2, v3, v4));
  }

  @Override
  public Float get(int[] element) {
    Preconditions.checkArgument(this.rank == element.length);
    return storageF.get(indexCalc.get(element));
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

}
