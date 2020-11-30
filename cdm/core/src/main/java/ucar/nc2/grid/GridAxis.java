/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.ma2.RangeIterator;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.util.Indent;
import ucar.unidata.util.StringUtil2;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.*;

/** A GridAxis is a Coordinate Axis for Grids. */
@Immutable
public abstract class GridAxis implements Iterable<Object> {
  private static final Logger logger = LoggerFactory.getLogger(GridAxis.class);

  /** The spacing of the coordinate values, used for 1D axes. */
  public enum Spacing {
    /**
     * Regularly spaced points (start, end, npts); start and end are midpoints, edges halfway between midpoints,
     * resol = (start - end) / (npts-1)
     */
    regularPoint, //
    /** Irregular spaced points values[npts]; edges halfway between coords. */
    irregularPoint, //

    /**
     * Regular contiguous intervals (start, end, npts); start and end are edges, midpoints halfway between edges,
     * resol = (start - end) / npts.
     */
    regularInterval, //
    /** Irregular contiguous intervals values[npts+1]; values are the edges, midpoints halfway between edges. */
    contiguousInterval, //
    /**
     * Irregular discontiguous intervals values[2*npts]; values are the edges: low0, high0, low1, high1, ...
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

  public abstract Array<Double> getCoordsAsArray();

  public abstract Array<Double> getCoordBoundsAsArray();

  /** Create a subset of this axis based on the SubsetParams. */
  // TODO throw an Exception when subset fails?
  @Nullable
  public abstract GridAxis subset(GridSubset params, Formatter errlog);

  // called only on dependent axes. pass in the subsetted independent axis
  public abstract Optional<GridAxis> subsetDependent(GridAxis1D subsetIndAxis, Formatter errlog);

  // Iterator over which coordinates wanted. TODO only in axis1d? Only for subset??
  public abstract RangeIterator getRangeIterator();

  /////////////////////////////////////////////////////

  public String getName() {
    return name;
  }

  public String getUnits() {
    return units;
  }

  @Nullable
  public String getDescription() {
    return description;
  }

  public AxisType getAxisType() {
    return axisType;
  }

  public AttributeContainer attributes() {
    return attributes;
  }

  public Spacing getSpacing() {
    return spacing;
  }

  /** spacing.isRegular(). */
  public boolean isRegular() {
    return spacing.isRegular();
  }

  /** spacing.isInterval(). */
  public boolean isInterval() {
    return spacing.isInterval();
  }

  /** When spacing.isRegular, same as increment, otherwise the spacing average or mode, used for information only. */
  public double getResolution() {
    return resolution;
  }

  public DependenceType getDependenceType() {
    return dependenceType;
  }

  public ImmutableList<String> getDependsOn() {
    return dependsOn;
  }

  // TODO maybe subset should return different class, since you cant subset a subset?
  public boolean isSubset() {
    return isSubset;
  }

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
    GridAxis objects = (GridAxis) o;
    return Double.compare(objects.resolution, resolution) == 0 && name.equals(objects.name)
        && Objects.equals(description, objects.description) && Objects.equals(units, objects.units)
        && axisType == objects.axisType && attributes.equals(objects.attributes)
        && dependenceType == objects.dependenceType && dependsOn.equals(objects.dependsOn) && spacing == objects.spacing
        && Arrays.equals(values, objects.values);
  }

  @Override
  public int hashCode() {
    int result =
        Objects.hash(name, description, units, axisType, attributes, dependenceType, dependsOn, spacing, resolution);
    result = 31 * result + Arrays.hashCode(values);
    return result;
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

    f.format("%saxisType=%s units='%s' desc='%s'%n", indent, axisType, units, description);

    indent.incr();
    for (Attribute att : attributes) {
      f.format("%s%s%n", indent, att);
    }
    f.format("%n");
    indent.decr();

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

  // TODO remove from public
  public double[] getValues() {
    // cant allow values array to escape, must be immutable
    return values == null ? null : java.util.Arrays.copyOf(values, values.length);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  protected final String name;
  protected final String description;
  protected final String units;
  protected final AxisType axisType;
  protected final AttributeContainer attributes;
  protected final DependenceType dependenceType;
  protected final ImmutableList<String> dependsOn; // independent axes or dimensions

  protected final Spacing spacing;
  protected final double resolution;
  protected final double[] values; // null if isRegular, len= ncoords (irregularPoint), ncoords+1 (contiguous interval),
                                   // or 2*ncoords (discontinuous interval)

  protected final boolean isSubset;

  protected GridAxis(Builder<?> builder) {
    Preconditions.checkNotNull(builder.name);
    Preconditions.checkNotNull(builder.axisType);
    Preconditions.checkNotNull(builder.spacing);

    if (builder.units == null) {
      this.units = builder.attributes.findAttributeString(CDM.UNITS, "");
    } else {
      this.units = builder.units;
    }

    if (builder.description == null) {
      this.description = builder.attributes.findAttributeString(CDM.LONG_NAME, null);
    } else {
      this.description = builder.description;
    }

    this.name = builder.name;
    this.axisType = builder.axisType;
    this.attributes = builder.attributes.toImmutable();
    this.dependenceType = builder.dependenceType;
    this.dependsOn = builder.dependsOn == null ? ImmutableList.of() : ImmutableList.copyOf(builder.dependsOn);

    this.spacing = builder.spacing;
    this.values = builder.values;
    this.resolution = builder.resolution;

    this.isSubset = builder.isSubset;
  }

  // Add local fields to the builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> builder) {
    builder.setName(this.name).setUnits(this.units).setDescription(this.getDescription()).setAxisType(this.axisType)
        .setAttributes(this.attributes).setDependenceType(this.dependenceType).setDependsOn(this.dependsOn)
        .setSpacing(this.spacing).setValues(this.values).setIsSubset(this.isSubset);

    return builder;
  }

  public static abstract class Builder<T extends Builder<T>> {
    private String name; // required
    private String description;
    private String units;
    AxisType axisType; // required
    private AttributeContainerMutable attributes = new AttributeContainerMutable(null);
    DependenceType dependenceType = DependenceType.independent; // default
    private ArrayList<String> dependsOn; // independent axes or dimensions

    Spacing spacing; // required
    double resolution;
    boolean isSubset;

    // may be lazy eval
    protected double[] values; // null if isRegular, len = ncoords, ncoords+1, or 2*ncoords

    protected abstract T self();

    public T setName(String name) {
      this.name = name;
      return self();
    }

    public T setDescription(String description) {
      this.description = description;
      return self();
    }

    public T setAxisType(AxisType axisType) {
      this.axisType = axisType;
      return self();
    }

    public T addAttribute(Attribute att) {
      this.attributes.addAttribute(att);
      return self();
    }

    public T setAttributes(AttributeContainer attributes) {
      this.attributes = new AttributeContainerMutable(null, attributes);
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

    /** When spacing.isRegular, same as increment. Otherwise the spacing average or mode, used for information only. */
    public T setResolution(double resolution) {
      this.resolution = resolution;
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

    /**
     * Spacing.regularXXX: not used
     * Spacing.irregularPoint: pts[ncoords]
     * Spacing.contiguousInterval: edges[ncoords+1]
     * Spacing.discontiguousInterval: bounds[2*ncoords]
     */
    public T setValues(double[] values) {
      this.values = values;
      return self();
    }

    @Override
    public String toString() {
      return name;
    }

    T initFromVariableDS(VariableDS vds) {
      return setName(vds.getShortName()).setUnits(vds.getUnitsString()).setDescription(vds.getDescription())
          .setAttributes(vds.attributes());
    }
  }
}
