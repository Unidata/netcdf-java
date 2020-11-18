/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.ma2.DataType;
import ucar.ma2.RangeIterator;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.util.Indent;
import ucar.unidata.util.StringUtil2;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;

/** A GridAxis is a Coordinate Axis for Grids. */
public abstract class GridAxis implements Iterable<Object> {
  private static final Logger logger = LoggerFactory.getLogger(GridAxis.class);

  /** The spacing of the coordinate values, used for 1D axes. */
  public enum Spacing {
    /**
     * Regularly spaced points (start, end, npts); start and end are pts, edges halfway between coords, resol = (start -
     * end) / (npts-1)
     */
    regularPoint, //
    /** Irregular spaced points (values, npts); edges halfway between coords. */
    irregularPoint, //
    /** Regular contiguous intervals (start, end, npts); start and end are edges, resol = (start - end) / npts. */
    regularInterval, //
    /**
     * Irregular contiguous intervals (values, npts); values are the edges, values[npts+1], coord halfway between edges.
     */
    contiguousInterval, //
    /**
     * Irregular discontiguous intervals (values, npts); values are the edges, values[2*npts]: low0, high0, low1, high1,
     * ...
     * Note that monotonicity is not guaranteed, and is ambiguous.
     */
    discontiguousInterval; //

    /** If the spacing is regular. */
    public boolean isRegular() {
      return (this == Spacing.regularPoint) || (this == Spacing.regularInterval);
    }

    /** If the coordinate values are intervals. */
    public boolean isInterval() {
      return this == Spacing.regularInterval || this == Spacing.contiguousInterval
          || this == Spacing.discontiguousInterval;
    }
  }

  /** The way that the Axis depends on other axes. */
  public enum DependenceType {
    /** Has its own dimension, so is a coordinate variable, eg x(x). */
    independent, //
    /** Auxilary coordinate, eg reftime(time) or time_bounds(time). */
    dependent, //
    /** A scalar doesnt involve indices. Eg the reference time is often a scalar. */
    scalar, //
    /** A coordinate needing two dimensions, eg lat(x,y). */
    twoD, //
    /** Eg time(reftime, hourOfDay). */
    fmrcReg, //
    /** Eg swath(scan, scanAcross). */
    dimension //
  }

  /** Create a subset of this axis based on the SubsetParams. */
  // TODO throw an Exception when subset fails?
  @Nullable
  public abstract GridAxis subset(GridSubset params, Formatter errlog);

  // called only on dependent axes. pass in independent axis
  @Nullable
  public abstract GridAxis subsetDependent(GridAxis1D dependsOn, Formatter errlog);

  public abstract Array<Double> getCoordsAsArray();

  public abstract Array<Double> getCoordBoundsAsArray();

  /////////////////////////////////////////////////////

  public String getName() {
    return name;
  }

  public String getUnits() {
    return units;
  }

  public String getDescription() {
    return description;
  }

  public AxisType getAxisType() {
    return axisType;
  }

  public AttributeContainer attributes() {
    return attributes;
  }

  public int getNcoords() {
    return ncoords;
  }

  public Spacing getSpacing() {
    return spacing;
  }

  public boolean isRegular() {
    return spacing.isRegular();
  }

  public boolean isInterval() {
    return spacing.isInterval();
  }

  /** When isRegular, same as increment, otherwise an average = (end - start) / npts. */
  public double getResolution() {
    return resolution;
  }

  public double getStartValue() {
    return startValue;
  }

  public double getEndValue() {
    return endValue;
  }

  public DependenceType getDependenceType() {
    return dependenceType;
  }

  public boolean isScalar() {
    return dependenceType == DependenceType.scalar;
  }

  public ImmutableList<String> getDependsOn() {
    return dependsOn;
  }

  // LOOK maybe subset should return different class, since you cant subset a subset?
  public boolean isSubset() {
    return isSubset;
  }

  // Iterator over which coordinates wanted. TODO only in axis1d? Only for subset??
  public abstract RangeIterator getRangeIterator();

  // @Override
  public int compareTo(GridAxis o) {
    return axisType.axisOrder() - o.axisType.axisOrder();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GridAxis gridAxis = (GridAxis) o;
    return ncoords == gridAxis.ncoords && Double.compare(gridAxis.startValue, startValue) == 0
        && Double.compare(gridAxis.endValue, endValue) == 0 && Double.compare(gridAxis.resolution, resolution) == 0
        && Objects.equal(name, gridAxis.name) && Objects.equal(description, gridAxis.description)
        && Objects.equal(units, gridAxis.units) && dataType == gridAxis.dataType && axisType == gridAxis.axisType
        && Objects.equal(attributes, gridAxis.attributes) && dependenceType == gridAxis.dependenceType
        && Objects.equal(dependsOn, gridAxis.dependsOn) && spacing == gridAxis.spacing
        && Objects.equal(reader, gridAxis.reader) && Objects.equal(values, gridAxis.values);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, description, units, dataType, axisType, attributes, dependenceType, dependsOn,
        ncoords, spacing, startValue, endValue, resolution, reader, values);
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    Indent indent = new Indent(2);
    toString(f, indent);
    return f.toString();
  }

  public void toString(Formatter f, Indent indent) {
    f.format("%sCoordAxis '%s' (%s) ", indent, name, getClass().getName());
    indent.incr();

    f.format("%s", getDependenceType());
    if (!dependsOn.isEmpty()) {
      f.format(" :");
      for (String s : dependsOn)
        f.format(" %s", s);
    }
    f.format("%n");

    f.format("%saxisType=%s dataType=%s units='%s' desc='%s'%n", indent, axisType, dataType, units, description);
    if (getResolution() != 0.0)
      f.format(" resolution=%f", resolution);
    f.format("%n");

    indent.incr();
    for (Attribute att : attributes) {
      f.format("%s%s%n", indent, att);
    }
    f.format("%n");
    indent.decr();

    f.format("%snpts: %d [%f,%f] spacing=%s", indent, ncoords, startValue, endValue, spacing);
    if (values != null) {
      int n = values.length;
      switch (spacing) {
        case irregularPoint:
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
    indent.decr();
  }

  public String getSummary() {
    Formatter f = new Formatter();
    f.format("start=%f end=%f %s %s resolution=%f", startValue, endValue, units, spacing, resolution);
    f.format(" (npts=%d)", ncoords);
    return f.toString();
  }

  ///////////////////////////////////////////////
  // LOOK needed, or is it done all in the builder??

  private boolean valuesLoaded;

  // will return null when isRegular, otherwise reads values if needed
  public double[] getValues() {
    // cant allow values array to escape, must be immutable
    return values == null ? null : Arrays.copyOf(values, values.length);
  }

  protected void loadValuesIfNeeded() {
    synchronized (this) {
      if (isRegular() || valuesLoaded)
        return;
      if (values == null && reader != null)
        try {
          values = reader.readCoordValues(this);
        } catch (IOException e) { // TODO
          logger.error("Failed to read " + name, e);
        }
      valuesLoaded = true;
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  protected final String name;
  protected final String description;
  protected final String units;
  protected final DataType dataType;
  protected final AxisType axisType;
  protected final AttributeContainer attributes;
  protected final DependenceType dependenceType;
  protected final ImmutableList<String> dependsOn; // independent axes or dimensions

  protected final int ncoords; // number of coordinates (not always same as values)
  protected final Spacing spacing;
  protected final double startValue; // only for regular
  protected final double endValue;
  protected final double resolution;
  protected final GridAxisReader reader; // LOOK when is this needed?
  protected double[] values; // null if isRegular, or use reader for lazy eval

  protected final boolean isSubset;

  protected GridAxis(Builder<?> builder) {
    Preconditions.checkNotNull(builder.name);
    Preconditions.checkNotNull(builder.axisType);
    this.name = builder.name;
    this.description = builder.description;
    this.units = builder.units;
    this.dataType = builder.dataType;
    this.axisType = builder.axisType;
    this.attributes = builder.attributes;
    this.dependenceType = builder.dependenceType;
    this.dependsOn = builder.dependsOn == null ? ImmutableList.of() : ImmutableList.copyOf(builder.dependsOn);

    this.spacing = builder.spacing;
    this.values = builder.values;
    this.reader = builder.reader; // used only if values == null

    this.startValue = builder.startValue;
    this.endValue = builder.endValue;
    this.ncoords = builder.ncoords;
    this.resolution = builder.resolution;

    this.isSubset = builder.isSubset;
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> builder) {
    builder.setName(this.name).setUnits(this.units).setDescription(this.getDescription()).setDataType(this.dataType)
        .setAxisType(this.axisType).setAttributes(this.attributes).setDependenceType(this.dependenceType)
        .setDependsOn(this.dependsOn).setSpacing(this.spacing).setValues(this.values).setReader(this.reader)
        .setGenerated(this.ncoords, this.startValue, this.endValue, this.resolution).setIsSubset(this.isSubset);

    return builder;
  }

  /** A builder taking fields from a VariableDS */
  public static Builder<?> builder(VariableDS vds) {
    return builder().initFromVariableDS(vds);
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

  public static abstract class Builder<T extends Builder<T>> {
    private String name;
    private String description;
    private String units;
    private DataType dataType;
    AxisType axisType; // ucar.nc2.constants.AxisType ordinal
    private AttributeContainer attributes;
    DependenceType dependenceType;
    private ArrayList<String> dependsOn; // independent axes or dimensions

    int ncoords; // number of coordinates (not always same as values)
    Spacing spacing;
    double startValue;
    double endValue;
    double resolution;
    GridAxisReader reader;
    boolean isSubset;

    // may be lazy eval
    protected double[] values; // null if isRegular, or use CoordAxisReader for lazy eval

    protected abstract T self();

    public T setName(String name) {
      this.name = name;
      return self();
    }

    public T setDescription(String description) {
      this.description = description;
      return self();
    }

    public T setDataType(DataType dataType) {
      this.dataType = dataType;
      return self();
    }

    public T setAxisType(AxisType axisType) {
      this.axisType = axisType;
      return self();
    }

    public T setAttributes(AttributeContainer attributes) {
      this.attributes = attributes;
      return self();
    }

    public T setDependenceType(DependenceType dependenceType) {
      this.dependenceType = dependenceType;
      return self();
    }

    public T setDependsOn(List<String> dependsOn) {
      this.dependsOn = new ArrayList<>(dependsOn);
      return self();
    }

    public T setDependsOn(String dependsOn) {
      setDependsOn(StringUtil2.splitList(dependsOn));
      return self();
    }

    public T setSpacing(Spacing spacing) {
      this.spacing = spacing;
      return self();
    }

    public T setNcoords(int ncoords) {
      this.ncoords = ncoords;
      return self();
    }

    public T setResolution(double resolution) {
      this.resolution = resolution;
      return self();
    }

    public T setGenerated(int ncoords, double startValue, double endValue, double resolution) {
      this.ncoords = ncoords;
      this.startValue = startValue;
      this.endValue = endValue;
      this.resolution = resolution;
      return self();
    }

    public T setReader(GridAxisReader reader) {
      this.reader = reader;
      return self();
    }

    public T setIsSubset(boolean subset) {
      isSubset = subset;
      return self();
    }

    public T setUnits(String units) {
      this.units = units;
      return self();
    }

    public T setValues(double[] values) {
      this.values = values;
      return self();
    }

    public T initFromVariableDS(VariableDS vds) {
      return setName(vds.getShortName()).setUnits(vds.getUnitsString()).setDescription(vds.getDescription())
          .setDataType(vds.getDataType()).setAttributes(vds.attributes()).setNcoords((int) vds.getSize());
    }
  }
}
