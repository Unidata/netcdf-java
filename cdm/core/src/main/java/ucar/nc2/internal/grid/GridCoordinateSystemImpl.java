/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import ucar.ma2.RangeIterator;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.*;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxis1D;
import ucar.nc2.grid.GridAxis1DTime;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.unidata.geoloc.*;

import javax.annotation.Nullable;
import java.util.*;

/** Implementation of GridCoordSystem */
class GridCoordinateSystemImpl implements GridCoordinateSystem {
  private final String name;
  private final Projection projection;
  private final ImmutableList<GridAxis> axes;
  private final ImmutableList<CoordinateTransform> transforms;
  private final Set<Dimension> domain;

  private final GridAxis1D xaxis, yaxis;
  private final GridAxis1D vertAxis, ensAxis;
  private final GridAxis1DTime timeAxis, rtAxis, timeOffsetAxis;

  private final FeatureType featureType;
  private final boolean isLatLon;

  /**
   * Create a GeoGridCoordSys from an existing Coordinate System.
   * This will choose which axes are the XHoriz, YHoriz, Vertical, Time, RunTIme, Ensemble.
   * If theres a Projection, it will set its map area
   *
   * @param builder create from this GridCoordSystemBuilder.
   */
  GridCoordinateSystemImpl(GridCoordSystemBuilder builder, Map<String, GridAxis> gridAxes) {
    // make name based on coordinate
    this.name = CoordinateSystem.makeName(builder.allAxes);
    this.projection = builder.orgProj;
    this.featureType = builder.type;
    this.isLatLon = builder.isLatLon;

    ImmutableList.Builder<GridAxis> axesb = ImmutableList.builder();
    for (CoordinateAxis axis : builder.allAxes) {
      if (axis.getAxisType() == null) {
        continue;
      }
      axesb.add(Preconditions.checkNotNull(gridAxes.get(axis.getFullName())));
    }
    this.axes = axesb.build();

    this.transforms = ImmutableList.copyOf(builder.coordTransforms);

    ImmutableSet.Builder<Dimension> domainb = ImmutableSet.builder();
    for (CoordinateAxis axis : builder.independentAxes) {
      domainb.addAll(axis.getDimensions());
    }
    this.domain = domainb.build();

    this.xaxis = (GridAxis1D) findCoordAxis(AxisType.GeoX, AxisType.Lon);
    this.yaxis = (GridAxis1D) findCoordAxis(AxisType.GeoY, AxisType.Lat);
    this.vertAxis = (GridAxis1D) findCoordAxis(AxisType.GeoZ, AxisType.Height, AxisType.Pressure);
    this.ensAxis = (GridAxis1D) findCoordAxis(AxisType.Ensemble);
    this.timeAxis = (GridAxis1DTime) findCoordAxis(AxisType.Time);
    this.rtAxis = (GridAxis1DTime) findCoordAxis(AxisType.RunTime);
    this.timeOffsetAxis = (GridAxis1DTime) findCoordAxis(AxisType.TimeOffset);
  }

  @Override
  public String getName() {
    return name;
  }

  public FeatureType getCoverageType() {
    return featureType;
  }

  @Override
  public ImmutableList<GridAxis> getCoordAxes() {
    return this.axes;
  }

  public GridAxis findCoordAxis(String shortName) {
    for (GridAxis axis : axes) {
      if (axis.getName().equals(shortName))
        return axis;
    }
    return null;
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

  public ImmutableList<CoordinateTransform> getCoordTransforms() {
    return transforms;
  }

  /** get the X Horizontal axis (either GeoX or Lon) */
  @Override
  public GridAxis1D getXHorizAxis() {
    return this.xaxis;
  }

  /** get the Y Horizontal axis (either GeoY or Lat) */
  @Override
  public GridAxis1D getYHorizAxis() {
    return this.yaxis;
  }

  /** get the Vertical axis (either Geoz, Height, or Pressure) */
  @Override
  @Nullable
  public GridAxis1D getVerticalAxis() {
    return this.vertAxis;
  }

  @Override
  @Nullable
  public GridAxis1DTime getTimeAxis() {
    return this.timeAxis;
  }

  @Override
  @Nullable
  public GridAxis1DTime getRunTimeAxis() {
    return this.rtAxis;
  }

  @Override
  @Nullable
  public GridAxis1D getEnsembleAxis() {
    return this.ensAxis;
  }

  @Override
  @Nullable
  public Projection getProjection() {
    return projection;
  }

  /**
   * is this a Lat/Lon coordinate system?
   */
  public boolean isLatLon() {
    return isLatLon;
  }

  /**
   * Is this a global coverage over longitude ?
   *
   * @return true if isLatLon and longitude wraps
   */
  public boolean isGlobalLon() {
    if (!isLatLon)
      return false;
    GridAxis1D lon = getXHorizAxis();
    double first = lon.getCoordEdgeFirst();
    double last = lon.getCoordEdgeLast();
    double min = Math.min(first, last);
    double max = Math.max(first, last);
    return (max - min) >= 360;
  }

  /**
   * true if x and y axes are CoordinateAxis1D and are regular
   */
  public boolean isRegularSpatial() {
    if (!isRegularSpatial(getXHorizAxis()))
      return false;
    return isRegularSpatial(getYHorizAxis());
  }

  @Override
  public Optional<CoordReturn> findXYindexFromCoord(double x, double y) {
    return Optional.empty();
  }

  private boolean isRegularSpatial(GridAxis1D axis) {
    if (axis == null)
      return true;
    return axis.isRegular();
  }

  private String horizStaggerType;

  public String getHorizStaggerType() {
    return horizStaggerType;
  }

  public void setHorizStaggerType(String horizStaggerType) {
    this.horizStaggerType = horizStaggerType;
  }

  private ProjectionRect mapArea;

  /**
   * Get the x,y bounding box in projection coordinates.
   */
  @Override
  public ProjectionRect getBoundingBox() {
    if (mapArea == null) {
      GridAxis1D xaxis1 = getXHorizAxis();
      GridAxis1D yaxis1 = getYHorizAxis();

      mapArea = new ProjectionRect(xaxis1.getCoordEdgeFirst(), yaxis1.getCoordEdgeFirst(), xaxis1.getCoordEdgeLast(),
          yaxis1.getCoordEdgeLast());
    }

    return mapArea;
  }

  /**
   * Get the Lat/Lon coordinates of the midpoint of a grid cell, using the x,y indices
   *
   * @param xindex x index
   * @param yindex y index
   * @return lat/lon coordinate of the midpoint of the cell
   *
   *         public LatLonPoint getLatLon(int xindex, int yindex) {
   *         double x, y;
   * 
   *         GridAxis1D horizXaxis = getXHorizAxis();
   *         GridAxis1D horizYaxis = getYHorizAxis();
   *         if (horizXaxis instanceof CoordinateAxis1D) {
   *         CoordinateAxis1D horiz1D = (CoordinateAxis1D) horizXaxis;
   *         x = horiz1D.getCoordValue(xindex);
   *         } else {
   *         CoordinateAxis2D horiz2D = (CoordinateAxis2D) horizXaxis;
   *         x = horiz2D.getCoordValue(yindex, xindex);
   *         }
   * 
   *         if (horizYaxis instanceof CoordinateAxis1D) {
   *         CoordinateAxis1D horiz1D = (CoordinateAxis1D) horizYaxis;
   *         y = horiz1D.getCoordValue(yindex);
   *         } else {
   *         CoordinateAxis2D horiz2D = (CoordinateAxis2D) horizYaxis;
   *         y = horiz2D.getCoordValue(yindex, xindex);
   *         }
   * 
   *         return isLatLon() ? LatLonPoint.create(y, x) : getLatLon(x, y);
   *         }
   * 
   *         public LatLonPoint getLatLon(double xcoord, double ycoord) {
   *         Projection dataProjection = getProjection();
   *         return dataProjection.projToLatLon(ProjectionPoint.create(xcoord, ycoord));
   *         }
   */

  private LatLonRect llbb;

  /**
   * Get horizontal bounding box in lat, lon coordinates.
   *
   * @return lat, lon bounding box.
   */
  @Override
  public LatLonRect getLatLonBoundingBox() {

    if (llbb == null) {
      GridAxis1D horizXaxis = getXHorizAxis();
      GridAxis1D horizYaxis = getYHorizAxis();
      if (isLatLon()) {
        double startLat = horizYaxis.getCoordEdgeFirst();
        double startLon = horizXaxis.getCoordEdgeFirst();

        double deltaLat = horizYaxis.getCoordEdgeLast() - startLat;
        double deltaLon = horizXaxis.getCoordEdgeLast() - startLon;

        LatLonPoint llpt = LatLonPoint.create(startLat, startLon);
        llbb = new LatLonRect.Builder(llpt, deltaLat, deltaLon).build();

      } else {
        Projection dataProjection = getProjection();
        ProjectionRect bb = getBoundingBox();
        if (bb != null) {
          llbb = dataProjection.projToLatLonBB(bb);
        }
      }
    }

    return llbb;
  }

  @Override
  public String toString() {
    Formatter buff = new Formatter();
    show(buff, false);
    return buff.toString();
  }

  public void show(Formatter f, boolean showCoords) {
    f.format("Coordinate System (%s)%n", getName());

    showCoordinateAxis(getRunTimeAxis(), f, showCoords);
    showCoordinateAxis(getEnsembleAxis(), f, showCoords);
    showCoordinateAxis(getTimeAxis(), f, showCoords);
    showCoordinateAxis(getVerticalAxis(), f, showCoords);
    showCoordinateAxis(getYHorizAxis(), f, showCoords);
    showCoordinateAxis(getXHorizAxis(), f, showCoords);

    if (projection != null) {
      f.format(" Projection: %s %s%n", projection.getName(), projection.paramsToString());
    }
  }

  private void showCoordinateAxis(GridAxis1D axis, Formatter f, boolean showCoords) {
    if (axis == null)
      return;
    f.format(" rt=%s (%s)", axis.getName(), axis.getClass().getName());
    if (showCoords)
      showCoords(axis, f);
    f.format("%n");
  }

  private void showCoords(GridAxis axis, Formatter f) {
    if (axis instanceof GridAxis1D && axis.getDataType().isNumeric()) {
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
    f.format(" %s%n", axis.getUnits());
  }

  @Override
  public Iterable<Dimension> getDomain() {
    return domain;
  }

  @Override
  public boolean isProductSet() {
    return true;
  }

  @Override
  public Optional<GridCoordinateSystem> subset(SubsetParams params, Formatter errLog) {
    throw new UnsupportedOperationException();
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

}
