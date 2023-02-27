/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.dap4lib;

import dap4.core.util.DapException;
import dap4.core.util.Slice;
import ucar.ma2.Index;

import java.util.ArrayList;
import java.util.List;

public class D4Index extends ucar.ma2.Index {

  public static final D4Index SCALAR = new D4Index(0);

  //////////////////////////////////////////////////
  // Static methods

  /**
   * Given an offset (single index) and a set of dimensions
   * compute the set of dimension indices that correspond
   * to the offset.
   */
  static public Index offsetToIndex(int offset, int[] shape) {
    // offset = d3*(d2*(d1*(x1))+x2)+x3
    int[] indices = new int[shape.length];
    for (int i = shape.length - 1; i >= 0; i--) {
      indices[i] = offset % shape[i];
      offset = (offset - indices[i]) / shape[i];
    }
    return new Index(indices, shape);
  }

  /**
   * Convert ucar.ma2.Index to list of slices
   * 
   * @param indices to convert
   * @return list of corresponding slices
   */

  static public List<Slice> indexToSlices(ucar.ma2.Index indices) throws DapException {
    // short circuit the scalar case
    int rank = indices.getRank();
    if (rank == 0)
      return Slice.SCALARSLICES;
    List<Slice> slices = new ArrayList<>(rank);
    int[] counter = indices.getCurrentCounter();
    for (int i = 0; i < rank; i++) {
      int isize = counter[i];
      slices.add(new Slice(isize, isize + 1, 1, indices.getShape(i)));
    }
    return slices;
  }

  /**
   * If a set of slices refers to a single position,
   * then return the corresponding Index. Otherwise,
   * throw Exception.
   *
   * @param slices
   * @return Index corresponding to slices
   * @throws DapException
   */
  static public D4Index slicesToIndex(List<Slice> slices) throws DapException {
    int[] positions = new int[slices.size()];
    int[] dimsizes = new int[slices.size()];
    for (int i = 0; i < positions.length; i++) {
      Slice s = slices.get(i);
      if (s.getCount() != 1)
        throw new DapException("Attempt to convert non-singleton sliceset to index");
      positions[i] = s.getFirst();
      dimsizes[i] = s.getMax();
    }
    D4Index result = new D4Index(dimsizes);
    result.set(positions);
    return result;
  }

  //////////////////////////////////////////////////
  // Constructor(s)

  public D4Index(int rank) {
    super(rank);
  }

  public D4Index(int[] _shape) {
    super(_shape);
  }

  public D4Index(D4Index index) {
    this(index.getRank());
    if (this.rank > 0) {
      System.arraycopy(index.getCurrentCounter(), 0, this.current, 0, this.rank);
      System.arraycopy(index.getShape(), 0, this.getShape(), 0, this.rank);
    }
  }

  public D4Index(int[] indices, int[] dimsizes) {
    super(dimsizes);
    if (this.rank > 0) {
      System.arraycopy(indices, 0, this.current, 0, this.rank);
    }
  }

  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append('[');
    for (int i = 0; i < this.rank; i++) {
      if (i > 0)
        buf.append(',');
      buf.append(this.current[i]);
      buf.append('/');
      buf.append(this.shape[i]);
    }
    buf.append("](");
    buf.append(this.index());
    buf.append(")");
    return buf.toString();
  }

  /**
   * Compute the linear index
   * from the current odometer indices.
   * Not quite the same as super.currentElement(),
   * which does not use shape[], but does use the super's
   * "unexpected" notion of stride, although result may be same.
   */
  public int index() {
    int offset = this.offset;
    int[] cur = getCurrentCounter();
    int[] sh = getShape();
    for (int i = 0; i < cur.length; i++) {
      offset *= sh[i];
      offset += cur[i];
    }
    return offset;
  }

  public int getCurrentCounter(int i) {
    if (i < 0 || i >= this.rank)
      throw new IllegalArgumentException();
    return getCurrentCounter()[i];
  }

  public boolean isScalar() {
    return (rank == 0 && getCurrentCounter().length == 1 && index() == 1);
  }

}
