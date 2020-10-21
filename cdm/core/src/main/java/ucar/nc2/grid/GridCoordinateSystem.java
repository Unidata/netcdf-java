/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import javax.annotation.Nullable;

import ucar.ma2.RangeIterator;
import ucar.nc2.Dimension;
import ucar.nc2.ft2.coverage.SubsetParams;

import java.util.*;

/**
 * A Coordinate System for gridded data. Assume:
 * <ul>
 * <li>Has one dimensional X, Y, Z, T, E axes.
 * <li>T is CoordinateAxisTime.
 * <li>An optional HorizontalTransform can provide a lat and lon coordinate that may be 1 or 2 dimensional.
 * <li>An optional VerticalTransform can provide a height or pressure coordinate that may be 1-4 dimensional.
 * </ul>
 * <p/>
 */
public interface GridCoordinateSystem {

  /** The name of the Grid Coordinate System. */
  String getName();

  /** Get the dimensions used by any of the Axes in the Coordinate System. */
  Iterable<Dimension> getDomain();

  /** True if all axes are 1 dimensional. */
  boolean isProductSet();

  Iterable<GridAxis> getCoordAxes();

  /** Get the X axis. (either GeoX or Lon) */
  GridAxis1D getXHorizAxis();

  /** Get the Y axis. (either GeoY or Lat) */
  GridAxis1D getYHorizAxis();

  /** Get the Z axis. */
  @Nullable
  GridAxis1D getVerticalAxis();

  /** Get the Time axis. */
  GridAxis1DTime getTimeAxis();

  /** Get the ensemble axis. */
  @Nullable
  GridAxis1D getEnsembleAxis();

  /**
   * Get the RunTime axis. Must be 1 dimensional.
   * A runtime coordinate must be a udunit date or ISO String, so it can always be converted to a Date.
   * Typical meaning is the date that a Forecast Model Run is made.
   * 
   * @return RunTime CoordinateAxis, may be null.
   */
  GridAxis1DTime getRunTimeAxis();

  /**
   * Get the Projection CoordinateTransform. It must exist if !isLatLon().
   * 
   * @return ProjectionCT or null.
   * @Nullable
   *           ProjectionCT getProjectionCT();
   */

  /**
   * Get the Projection that performs the transform math.
   * Same as getProjectionCT().getProjection().
   * 
   * @return Projection or null.
   */
  @Nullable
  ucar.unidata.geoloc.Projection getProjection();

  /**
   * Get the Vertical CoordinateTransform, it it exists.
   * 
   * @return VerticalCT or null.
   * @Nullable
   *           VerticalCT getVerticalCT();
   * 
   *           /**
   *           Get the VerticalTransform that performs the transform math.
   *           Same as getVerticalCT().getVerticalTransform().
   * 
   * @return VerticalTransform or null.
   * @Nullable
   *           VerticalTransform getVerticalTransform();
   */

  // horiz

  /**
   * Does this use lat/lon horizontal axes?
   * If not, then the horizontal axes are GeoX, GeoY, and there must be a Projection defined.
   * 
   * @return true if lat/lon horizontal axes
   */
  boolean isLatLon();

  /**
   * Is this a global coverage over longitude ?
   * 
   * @return true if isLatLon and longitude extent >= 360 degrees
   */
  boolean isGlobalLon();

  /**
   * Get horizontal bounding box in lat, lon coordinates.
   * For projection, only an approximation based on corners.
   * 
   * @return LatLonRect bounding box.
   */
  ucar.unidata.geoloc.LatLonRect getLatLonBoundingBox();

  /**
   * Get horizontal bounding box in projection coordinates.
   * For lat/lon, the ProjectionRect has units of degrees north and east.
   * 
   * @return ProjectionRect bounding box.
   */
  ucar.unidata.geoloc.ProjectionRect getBoundingBox();

  /**
   * True if both X and Y axes are 1 dimensional and are regularly spaced.
   * 
   * @return true if both X and Y axes are 1 dimensional and are regularly spaced.
   */
  boolean isRegularSpatial();

  class CoordReturn {
    public int x, y;
    public double xcoord, ycoord;
  }

  Optional<CoordReturn> findXYindexFromCoord(double x, double y);

  Optional<GridCoordinateSystem> subset(SubsetParams params, Formatter errLog);

  List<RangeIterator> getRanges();

  /**
   * Get Index Ranges for the given lat, lon bounding box.
   * For projection, only an approximation based on corners.
   * Must have GridAxis1D or 2D for x and y axis.
   *
   * @param llbb a lat/lon bounding box.
   * @return list of 2 Range objects, first y then x.
   * @throws ucar.ma2.InvalidRangeException if llbb generates bad ranges
   * 
   *         java.util.List<Range> getRangesFromLatLonRect(ucar.unidata.geoloc.LatLonRect llbb) throws
   *         InvalidRangeException;
   * 
   *         /**
   *         Given a point in x,y coordinate space, find the x,y indices.
   *
   * @param x_coord position in x coordinate space, ie, units of getXHorizAxis().
   * @param y_coord position in y coordinate space, ie, units of getYHorizAxis().
   * @param result optionally pass in the result array to use.
   * @return int[2], 0=x, 1=y indices of the point. These will be -1 if out of range.
   * 
   *         int[] findXYindexFromCoord(double x_coord, double y_coord, int[] result);
   * 
   *         /**
   *         Given a point in x,y coordinate space, find the x,y indices.
   *         If outside the range, the closest point is returned
   *
   * @param x_coord position in x coordinate space, ie, units of getXHorizAxis().
   * @param y_coord position in y coordinate space, ie, units of getYHorizAxis().
   * @param result optionally pass in the result array to use.
   * @return int[2], 0=x, 1=y indices of the point.
   * 
   *         int[] findXYindexFromCoordBounded(double x_coord, double y_coord, int[] result);
   * 
   *         /**
   *         Given a lat,lon point, find the x,y index of the containing ucar.nc2.grid point.
   *
   * @param lat latitude position.
   * @param lon longitude position.
   * @param result put result in here, may be null
   * @return int[2], 0=x,1=y indices in the coordinate system of the point. These will be -1 if out of range.
   * 
   *         int[] findXYindexFromLatLon(double lat, double lon, int[] result);
   * 
   *         /**
   *         Given a lat,lon point, find the x,y index of the containing ucar.nc2.grid point.
   *         If outside the range, the closest point is returned
   *
   * @param lat latitude position.
   * @param lon longitude position.
   * @param result return result here, may be null
   * @return int[2], 0=x,1=y indices in the coordinate system of the point.
   * 
   *         int[] findXYindexFromLatLonBounded(double lat, double lon, int[] result);
   * 
   *         /**
   *         Get the Lat/Lon coordinates of the midpoint of a ucar.nc2.grid cell, using the x,y indices.
   *
   * @param xindex x index
   * @param yindex y index
   * @return lat/lon coordinate of the midpoint of the cell
   * 
   *         LatLonPoint getLatLon(int xindex, int yindex);
   * 
   *         /**
   *         True if increasing z coordinate values means "up" in altitude
   *
   * @return true if increasing z coordinate values means "up" in altitude
   *         boolean isZPositive();
   */
}
