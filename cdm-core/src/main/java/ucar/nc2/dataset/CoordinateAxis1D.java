/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import ucar.array.ArrayType;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.Index;
import ucar.nc2.Group;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.unidata.util.Format;
import java.io.IOException;

/**
 * A 1-dimensional Coordinate Axis, this is a legacy class, use GridAxis1D for new code.
 * Values must be monotonic.
 * If CHAR valued, it will have rank 2, otherwise it will have rank 1.
 * If String or CHAR valued, only <i>getCoordName()</i> can be called.
 * <p>
 * If the coordinates are regularly spaced, <i>isRegular()</i> is true, and the values are equal to
 * <i>getStart()</i> + i * <i>getIncrement()</i>.
 * <p>
 * This will also set "cell bounds" for this axis. By default, the cell bounds are midway between the coordinates
 * values, and are therefore contiguous, and can be accessed though getCoordEdge(i).
 * <p>
 * The only way the bounds can be set is if the coordinate variable has an attribute "bounds" that points to another
 * variable bounds(ncoords,2), containing the cell bounds, and must lie between the coordinate values.
 * In this case isContiguous() is true when bounds1(i+1) == bounds2(i) for all i.
 */
public class CoordinateAxis1D extends CoordinateAxis {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordinateAxis1D.class);

  /**
   * The "name" of the ith coordinate. If nominal, this is all there is to a coordinate.
   * If numeric, this will return a String representation of the coordinate.
   *
   * @param index which one ?
   * @return the ith coordinate value as a String
   */
  public String getCoordName(int index) {
    doRead();
    if (isNumeric())
      return Format.d(getCoordValue(index), 5, 8);
    else
      return names[index];
  }

  /**
   * Get the ith coordinate value. This is the value of the coordinate axis at which
   * the data value is associated. These must be strictly monotonic.
   *
   * @param index which coordinate. Between 0 and getNumElements()-1 inclusive.
   * @return coordinate value.
   * @throws UnsupportedOperationException if !isNumeric()
   */
  public double getCoordValue(int index) {
    if (!isNumeric())
      throw new UnsupportedOperationException("CoordinateAxis1D.getCoordValue() on non-numeric");
    doRead();
    return coords[index];
  }

  /**
   * Get the ith coordinate edge. Exact only if isContiguous() is true, otherwise use getBound1() and getBound2().
   * This is the value where the underlying grid element switches
   * from "belonging to" coordinate value i-1 to "belonging to" coordinate value i.
   * In some grids, this may not be well defined, and so should be considered an
   * approximation or a visualization hint.
   * <p>
   * 
   * <pre>
   *  Coordinate edges must be strictly monotonic:
   *    coordEdge(0) < coordValue(0) < coordEdge(1) < coordValue(1) ...
   *    ... coordEdge(i) < coordValue(i) < coordEdge(i+1) < coordValue(i+1) ...
   *    ... coordEdge(n-1) < coordValue(n-1) < coordEdge(n)
   * </pre>
   *
   * @param index which coordinate. Between 0 and getNumElements() inclusive.
   * @return coordinate edge.
   * @throws UnsupportedOperationException if !isNumeric()
   */
  public double getCoordEdge(int index) {
    if (!isNumeric())
      throw new UnsupportedOperationException("CoordinateAxis1D.getCoordEdge() on non-numeric");
    if (!wasBoundsDone)
      makeBounds();
    return edge[index];
  }

  /**
   * Get the coordinate values as a double array.
   *
   * @return coordinate value.
   * @throws UnsupportedOperationException if !isNumeric()
   */
  public double[] getCoordValues() {
    if (!isNumeric())
      throw new UnsupportedOperationException("CoordinateAxis1D.getCoordValues() on non-numeric");
    doRead();
    return coords.clone();
  }

  /**
   * Get the coordinate edges as a double array.
   * Exact only if isContiguous() is true, otherwise use getBound1() and getBound2().
   *
   * @return coordinate edges.
   * @throws UnsupportedOperationException if !isNumeric()
   */
  public double[] getCoordEdges() {
    if (!isNumeric())
      throw new UnsupportedOperationException("CoordinateAxis1D.getCoordEdges() on non-numeric");
    if (!wasBoundsDone)
      makeBounds();
    return edge.clone();
  }

  /**
   * If the edges are contiguous (true) or disjoint (false).
   * Caution: many datasets do not explicitly specify this info, this is often a guess; default is true.
   */
  public boolean isContiguous() {
    if (!wasBoundsDone)
      makeBounds(); // this sets isContiguous
    return isContiguous;
  }

  ///////////////////////////////////////////////

  /**
   * If this coordinate has interval values.
   * If so, then one should use getBound1, getBound2, and not getCoordEdges()
   *
   * @return true if coordinate has interval values
   */
  public boolean isInterval() {
    if (!wasBoundsDone)
      makeBounds(); // this sets isInterval
    return isInterval;
  }

  /**
   * Get the coordinate bound1 as a double array.
   * bound1[i] # coordValue[i] # bound2[i], where # is < if increasing (bound1[i] < bound1[i+1])
   * else < if decreasing.
   *
   * @return coordinate bound1.
   * @throws UnsupportedOperationException if !isNumeric()
   */
  public double[] getBound1() {
    if (!isNumeric())
      throw new UnsupportedOperationException("CoordinateAxis1D.getBound1() on non-numeric");
    if (!wasBoundsDone)
      makeBounds();
    if (bound1 == null)
      makeBoundsFromEdges();
    assert bound1 != null;
    return bound1.clone();
  }

  /**
   * Get the coordinate bound1 as a double array.
   * bound1[i] # coordValue[i] # bound2[i], where # is < if increasing (bound1[i] < bound1[i+1])
   * else < if decreasing.
   *
   * @return coordinate bound2.
   * @throws UnsupportedOperationException if !isNumeric()
   */
  public double[] getBound2() {
    if (!isNumeric())
      throw new UnsupportedOperationException("CoordinateAxis1D.getBound2() on non-numeric");
    if (!wasBoundsDone)
      makeBounds();
    if (bound2 == null)
      makeBoundsFromEdges();
    assert bound2 != null;
    return bound2.clone();
  }

  /**
   * Get the coordinate bounds for the ith coordinate.
   * Can use this for isContiguous() true or false.
   *
   * @param i coordinate index
   * @return double[2] edges for ith coordinate
   */
  public double[] getCoordBounds(int i) {
    if (!wasBoundsDone)
      makeBounds();

    double[] e = new double[2];
    if (isContiguous()) {
      e[0] = getCoordEdge(i);
      e[1] = getCoordEdge(i + 1);
    } else {
      e[0] = bound1[i];
      e[1] = bound2[i];
    }
    return e;
  }

  /**
   * Given a coordinate value, find what grid element contains it.
   * This means that
   * 
   * <pre>
   * edge[i] <= value < edge[i+1] (if values are ascending)
   * edge[i] > value >= edge[i+1] (if values are descending)
   * </pre>
   *
   * @param coordVal position in this coordinate system
   * @return index of grid point containing it, or -1 if outside grid area
   */
  public int findCoordElement(double coordVal) {
    if (!isNumeric())
      throw new UnsupportedOperationException("CoordinateAxis.findCoordElement() on non-numeric");

    if (isRegular())
      return findCoordElementRegular(coordVal, false);
    if (isContiguous())
      return findCoordElementIrregular(coordVal, false);
    else
      return findCoordElementNonContiguous(coordVal, false);
  }

  /**
   * Given a coordinate position, find what grid element contains it, or is closest to it.
   *
   * @param coordVal position in this coordinate system
   * @return index of grid point containing it, or best estimate of closest grid interval.
   */
  public int findCoordElementBounded(double coordVal) {
    if (!isNumeric())
      throw new UnsupportedOperationException("CoordinateAxis.findCoordElementBounded() on non-numeric");

    // the scalar or len-1 case:
    if (this.getSize() == 1)
      return 0;

    if (isRegular())
      return findCoordElementRegular(coordVal, true);
    if (isContiguous())
      return findCoordElementIrregular(coordVal, true);
    else
      return findCoordElementNonContiguous(coordVal, true);
  }

  //////////////////////////////////////////////////////////////////
  // following is from Jon Blower's ncWMS
  // faster routines for coordValue -> index search
  // significantly modified

  /**
   * Optimize the regular case
   * Gets the index of the given point. Uses index = (value - start) / stride,
   * hence this is faster than an exhaustive search.
   * from jon blower's ncWMS.
   *
   * @param coordValue The value along this coordinate axis
   * @param bounded if false and not in range, return -1, else nearest index
   * @return the index that is nearest to this point, or -1 if the point is
   *         out of range for the axis
   */
  private int findCoordElementRegular(double coordValue, boolean bounded) {
    int n = (int) this.getSize();

    // the scalar or len-1 case:
    if (this.getSize() == 1) {
      return 0;
    }

    double distance = coordValue - this.start;
    double exactNumSteps = distance / this.increment;
    int index = (int) Math.round(exactNumSteps);
    if (index < 0)
      return bounded ? 0 : -1;
    else if (index >= n)
      return bounded ? n - 1 : -1;
    return index;
  }

  /**
   * Performs a binary search to find the index of the element of the array
   * whose value is contained in the interval, so must be contiguous.
   *
   * @param target The value to search for
   * @param bounded if false, and not in range, return -1, else nearest index
   * @return the index of the element in values whose value is closest to target,
   *         or -1 if the target is out of range
   */
  private int findCoordElementIrregular(double target, boolean bounded) {
    int n = (int) this.getSize();
    int low = 0;
    int high = n;

    if (isAscending) {
      // Check that the point is within range
      if (target < this.edge[low])
        return bounded ? 0 : -1;
      else if (target > this.edge[high])
        return bounded ? n - 1 : -1;

      // do a binary search to find the nearest index
      int mid;
      while (high > low + 1) {
        mid = (low + high) / 2;
        double midVal = this.edge[mid];
        if (midVal == target)
          return mid;
        else if (midVal < target)
          low = mid;
        else
          high = mid;
      }

      return low;

    } else {

      // Check that the point is within range
      if (target > this.edge[low])
        return bounded ? 0 : -1;
      else if (target < this.edge[high])
        return bounded ? n - 1 : -1;

      // do a binary search to find the nearest index
      int mid;
      while (high > low + 1) {
        mid = (low + high) / 2;
        double midVal = this.edge[mid];
        if (midVal == target)
          return mid;
        else if (midVal < target)
          high = mid;
        else
          low = mid;
      }

      return high - 1;
    }
  }

  /**
   * Given a coordinate position, find what grid element contains it.
   * Only use if isContiguous() == false
   * This algorithm does a linear search in the bound1[] amd bound2[] array.
   * <p>
   * This means that
   * 
   * <pre>
   * edge[i] <= pos < edge[i+1] (if values are ascending)
   * edge[i] > pos >= edge[i+1] (if values are descending)
   * </pre>
   *
   * @param target The value to search for
   * @param bounded if false, and not in range, return -1, else nearest index
   * @return the index of the element in values whose value is closest to target,
   *         or -1 if the target is out of range
   */
  private int findCoordElementNonContiguous(double target, boolean bounded) {

    double[] bounds1 = getBound1();
    double[] bounds2 = getBound2();
    int n = bounds1.length;

    if (isAscending) {
      // Check that the point is within range
      if (target < bounds1[0])
        return bounded ? 0 : -1;
      else if (target > bounds2[n - 1])
        return bounded ? n - 1 : -1;

      int[] idx = findSingleHit(bounds1, bounds2, target);
      if (idx[0] == 0 && !bounded)
        return -1; // no hits
      if (idx[0] == 1)
        return idx[1]; // one hit

      // multiple hits = choose closest to the midpoint i guess
      return findClosest(coords, target);

    } else {

      // Check that the point is within range
      if (target > bounds1[0])
        return bounded ? 0 : -1;
      else if (target < bounds2[n - 1])
        return bounded ? n - 1 : -1;

      int[] idx = findSingleHit(bounds2, bounds1, target);
      if (idx[0] == 0 && !bounded)
        return -1; // no hits
      if (idx[0] == 1)
        return idx[1];

      // multiple hits = choose closest to the midpoint i guess
      return findClosest(getCoordValues(), target);
    }
  }

  // return index if only one match, else -1
  private int[] findSingleHit(double[] low, double[] high, double target) {
    int hits = 0;
    int idxFound = -1;
    int n = low.length;
    for (int i = 0; i < n; i++) {
      if ((low[i] <= target) && (target <= high[i])) {
        hits++;
        idxFound = i;
      }
    }
    return new int[] {hits, idxFound};
  }

  // return index of closest value to target
  private int findClosest(double[] values, double target) {
    double minDiff = Double.MAX_VALUE;
    int idxFound = -1;
    int n = values.length;
    for (int i = 0; i < n; i++) {
      double diff = Math.abs(values[i] - target);
      if (diff < minDiff) {
        minDiff = diff;
        idxFound = i;
      }
    }
    return idxFound;
  }

  ///////////////////////////////////////////////////////////////////////////////
  // check if Regular

  /**
   * Get increment value if isRegular()
   *
   * @return increment value if isRegular()
   */
  public double getIncrement() {
    calcIsRegular();
    return increment;
  }

  /**
   * If true, then value(i) = <i>getStart()</i> + i * <i>getIncrement()</i>.
   *
   * @return if evenly spaced.
   */
  public boolean isRegular() {
    calcIsRegular();
    return isRegular;
  }

  private void calcIsRegular() {
    if (wasCalcRegular)
      return;
    doRead();

    if (!isNumeric())
      isRegular = false;
    else if (getSize() < 2)
      isRegular = true;
    else {
      start = getCoordValue(0);
      int n = (int) getSize();
      increment = (getCoordValue(n - 1) - getCoordValue(0)) / (n - 1);
      isRegular = true;
      for (int i = 1; i < getSize(); i++)
        if (!ucar.nc2.util.Misc.nearlyEquals(getCoordValue(i) - getCoordValue(i - 1), increment, 5.0e-3)) {
          isRegular = false;
          break;
        }
    }
    wasCalcRegular = true;
  }

  ///////////////////////////////////////////////////////////////////////////////


  private void doRead() {
    if (wasRead) {
      return;
    }
    if (isNumeric()) {
      readValues();
      wasRead = true;
      if (getSize() < 2)
        isAscending = true;
      else
        isAscending = getCoordValue(0) < getCoordValue(1);
      // calcIsRegular(); */
    } else if (getArrayType() == ArrayType.STRING) {
      readStringValues();
      wasRead = true;
    } else {
      readCharValues();
      wasRead = true;
    }
  }

  // only used if String
  private void readStringValues() {
    int count = 0;
    Array<String> data;
    try {
      data = (Array<String>) readArray();
    } catch (IOException ioe) {
      log.error("Error reading string coordinate values ", ioe);
      throw new IllegalStateException(ioe);
    }

    names = new String[(int) data.getSize()];
    for (String s : data) {
      names[count++] = s;
    }
  }

  private void readCharValues() {
    int count = 0;
    Array<Byte> data;
    try {
      data = (Array<Byte>) readArray();
    } catch (IOException ioe) {
      log.error("Error reading char coordinate values ", ioe);
      throw new IllegalStateException(ioe);
    }
    names = new String[(int) data.getSize()];
    Array<String> sdata = Arrays.makeStringsFromChar(data);
    for (String s : sdata) {
      names[count++] = s;
    }
  }

  protected void readValues() {
    Array<?> data;
    try {
      data = readArray();
      // if (!hasCachedData()) setCachedData(data, false); //cache data for subsequent reading
    } catch (IOException ioe) {
      log.error("Error reading coordinate values ", ioe);
      throw new IllegalStateException(ioe);
    }
    Array<Double> ddata = Arrays.toDouble(data);
    Arrays.copyPrimitiveArray(ddata);
    coords = (double[]) Arrays.copyPrimitiveArray(ddata);
  }

  /**
   * Calculate bounds, set isInterval, isContiguous
   */
  private void makeBounds() {
    doRead();
    if (isNumeric()) {
      if (!makeBoundsFromAux()) {
        makeEdges();
      }
    }
    wasBoundsDone = true;
  }

  private boolean makeBoundsFromAux() {
    String boundsVarName = attributes().findAttributeString(CF.BOUNDS, null);
    if (boundsVarName == null) {
      return false;
    }
    VariableDS boundsVar = (VariableDS) getParentGroup().findVariableLocal(boundsVarName);
    if (null == boundsVar)
      return false;
    if (2 != boundsVar.getRank())
      return false;

    if (getDimension(0) != boundsVar.getDimension(0))
      return false;
    if (2 != boundsVar.getDimension(1).getLength())
      return false;

    Array<Number> data;
    try {
      data = (Array<Number>) boundsVar.readArray();
    } catch (IOException e) {
      log.warn("CoordinateAxis1D.hasBounds read failed ", e);
      return false;
    }

    assert (data.getRank() == 2) && (data.getShape()[1] == 2) : "incorrect shape data for variable " + boundsVar;

    // extract the bounds
    int n = getShape(0);
    double[] value1 = new double[n];
    double[] value2 = new double[n];
    Index ima = data.getIndex();
    for (int i = 0; i < n; i++) {
      ima.set0(i);
      value1[i] = data.get(ima.set1(0)).doubleValue();
      value2[i] = data.get(ima.set1(1)).doubleValue();
    }

    // decide if they are contiguous
    boolean contig = true;
    for (int i = 0; i < n - 1; i++) {
      if (!ucar.nc2.util.Misc.nearlyEquals(value1[i + 1], value2[i]))
        contig = false;
    }

    if (contig) {
      edge = new double[n + 1];
      edge[0] = value1[0];
      System.arraycopy(value2, 0, edge, 1, n + 1 - 1);
    } else { // what does edge mean when not contiguous ??
      edge = new double[n + 1];
      edge[0] = value1[0];
      for (int i = 1; i < n; i++)
        edge[i] = (value1[i] + value2[i - 1]) / 2;
      edge[n] = value2[n - 1];
      isContiguous = false;
    }

    bound1 = value1;
    bound2 = value2;
    isInterval = true;

    return true;
  }

  private void makeEdges() {
    int size = (int) getSize();
    edge = new double[size + 1];
    if (size < 1)
      return;
    for (int i = 1; i < size; i++)
      edge[i] = (coords[i - 1] + coords[i]) / 2;
    edge[0] = coords[0] - (edge[1] - coords[0]);
    edge[size] = coords[size - 1] + (coords[size - 1] - edge[size - 1]);
    isContiguous = true;
  }

  private void makeBoundsFromEdges() {
    int size = (int) getSize();
    if (size == 0)
      return;

    bound1 = new double[size];
    bound2 = new double[size];
    for (int i = 0; i < size; i++) {
      bound1[i] = edge[i];
      bound2[i] = edge[i + 1];
    }

    // flip if needed
    if (bound1[0] > bound2[0]) {
      double[] temp = bound1;
      bound1 = bound2;
      bound2 = temp;
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  protected boolean isContiguous;

  // These are all calculated, I think?
  protected boolean wasRead; // have the data values been read
  private boolean wasBoundsDone; // have we created the bounds arrays if exists ?
  private boolean isInterval; // is this an interval coordinates - then should use bounds
  private boolean isAscending;

  // read in on doRead()
  protected double[] coords; // coordinate values, must be between edges
  private String[] names; // only set if String or char values

  // defer making until asked, use makeBounds()
  private double[] edge; // n+1 edges, edge[k] < midpoint[k] < edge[k+1]
  private double[] bound1, bound2; // may be contiguous or not

  private boolean wasCalcRegular; // have we checked if the data is regularly spaced ?
  private boolean isRegular;
  private double start, increment;

  protected CoordinateAxis1D(Builder<?> builder, Group parentGroup) {
    super(builder, parentGroup);
    this.isContiguous = builder.isContiguous;
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the Builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    b.setIsContiguous(this.isContiguous);
    return (CoordinateAxis1D.Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  @Deprecated
  public static abstract class Builder<T extends Builder<T>> extends CoordinateAxis.Builder<T> {
    private boolean built;
    protected boolean isContiguous = true;

    protected abstract T self();

    public T setIsContiguous(boolean isContiguous) {
      this.isContiguous = isContiguous;
      return self();
    }

    public CoordinateAxis1D build(Group parentGroup) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new CoordinateAxis1D(this, parentGroup);
    }
  }

}
