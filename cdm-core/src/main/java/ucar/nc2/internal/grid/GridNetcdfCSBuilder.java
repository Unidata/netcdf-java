/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import ucar.array.Array;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.TransformType;
import ucar.nc2.geoloc.vertical.VerticalTransformFactory;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridAxisSpacing;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridHorizCoordinateSystem;
import ucar.nc2.grid.GridHorizCurvilinear;
import ucar.nc2.grid.Grids;
import ucar.unidata.geoloc.Projection;
import ucar.nc2.geoloc.vertical.VerticalTransform;
import ucar.unidata.geoloc.projection.CurvilinearProjection;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Builder of GridCoordinateSystem, using DatasetClassifier.CoordSysClassifier.
 */
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
    builder.setVerticalTransform(makeVerticalTransform(classifier, errlog));

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

    if (classifier.getFeatureType() == FeatureType.CURVILINEAR) {
      Preconditions.checkNotNull(classifier.lataxis);
      Preconditions.checkNotNull(classifier.lonaxis);
      ucar.array.Array<?> latdata = classifier.lataxis.readArray();
      ucar.array.Array<?> londata = classifier.lonaxis.readArray();
      builder.setCurvilinearData(latdata, londata);

      // create fake 1d axes if needed
      if (classifier.xaxis == null) {
        Dimension xdim = classifier.lataxis.getDimension(1);
        builder.addAxis(GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName(xdim.getShortName()).setUnits("")
            .setDescription("fake 1d xaxis for curvilinear grid").setRegular(xdim.getLength(), 0.0, 1.0)
            .setSpacing(GridAxisSpacing.regularPoint).build());
      }
      if (classifier.yaxis == null) {
        Dimension ydim = classifier.lataxis.getDimension(0);
        builder.addAxis(GridAxisPoint.builder().setAxisType(AxisType.GeoY).setName(ydim.getShortName()).setUnits("")
            .setDescription("fake 1d yaxis for curvilinear grid").setRegular(ydim.getLength(), 0.0, 1.0)
            .setSpacing(GridAxisSpacing.regularPoint).build());
      }
    }

    if (builder.axes.size() < 2) {
      return Optional.empty();
    }

    try {
      return Optional.of(builder.build());
    } catch (Exception e) {
      e.printStackTrace();
      errlog.format("createFromClassifier '%s' exception %s%n", classifier.cs.getName(), e.getMessage());
      return Optional.empty();
    }
  }

  @Nullable
  private static VerticalTransform makeVerticalTransform(DatasetClassifier.CoordSysClassifier classifier,
      Formatter errlog) {
    for (CoordinateTransform ct : classifier.getCoordinateSystem().getCoordinateTransforms()) {
      if (ct.getTransformType() == TransformType.Vertical) {
        Optional<VerticalTransform> vto = VerticalTransformFactory.makeVerticalTransform(classifier.getDataset(),
            classifier.getCoordinateSystem(), ct.getCtvAttributes(), errlog);
        if (vto.isPresent()) {
          return vto.get();
        }
      }
    }
    return null;
  }

  ////////////////////////////////////////////////////////////////////
  private String name;
  private FeatureType featureType = FeatureType.GRID; // can it be different? Curvilinear?
  private Projection projection;
  private ucar.nc2.geoloc.vertical.VerticalTransform verticalTransform;
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

  public GridNetcdfCSBuilder setVerticalTransform(
      @Nullable ucar.nc2.geoloc.vertical.VerticalTransform verticalTransform) {
    this.verticalTransform = verticalTransform;
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

    axes.sort(new Grids.AxisComparator());
    GridAxis<?> xaxis = findCoordAxisByType(AxisType.GeoX, AxisType.Lon);
    GridAxis<?> yaxis = findCoordAxisByType(AxisType.GeoY, AxisType.Lat);
    GridHorizCoordinateSystem horizCsys = makeHorizCS(xaxis, yaxis, this.projection, this.latdata, this.londata);
    GridTimeCS tcs = makeTimeCS();
    return new GridCoordinateSystem(this.axes, tcs, this.verticalTransform, horizCsys);
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

  private GridTimeCS makeTimeCS() {
    GridAxis<?> rtAxis = axes.stream().filter(a -> a.getAxisType() == AxisType.RunTime).findFirst().orElse(null);
    GridAxis<?> toAxis = axes.stream().filter(a -> a.getAxisType() == AxisType.TimeOffset).findFirst().orElse(null);
    GridAxis<?> timeAxis = axes.stream().filter(a -> a.getAxisType() == AxisType.Time).findFirst().orElse(null);
    GridAxis<?> useAxis = (toAxis != null) ? toAxis : timeAxis;

    if (rtAxis != null && useAxis != null) {
      return GridTimeCS.createSingleOrOffset((GridAxisPoint) rtAxis, useAxis);
    } else if (useAxis != null) {
      return GridTimeCS.createObservation(useAxis);
    }
    // ok to not have a time coordinate
    return null;
  }

  private GridHorizCoordinateSystem makeHorizCS(GridAxis<?> xaxis, GridAxis<?> yaxis, @Nullable Projection projection,
      Array<Number> latdata, Array<Number> londata) {

    // LOOK heres now to find horizStaggerType in WRF NMM
    // String horizStaggerType = xaxis.attributes().findAttributeString(_Coordinate.Stagger, null);

    if (projection instanceof CurvilinearProjection) {
      return GridHorizCurvilinear.create((GridAxisPoint) xaxis, (GridAxisPoint) yaxis, latdata, londata);
    } else {
      Preconditions.checkArgument(xaxis instanceof GridAxisPoint);
      Preconditions.checkArgument(yaxis instanceof GridAxisPoint);
      return new GridHorizCoordinateSystem((GridAxisPoint) xaxis, (GridAxisPoint) yaxis, projection);
    }
  }

}
