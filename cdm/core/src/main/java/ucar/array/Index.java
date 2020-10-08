/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;

/**
 * Indexes for Arrays. An Index refers to a particular element of an array.
 * This is a generalization of index as int[].
 * TODO: Some of these methods, maybe the whole class is to ease the transition from earlier versions.
 * TODO: evaluate how useful this is.
 */
public class Index {
  private int[] current; // current element's index
  private final IndexFn indexFn;

  protected Index(int rank, IndexFn indexFn) {
    current = new int[rank];
    this.indexFn = indexFn;
  }

  /** Get the current index as int[] . */
  public int[] getCurrentIndex() {
    return current;
  }

  /** Set the current multidim index using 1-d element index. */
  public Index setElem(long elem) {
    this.current = indexFn.odometer(elem);
    return this;
  }

  /**
   * set current element at dimension dim to v
   *
   * @param dim set this dimension
   * @param value to this value
   */
  public Index setDim(int dim, int value) {
    Preconditions.checkArgument(dim < current.length);
    current[dim] = value;
    return this;
  }

  /** set current element at dimension 0 to v */
  public Index set0(int v) {
    setDim(0, v);
    return this;
  }

  /** set current element at dimension 1 to v */
  public Index set1(int v) {
    setDim(1, v);
    return this;
  }

  /** set current element at dimension 2 to v */
  public Index set2(int v) {
    setDim(2, v);
    return this;
  }

  /** set current element at dimension 3 to v */
  public Index set3(int v) {
    setDim(3, v);
    return this;
  }

  /** set current element at dimension 4 to v */
  public Index set4(int v) {
    setDim(4, v);
    return this;
  }

  /** set current element at dimension 5 to v */
  public Index set5(int v) {
    setDim(5, v);
    return this;
  }

  /** set current element at dimension 6 to v */
  public Index set6(int v) {
    setDim(6, v);
    return this;
  }

  /**
   * Set the current element's index.
   *
   * @param index set current value to these values, number of values must be equal to the rank.
   * @throws ArrayIndexOutOfBoundsException if index.length != rank.
   */
  public Index set(int... index) {
    if (index.length != current.length)
      throw new ArrayIndexOutOfBoundsException();
    if (current.length == 0)
      return this;
    System.arraycopy(index, 0, current, 0, index.length);
    return this;
  }

}
