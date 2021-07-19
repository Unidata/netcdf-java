/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid2;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import ucar.array.Range;
import ucar.nc2.internal.grid2.SubsetIntervalHelper;
import ucar.nc2.util.Indent;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Interval Grid coordinates.
 */
@Immutable
public class GridAxisInterval extends GridAxis<CoordInterval> implements Iterable<CoordInterval> {

  @Override
  public int getNominalSize() {
    return ncoords;
  }

  @Override
  @Nullable
  public Optional<GridAxisInterval> subset(GridSubset params, Formatter errlog) {
    if (params == null || params.isEmpty()) {
      return Optional.of(this);
    }
    SubsetIntervalHelper helper = new SubsetIntervalHelper(this);
    GridAxisInterval.Builder<?> builder = helper.subsetBuilder(params, errlog);
    if (builder == null) {
      return Optional.empty();
    }
    return Optional.of(builder.build());
  }

  @Override
  public Range getSubsetRange() {
    return this.range != null ? range : Range.make(this.name, this.ncoords);
  }

  @Override
  public CoordInterval getCoordInterval(int index) {
    return CoordInterval.create(getCoordEdge1(index), getCoordEdge2(index));
  }

  @Override
  public Object getCoordinate(int index) {
    return getCoordInterval(index);
  }

  /** The midpoint of the interval, cast to a double. */
  @Override
  public double getCoordMidpoint(int index) {
    return (getCoordEdge1(index) + getCoordEdge2(index)) / 2;
  }

  // LOOK double vs int
  private double getCoordEdge1(int index) {
    if (index < 0 || index >= ncoords) {
      throw new IllegalArgumentException("Index out of range=" + index);
    }

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
    if (index < 0 || index >= ncoords) {
      throw new IllegalArgumentException("Index out of range=" + index);
    }

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
      return getCoordInterval(current++);
    }
  }


  // LOOK cant let values escape
  @Override
  public int binarySearch(double want) {
    return Arrays.binarySearch(values, want); // LOOK what about discontinuous ?? wont work??
  }


  //////////////////////////////////////////////////////////////
  final int ncoords; // number of coordinates
  final double startValue; // only for regular
  final double endValue; // why needed?
  final Range range; // for subset, tracks the indexes in the original
  private final double[] values; // null if isRegular, len= ncoords+1 (contiguous interval),
  // or 2*ncoords (discontinuous interval) (min0, max0, min1, max1, min2, max2, ...)

  protected GridAxisInterval(Builder<?> builder) {
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

  void toString(Formatter f, Indent indent) {
    super.toString(f, indent);

    f.format("%snpts: %d [%f,%f] resolution=%f spacing=%s", indent, ncoords, startValue, endValue, resolution, spacing);
    f.format("%s range=%s isSubset=%s", indent, range, isSubset);
    f.format("%n");

    if (values != null) {
      int n = values.length;
      switch (spacing) {
        case contiguousInterval:
          f.format("%scontiguous values (%d)=", indent, n);
          for (double v : values)
            f.format("%f,", v);
          f.format("%n");
          break;

        case discontiguousInterval:
          f.format("%sdiscontiguous values (%d)=", indent, n);
          for (int i = 0; i < n; i += 2)
            f.format("(%f,%f) ", values[i], values[i + 1]);
          f.format("%n");
          break;
      }
    }
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> builder) {
    builder.setNcoords(this.ncoords).setResolution(this.resolution).setRange(this.range);
    if (isRegular()) {
      builder.setRegular(this.ncoords, this.startValue, this.resolution);
    } else {
      builder.setValues(this.values);
    }
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
    // sneaky way to let Gcdm get at the private data
    public double startValue;
    public double endValue;
    public double[] values; // null if isRegular; else len ncoords+1 (irregular), or 2*ncoords (discontinuous)

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
      double[] copy = new double[values.length];
      System.arraycopy(values, 0, copy, 0, values.length);
      this.values = copy;
      return self();
    }

    public T setValues(List<Number> values) {
      this.values = new double[values.size()];
      for (int i = 0; i < values.size(); i++) {
        this.values[i] = values.get(i).doubleValue();
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
      if (this.resolution == 0 && spacing == GridAxisSpacing.contiguousInterval && this.values.length > 1) {
        this.resolution = (this.values[this.values.length - 1] - this.values[0]) / (this.values.length - 1);
      }
      return new GridAxisInterval(this);
    }
  }


}
