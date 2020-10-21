/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.collect.ImmutableList;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.RangeComposite;
import ucar.ma2.RangeIterator;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.util.Indent;
import ucar.nc2.util.NamedObject;
import ucar.unidata.util.Format;

import javax.annotation.concurrent.Immutable;
import java.util.*;

/** Grid CoordAxis 1D case */
@Immutable
public class GridAxis1D extends GridAxis {

  public RangeIterator getRangeIterator() {
    if (getDependenceType() == GridAxis.DependenceType.scalar)
      return Range.EMPTY;

    try {
      return new Range(axisType.toString(), 0, ncoords - 1);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e);
    }
  }

  public Range getRange() {
    if (getDependenceType() == GridAxis.DependenceType.scalar)
      return Range.EMPTY;

    try {
      return new Range(axisType.toString(), 0, ncoords - 1);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e);
    }
  }

  public int[] getShape() {
    if (getDependenceType() == GridAxis.DependenceType.scalar)
      return new int[0];
    return new int[] {ncoords};
  }

  @Override
  public void toString(Formatter f, Indent indent) {
    super.toString(f, indent);
    f.format("%s range=%s isSubset=%s", indent, range, isSubset());
    f.format("%n");
  }

  ///////////////////////////////////////////////////////////////////
  // Spacing

  public boolean isAscending() {
    loadValuesIfNeeded();
    switch (spacing) {
      case regularInterval:
      case regularPoint:
        return getResolution() > 0;

      case irregularPoint:
        return values[0] <= values[ncoords - 1];

      case contiguousInterval:
        return values[0] <= values[ncoords];

      case discontiguousInterval:
        return values[0] <= values[2 * ncoords - 1];
    }
    throw new IllegalStateException("unknown spacing" + spacing);
  }

  public double getCoordMidpoint(int index) {
    if (index < 0 || index >= getNcoords())
      throw new IllegalArgumentException("Index out of range=" + index);
    loadValuesIfNeeded();

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
    loadValuesIfNeeded();

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
    loadValuesIfNeeded();

    switch (spacing) {
      case regularPoint:
        if (index < 0 || index >= ncoords)
          throw new IllegalArgumentException("Index out of range " + index);
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

  public double getCoordEdgeFirst() {
    return getCoordEdge1(0);
  }

  public double getCoordEdgeLast() {
    return getCoordEdge2(ncoords - 1);
  }

  @Override
  public Array<Double> getCoordsAsArray() {
    double[] vals = new double[ncoords];
    for (int i = 0; i < ncoords; i++) {
      vals[i] = getCoordMidpoint(i);
    }

    Array<Double> result;
    if (dependenceType == DependenceType.scalar) {
      result = Arrays.factory(getDataType(), new int[0], vals);
    } else {
      result = Arrays.factory(getDataType(), new int[] {ncoords}, vals);
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
    return Arrays.factory(getDataType(), new int[] {ncoords, 2}, vals);
  }

  @Override
  public Optional<GridAxis> subset(double minValue, double maxValue, int stride, Formatter errLog) {
    GridAxis1DHelper helper = new GridAxis1DHelper(this);
    Optional<GridAxis1D.Builder<?>> buildero = helper.subset(minValue, maxValue, stride, errLog);
    return buildero.map(GridAxis1D::new);
  }

  // CalendarDate, double[2], or Double
  public Object getCoordObject(int index) {
    if (isInterval())
      return new double[] {getCoordEdge1(index), getCoordEdge2(index)};
    return getCoordMidpoint(index);
  }

  /** @deprecated will be moved in ver6 */
  @Deprecated
  public List<NamedObject> getCoordValueNames() {
    loadValuesIfNeeded();

    List<NamedObject> result = new ArrayList<>();
    for (int i = 0; i < ncoords; i++) {
      Object value = null;
      switch (spacing) {
        case regularPoint:
        case irregularPoint:
          value = Format.d(getCoordMidpoint(i), 3);
          break;

        case regularInterval:
        case contiguousInterval:
        case discontiguousInterval:
          value = CoordInterval.create(getCoordEdge1(i), getCoordEdge2(i), 3);
          break;
      }
      result.add(NamedObject.create(value, value + " " + getUnits()));
    }

    return result;
  }

  @Override
  public Optional<GridAxis> subset(SubsetParams params, Formatter errLog) {
    Optional<Builder<?>> buildero = subsetBuilder(params, errLog);
    return buildero.map(GridAxis1D::new);
  }

  // only for longitude, only for regular (do we need a subclass for longitude 1D coords ??
  public Optional<GridAxis1D> subsetByIntervals(List<Arrays.MinMax> lonIntvs, int stride, Formatter errLog) {
    if (axisType != AxisType.Lon) {
      errLog.format("subsetByIntervals only for longitude");
      return Optional.empty();
    }
    if (!isRegular()) {
      errLog.format("subsetByIntervals only for regular longitude");
      return Optional.empty();
    }

    // adjust the resolution of the subset based on stride
    double subsetResolution = stride > 1 ? stride * resolution : resolution;

    GridAxis1DHelper helper = new GridAxis1DHelper(this);

    double start = Double.NaN;
    boolean first = true;
    List<RangeIterator> ranges = new ArrayList<>();
    for (Arrays.MinMax lonIntv : lonIntvs) {
      if (first)
        start = lonIntv.min();
      first = false;

      Optional<RangeIterator> opt = helper.makeRange(lonIntv.min(), lonIntv.max(), stride, errLog);
      if (!opt.isPresent()) {
        return Optional.empty();
      }
      ranges.add(opt.get());
    }

    RangeComposite compositeRange = new RangeComposite(AxisType.Lon.toString(), ranges);
    // number of points in the subset
    int npts = compositeRange.length();
    // need to use the subset resolution to figure out the end
    double end = start + npts * subsetResolution;

    Builder<?> builder = toBuilder(); // copy
    builder.subset(npts, start, end, subsetResolution, null);
    builder.setRange(null);
    builder.setCompositeRange(compositeRange);

    return Optional.of(new GridAxis1D(builder));
  }

  public Optional<GridAxis1D> subsetByIndex(Range range, Formatter errLog) {
    try {
      GridAxis1DHelper helper = new GridAxis1DHelper(this);
      GridAxis1D.Builder<?> builder = helper.subsetByIndex(range);
      return Optional.of(new GridAxis1D(builder));
    } catch (InvalidRangeException e) {
      errLog.format("%s", e.getMessage());
      return Optional.empty();
    }
  }

  // LOOK incomplete handling of subsetting params
  Optional<GridAxis1D.Builder<?>> subsetBuilder(SubsetParams params, Formatter errLog) {
    if (params == null) {
      return Optional.of(this.toBuilder());
    }

    GridAxis1DHelper helper = new GridAxis1DHelper(this);

    switch (getAxisType()) {
      case GeoZ:
      case Pressure:
      case Height:
        Double dval = params.getVertCoord();
        if (dval != null) {
          return Optional.of(helper.subsetClosest(dval));
        }
        // use midpoint of interval LOOK may not always be unique
        double[] intv = params.getVertCoordIntv();
        if (intv != null) {
          return Optional.of(helper.subsetClosest((intv[0] + intv[1]) / 2));
        }

        double[] vertRange = params.getVertRange(); // used by WCS
        if (vertRange != null) {
          return helper.subset(vertRange[0], vertRange[1], 1, errLog);
        }

        // default is all
        break;

      case Ensemble:
        Double eval = params.getDouble(SubsetParams.ensCoord);
        if (eval != null) {
          return Optional.of(helper.subsetClosest(eval));
        }
        // default is all
        break;

      // x,y get seperately subsetted
      case GeoX:
      case GeoY:
      case Lat:
      case Lon:
        throw new IllegalArgumentException();
      // return null; // LOOK heres a case where null is "correct"

      default:
        // default is all
        break;
    }

    // otherwise return copy the original axis
    return Optional.of(this.toBuilder());
  }

  @Override
  public Optional<GridAxis> subsetDependent(GridAxis1D dependsOn, Formatter errLog) {
    GridAxis1D.Builder<?> builder;
    try {
      builder = new GridAxis1DHelper(this).subsetByIndex(dependsOn.getRange()); // LOOK Other possible subsets?
    } catch (InvalidRangeException e) {
      errLog.format("%s", e.getMessage());
      return Optional.empty();
    }
    return Optional.of(new GridAxis1D(builder));
  }

  //////////////////////////////////////////////////////////////

  final Range range; // for subset, tracks the indexes in the original
  final RangeComposite crange;

  GridAxis1D(Builder<?> builder) {
    super(builder);

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
    this.crange = builder.crange;
  }

  public GridAxis1D.Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the builder.
  protected GridAxis1D.Builder<?> addLocalFieldsToBuilder(GridAxis1D.Builder<? extends GridAxis.Builder<?>> builder) {
    builder.setRange(this.range).setCompositeRange(this.crange);
    return addLocalFieldsToBuilder(builder);
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
    // does this really describe all subset possibilities? what about RangeScatter, composite ??
    private Range range; // for subset, tracks the indexes in the original
    private RangeComposite crange;
    private boolean built = false;

    public T setRange(Range range) {
      this.range = range;
      return self();
    }

    public T setCompositeRange(RangeComposite crange) {
      this.crange = crange;
      return self();
    }

    T subset(String dependsOn, Spacing spacing, int ncoords, double[] values) {
      assert values != null;
      if (dependsOn != null) {
        this.dependenceType = DependenceType.dependent;
        setDependsOn(ImmutableList.of(dependsOn));
      }
      this.spacing = spacing;
      this.ncoords = ncoords;
      this.reader = null;
      this.values = values;
      this.isSubset = true;

      return self();
    }

    T subset(int ncoords, double startValue, double endValue, double resolution, double[] values) {
      this.ncoords = ncoords;
      this.startValue = startValue;
      this.endValue = endValue;
      this.resolution = resolution;
      this.values = values;
      this.isSubset = true;

      return self();
    }

    public GridAxis1D build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new GridAxis1D(this);
    }

  }

}

