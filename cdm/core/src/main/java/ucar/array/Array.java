/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.base.Objects;
import java.util.Iterator;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.DataType;
import ucar.ma2.Section;

/** Superclass for implementations of multidimensional arrays. */
@Immutable
public abstract class Array<T> implements Iterable<T> {

  /** Iterates in canonical order over all the elements of the Array. */
  @Override
  public abstract Iterator<T> iterator();

  abstract Iterator<T> fastIterator();

  /**
   * Get the element indicated by the list of multidimensional indices.
   * 
   * @param index list of indices, one for each dimension. For vlen, the last is ignored.
   */
  public abstract T get(int... index);

  /**
   * Get the element indicated by Index.
   * 
   * @param index multidimensional indices.
   */
  public abstract T get(Index index);

  /** Get the first element of the Array */
  public T getScalar() {
    return this.get(this.getIndex());
  }

  /** The datatype for this array */
  public DataType getDataType() {
    return this.dataType;
  }

  /** Is variable length and will be represented by Vlen\<T\> */
  public boolean isVlen() {
    return false;
  }

  /** An Index that can be used instead of int[], with the same rank as this Array. */
  public Index getIndex() {
    return new Index(this.rank, this.indexFn);
  }

  /** Get the number of dimensions of the array. */
  public int getRank() {
    return rank;
  }

  /** Get the shape: length of array in each dimension. */
  public int[] getShape() {
    return indexFn.getShape();
  }

  /** Get the section: list of Ranges, one for each dimension. */
  public Section getSection() {
    return indexFn.getSection();
  }

  /** Get the total number of elements in the array. Excludes vlen dimensins. */
  public long length() {
    return indexFn.length();
  }

  @Override
  public String toString() {
    StringBuilder sbuff = new StringBuilder();
    boolean first = true;
    for (T value : this) {
      if (!first) {
        sbuff.append(", ");
      }
      sbuff.append(value);
      first = false;
    }
    return sbuff.toString();
  }

  /** Equal if the type and indexFn are equal, doesnt test the contents. TODO */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Array)) {
      return false;
    }
    Array<?> array = (Array<?>) o;
    return getRank() == array.getRank() && getDataType() == array.getDataType()
        && Objects.equal(indexFn, array.indexFn);
  }

  /** Consistent with equals. */
  @Override
  public int hashCode() {
    return Objects.hashCode(getDataType(), indexFn, getRank());
  }

  //////////////////////////////////////////////////////////
  // package private

  final DataType dataType;
  final IndexFn indexFn;
  final int rank;

  Array(DataType dataType, int[] shape) {
    this.dataType = dataType;
    this.rank = shape.length;
    this.indexFn = IndexFn.builder(shape).build();
  }

  Array(DataType dataType, IndexFn indexFn) {
    this.dataType = dataType;
    this.rank = indexFn.getRank();
    this.indexFn = indexFn;
  }

  abstract void arraycopy(int srcPos, Object dest, int destPos, long length);

  /** Get underlying storage. */
  abstract Storage<T> storage();

  /** Get the IndexFn for this Array. */
  IndexFn indexFn() {
    return indexFn;
  }

  /**
   * Create new Array with given IndexFn and the same backing store
   *
   * @param view use this IndexFn
   * @return a view of the Array using the given IndexFn
   */
  abstract Array<T> createView(IndexFn view);

}

