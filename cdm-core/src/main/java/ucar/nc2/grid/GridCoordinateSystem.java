/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.collect.ImmutableList;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.geoloc.vertical.VerticalTransform;
import ucar.unidata.geoloc.projection.CurvilinearProjection;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * A Coordinate System for gridded data, consisting of orthogonal 1D GridAxes.
 * In some cases, the shape is an approximation, so that when reading data, the returned Array in
 * MaterializedCoordinateSyste will have a different shape, reflecting the actual data.
 */
public class GridCoordinateSystem {

  /** The name of the Grid Coordinate System. */
  public String getName() {
    return this.name;
  }

  /** Always GRID or CURVILINEAR. */
  public FeatureType getFeatureType() {
    return featureType;
  }

  /** The GridAxes that constitute this Coordinate System. */
  public ImmutableList<GridAxis<?>> getGridAxes() {
    return this.axes;
  }

  /** Find the named axis. */
  public Optional<GridAxis<?>> findAxis(String axisName) {
    return getGridAxes().stream().filter(g -> g.getName().equals(axisName)).findFirst();
  }

  /** Find the first axis having one of the given AxisTypes. Search in order given. */
  @Nullable
  public GridAxis<?> findCoordAxisByType(AxisType... axisType) {
    for (AxisType type : axisType) {
      for (GridAxis<?> axis : axes) {
        if (axis.getAxisType() == type)
          return axis;
      }
    }
    return null;
  }

  /** Get the Time CoordinateSystem. Null if there are no time coordinates. */
  public GridTimeCoordinateSystem getTimeCoordinateSystem() {
    return tcs;
  }

  /** Get the ensemble axis, if any. */
  @Nullable
  public GridAxisPoint getEnsembleAxis() {
    return (GridAxisPoint) getGridAxes().stream().filter(a -> a.getAxisType() == AxisType.Ensemble).findFirst()
        .orElse(null);
  }

  /** Get the Z axis (GeoZ, Height, Pressure), if any. */
  @Nullable
  public GridAxis<?> getVerticalAxis() {
    return getGridAxes().stream().filter(a -> a.getAxisType().isVert()).findFirst().orElse(null);
  }

  /** Get the vertical transform, if any. */
  @Nullable
  public VerticalTransform getVerticalTransform() {
    return verticalTransform;
  }

  /** True if increasing z coordinate values means "up" in altitude */
  public boolean isZPositive() {
    GridAxis<?> vertAxis = getVerticalAxis();
    if (vertAxis == null) {
      return false;
    }
    String positive = vertAxis.attributes.findAttributeString(CF.POSITIVE, null);
    if (positive != null) {
      return positive.equalsIgnoreCase(CF.POSITIVE_UP);
    }
    if (vertAxis.getAxisType() == AxisType.Height) {
      return true;
    }
    // make something up
    return vertAxis.getAxisType() != AxisType.Pressure;
  }

  /** Get the X axis (either GeoX or Lon). */
  public GridAxisPoint getXHorizAxis() {
    return getHorizCoordinateSystem().getXHorizAxis();
  }

  /** Get the Y axis (either GeoY or Lat). */
  public GridAxisPoint getYHorizAxis() {
    return getHorizCoordinateSystem().getYHorizAxis();
  }

  /** Get the Horizontal CoordinateSystem. */
  public GridHorizCoordinateSystem getHorizCoordinateSystem() {
    return hcs;
  }

  /** Nominal shape, may differ from materialized shape. */
  public List<Integer> getNominalShape() {
    List<Integer> result = new ArrayList<>();
    if (getTimeCoordinateSystem() != null) {
      result.addAll(getTimeCoordinateSystem().getNominalShape());
    }
    if (getEnsembleAxis() != null && getEnsembleAxis().getDependenceType() == GridAxisDependenceType.independent) {
      result.add(getEnsembleAxis().getNominalSize());
    }
    if (getVerticalAxis() != null && getVerticalAxis().getDependenceType() == GridAxisDependenceType.independent) {
      result.add(getVerticalAxis().getNominalSize());
    }
    result.addAll(getHorizCoordinateSystem().getShape());
    return result;
  }

  /** Create a MaterializedCoordinateSystem based on the supplied subset parameters. */
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
    hcs.subset(params, builder, errlog).ifPresentOrElse(hcs -> builder.setHorizCoordSys(hcs), () -> fail.set(true));

    if (fail.get()) {
      return Optional.empty();
    } else {
      return Optional.of(builder.build());
    }
  }

  //////////////////////////////////////////////////////////////////////////////////////////
  private final ImmutableList<GridAxis<?>> axes;
  private final GridHorizCoordinateSystem hcs;
  @Nullable
  private final GridTimeCoordinateSystem tcs;
  private final String name;
  private final FeatureType featureType;
  private final VerticalTransform verticalTransform;

  public GridCoordinateSystem(List<GridAxis<?>> axes, @Nullable GridTimeCoordinateSystem tcs,
      @Nullable VerticalTransform verticalTransform, GridHorizCoordinateSystem hcs) {

    ArrayList<GridAxis<?>> mutable = new ArrayList<>(axes);
    mutable.sort(new Grids.AxisComparator());
    this.axes = ImmutableList.copyOf(mutable);
    this.tcs = tcs;
    this.hcs = hcs;
    this.featureType =
        hcs.getProjection() instanceof CurvilinearProjection ? FeatureType.CURVILINEAR : FeatureType.GRID;
    this.verticalTransform = verticalTransform;

    List<String> names = this.axes.stream().map(a -> a.getName()).collect(Collectors.toList());
    this.name = String.join(" ", names);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GridCoordinateSystem that = (GridCoordinateSystem) o;
    return axes.equals(that.axes) && hcs.equals(that.hcs) && Objects.equals(tcs, that.tcs) && name.equals(that.name)
        && featureType == that.featureType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(axes, hcs, tcs, name, featureType);
  }

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

    if (getVerticalTransform() != null) {
      f.format(" VerticalTransform: %s%n", getVerticalTransform());
    }

    if (hcs.getProjection() != null) {
      f.format(" Projection: %s %s%n", hcs.getProjection().getName(), hcs.getProjection());
    }
    f.format(" LLbb=%s%n", hcs.getLatLonBoundingBox());
    if ((hcs.getProjection() != null) && !hcs.isLatLon()) {
      f.format(" bb= %s%n", hcs.getBoundingBox());
    }
    if (tcs != null) {
      f.format("%n%s%n", tcs);
    }
    f.format("%n%s%n", hcs);
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

  /** Function description, eg GRID(T,Z,Y,Z):R */
  public String showFnSummary() {
    if (featureType == null)
      return "";

    Formatter f2 = new Formatter();
    f2.format("%s(", featureType);

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

}
