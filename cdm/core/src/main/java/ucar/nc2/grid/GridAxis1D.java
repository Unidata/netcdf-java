/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.RangeIterator;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.internal.grid.GridAxis1DHelper;
import ucar.nc2.util.Indent;
import ucar.nc2.util.MinMax;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.*;

import static ucar.nc2.grid.GridAxis.Spacing.discontiguousInterval;

/** Grid CoordAxis 1D concrete case. */
@Immutable
public class GridAxis1D extends GridAxis {

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    GridAxis1D objects = (GridAxis1D) o;
    return ncoords == objects.ncoords && Double.compare(objects.startValue, startValue) == 0
        && Double.compare(objects.endValue, endValue) == 0 && Objects.equals(range, objects.range);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), ncoords, startValue, endValue, range);
  }

  @Override
  public void toString(Formatter f, Indent indent) {
    super.toString(f, indent);

    f.format("%snpts: %d [%f,%f] resolution=%f spacing=%s", indent, ncoords, startValue, endValue, resolution, spacing);
    f.format("%s range=%s isSubset=%s", indent, range, isSubset());
    f.format("%n");
  }


  public String getSummary() {
    Formatter f = new Formatter();
    f.format("start=%f end=%f %s %s resolution=%f", startValue, endValue, units, spacing, resolution);
    f.format(" (npts=%d)", ncoords);
    return f.toString();
  }

  ///////////////////////////////////////////////////////////////////

  public boolean isAscending() {
    switch (spacing) {
      case regularInterval:
      case regularPoint:
        return getResolution() > 0;

      case irregularPoint:
        return values[0] <= values[ncoords - 1];

      case contiguousInterval:
        return values[0] <= values[ncoords];

      case discontiguousInterval: // actually ambiguous
        return values[0] <= values[2 * ncoords - 1];
    }
    throw new IllegalStateException("unknown spacing" + spacing);
  }

  public MinMax getCoordEdgeMinMax() {
    if (spacing != discontiguousInterval) {
      double min = Math.min(getCoordEdge1(0), getCoordEdge2(ncoords - 1));
      double max = Math.max(getCoordEdge1(0), getCoordEdge2(ncoords - 1));
      return MinMax.create(min, max);
    } else {
      double max = -Double.MAX_VALUE;
      double min = Double.MAX_VALUE;
      for (int i = 0; i < ncoords; i++) {
        min = Math.min(min, getCoordEdge1(i));
        min = Math.min(min, getCoordEdge2(i));
        max = Math.max(max, getCoordEdge1(i));
        max = Math.max(max, getCoordEdge2(i));
      }
      return MinMax.create(min, max);
    }
  }

  public double getCoordMidpoint(int index) {
    if (index < 0 || index >= getNcoords())
      throw new IllegalArgumentException("Index out of range=" + index);

    switch (spacing) {
      case regularPoint:
        return startValue + index * getResolution();

      case irregularPoint:
        return values[index];

      case regularInterval:
        return startValue + (index + .5) * getResolution();

      case contiguousInterval:
      case discontiguousInterval:
        return (getCoordEdge1(index) + getCoordEdge2(index)) / 2;
    }
    throw new IllegalStateException("Unknown spacing=" + spacing);
  }

  public double getCoordEdge1(int index) {
    if (index < 0 || index >= getNcoords())
      throw new IllegalArgumentException("Index out of range=" + index);

    switch (spacing) {
      case regularPoint:
        return startValue + (index - .5) * getResolution();

      case regularInterval:
        return startValue + index * getResolution();

      case irregularPoint:
        if (index > 0)
          return (values[index - 1] + values[index]) / 2;
        else
          return values[0] - (values[1] - values[0]) / 2;

      case contiguousInterval:
        return values[index];

      case discontiguousInterval:
        return values[2 * index];
    }
    throw new IllegalStateException("Unknown spacing=" + spacing);
  }

  public double getCoordEdge2(int index) {
    if (index < 0 || index >= getNcoords())
      throw new IllegalArgumentException("Index out of range=" + index);

    switch (spacing) {
      case regularPoint:
        return startValue + (index + .5) * getResolution();

      case regularInterval:
        return startValue + (index + 1) * getResolution();

      case irregularPoint:
        if (index < ncoords - 1)
          return (values[index] + values[index + 1]) / 2;
        else
          return values[index] + (values[index] - values[index - 1]) / 2;

      case contiguousInterval:
        return values[index + 1];

      case discontiguousInterval:
        return values[2 * index + 1];
    }
    throw new IllegalStateException("Unknown spacing=" + spacing);
  }

  public CoordInterval getCoordInterval(int index) {
    return CoordInterval.create(getCoordEdge1(index), getCoordEdge2(index));
  }

  @Override
  public Array<Double> getCoordsAsArray() {
    double[] vals = new double[ncoords];
    for (int i = 0; i < ncoords; i++) {
      vals[i] = getCoordMidpoint(i);
    }

    Array<Double> result;
    if (dependenceType == DependenceType.scalar) {
      result = Arrays.factory(DataType.DOUBLE, new int[0], vals);
    } else {
      result = Arrays.factory(DataType.DOUBLE, new int[] {ncoords}, vals);
    }

    return result;
  }

  @Override
  public Array<Double> getCoordBoundsAsArray() {
    double[] vals = new double[2 * ncoords];
    int count = 0;
    for (int i = 0; i < ncoords; i++) {
      vals[count++] = getCoordEdge1(i);
      vals[count++] = getCoordEdge2(i);
    }
    return Arrays.factory(DataType.DOUBLE, new int[] {ncoords, 2}, vals);
  }

  /** The number of coordinates. Coord or Interval. */
  public int getNcoords() {
    return ncoords;
  }

  /** Starting value when spacing.isRegular(). Coord or Interval. */
  public double getStartValue() {
    return startValue;
  }

  /** Ending value when spacing.isRegular(). Coord or Interval. */
  public double getEndValue() {
    return endValue;
  }

  /** Iterates over coordinate values, either Double or CoordInterval. */
  @Override
  public Iterator<Object> iterator() {
    return new CoordIterator();
  }

  private class CoordIterator extends AbstractIterator<Object> {
    private int current = 0;

    @Override
    protected Object computeNext() {
      if (current >= getNcoords()) {
        return endOfData();
      }
      Object result = spacing != discontiguousInterval ? Double.valueOf(getCoordMidpoint(current))
          : CoordInterval.create(getCoordEdge1(current), getCoordEdge2(current));
      current++;
      return result;
    }
  }

  /////////////////////////////////////////////////////////////////////////////
  // subsetting

  @Override
  public RangeIterator getRangeIterator() {
    if (range != null) {
      return range;
    }
    try {
      return new Range(axisType.toString(), 0, ncoords - 1);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // not possible
    }
  }

  public Range getRange() {
    if (getDependenceType() == GridAxis.DependenceType.scalar) {
      return Range.EMPTY;
    }

    try {
      return new Range(axisType.toString(), 0, ncoords - 1);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  @Nullable
  public GridAxis subset(GridSubset params, Formatter errLog) {
    if (params == null) {
      return this;
    }
    GridAxis1D.Builder<?> builder = subsetBuilder(params, errLog);
    return (builder == null) ? null : builder.build();
  }

  // TODO incomplete handling of subsetting params
  @Nullable
  private GridAxis1D.Builder<?> subsetBuilder(GridSubset params, Formatter errLog) {
    GridAxis1DHelper helper = new GridAxis1DHelper(this);
    switch (getAxisType()) {
      case GeoZ:
      case Pressure:
      case Height: {
        Object dval = params.getVertCoord();
        if (dval != null) {
          if (dval instanceof Double) {
            return helper.subsetClosest((Double) dval);
          } else if (dval instanceof CoordInterval) {
            return helper.subsetClosest((CoordInterval) dval);
          }
        }

        // default is all
        break;
      }

      case Ensemble: {
        Double eval = params.getEnsCoord();
        if (eval != null) {
          return helper.subsetClosest(eval);
        }
        // default is all
        break;
      }

      case TimeOffset: {
        Double oval = params.getTimeOffset();
        if (oval != null) {
          return helper.subsetClosest(oval);
        }

        // If a time interval is sent, search for match.
        CoordInterval timeOffsetIntv = params.getTimeOffsetIntv();
        if (timeOffsetIntv != null) {
          return helper.subsetClosest(timeOffsetIntv);
        }

        // TODO do we need this?
        if (params.getTimeOffsetFirst()) {
          return helper.makeSubsetByIndex(new Range(1));
        }

        // default is all
        break;
      }

      // These are subsetted by the HorizCS
      case GeoX:
      case GeoY:
      case Lat:
      case Lon:
        return null;

      default:
        // default is all
        break;
    }

    // otherwise return copy of the original axis
    return this.toBuilder();
  }

  @Override
  public Optional<GridAxis> subsetDependent(GridAxis1D subsetIndAxis, Formatter errLog) {
    GridAxis1D.Builder<?> builder;
    builder = new GridAxis1DHelper(this).makeSubsetByIndex(subsetIndAxis.getRange());
    return Optional.of(builder.build());
  }

  //////////////////////////////////////////////////////////////
  final int ncoords; // number of coordinates
  final double startValue; // only for regular
  final double endValue;
  final Range range; // for subset, tracks the indexes in the original

  GridAxis1D(Builder<?> builder) {
    super(builder);

    Preconditions.checkArgument(builder.ncoords > 0);
    this.ncoords = builder.ncoords;
    this.startValue = builder.startValue;
    this.endValue = builder.endValue;

    if (axisType == null && builder.dependenceType == DependenceType.independent) {
      throw new IllegalArgumentException("independent axis must have type");
    }

    // make sure range has axisType as the name
    String rangeName = (axisType != null) ? axisType.toString() : null;
    if (builder.range != null) {
      this.range = (rangeName != null) ? builder.range.copyWithName(rangeName) : builder.range;
    } else {
      this.range = Range.make(rangeName, getNcoords());
    }
  }

  public GridAxis1D.Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends GridAxis.Builder<?>> builder) {
    builder.setRegular(this.ncoords, this.startValue, this.endValue, this.resolution).setRange(this.range);
    return (Builder<?>) super.addLocalFieldsToBuilder(builder);
  }

  /** A builder taking fields from a VariableDS */
  public static Builder<?> builder(VariableDS vds) {
    return builder().initFromVariableDS(vds);
  }

  /** Get Builder for this class that allows subclassing. */
  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> extends GridAxis.Builder<T> {
    int ncoords; // number of coordinates, required
    double startValue;
    double endValue;

    // does this really describe all subset possibilities? what about RangeScatter, composite ??
    private Range range; // for subset, tracks the indexes in the original
    private boolean built = false;

    public T setNcoords(int ncoords) {
      this.ncoords = ncoords;
      return self();
    }

    /**
     * Only used when spacing.isRegular.
     * regularPoint: start, end are pts; end = start + (ncoords - 1) * increment.
     * regularInterval: start, end are edges; end = start + ncoords * increment.
     */
    public T setRegular(int ncoords, double startValue, double endValue, double increment) {
      this.ncoords = ncoords;
      this.startValue = startValue;
      this.endValue = endValue;
      this.resolution = increment;
      return self();
    }

    public T setRange(Range range) {
      this.range = range;
      return self();
    }

    public T subset(int ncoords, double startValue, double endValue, double resolution, Range range) {
      this.ncoords = ncoords;
      this.startValue = startValue;
      this.endValue = endValue;
      this.resolution = resolution;
      this.range = range;
      this.isSubset = true;
      this.values = makeValues(range);
      return self();
    }

    private double[] makeValues(Range range) {
      if (spacing.isRegular()) {
        return null;
      }

      double[] subsetValues = null;
      int count = 0;
      switch (spacing) {
        case irregularPoint:
          subsetValues = new double[ncoords];
          for (int i : range) {
            subsetValues[count++] = values[i];
          }
          break;

        case contiguousInterval:
          subsetValues = new double[ncoords + 1]; // need npts+1
          for (int i : range) {
            subsetValues[count++] = values[i];
          }
          subsetValues[count] = values[range.last() + 1];
          break;

        case discontiguousInterval:
          subsetValues = new double[2 * ncoords]; // need 2*npts
          for (int i : range) {
            subsetValues[count++] = values[2 * i];
            subsetValues[count++] = values[2 * i + 1];
          }
          break;
      }
      return subsetValues;
    }

    public GridAxis1D build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new GridAxis1D(this);
    }
  }

}

