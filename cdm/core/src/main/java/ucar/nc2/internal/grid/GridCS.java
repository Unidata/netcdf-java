/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import ucar.ma2.RangeIterator;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.*;
import ucar.nc2.grid.*;
import ucar.nc2.internal.dataset.DatasetClassifier;
import ucar.unidata.geoloc.*;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.*;
import java.util.stream.Collectors;

/** Implementation of GridCoordinateSystem. */
@Immutable
public class GridCS implements GridCoordinateSystem {

  @Override
  public String getName() {
    return name;
  }

  // needed? is it always GRID?
  public FeatureType getFeatureType() {
    return featureType;
  }

  @Override
  public ImmutableList<GridAxis> getGridAxes() {
    return this.axes;
  }

  @Override
  public Optional<GridAxis> findAxis(String axisName) {
    for (GridAxis axis : axes) {
      if (axis.getName().equals(axisName))
        return Optional.of(axis);
    }
    return Optional.empty();
  }

  // search in order given
  public GridAxis findCoordAxis(AxisType... axisType) {
    for (AxisType type : axisType) {
      for (GridAxis axis : axes) {
        if (axis.getAxisType() == type)
          return axis;
      }
    }
    return null;
  }

  @Override
  @Nullable
  public GridAxis1D getEnsembleAxis() {
    return (GridAxis1D) findCoordAxis(AxisType.Ensemble);
  }

  @Override
  public GridAxis getXHorizAxis() {
    return findCoordAxis(AxisType.GeoX, AxisType.Lon);
  }

  @Override
  public GridAxis getYHorizAxis() {
    return findCoordAxis(AxisType.GeoY, AxisType.Lat);
  }

  @Override
  @Nullable
  public GridAxis1D getVerticalAxis() {
    return (GridAxis1D) findCoordAxis(AxisType.GeoZ, AxisType.Height, AxisType.Pressure);
  }

  @Override
  @Nullable
  public GridAxis1DTime getTimeAxis() {
    return (GridAxis1DTime) findCoordAxis(AxisType.Time);
  }

  @Override
  @Nullable
  public GridAxis1DTime getRunTimeAxis() {
    return (GridAxis1DTime) findCoordAxis(AxisType.RunTime);
  }

  @Override
  @Nullable
  public GridAxis getTimeOffsetAxis() {
    return findCoordAxis(AxisType.TimeOffset);
  }

  @Override
  public GridHorizCoordinateSystem getHorizCoordSystem() {
    return horizCsys;
  }

  @Override
  public String toString() {
    return getName();
  }

  public void show(Formatter f, boolean showCoords) {
    f.format("Coordinate System (%s)%n", getName());

    showCoordinateAxis(getRunTimeAxis(), f, showCoords);
    showCoordinateAxis(getEnsembleAxis(), f, showCoords);
    showCoordinateAxis(getTimeAxis(), f, showCoords);
    showCoordinateAxis(getVerticalAxis(), f, showCoords);
    showCoordinateAxis(getYHorizAxis(), f, showCoords);
    showCoordinateAxis(getXHorizAxis(), f, showCoords);

    if (horizCsys.getProjection() != null) {
      f.format(" Projection: %s %s%n", horizCsys.getProjection().getName(), horizCsys.getProjection().paramsToString());
    }

    f.format(" LLbb=%s%n", horizCsys.getLatLonBoundingBox());
    if ((horizCsys.getProjection() != null) && !horizCsys.isLatLon()) {
      f.format(" bb= %s%n", horizCsys.getBoundingBox());
    }
  }

  private void showCoordinateAxis(GridAxis axis, Formatter f, boolean showCoords) {
    if (axis == null) {
      return;
    }
    String className = axis.getClass().getName();
    int pos = className.lastIndexOf(".");
    f.format(" %s (%s) ", axis.getName(), className.substring(pos + 1));
    if (showCoords) {
      showCoords(axis, f);
    }
    f.format("%n");
  }

  private void showCoords(GridAxis axis, Formatter f) {
    if (axis instanceof GridAxis1D) {
      GridAxis1D axis1D = (GridAxis1D) axis;
      if (!axis1D.isInterval()) {
        for (double anE : axis1D.getCoordsAsArray()) {
          f.format("%f,", anE);
        }
      } else {
        for (int i = 0; i < axis1D.getNcoords(); i++) {
          f.format("(%f,%f) = %f%n", axis1D.getCoordEdge1(i), axis1D.getCoordEdge2(i),
              axis1D.getCoordEdge2(i) - axis1D.getCoordEdge1(i));
        }
      }
    }
    f.format(" %s", axis.getUnits());
  }

  @Override
  public String showFnSummary() {
    if (featureType == null)
      return "";

    Formatter f2 = new Formatter();
    f2.format("%s(", featureType.toString());

    ArrayList<GridAxis> otherAxes = new ArrayList<>();
    int count = 0;
    for (GridAxis axis : axes) {
      if (axis.getDependenceType() != GridAxis.DependenceType.independent) {
        otherAxes.add(axis);
        continue;
      }
      if (count > 0) {
        f2.format(",");
      }
      f2.format("%s", axis.getAxisType() == null ? axis.getName() : axis.getAxisType().getCFAxisName());
      count++;
    }
    f2.format(")");

    count = 0;
    for (GridAxis axis : otherAxes) {
      f2.format(count == 0 ? ": " : ",");
      f2.format("%s", axis.getAxisType() == null ? axis.getName() : axis.getAxisType().getCFAxisName());
      count++;
    }
    return f2.toString();
  }

  @Override
  public List<RangeIterator> getRanges() {
    List<RangeIterator> result = new ArrayList<>();
    for (GridAxis axis : axes) {
      if (axis.getDependenceType() == GridAxis.DependenceType.independent) {
        result.add(axis.getRangeIterator());
      }
    }
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GridCS gridCS = (GridCS) o;
    boolean ok = featureType == gridCS.featureType && name.equals(gridCS.name) && horizCsys.equals(gridCS.horizCsys);
    for (GridAxis axis : getGridAxes()) {
      ok = gridCS.findAxis(axis.getName()).isPresent();
    }
    return ok;
  }

  @Override
  public int hashCode() {
    return Objects.hash(axes, featureType, name, horizCsys);
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Optional<GridCoordinateSystem> subset(GridSubset params, Formatter errlog) {
    Formatter errMessages = new Formatter();

    Builder<?> builder = this.toBuilder();
    builder.clearAxes();
    for (GridAxis axis : getGridAxes()) {
      if (axis.getDependenceType() == GridAxis.DependenceType.dependent) {
        continue;
      }

      GridAxis subsetAxis = axis.subset(params, errMessages);
      if (subsetAxis != null) {
        builder.addAxis(subsetAxis);

        // subset any dependent axes
        if (subsetAxis instanceof GridAxis1D) {
          GridAxis1D subsetInd = (GridAxis1D) subsetAxis; // independent always 1D
          for (GridAxis dependent : this.dependMap.get(axis)) {
            dependent.subsetDependent(subsetInd, errMessages).ifPresent(builder::addAxis);
          }
        }
      }
    }
    for (GridAxis xyaxis : horizCsys.subset(params, errlog)) {
      builder.addAxis(xyaxis);
    }

    String errs = errMessages.toString();
    if (!errs.isEmpty()) {
      errlog.format("%s", errs);
      return Optional.empty();
    }

    return Optional.of(builder.build());
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  private final ImmutableList<GridAxis> axes;
  private final FeatureType featureType; // TODO redo FeatureType
  private final String name;
  private final GridHorizCoordinateSystem horizCsys;
  private final ImmutableMultimap<GridAxis, GridAxis> dependMap;

  /**
   * Create a GridCoordinateSystem from a DatasetClassifier.CoordSysClassifier.
   *
   * @param classifier the classifier.
   * @param gridAxes The gridAxes already built, so there are no duplicates as we make the coordSys.
   */
  GridCS(DatasetClassifier.CoordSysClassifier classifier, Map<String, GridAxis> gridAxes) {
    this.featureType = classifier.getFeatureType();

    ArrayList<GridAxis> axesb = new ArrayList<>();
    for (CoordinateAxis axis : classifier.getAxesUsed()) {
      GridAxis gaxis = gridAxes.get(axis.getFullName());
      axesb.add(Preconditions.checkNotNull(gaxis, "Missing Coordinate Axis " + axis.getFullName()));
    }
    axesb.sort(new Grids.AxisComparator());
    this.axes = ImmutableList.copyOf(axesb);
    List<String> names = axes.stream().map(GridAxis::getName).collect(Collectors.toList());
    this.name = String.join(" ", names);

    this.horizCsys = GridHorizCS.create(getXHorizAxis(), getYHorizAxis(), classifier.getProjection());
    this.dependMap = makeDependMap();
  }

  private ImmutableMultimap<GridAxis, GridAxis> makeDependMap() {
    ImmutableMultimap.Builder<GridAxis, GridAxis> dependMapb = ImmutableMultimap.builder();
    for (GridAxis axis : this.axes) {
      if (axis.getDependenceType() == GridAxis.DependenceType.dependent) {
        for (String dependsOn : axis.getDependsOn()) {
          findAxis(dependsOn).ifPresent(indAxis -> dependMapb.put(axis, indAxis));
        }
      }
    }
    return dependMapb.build();
  }

  public GridCS.Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the builder.
  protected GridCS.Builder<?> addLocalFieldsToBuilder(GridCS.Builder<? extends GridCS.Builder<?>> builder) {
    builder.setName(this.name).setFeatureType(this.featureType).setProjection(horizCsys.getProjection())
        .setAxes(this.axes);

    return builder;
  }

  private GridCS(Builder<?> builder) {
    Preconditions.checkNotNull(builder.axes);
    Preconditions.checkNotNull(builder.projection);
    this.name = builder.name;
    this.featureType = builder.featureType;
    this.axes = ImmutableList.copyOf(builder.axes);
    this.horizCsys = GridHorizCS.create(getXHorizAxis(), getYHorizAxis(), builder.projection);
    this.dependMap = makeDependMap();
  }

  private GridCS(Builder<?> builder, List<GridAxis> gridAxes) {
    Preconditions.checkNotNull(builder.axesNames);
    Preconditions.checkNotNull(builder.projection);
    this.name = builder.name;
    this.featureType = builder.featureType;

    ImmutableList.Builder<GridAxis> axesb = ImmutableList.builder();
    for (String name : builder.axesNames) {
      gridAxes.stream().filter(a -> a.getName().equals(name)).findFirst().ifPresent(axesb::add);
    }
    this.axes = axesb.build();

    this.horizCsys = GridHorizCS.create(getXHorizAxis(), getYHorizAxis(), builder.projection);
    this.dependMap = makeDependMap();
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

  public static abstract class Builder<T extends GridCS.Builder<T>> {
    private String name;
    private FeatureType featureType;
    private Projection projection;
    private ArrayList<GridAxis> axes = new ArrayList<>();
    private List<String> axesNames;

    private boolean built;

    protected abstract T self();

    public T setName(String name) {
      this.name = name;
      return self();
    }

    public T setFeatureType(FeatureType featureType) {
      this.featureType = featureType;
      return self();
    }

    public T setProjection(Projection projection) {
      this.projection = projection;
      return self();
    }

    public T clearAxes() {
      this.axes = new ArrayList<>();
      return self();
    }

    public T setAxes(List<GridAxis> axes) {
      this.axes = new ArrayList<>(axes);
      return self();
    }

    public T addAxis(GridAxis axis) {
      this.axes.add(axis);
      return self();
    }

    public T setAxisNames(List<String> axesNames) {
      this.axesNames = axesNames;
      return self();
    }

    /** Axes must be passed in. */
    public GridCS build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new GridCS(this);
    }

    /** Axes are matched from names. */
    public GridCS build(List<GridAxis> gridAxes) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new GridCS(this, gridAxes);
    }
  }

}
