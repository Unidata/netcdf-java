/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.array.Section;
import ucar.nc2.Group;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A 2-dimensional numeric Coordinate Axis. Must be invertible meaning, roughly, that
 * if you draw lines connecting the points, none would cross.
 * 
 * @deprecated use GridAxis2D
 */
@Deprecated
public class CoordinateAxis2D extends CoordinateAxis {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordinateAxis2D.class);

  /**
   * Get the coordinate value at the i, j index.
   *
   * @param i index 0 (fastest varying, right-most)
   * @param j index 1
   * @return midpoint.get(j, i).
   */
  public double getCoordValue(int j, int i) {
    if (coords == null)
      doRead();
    return coords.get(j, i);
  }

  private void doRead() {
    Array data;
    try {
      data = readArray();
      // if (!hasCachedData()) setCachedData(data, false); //cache data for subsequent reading
    } catch (IOException ioe) {
      log.error("Error reading coordinate values " + ioe);
      throw new IllegalStateException(ioe);
    }

    if (data.getRank() != 2)
      throw new IllegalArgumentException("must be 2D");

    coords = Arrays.toDouble(data);

    if (this.axisType == AxisType.Lon) {
      coords = makeConnectedLon(coords);
    }
  }

  public boolean isInterval() {
    if (!intervalWasComputed) {
      isInterval = computeIsInterval();
    }
    return isInterval;
  }

  private Array<Double> makeConnectedLon(Array<Double> mid) {
    int[] shape = mid.getShape();
    int ny = shape[0];
    int nx = shape[1];
    double[] result = new double[nx * ny];

    // first row
    double connect = mid.get(0, 0);
    for (int i = 1; i < nx; i++) {
      connect = connectLon(connect, mid.get(0, i));
      result[i] = connect;
    }

    // other rows
    for (int j = 1; j < ny; j++) {
      connect = mid.get(j - 1, 0);
      for (int i = 0; i < nx; i++) {
        connect = connectLon(connect, mid.get(j, i));
        result[j * nx + i] = connect;
      }
    }
    return Arrays.factory(ArrayType.DOUBLE, shape, result);
  }

  private static final double MAX_JUMP = 100.0; // larger than you would ever expect

  private static double connectLon(double connect, double val) {
    if (Double.isNaN(connect) || Double.isNaN(val)) {
      return val;
    }
    double diff = val - connect;
    if (Math.abs(diff) < MAX_JUMP) {
      return val; // common case fast
    }
    // we have to add or subtract 360
    double result = diff > 0 ? val - 360 : val + 360;
    double diff2 = connect - result;
    if ((Math.abs(diff2)) < Math.abs(diff)) {
      val = result;
    }
    return val;
  }

  /**
   * Create a new CoordinateAxis2D as a section of this CoordinateAxis2D.
   *
   * @param r1 the section on the first index
   * @param r2 the section on the second index
   * @return a section of this CoordinateAxis2D
   * @throws InvalidRangeException if specified Ranges are invalid
   */
  public CoordinateAxis2D section(Range r1, Range r2) throws InvalidRangeException {
    List<Range> section = new ArrayList<>();
    section.add(r1);
    section.add(r2);
    return (CoordinateAxis2D) section(new Section(section));
  }

  public Array<Double> getCoordValuesArray() {
    if (coords == null) {
      doRead();
    }
    return coords;
  }

  /**
   * Only call if isInterval()
   *
   * @return bounds array pr null if not an interval
   */
  public Array<Double> getCoordBoundsArray() {
    if (coords == null) {
      doRead();
    }
    return makeBoundsFromAux();
  }

  ///////////////////////////////////////////////////////////////////////////////
  // bounds calculations

  /** makes bounds from a CF.BOUNDS variable. */
  private Array<Double> makeBoundsFromAux() {
    if (!computeIsInterval())
      return null;
    String boundsVarName = attributes().findAttributeString(CF.BOUNDS, null);
    if (boundsVarName == null) {
      return null;
    }
    VariableDS boundsVar = (VariableDS) getParentGroup().findVariableLocal(boundsVarName);

    Array<Number> data;
    try {
      // boundsVar.setUseNaNs(false); // missing values not allowed
      data = (Array<Number>) boundsVar.readArray();
    } catch (IOException e) {
      log.warn("CoordinateAxis2D.makeBoundsFromAux read failed ", e);
      return null;
    }

    assert (data.getRank() == 3) && (data.getShape()[2] == 2) : "incorrect shape data for variable " + boundsVar;
    return Arrays.toDouble(data);
  }

  private boolean computeIsInterval() {
    intervalWasComputed = true;
    String boundsVarName = attributes().findAttributeString(CF.BOUNDS, null);
    if (boundsVarName == null) {
      return false;
    }
    VariableDS boundsVar = (VariableDS) getParentGroup().findVariableLocal(boundsVarName);
    if (null == boundsVar)
      return false;
    if (3 != boundsVar.getRank())
      return false;
    if (getDimension(0) != boundsVar.getDimension(0))
      return false;
    if (getDimension(1) != boundsVar.getDimension(1))
      return false;
    return 2 == boundsVar.getDimension(2).getLength();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  // These are all calculated, I think?
  private Array<Double> coords;
  private boolean isInterval;
  private boolean intervalWasComputed;

  protected CoordinateAxis2D(Builder<?> builder, Group parentGroup) {
    super(builder, parentGroup);
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    return (Builder<?>) super.addLocalFieldsToBuilder(b);

  }

  /**
   * Get Builder for this class that allows subclassing.
   * 
   * @see "https://community.oracle.com/blogs/emcmanus/2010/10/24/using-builder-pattern-subclasses"
   */
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

    protected abstract T self();

    public CoordinateAxis2D build(Group parentGroup) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new CoordinateAxis2D(this, parentGroup);
    }
  }
}
