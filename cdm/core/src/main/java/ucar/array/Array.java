/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import java.util.Iterator;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.DataType;
import ucar.ma2.Section;

/** Superclass for implementations of multidimensional arrays. */
@Immutable
public abstract class Array<T> implements Iterable<T> {
  final DataType dataType;
  final Strides indexCalc;
  final int rank;

  Array(DataType dataType, int[] shape) {
    this.dataType = dataType;
    this.rank = shape.length;
    this.indexCalc = Strides.builder(shape).build();
  }

  Array(DataType dataType, Strides shape) {
    this.dataType = dataType;
    this.rank = shape.getRank();
    this.indexCalc = shape;
  }

  @Override
  public abstract Iterator<T> iterator();

  public abstract Iterator<T> fastIterator();

  /**
   * Get the element indicated by the list of multidimensional indices.
   * 
   * @param index list of indices, one for each dimension. For vlen, the last is ignored.
   */
  public abstract T get(int... index);

  /**
   * Get the element indicated by Index.
   * 
   * @param index list of indices, one for each dimension. For vlen, the last is ignored.
   */
  public abstract T get(Index index);

  /** Return the datatype for this array */
  public DataType getDataType() {
    return this.dataType;
  }

  /** An Index that can be used instead of int[], with the same rank as this Array. */
  public Index getIndex() {
    return new Index(this.rank);
  }

  /** Get the number of dimensions of the array. */
  public int getRank() {
    return rank;
  }

  /** Get the shape: length of array in each dimension. */
  public int[] getShape() {
    return indexCalc.getShape();
  }

  /** Get the section: list of Ranges, one for each dimension. */
  public Section getSection() {
    return indexCalc.getSection();
  }

  /** Get the total number of elements in the array. */
  public long getSize() {
    return indexCalc.getSize();
  }

  /** Get the total number of bytes in the array. */
  public long getSizeBytes() {
    return indexCalc.getSize() * dataType.getSize();
  }

  /**
   * Find whether the underlying data should be interpreted as unsigned.
   * Only affects byte, short, int, and long.
   * When true, conversions to wider types are handled correctly.
   *
   * @return true if the data is an unsigned integer type.
   */
  public boolean isUnsigned() {
    return dataType.isUnsigned();
  }

  public boolean isVlen() {
    return indexCalc.isVlen();
  }

  // Mimic of System.arraycopy(Object src, int srcPos, Object dest, int destPos, int length);
  abstract void arraycopy(int srcPos, Object dest, int destPos, long length);

  /** Get underlying storage. */
  abstract Storage<T> storage();

  /** Get the Strides for this Array. */
  Strides strides() {
    return indexCalc;
  }

  /**
   * Create new Array with given Strides and the same backing store
   *
   * @param index use this Strides
   * @return a view of the Array using the given Index
   */
  abstract Array<T> createView(Strides index);

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

}

