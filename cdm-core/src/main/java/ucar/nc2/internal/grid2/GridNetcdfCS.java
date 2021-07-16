/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.grid2;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.grid.GridSubset;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisDependenceType;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.GridCoordinateSystem;
import ucar.nc2.grid2.GridHorizCoordinateSystem;
import ucar.nc2.grid2.GridTimeCoordinateSystem;
import ucar.nc2.grid2.Grids;
import ucar.nc2.grid2.MaterializedCoordinateSystem;
import ucar.unidata.geoloc.Projection;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/** Implementation of GridCoordinateSystem. */
@Immutable
public class GridNetcdfCS implements GridCoordinateSystem {

  /**
   * Create a GridCoordinateSystem from a DatasetClassifier.CoordSysClassifier.
   *
   * @param classifier the classifier.
   * @param gridAxes The gridAxes already built, so there are no duplicates as we make the coordSys.
   */
  static Optional<GridNetcdfCS> createFromClassifier(DatasetClassifier.CoordSysClassifier classifier,
      Map<String, GridAxis<?>> gridAxes, Formatter errlog) {
    GridNetcdfCS.Builder<?> builder = GridNetcdfCS.builder();
    builder.setFeatureType(classifier.getFeatureType());
    builder.setProjection(classifier.getProjection());

    ArrayList<GridAxis<?>> axesb = new ArrayList<>();
    for (CoordinateAxis axis : classifier.getAxesUsed()) {
      GridAxis<?> gaxis = gridAxes.get(axis.getFullName());
      if (gaxis == null) {
        errlog.format("Missing Coordinate Axis= %s%n", axis.getFullName());
        continue;
      }
      axesb.add(gaxis);
    }
    builder.setAxes(axesb);

    try {
      return Optional.of(builder.build());
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  @Override
  public String getName() {
    return name;
  }

  // needed? is it always GRID?
  public FeatureType getFeatureType() {
    return featureType;
  }

  @Override
  public ImmutableList<GridAxis<?>> getGridAxes() {
    return this.axes;
  }

  // search in order given
  @Nullable
  public GridAxis<?> findCoordAxis(AxisType... axisType) {
    for (AxisType type : axisType) {
      for (GridAxis<?> axis : axes) {
        if (axis.getAxisType() == type)
          return axis;
      }
    }
    return null;
  }

  @Override
  @Nullable
  public GridTimeCoordinateSystem getTimeCoordinateSystem() {
    return tcs;
  }

  @Override
  public GridHorizCoordinateSystem getHorizCoordinateSystem() {
    return horizCsys;
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    show(f, false);
    return f.toString();
  }

  void show(Formatter f, boolean showCoords) {
    f.format("Coordinate System (%s)%n", getName());

    if (tcs != null) {
      showCoordinateAxis(tcs.getRunTimeAxis(), f, showCoords);
      showCoordinateAxis(tcs.getTimeOffsetAxis(0), f, showCoords);
    }
    showCoordinateAxis(getEnsembleAxis(), f, showCoords);
    showCoordinateAxis(getVerticalAxis(), f, showCoords);
    showCoordinateAxis(getYHorizAxis(), f, showCoords);
    showCoordinateAxis(getXHorizAxis(), f, showCoords);
    f.format("%n");

    if (horizCsys.getProjection() != null) {
      f.format(" Projection: %s %s%n", horizCsys.getProjection().getName(), horizCsys.getProjection().paramsToString());
    }
    f.format(" LLbb=%s%n", horizCsys.getLatLonBoundingBox());
    if ((horizCsys.getProjection() != null) && !horizCsys.isLatLon()) {
      f.format(" bb= %s%n", horizCsys.getBoundingBox());
    }
    if (tcs != null) {
      f.format("%n%s%n", tcs);
    }
  }

  private void showCoordinateAxis(GridAxis<?> axis, Formatter f, boolean showCoords) {
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

  private void showCoords(GridAxis<?> axis, Formatter f) {
    for (Object anE : axis) {
      f.format("%s,", anE);
    }
    f.format(" %s", axis.getUnits());
  }

  @Override
  public String showFnSummary() {
    if (featureType == null)
      return "";

    Formatter f2 = new Formatter();
    f2.format("%s(", featureType.toString());

    ArrayList<GridAxis<?>> otherAxes = new ArrayList<>();
    int count = 0;
    for (GridAxis<?> axis : axes) {
      if (axis.getDependenceType() != GridAxisDependenceType.independent) {
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
    for (GridAxis<?> axis : otherAxes) {
      f2.format(count == 0 ? ": " : ",");
      f2.format("%s", axis.getAxisType() == null ? axis.getName() : axis.getAxisType().getCFAxisName());
      count++;
    }
    return f2.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GridNetcdfCS gridCS = (GridNetcdfCS) o;
    boolean ok = featureType == gridCS.featureType && name.equals(gridCS.name) && horizCsys.equals(gridCS.horizCsys);
    for (GridAxis<?> axis : getGridAxes()) {
      ok = gridCS.findAxis(axis.getName()).isPresent();
    }
    return ok;
  }

  @Override
  public int hashCode() {
    return Objects.hash(axes, featureType, name, horizCsys);
  }

  /////////////////////////////////////////////////////////////////////////////////////////
  /** Subsetting the coordinate system, then using that subset to do the read. Special to Netcdf, not general. */
  public Optional<MaterializedCoordinateSystem> subset(GridSubset params, Formatter errlog) {
    MaterializedCoordinateSystem.Builder builder = MaterializedCoordinateSystem.builder();
    AtomicBoolean fail = new AtomicBoolean(false); // gets around need for final variable in lambda

    if (tcs != null) {
      tcs.subset(params, errlog).ifPresentOrElse(builder::setTimeCoordSys, () -> fail.set(true));
    }
    if (getEnsembleAxis() != null) {
      getEnsembleAxis().subset(params, errlog).ifPresentOrElse(builder::setEnsAxis, () -> fail.set(true));
    }
    if (getVerticalAxis() != null) {
      getVerticalAxis().subset(params, errlog).ifPresentOrElse(builder::setVertAxis, () -> fail.set(true));
    }
    horizCsys.subset(params, errlog).ifPresentOrElse(hcs -> builder.setHorizCoordSys(hcs), () -> fail.set(true));

    if (fail.get()) {
      return Optional.empty();
    } else {
      return Optional.of(builder.build());
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  private final ImmutableList<GridAxis<?>> axes; // LOOK what order are these in?
  private final FeatureType featureType; // TODO redo FeatureType
  private final String name;
  private final GridHorizCoordinateSystem horizCsys;
  private final @Nullable GridNetcdfTimeCS tcs;

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> builder) {
    builder.setName(this.name).setFeatureType(this.featureType).setProjection(horizCsys.getProjection())
        .setAxes(this.axes);

    return builder;
  }

  private GridNetcdfCS(Builder<?> builder) {
    Preconditions.checkNotNull(builder.axes);
    Preconditions.checkNotNull(builder.projection);

    builder.axes.sort(new Grids.AxisComparator());
    if (builder.name == null) {
      List<String> names = builder.axes.stream().map(GridAxis::getName).collect(Collectors.toList());
      builder.setName(String.join(" ", names));
    }
    this.name = builder.name;
    this.featureType = builder.featureType;
    this.axes = ImmutableList.copyOf(builder.axes);
    this.horizCsys = makeHorizCS(findCoordAxis(AxisType.GeoX, AxisType.Lon), findCoordAxis(AxisType.GeoY, AxisType.Lat),
        builder.projection);
    this.tcs = makeTimeCS();
  }

  private GridNetcdfCS(Builder<?> builder, List<GridAxis<?>> gridAxes) {
    Preconditions.checkNotNull(builder.axesNames);
    Preconditions.checkNotNull(builder.projection);
    this.name = builder.name;
    this.featureType = builder.featureType;

    ImmutableList.Builder<GridAxis<?>> axesb = ImmutableList.builder();
    for (String name : builder.axesNames) {
      gridAxes.stream().filter(a -> a.getName().equals(name)).findFirst().ifPresent(axesb::add);
    }
    this.axes = axesb.build();

    this.horizCsys = makeHorizCS(findCoordAxis(AxisType.GeoX, AxisType.Lon), findCoordAxis(AxisType.GeoY, AxisType.Lat),
        builder.projection);
    this.tcs = makeTimeCS();
  }

  private GridNetcdfTimeCS makeTimeCS() {
    GridAxis<?> rtAxis = axes.stream().filter(a -> a.getAxisType() == AxisType.RunTime).findFirst().orElse(null);
    GridAxis<?> toAxis = axes.stream().filter(a -> a.getAxisType() == AxisType.TimeOffset).findFirst().orElse(null);
    GridAxis<?> timeAxis = axes.stream().filter(a -> a.getAxisType() == AxisType.Time).findFirst().orElse(null);
    GridAxis<?> useAxis = (toAxis != null) ? toAxis : timeAxis;

    if (rtAxis != null && useAxis != null) {
      return GridNetcdfTimeCS.create((GridAxisPoint) rtAxis, useAxis);
    } else if (useAxis != null) {
      return GridNetcdfTimeCS.create(useAxis);
    }
    // ok to not have a time coordinate
    return null;
  }

  public static GridHorizCoordinateSystem makeHorizCS(GridAxis<?> xaxis, GridAxis<?> yaxis,
      @Nullable Projection projection) {
    Preconditions.checkArgument(xaxis instanceof GridAxisPoint);
    Preconditions.checkArgument(yaxis instanceof GridAxisPoint);
    // LOOK heres now to find horizStaggerType in WRF NMM
    String horizStaggerType = xaxis.attributes().findAttributeString(_Coordinate.Stagger, null);
    return new GridHorizCoordinateSystem((GridAxisPoint) xaxis, (GridAxisPoint) yaxis, projection);
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
    private FeatureType featureType = FeatureType.GRID; // can it be different?
    private Projection projection;
    private ArrayList<GridAxis<?>> axes = new ArrayList<>();
    private List<String> axesNames;
    private List<String> transformNames;

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

    public T setAxes(List<GridAxis<?>> axes) {
      this.axes = new ArrayList<>(axes);
      return self();
    }

    public T addAxis(GridAxis<?> axis) {
      this.axes.add(axis);
      return self();
    }

    // LOOK: used ?
    public T setAxisNames(List<String> axesNames) {
      this.axesNames = axesNames;
      return self();
    }

    /** Axes must have been passed in. */
    public GridNetcdfCS build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new GridNetcdfCS(this);
    }

    /** Axes are matched from names. LOOK: used ? */
    public GridNetcdfCS build(List<GridAxis<?>> gridAxes) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new GridNetcdfCS(this, gridAxes);
    }
  }

}
