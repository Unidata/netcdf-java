/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;

/**
 * Indexes for Multidimensional arrays. An Index refers to a particular element of an array.
 * This is a generalization of index as int[].
 * TODO: Some of these methods, maybe the whole class is to ease the transition from earlier versions.
 * TODO: evaluate how useful this is.
 */
public class Index {
  private final int[] current; // current element's index

  protected Index(int rank) {
    current = new int[rank];
  }

  /** Get the current index as int[] . */
  public int[] getCurrentIndex() {
    return current;
  }

  /**
   * Set the current element's index.
   *
   * @param index set current value to these values
   * @return this, so you can use A.get(i.set(i))
   * @throws ArrayIndexOutOfBoundsException if index.length != rank.
   */
  public Index set(int[] index) {
    if (index.length > current.length)
      throw new ArrayIndexOutOfBoundsException();
    if (current.length == 0)
      return this;
    System.arraycopy(index, 0, current, 0, index.length);
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

  /** set current element at dimension 0 to v0 */
  public Index set(int v0) {
    setDim(0, v0);
    return this;
  }

  /** set current element at dimension 0,1 to v0,v1 */
  public Index set(int v0, int v1) {
    setDim(0, v0);
    setDim(1, v1);
    return this;
  }

  /** set current element at dimension 0,1,2 to v0,v1,v2 */
  public Index set(int v0, int v1, int v2) {
    setDim(0, v0);
    setDim(1, v1);
    setDim(2, v2);
    return this;
  }

  /** set current element at dimension 0,1,2,3 to v0,v1,v2,v3 */
  public Index set(int v0, int v1, int v2, int v3) {
    setDim(0, v0);
    setDim(1, v1);
    setDim(2, v2);
    setDim(3, v3);
    return this;
  }

  /** set current element at dimension 0,1,2,3,4 to v0,v1,v2,v3,v4 */
  public Index set(int v0, int v1, int v2, int v3, int v4) {
    setDim(0, v0);
    setDim(1, v1);
    setDim(2, v2);
    setDim(3, v3);
    setDim(4, v4);
    return this;
  }

  /** set current element at dimension 0,1,2,3,4,5 to v0,v1,v2,v3,v4,v5 */
  public Index set(int v0, int v1, int v2, int v3, int v4, int v5) {
    setDim(0, v0);
    setDim(1, v1);
    setDim(2, v2);
    setDim(3, v3);
    setDim(4, v4);
    setDim(5, v5);
    return this;
  }

  /** set current element at dimension 0,1,2,3,4,5,6 to v0,v1,v2,v3,v4,v5,v6 */
  public Index set(int v0, int v1, int v2, int v3, int v4, int v5, int v6) {
    setDim(0, v0);
    setDim(1, v1);
    setDim(2, v2);
    setDim(3, v3);
    setDim(4, v4);
    setDim(5, v5);
    setDim(6, v6);
    return this;
  }

}
