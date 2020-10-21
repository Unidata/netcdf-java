/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.RangeIterator;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.util.Indent;

import java.io.IOException;
import java.util.*;

/** GridAxis abstract superclass */
public abstract class GridAxis implements Comparable<GridAxis> {
  private static final Logger logger = LoggerFactory.getLogger(GridAxis.class);

  public enum Spacing {
    regularPoint, // regularly spaced points (start, end, npts), start and end are pts, edges halfway between coords,
                  // resol = (start - end) / (npts-1)
    irregularPoint, // irregular spaced points (values, npts), edges halfway between coords
    regularInterval, // regular contiguous intervals (start, end, npts), start and end are edges, resol = (start - end)
                     // / npts
    contiguousInterval, // irregular contiguous intervals (values, npts), values are the edges, values[npts+1], coord
                        // halfway between edges
    discontiguousInterval // irregular discontiguous spaced intervals (values, npts), values are the edges,
                          // values[2*npts]: low0, high0, low1, high1, ...
  }

  public enum DependenceType {
    independent, // has its own dimension, is a coordinate variable, eg x(x)
    dependent, // aux coordinate, eg reftime(time) or time_bounds(time);
    scalar, // eg reftime
    twoD, // lat(x,y)
    fmrcReg, // time(reftime, hourOfDay)
    dimension // swath(scan, scanAcross)
  }

  @Override
  public int compareTo(GridAxis o) {
    return axisType.axisOrder() - o.axisType.axisOrder();
  }

  // create a subset of this axis based on the SubsetParams. return copy if no subset requested, or params = null
  public abstract Optional<GridAxis> subset(SubsetParams params, Formatter errlog);

  // called from HorizCoordSys
  public abstract Optional<GridAxis> subset(double minValue, double maxValue, int stride, Formatter errLog);

  // called only on dependent axes. pass in independent axis
  public abstract Optional<GridAxis> subsetDependent(GridAxis1D dependsOn, Formatter errlog);

  public abstract Array<Double> getCoordsAsArray();

  public abstract Array<Double> getCoordBoundsAsArray();

  /////////////////////////////////////////////////////

  public String getName() {
    return name;
  }

  public DataType getDataType() {
    return dataType;
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
    return (spacing == Spacing.regularPoint) || (spacing == Spacing.regularInterval);
  }

  // Same as increment, used when isRegular
  public double getResolution() {
    return resolution;
  }

  public double getStartValue() {
    return startValue;
  }

  public double getEndValue() {
    return endValue;
  }

  public String getUnits() {
    return units;
  }

  public String getDescription() {
    return description;
  }

  public DependenceType getDependenceType() {
    return dependenceType;
  }

  public boolean isScalar() {
    return dependenceType == DependenceType.scalar;
  }

  public String getDependsOn() {
    Formatter result = new Formatter();
    for (String name : dependsOn)
      result.format("%s ", name);
    return result.toString().trim();
  }

  public List<String> getDependsOnList() {
    return dependsOn;
  }

  public boolean getHasData() {
    return values != null;
  }

  public boolean isSubset() {
    return isSubset;
  }

  public boolean isInterval() {
    return spacing == Spacing.regularInterval || spacing == Spacing.contiguousInterval
        || spacing == Spacing.discontiguousInterval;
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    Indent indent = new Indent(2);
    toString(f, indent);
    return f.toString();
  }

  public RangeIterator getRangeIterator() {
    if (getDependenceType() == GridAxis.DependenceType.scalar)
      return Range.EMPTY;

    try {
      return new Range(axisType.toString(), 0, ncoords - 1);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e);
    }
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

    for (Attribute att : attributes) {
      f.format("%s%s%n", indent, att);
    }

    f.format("%snpts: %d [%f,%f] spacing=%s", indent, ncoords, startValue, endValue, spacing);
    if (getResolution() != 0.0)
      f.format(" resolution=%f", resolution);
    f.format("%n");

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

  private boolean valuesLoaded;

  protected void loadValuesIfNeeded() {
    synchronized (this) {
      if (isRegular() || valuesLoaded)
        return;
      if (values == null && reader != null)
        try {
          values = reader.readCoordValues(this);
        } catch (IOException e) {
          logger.error("Failed to read " + name, e);
        }
      valuesLoaded = true;
    }
  }

  // will return null when isRegular, otherwise reads values if needed
  public double[] getValues() {
    loadValuesIfNeeded();
    // cant allow values array to escape, must be immutable
    return values == null ? null : Arrays.copyOf(values, values.length);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  protected final String name;
  protected final String description;
  protected final String units;
  protected final DataType dataType;
  protected final AxisType axisType; // ucar.nc2.constants.AxisType ordinal
  protected final AttributeContainer attributes;
  protected final DependenceType dependenceType;
  protected final List<String> dependsOn; // independent axes or dimensions

  protected final int ncoords; // number of coordinates (not always same as values)
  protected final Spacing spacing;
  protected final double startValue;
  protected final double endValue;
  protected final double resolution;
  protected final GridAxisReader reader;
  protected double[] values; // null if isRegular, or use GridAxisReader for lazy eval

  protected final boolean isSubset;

  protected GridAxis(Builder<?> builder) {
    this.name = builder.name;
    this.description = builder.description;
    this.units = builder.units;
    this.dataType = builder.dataType;
    this.axisType = builder.axisType;
    this.attributes = builder.attributes;
    this.dependenceType = builder.dependenceType;
    this.dependsOn = builder.dependsOn == null ? Collections.emptyList() : builder.dependsOn;

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
  public static GridAxis1D.Builder<?> builder(VariableDS vds) {
    return (GridAxis1D.Builder<?>) builder().initFromVariableDS(vds);
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
    private DataType dataType;
    protected AxisType axisType; // ucar.nc2.constants.AxisType ordinal
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

    private String units;

    // may be lazy eval
    protected double[] values; // null if isRegular, or use CoordAxisReader for lazy eval

    private boolean built;

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
      this.dependsOn = new ArrayList(dependsOn);
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
