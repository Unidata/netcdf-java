/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid2;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import ucar.array.Range;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.grid.GridSubset;
import ucar.unidata.util.StringUtil2;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * A GridAxis represents a 1D Coordinate Variable.
 */
public abstract class GridAxis implements Comparable<GridAxis> {

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getUnits() {
    return units;
  }

  public AxisType getAxisType() {
    return axisType;
  }

  public AttributeContainer attributes() {
    return attributes;
  }

  public GridAxisSpacing getSpacing() {
    return spacing;
  }

  public boolean isRegular() {
    return spacing.isRegular();
  }

  public boolean isInterval() {
    return spacing.isInterval();
  }

  /** For isRegular, this is also the increment. */
  public double getResolution() {
    return resolution;
  }

  public GridAxisDependenceType getDependenceType() {
    return dependenceType;
  }

  public abstract int getNominalSize();

  @Nullable
  public abstract GridAxis subset(GridSubset params, Formatter errlog);

  public abstract Range getRange(); // subset only ?

  @Override
  public int compareTo(GridAxis o) {
    return axisType.axisOrder() - o.axisType.axisOrder();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  protected final String name;
  protected final String units;
  protected final String description;
  protected final AxisType axisType;
  protected final AttributeContainer attributes;
  protected final GridAxisDependenceType dependenceType;
  protected final ImmutableList<String> dependsOn; // independent axes or dimensions

  protected final GridAxisSpacing spacing;
  protected final double resolution;

  protected final boolean isSubset;

  protected GridAxis(GridAxis.Builder<?> builder) {
    Preconditions.checkNotNull(builder.name);
    Preconditions.checkNotNull(builder.axisType);
    if (builder.spacing == null) {
      System.out.printf("HEY");
    }
    Preconditions.checkNotNull(builder.spacing);

    if (builder.units == null) {
      this.units = builder.attributes.findAttributeString(CDM.UNITS, "");
    } else {
      this.units = builder.units;
    }

    if (builder.description == null) {
      this.description = builder.attributes.findAttributeString(CDM.LONG_NAME, "");
    } else {
      this.description = builder.description;
    }

    this.name = builder.name;
    this.axisType = builder.axisType;
    this.attributes = builder.attributes.toImmutable();
    this.dependenceType = builder.dependenceType;
    this.dependsOn = builder.dependsOn == null ? ImmutableList.of() : ImmutableList.copyOf(builder.dependsOn);

    this.spacing = builder.spacing;
    this.resolution = builder.resolution;

    this.isSubset = builder.isSubset;
  }

  // Add local fields to the builder.
  protected GridAxis.Builder<?> addLocalFieldsToBuilder(GridAxis.Builder<? extends GridAxis.Builder<?>> builder) {
    builder.setName(this.name).setUnits(this.units).setDescription(this.getDescription()).setAxisType(this.axisType)
        .setAttributes(this.attributes).setDependenceType(this.dependenceType).setDependsOn(this.dependsOn)
        .setSpacing(this.spacing).setIsSubset(this.isSubset);

    return builder;
  }

  public static abstract class Builder<T extends GridAxis.Builder<T>> {
    private String name; // required
    private String description;
    private String units;
    public AxisType axisType; // required
    private AttributeContainerMutable attributes = new AttributeContainerMutable(null);
    GridAxisDependenceType dependenceType = GridAxisDependenceType.independent; // default
    private ArrayList<String> dependsOn; // independent axes or dimensions

    GridAxisSpacing spacing; // required
    double resolution;
    boolean isSubset;

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

    public T setDependenceType(GridAxisDependenceType dependenceType) {
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

    public T setSpacing(GridAxisSpacing spacing) {
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

    @Override
    public String toString() {
      return name;
    }

    public abstract GridAxis build();
  }
}