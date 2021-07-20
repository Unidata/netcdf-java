/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.grid2;

import com.google.common.base.Preconditions;
import ucar.array.Array;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.GridCoordinateSystem;
import ucar.nc2.grid2.GridHorizCoordinateSystem;
import ucar.nc2.grid2.GridHorizCurvilinear;
import ucar.nc2.grid2.Grids;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.projection.Curvilinear;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of GridCoordinateSystem.
 */
@Immutable
public class GridNetcdfCSBuilder {

  /**
   * Create a GridCoordinateSystem from a DatasetClassifier.CoordSysClassifier.
   *
   * @param classifier the classifier.
   * @param gridAxes The gridAxes already built, so there are no duplicates as we make the coordSys.
   */
  public static Optional<GridCoordinateSystem> createFromClassifier(DatasetClassifier.CoordSysClassifier classifier,
      Map<String, GridAxis<?>> gridAxes, Formatter errlog) throws IOException {

    GridNetcdfCSBuilder builder = new GridNetcdfCSBuilder();
    builder.setFeatureType(classifier.getFeatureType());
    builder.setProjection(classifier.getProjection());

    if (classifier.getFeatureType() == FeatureType.CURVILINEAR) {
      Preconditions.checkNotNull(classifier.lataxis);
      Preconditions.checkNotNull(classifier.lonaxis);
      ucar.array.Array<?> latdata = classifier.lataxis.readArray();
      ucar.array.Array<?> londata = classifier.lonaxis.readArray();
      builder.setCurvilinearData(latdata, londata);
    }

    ArrayList<GridAxis<?>> axesb = new ArrayList<>();
    for (CoordinateAxis axis : classifier.getAxesUsed()) {
      GridAxis<?> gaxis = gridAxes.get(axis.getFullName());
      if (gaxis == null) {
        errlog.format("Missing Coordinate Axis= %s%n", axis.getFullName());
      } else {
        axesb.add(gaxis);
      }
    }
    builder.setAxes(axesb);

    try {
      return Optional.of(builder.build());
    } catch (Exception e) {
      e.printStackTrace();
      errlog.format("createFromClassifier '%s' exception %s%n", classifier.cs.getName(), e.getMessage());
      return Optional.empty();
    }
  }

  ////////////////////////////////////////////////////////////////////
  private String name;
  private FeatureType featureType = FeatureType.GRID; // can it be different?
  private Projection projection;
  private ArrayList<GridAxis<?>> axes = new ArrayList<>();
  private Array<Number> latdata;
  private Array<Number> londata;

  private boolean built;

  public GridNetcdfCSBuilder setName(String name) {
    this.name = name;
    return this;
  }

  public GridNetcdfCSBuilder setFeatureType(FeatureType featureType) {
    this.featureType = featureType;
    return this;
  }

  public GridNetcdfCSBuilder setProjection(Projection projection) {
    this.projection = projection;
    return this;
  }

  public GridNetcdfCSBuilder setCurvilinearData(Array<?> latdata, Array<?> londata) {
    Preconditions.checkArgument(latdata.getArrayType().isNumeric());
    Preconditions.checkArgument(londata.getArrayType().isNumeric());
    this.latdata = (Array<Number>) latdata;
    this.londata = (Array<Number>) londata;
    return this;
  }

  public GridNetcdfCSBuilder setAxes(List<GridAxis<?>> axes) {
    this.axes = new ArrayList<>(axes);
    return this;
  }

  public GridNetcdfCSBuilder addAxis(GridAxis<?> axis) {
    this.axes.add(axis);
    return this;
  }

  public GridCoordinateSystem build() {
    if (built)
      throw new IllegalStateException("already built");
    built = true;

    Preconditions.checkNotNull(axes);
    Preconditions.checkNotNull(projection);

    axes.sort(new Grids.AxisComparator());
    GridHorizCoordinateSystem horizCsys = makeHorizCS(findCoordAxisByType(AxisType.GeoX, AxisType.Lon),
        findCoordAxisByType(AxisType.GeoY, AxisType.Lat), this.projection, this.latdata, this.londata);
    GridNetcdfTimeCS tcs = makeTimeCS();

    return new GridCoordinateSystem(this.axes, tcs, horizCsys);
  }

  GridAxis<?> findCoordAxisByType(AxisType... axisType) {
    for (AxisType type : axisType) {
      for (GridAxis<?> axis : axes) {
        if (axis.getAxisType() == type)
          return axis;
      }
    }
    return null;
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

  private GridHorizCoordinateSystem makeHorizCS(GridAxis<?> xaxis, GridAxis<?> yaxis, @Nullable Projection projection,
      Array<Number> latdata, Array<Number> londata) {
    Preconditions.checkArgument(xaxis instanceof GridAxisPoint);
    Preconditions.checkArgument(yaxis instanceof GridAxisPoint);

    // LOOK heres now to find horizStaggerType in WRF NMM
    String horizStaggerType = xaxis.attributes().findAttributeString(_Coordinate.Stagger, null);

    if (projection instanceof Curvilinear) {
      return GridHorizCurvilinear.create((GridAxisPoint) xaxis, (GridAxisPoint) yaxis, latdata, londata);
    } else {
      return new GridHorizCoordinateSystem((GridAxisPoint) xaxis, (GridAxisPoint) yaxis, projection);
    }
  }

}
