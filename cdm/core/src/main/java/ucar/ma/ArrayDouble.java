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
public class ArrayDouble extends ucar.ma.Array<Double> {

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
  public Double get() {
    Preconditions.checkArgument(this.rank == 0);
    return storageD.get(0);
  }

  @Override
  public Double get(int v0) {
    Preconditions.checkArgument(this.rank == 1);
    return storageD.get(indexCalc.get(v0));
  }

  @Override
  public Double get(int v0, int v1) {
    Preconditions.checkArgument(this.rank == 2);
    return storageD.get(indexCalc.get(v0, v1));
  }

  @Override
  public Double get(int v0, int v1, int v2) {
    Preconditions.checkArgument(this.rank == 3);
    return storageD.get(indexCalc.get(v0, v1, v2));
  }

  @Override
  public Double get(int v0, int v1, int v2, int v3) {
    Preconditions.checkArgument(this.rank == 4);
    return storageD.get(indexCalc.get(v0, v1, v2, v3));
  }

  @Override
  public Double get(int v0, int v1, int v2, int v3, int v4) {
    Preconditions.checkArgument(this.rank == 5);
    return storageD.get(indexCalc.get(v0, v1, v2, v3, v4));
  }

  @Override
  public Double get(int[] element) {
    Preconditions.checkArgument(this.rank == element.length);
    return storageD.get(indexCalc.get(element));
  }

  @Override
  public Double get(Index index) {
    return get(index.getCurrentIndex());
  }

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

}
