/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid2;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import ucar.array.Range;
import ucar.nc2.grid.CoordInterval;
import ucar.nc2.grid.GridSubset;

import javax.annotation.Nullable;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;

public class GridAxisInterval extends GridAxis implements Iterable<CoordInterval> {

  @Override
  public int getNominalSize() {
    return ncoords;
  }

  @Nullable
  @Override
  public GridAxis subset(GridSubset params, Formatter errlog) {
    return null;
  }

  @Override
  public Range getRange() {
    return this.range != null ? range : Range.make(this.name, this.ncoords);
  }

  public CoordInterval getCoordinate(int index) {
    return CoordInterval.create(getCoordEdge1(index), getCoordEdge2(index));
  }

  // LOOK double vs int
  private double getCoordEdge1(int index) {
    if (index < 0 || index >= ncoords)
      throw new IllegalArgumentException("Index out of range=" + index);

    switch (spacing) {
      case regularInterval:
        return startValue + index * getResolution();

      case contiguousInterval:
        return values[index];

      case discontiguousInterval:
        return values[2 * index];
    }
    throw new IllegalStateException("Unknown spacing=" + spacing);
  }

  private double getCoordEdge2(int index) {
    if (index < 0 || index >= ncoords)
      throw new IllegalArgumentException("Index out of range=" + index);

    switch (spacing) {
      case regularInterval:
        return startValue + (index + 1) * getResolution();

      case contiguousInterval:
        return values[index + 1];

      case discontiguousInterval:
        return values[2 * index + 1];
    }
    throw new IllegalStateException("Unknown spacing=" + spacing);
  }

  @Override
  public Iterator<CoordInterval> iterator() {
    return new CoordIterator();
  }

  private class CoordIterator extends AbstractIterator<CoordInterval> {
    private int current = 0;

    @Override
    protected CoordInterval computeNext() {
      if (current >= ncoords) {
        return endOfData();
      }
      return getCoordinate(current++);
    }
  }

  //////////////////////////////////////////////////////////////
  final int ncoords; // number of coordinates
  final double startValue; // only for regular
  final double endValue; // why needed?
  final Range range; // for subset, tracks the indexes in the original
  final double[] values; // null if isRegular, len= ncoords+1 (contiguous interval), or 2*ncoords (discontinuous
                         // interval)

  public GridAxisInterval(Builder<?> builder) {
    super(builder);

    Preconditions.checkArgument(builder.ncoords > 0);
    this.ncoords = builder.ncoords;
    this.startValue = builder.startValue;
    this.endValue = builder.endValue;
    this.values = builder.values;

    if (axisType == null && builder.dependenceType == GridAxisDependenceType.independent) {
      throw new IllegalArgumentException("independent axis must have type");
    }

    // make sure range has axisType as the name
    String rangeName = (axisType != null) ? axisType.toString() : null;
    if (builder.range != null) {
      this.range = (rangeName != null) ? builder.range.copyWithName(rangeName) : builder.range;
    } else {
      this.range = Range.make(rangeName, ncoords);
    }
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> builder) {
    builder.setRegular(this.ncoords, this.startValue, this.resolution).setValues(this.values).setRange(this.range);
    return (Builder<?>) super.addLocalFieldsToBuilder(builder);
  }

  /** Get Builder for this class that allows subclassing. */
  public static Builder<?> builder() {
    return new GridAxisInterval.Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected GridAxisInterval.Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> extends GridAxis.Builder<T> {
    int ncoords; // number of coordinates, required
    double startValue;
    double endValue;
    protected double[] values; // null if isRegular, len = ncoords, ncoords+1, or 2*ncoords

    // does this really describe all subset possibilities? what about RangeScatter, composite ??
    private Range range; // for subset, tracks the indexes in the original
    private boolean built = false;

    public T setNcoords(int ncoords) {
      this.ncoords = ncoords;
      return self();
    }

    /**
     * Spacing.regularXXX: not used
     * Spacing.contiguousInterval: edges[ncoords+1]
     * Spacing.discontiguousInterval: bounds[2*ncoords]
     */
    public T setValues(double[] values) {
      this.values = values;
      return self();
    }

    public T setValues(List<Double> values) {
      this.values = new double[values.size()];
      for (int i = 0; i < values.size(); i++) {
        this.values[i] = values.get(i);
      }
      return self();
    }

    /**
     * Only used when spacing.regularInterval.
     * regularInterval: start, end are edges; end = start + ncoords * increment.
     */
    public T setRegular(int ncoords, double startValue, double increment) {
      this.ncoords = ncoords;
      this.startValue = startValue;
      this.endValue = startValue + ncoords * increment;
      setResolution(increment);
      setSpacing(GridAxisSpacing.regularInterval); // dangerous?
      return self();
    }

    @Override
    public T setSpacing(GridAxisSpacing spacing) {
      Preconditions.checkArgument(spacing.isInterval());
      super.setSpacing(spacing);
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
      double[] subsetValues;
      int count = 0;

      switch (spacing) {
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

        default:
          throw new IllegalStateException("illegal spacing = " + spacing);
      }
      return subsetValues;
    }

    public GridAxisInterval build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new GridAxisInterval(this);
    }
  }


}
