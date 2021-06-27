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

/** Point Grib coordinates with values stored in memory. */
public class GridAxisPoint extends GridAxis<Number> implements Iterable<Number> {

  @Override
  public int getNominalSize() {
    return ncoords;
  }

  @Nullable
  @Override
  public GridAxisPoint subset(GridSubset params, Formatter errlog) {
    return null;
  }

  @Override
  public Range getRange() {
    return this.range != null ? range : Range.make(this.name, this.ncoords);
  }

  public Number getCoordinate(int index) {
    if (index < 0 || index >= ncoords)
      throw new IllegalArgumentException("Index out of range=" + index);

    switch (spacing) {
      case regularPoint:
        return startValue + index * getResolution();

      case irregularPoint:
        return values[index];
    }
    throw new IllegalStateException("Unknown spacing=" + spacing);
  }

  public CoordInterval getCoordInterval(int index) {
    return CoordInterval.create(getCoordEdge1(index), getCoordEdge2(index));
  }

  // LOOK double vs int
  private double getCoordEdge1(int index) {
    if (index < 0 || index >= ncoords)
      throw new IllegalArgumentException("Index out of range=" + index);

    switch (spacing) {
      case regularPoint:
        return startValue + (index - .5) * getResolution();

      case irregularPoint:
        if (index > 0)
          return (values[index - 1] + values[index]) / 2;
        else
          return values[0] - (values[1] - values[0]) / 2;
    }
    throw new IllegalStateException("Unknown spacing=" + spacing);
  }

  private double getCoordEdge2(int index) {
    if (index < 0 || index >= ncoords)
      throw new IllegalArgumentException("Index out of range=" + index);

    switch (spacing) {
      case regularPoint:
        return startValue + (index + .5) * getResolution();

      case irregularPoint:
        if (index < ncoords - 1)
          return (values[index] + values[index + 1]) / 2;
        else
          return values[index] + (values[index] - values[index - 1]) / 2;
    }
    throw new IllegalStateException("Unknown spacing=" + spacing);
  }

  @Override
  public Iterator<Number> iterator() {
    return new CoordIterator();
  }

  private class CoordIterator extends AbstractIterator<Number> {
    private int current = 0;

    @Override
    protected Number computeNext() {
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

  public GridAxisPoint(Builder<?> builder) {
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
    return new GridAxisPoint.Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected GridAxisPoint.Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> extends GridAxis.Builder<T> {
    int ncoords; // number of coordinates, required
    double startValue;
    double endValue;
    protected double[] values; // null if isRegular, else len = ncoords

    // does this really describe all subset possibilities? what about RangeScatter, composite ??
    private Range range; // for subset, tracks the indexes in the original
    private boolean built = false;

    public T setNcoords(int ncoords) {
      this.ncoords = ncoords;
      return self();
    }

    /**
     * Spacing.regularXXX: not used
     * Spacing.irregularPoint: pts[ncoords]
     */
    public T setValues(double[] values) {
      this.values = values;
      this.ncoords = values.length;
      return self();
    }

    public T setValues(List<Number> values) {
      this.values = new double[values.size()];
      for (int i = 0; i < values.size(); i++) {
        this.values[i] = values.get(i).doubleValue(); // LOOK or store Number ? or Array for efficiency
      }
      this.ncoords = values.size();
      return self();
    }

    /**
     * Only used when spacing.regularPoint.
     * end = start + (ncoords - 1) * increment.
     */
    public T setRegular(int ncoords, double startValue, double increment) {
      this.ncoords = ncoords;
      this.startValue = startValue;
      this.endValue = startValue + increment * (ncoords - 1);
      setResolution(increment);
      setSpacing(GridAxisSpacing.regularPoint); // dangerous?
      return self();
    }

    @Override
    public T setSpacing(GridAxisSpacing spacing) {
      Preconditions.checkArgument(!spacing.isInterval());
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
      switch (spacing) {
        case regularPoint:
          return null;

        case irregularPoint:
          int count = 0;
          double[] subsetValues = new double[ncoords];
          for (int i : range) {
            subsetValues[count++] = values[i];
          }
          return subsetValues;

        default:
          throw new IllegalStateException("illegal spacing = " + spacing);
      }
    }

    public GridAxisPoint build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new GridAxisPoint(this);
    }
  }

}
