/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import ucar.ma2.RangeIterator;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.CoordinateTransform;

import java.util.*;

/** A Coordinate System for gridded data. */
public interface GridCoordinateSystem {

  /** The name of the Grid Coordinate System. */
  String getName();

  /** Get the dimensions used by any of the Axes in the Coordinate System. */
  Iterable<Dimension> getDomain();

  /** True if all axes are 1 dimensional. */
  boolean isProductSet();

  /** True if both X and Y axes are 1 dimensional and are regularly spaced. */
  boolean isRegularSpatial();

  /** Get the Coordinate Transforms for this Coordinate System. */
  ImmutableList<CoordinateTransform> getCoordTransforms();

  /** the GridAxes that constitute this Coordinate System */
  Iterable<GridAxis> getGridAxes();

  /** Get the X axis. (either GeoX or Lon) */
  GridAxis1D getXHorizAxis();

  /** Get the Y axis. (either GeoY or Lat) */
  GridAxis1D getYHorizAxis();

  /** Get the Z axis (GeoZ, Height, Pressure). */
  @Nullable
  GridAxis1D getVerticalAxis();

  /** Get the Time axis. */
  GridAxis1DTime getTimeAxis();

  /** Get the ensemble axis. */
  @Nullable
  GridAxis1D getEnsembleAxis();

  /** Get the RunTime axis. */
  GridAxis1DTime getRunTimeAxis();

  /** Get the Time Offset axis. */
  GridAxis1D getTimeOffsetAxis();

  /** Get the horizontal Projection, if any. */
  @Nullable
  ucar.unidata.geoloc.Projection getProjection();

  /** Get the Vertical Transform for this coordinate system, if any. */
  @Nullable
  ucar.nc2.dataset.VerticalCT getVerticalCT();

  /** Does this use lat/lon horizontal axes? */
  boolean isLatLon();

  /** Is this a global coverage over longitude ? */
  boolean isGlobalLon();

  /** Get horizontal bounding box in lat, lon coordinates. For projection, only an approximation based on corners. */
  ucar.unidata.geoloc.LatLonRect getLatLonBoundingBox();

  /** Get horizontal bounding box in projection coordinates. */
  ucar.unidata.geoloc.ProjectionRect getBoundingBox();

  String showFnSummary();

  void show(Formatter f, boolean showCoords);

  class CoordReturn {
    public int x, y;
    public double xcoord, ycoord;
  }

  Optional<CoordReturn> findXYindexFromCoord(double x, double y);

  // LOOK: Optional, Nullable, Exception?
  Optional<GridCoordinateSystem> subset(GridSubset params, Formatter errLog);

  // LOOK what is this?
  List<RangeIterator> getRanges();

  /*
   * Get Index Ranges for the given lat, lon bounding box.
   * For projection, only an approximation based on corners.
   * Must have GridAxis1D or 2D for x and y axis.
   *
   * @param llbb a lat/lon bounding box.
   * 
   * @return list of 2 Range objects, first y then x.
   * 
   * @throws ucar.ma2.InvalidRangeException if llbb generates bad ranges
   * 
   * java.util.List<Range> getRangesFromLatLonRect(ucar.unidata.geoloc.LatLonRect llbb) throws
   * InvalidRangeException;
   * 
   * /**
   * Given a point in x,y coordinate space, find the x,y indices.
   *
   * @param x_coord position in x coordinate space, ie, units of getXHorizAxis().
   * 
   * @param y_coord position in y coordinate space, ie, units of getYHorizAxis().
   * 
   * @param result optionally pass in the result array to use.
   * 
   * @return int[2], 0=x, 1=y indices of the point. These will be -1 if out of range.
   * 
   * int[] findXYindexFromCoord(double x_coord, double y_coord, int[] result);
   * 
   * /**
   * Given a point in x,y coordinate space, find the x,y indices.
   * If outside the range, the closest point is returned
   *
   * @param x_coord position in x coordinate space, ie, units of getXHorizAxis().
   * 
   * @param y_coord position in y coordinate space, ie, units of getYHorizAxis().
   * 
   * @param result optionally pass in the result array to use.
   * 
   * @return int[2], 0=x, 1=y indices of the point.
   * 
   * int[] findXYindexFromCoordBounded(double x_coord, double y_coord, int[] result);
   * 
   * /**
   * Given a lat,lon point, find the x,y index of the containing ucar.nc2.grid point.
   *
   * @param lat latitude position.
   * 
   * @param lon longitude position.
   * 
   * @param result put result in here, may be null
   * 
   * @return int[2], 0=x,1=y indices in the coordinate system of the point. These will be -1 if out of range.
   * 
   * int[] findXYindexFromLatLon(double lat, double lon, int[] result);
   * 
   * /**
   * Given a lat,lon point, find the x,y index of the containing ucar.nc2.grid point.
   * If outside the range, the closest point is returned
   *
   * @param lat latitude position.
   * 
   * @param lon longitude position.
   * 
   * @param result return result here, may be null
   * 
   * @return int[2], 0=x,1=y indices in the coordinate system of the point.
   * 
   * int[] findXYindexFromLatLonBounded(double lat, double lon, int[] result);
   * 
   * /**
   * Get the Lat/Lon coordinates of the midpoint of a ucar.nc2.grid cell, using the x,y indices.
   *
   * @param xindex x index
   * 
   * @param yindex y index
   * 
   * @return lat/lon coordinate of the midpoint of the cell
   * 
   * LatLonPoint getLatLon(int xindex, int yindex);
   * 
   * /**
   * True if increasing z coordinate values means "up" in altitude
   *
   * @return true if increasing z coordinate values means "up" in altitude
   * boolean isZPositive();
   */
}
