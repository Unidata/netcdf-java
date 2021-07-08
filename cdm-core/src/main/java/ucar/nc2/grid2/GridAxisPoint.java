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
import ucar.nc2.internal.grid2.SubsetPointHelper;
import ucar.nc2.util.Indent;

import javax.annotation.concurrent.Immutable;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static ucar.nc2.grid2.GridAxisSpacing.irregularPoint;

/**
 * Point Grid coordinates.
 * LOOK although we use Number, everything is internally a double. Grib wants integers.
 */
@Immutable
public class GridAxisPoint extends GridAxis<Number> implements Iterable<Number> {

  @Override
  public int getNominalSize() {
    return ncoords;
  }

  @Override
  public Optional<GridAxisPoint> subset(GridSubset params, Formatter errlog) {
    if (params == null || params.isEmpty()) {
      return Optional.of(this);
    }
    SubsetPointHelper helper = new SubsetPointHelper(this);
    GridAxisPoint.Builder<?> builder = helper.subsetBuilder(params, errlog);
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
  public Number getCoordinate(int index) {
    if (index < 0 || index >= ncoords) {
      throw new IllegalArgumentException("Index out of range=" + index);
    }

    switch (spacing) {
      case regularPoint:
        return startValue + index * getResolution();

      case irregularPoint:
      case nominalPoint:
        return values[index];
    }
    throw new IllegalStateException("Unknown spacing=" + spacing);
  }

  /** CoordIntervals are midway between the point, cast to a double. */
  @Override
  public CoordInterval getCoordInterval(int index) {
    return CoordInterval.create(getCoordEdge1(index), getCoordEdge2(index));
  }

  /** The same as getCoordinate(), cast to a double. */
  @Override
  public double getCoordMidpoint(int index) {
    return getCoordinate(index).doubleValue();
  }

  // LOOK double vs int
  private double getCoordEdge1(int index) {
    if (index < 0 || index >= ncoords) {
      throw new IllegalArgumentException("Index out of range=" + index);
    }

    switch (spacing) {
      case regularPoint:
        return startValue + (index - .5) * getResolution();

      case irregularPoint:
        if (index > 0) {
          return (values[index - 1] + values[index]) / 2;
        } else {
          return values[0] - (values[1] - values[0]) / 2;
        }

      case nominalPoint:
        return edges[index];
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
        if (index < ncoords - 1) {
          return (values[index] + values[index + 1]) / 2;
        } else {
          return values[index] + (values[index] - values[index - 1]) / 2;
        }

      case nominalPoint:
        return edges[index + 1];
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


  // LOOK cant let values escape
  @Override
  public int binarySearch(double want) {
    return Arrays.binarySearch(values, want);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    GridAxisPoint numbers = (GridAxisPoint) o;
    return ncoords == numbers.ncoords && Double.compare(numbers.startValue, startValue) == 0
        && Double.compare(numbers.endValue, endValue) == 0 && Objects.equals(range, numbers.range)
        && Arrays.equals(values, numbers.values);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(super.hashCode(), ncoords, startValue, endValue, range);
    result = 31 * result + Arrays.hashCode(values);
    return result;
  }

  //////////////////////////////////////////////////////////////
  final int ncoords; // number of coordinates
  final double startValue; // only for regular
  final double endValue; // why needed?
  final Range range; // for subset, tracks the indexes in the original
  private final double[] values; // null if isRegular, irregular or nominal then len= ncoords
  private final double[] edges; // nominal only: len = ncoords+1

  protected GridAxisPoint(Builder<?> builder) {
    super(builder);

    Preconditions.checkArgument(builder.ncoords > 0);
    this.ncoords = builder.ncoords;
    this.startValue = builder.startValue;
    this.endValue = builder.endValue;
    this.values = builder.values;
    if (this.values != null) {
      Preconditions.checkArgument(this.values.length == this.ncoords);
    }
    if (this.getSpacing() != GridAxisSpacing.regularPoint) {
      Preconditions.checkNotNull(this.values);
    }
    this.edges = builder.edges;
    if (this.edges != null) {
      Preconditions.checkArgument(this.edges.length == this.ncoords + 1);
    }
    if (this.getSpacing() == GridAxisSpacing.nominalPoint) {
      Preconditions.checkNotNull(this.values);
      Preconditions.checkNotNull(this.edges);
      for (int i = 0; i < ncoords; i++) {
        Preconditions.checkArgument(this.edges[i] <= this.values[i]);
      }
      Preconditions.checkArgument(this.edges[ncoords] >= this.values[ncoords - 1]);
    }

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

    if (values != null && spacing == irregularPoint) {
      f.format("%scontiguous values (%d)=", indent, values.length);
      for (double v : values)
        f.format("%f,", v);
      f.format("%n");
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
    builder.setEdges(this.edges);
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
    protected double[] edges; // only used if nominalPoint, len = ncoords+1

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
     * Spacing.nominalPoint: pts[ncoords]
     */
    public T setValues(double[] values) {
      double[] copy = new double[values.length];
      System.arraycopy(values, 0, copy, 0, values.length);
      this.values = copy;
      this.ncoords = values.length;
      return self();
    }

    /**
     * Spacing.nominalPoint: pts[ncoords+1]
     */
    public T setEdges(double[] edges) {
      this.edges = edges;
      return self();
    }

    // LOOK or store Number ? or Array for efficiency
    public T setValues(List<Number> values) {
      this.values = new double[values.size()];
      for (int i = 0; i < values.size(); i++) {
        this.values[i] = values.get(i).doubleValue();
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
      Preconditions.checkNotNull(range);
      Preconditions.checkArgument(ncoords == range.length());
      this.ncoords = ncoords;
      this.startValue = startValue;
      this.endValue = endValue;
      this.resolution = resolution;
      this.range = range;
      this.isSubset = true;
      this.values = makeValues(range);
      this.edges = makeEdges(range);
      return self();
    }

    private double[] makeValues(Range range) {
      switch (spacing) {
        case regularPoint:
          return null;

        case nominalPoint:
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

    private double[] makeEdges(Range range) {
      if (spacing == GridAxisSpacing.nominalPoint) {
        int count = 0;
        int lastEdge = 0;
        double[] subsetEdges = new double[ncoords + 1];
        for (int i : range) {
          subsetEdges[count++] = edges[i];
          lastEdge = i;
        }
        subsetEdges[count] = edges[lastEdge + 1];
        return subsetEdges;
      }
      return null;
    }

    public GridAxisPoint build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      if (this.resolution == 0 && this.values != null && this.values.length > 1) {
        this.resolution = (this.values[this.values.length - 1] - this.values[0]) / (this.values.length - 1);
      }
      return new GridAxisPoint(this);
    }
  }

}
