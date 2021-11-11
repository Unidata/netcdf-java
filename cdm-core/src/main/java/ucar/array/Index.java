/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Preconditions;

/**
 * Indexes for Arrays. An Index refers to a particular element of an array.
 * This is a generalization of index as int[].
 * A scalar has effectively int[1] = {1}
 * Mutable.
 */
public class Index {
  /** An Index of the given rank. */
  public static Index ofRank(int rank) {
    if (rank == 0) {
      rank = 1;
    }
    return new Index(new int[rank]);
  }

  /** An Index with the current value passed in. */
  public static Index of(int... current) {
    return new Index(current);
  }

  /////////////////////////////////////////////////////
  private int[] current; // current element's index

  Index(int[] index) {
    this.current = index;
  }

  /** Copy constructor. */
  public Index(Index from) {
    this.current = new int[from.current.length];
    System.arraycopy(from.current, 0, this.current, 0, from.current.length);
  }

  /**
   * Increment the ith dimension.
   * 
   * @param dim which dimension
   * @return the incremented Index.
   */
  public Index incr(int dim) {
    Preconditions.checkArgument(dim < this.current.length);
    setDim(dim, this.current[dim] + 1);
    return this;
  }

  /** Get the current index as int[] . */
  public int[] getCurrentIndex() {
    return current;
  }

  /**
   * Set current element at dimension dim
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
    if (index.length != current.length) {
      throw new ArrayIndexOutOfBoundsException(
          String.format("Number of indices (%d) must equal rank (%d)", index.length, current.length));
    }
    if (current.length == 0) {
      return this;
    }
    System.arraycopy(index, 0, current, 0, index.length);
    return this;
  }

}
