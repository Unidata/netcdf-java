/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import ucar.array.Range;
import ucar.nc2.constants.AxisType;
import ucar.nc2.internal.grid.CylindricalCoord;
import ucar.nc2.internal.grid.SubsetHelpers;
import ucar.nc2.internal.grid.SubsetPointHelper;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPoints;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.CurvilinearProjection;
import ucar.unidata.geoloc.projection.LatLonProjection;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Manages the Projection GeoX/GeoY or Lat/Lon horizontal CoordinateSystem, with orthogonal axes.
 * Note that when isLatLon() is true, the coordinates are LatLon projection coordinates, where the longitude
 * coordinates are not constrained to +/- 180, and are monotonic.
 */
@Immutable
public class GridHorizCoordinateSystem {

  /**
   * Get the 1D X axis (either GeoX or Lon).
   */
  public GridAxisPoint getXHorizAxis() {
    return xaxis;
  }

  /**
   * Get the 1D Y axis (either GeoY or Lat).
   */
  public GridAxisPoint getYHorizAxis() {
    return yaxis;
  }

  /**
   * Get the horizontal Projection.
   */
  public Projection getProjection() {
    return projection;
  }

  /**
   * Does this use lat/lon horizontal axes?
   */
  public boolean isLatLon() {
    return projection instanceof LatLonProjection;
  }

  /**
   * If its curvilinear (will be instance of HorizCurviliner)
   */
  public boolean isCurvilinear() {
    return projection instanceof CurvilinearProjection;
  }

  /**
   * Is this a global coverage over longitude ?
   */
  public boolean isGlobalLon() {
    if (!isLatLon()) {
      return false;
    }
    LatLonRect rect = getLatLonBoundingBox();
    return rect.getWidth() >= 360;
  }

  public boolean hasAxis(String axisName) {
    return xaxis.getName().equals(axisName) || yaxis.getName().equals(axisName);
  }

  /**
   * The nominal sizes of the yaxis, xaxis as a list.
   */
  public List<Integer> getShape() {
    return ImmutableList.of(getYHorizAxis().getNominalSize(), getXHorizAxis().getNominalSize());
  }

  /**
   * Get the horizontal coordinate units, null for latlon. Needed to convert projection units.
   */
  @Nullable
  public String getGeoUnits() {
    return isLatLon() ? null : xaxis.getUnits();
  }

  /**
   * Get horizontal bounding box in lat, lon coordinates. When not isLatLon(), only an approximation based on corners.
   */
  public LatLonRect getLatLonBoundingBox() {
    return llbb;
  }

  /**
   * Get horizontal bounding box in projection coordinates.
   */
  public ProjectionRect getBoundingBox() {
    return mapArea;
  }

  /**
   * Get the Lat/Lon coordinates of the midpoint of a grid cell, using the x,y indices.
   *
   * @param xindex x index
   * @param yindex y index
   * @return lat/lon coordinate of the midpoint of the cell
   */
  public LatLonPoint getLatLon(int xindex, int yindex) {
    double x = xaxis.getCoordinate(xindex).doubleValue();
    double y = yaxis.getCoordinate(yindex).doubleValue();
    return isLatLon() ? LatLonPoint.create(y, x) : makeLatLon(x, y);
  }

  LatLonPoint makeLatLon(double xcoord, double ycoord) {
    return projection.projToLatLon(ProjectionPoint.create(xcoord, ycoord));
  }

  /**
   * Create a subsetted GridHorizCoordinateSystem based on the given parameters.
   * 
   * @param params latlonBB, projBB, horizStride or latlonPoint
   * @param builder optional, needed when subsetting across the longitude seam.
   * @param errlog add error messages here
   * @return subsetted GridHorizCoordinateSystem
   */
  Optional<GridHorizCoordinateSystem> subset(GridSubset params, @Nullable MaterializedCoordinateSystem.Builder builder,
      Formatter errlog) {
    Preconditions.checkNotNull(params);
    Preconditions.checkNotNull(errlog);
    Integer horizStride = params.getHorizStride();
    if (horizStride == null || horizStride < 1) {
      horizStride = 1;
    }

    GridAxisPoint xaxisSubset;
    GridAxisPoint yaxisSubset;
    LatLonRect llbbt = params.getLatLonBoundingBox();
    ProjectionRect projbb = params.getProjectionBoundingBox();

    if (projbb == null && llbbt != null) {
      projbb = getProjection().latLonToProjBB(llbbt);
    }

    ProjectionPoint projCoord = params.getProjectionPoint();
    if (projCoord != null) {
      Optional<CoordReturn> resulto = findXYindexFromCoord(projCoord.getX(), projCoord.getY());
      if (resulto.isEmpty()) {
        errlog.format("subset failed to find index for ProjectionPoint.");
        return Optional.empty();
      }
      CoordReturn result = resulto.get();

      // use that index to create a subset of just that point.
      Preconditions.checkNotNull(yaxis);
      Preconditions.checkNotNull(xaxis);
      yaxisSubset = yaxis.toBuilder().subsetWithRange(Range.make(result.yindex, result.yindex)).build();
      xaxisSubset = xaxis.toBuilder().subsetWithRange(Range.make(result.xindex, result.xindex)).build();
      return Optional.of(new GridHorizCoordinateSystem(xaxisSubset, yaxisSubset, this.projection));
    }

    LatLonPoint latlon = params.getLatLonPoint();
    if (latlon != null) {
      ProjectionPoint projCoord2 = getProjection().latLonToProj(latlon);
      Optional<CoordReturn> resulto = findXYindexFromCoord(projCoord2.getX(), projCoord2.getY());
      if (resulto.isEmpty()) {
        errlog.format("subset failed to find index for latlonPoint.");
        return Optional.empty();
      }
      CoordReturn result = resulto.get();

      // use that index to create a subset of just that point.
      Preconditions.checkNotNull(yaxis);
      Preconditions.checkNotNull(xaxis);
      yaxisSubset = yaxis.toBuilder().subsetWithRange(Range.make(result.yindex, result.yindex)).build();
      xaxisSubset = xaxis.toBuilder().subsetWithRange(Range.make(result.xindex, result.xindex)).build();
      return Optional.of(new GridHorizCoordinateSystem(xaxisSubset, yaxisSubset, this.projection));
    }

    if (projbb != null) {
      SubsetPointHelper yhelper = new SubsetPointHelper(yaxis);
      Optional<GridAxisPoint.Builder<?>> ybo =
          yhelper.subsetRange(projbb.getMinY(), projbb.getMaxY(), horizStride, errlog);
      if (ybo.isEmpty()) {
        return Optional.empty();
      }
      yaxisSubset = ybo.get().build();

      if (builder != null && CylindricalCoord.isCylindrical(this)) {
        // possible subset across the longitude cylinder seam
        CylindricalCoord lonCoord = new CylindricalCoord(this);
        Optional<GridAxisPoint> axisO = lonCoord.subsetLon(projbb, horizStride, errlog);
        if (axisO.isEmpty()) {
          return Optional.empty();
        }
        xaxisSubset = axisO.get();
        if (lonCoord.needsSpecialRead()) {
          builder.setCylindricalCoord(lonCoord);
        }

      } else {
        SubsetPointHelper xhelper = new SubsetPointHelper(xaxis);
        Optional<GridAxisPoint.Builder<?>> xbo =
            xhelper.subsetRange(projbb.getMinX(), projbb.getMaxX(), horizStride, errlog);
        if (xbo.isEmpty()) {
          return Optional.empty();
        }
        xaxisSubset = xbo.get().build();
      }

    } else if (horizStride > 1) { // no bounding box, just horiz stride
      Preconditions.checkNotNull(yaxis);
      Preconditions.checkNotNull(xaxis);
      yaxisSubset = yaxis.toBuilder().subsetWithStride(horizStride).build();
      xaxisSubset = xaxis.toBuilder().subsetWithStride(horizStride).build();

    } else {
      return Optional.of(this);
    }

    return Optional.of(new GridHorizCoordinateSystem(xaxisSubset, yaxisSubset, this.projection));
  }

  ///////////////////////////////////////////////////////////////////////////////

  /**
   * The bounds of a particular grid cell, in projection coordinates. Return value from cells().
   */
  @Immutable
  public class CellBounds {
    /**
     * lower left.
     */
    public final CoordReturn ll;
    /**
     * lower right.
     */
    public final CoordReturn lr;
    /**
     * upper right.
     */
    public final CoordReturn ur;
    /**
     * upper left.
     */
    public final CoordReturn ul;
    /**
     * The data index
     */
    public final int xindex, yindex;

    public CellBounds(int xindex, int yindex) {
      this.xindex = xindex;
      this.yindex = yindex;
      CoordInterval xintv = xaxis.getCoordInterval(xindex);
      CoordInterval yintv = yaxis.getCoordInterval(yindex);
      this.ll = new CoordReturn(xintv.start(), yintv.start(), xindex, yindex);
      this.lr = new CoordReturn(xintv.end(), yintv.start(), xindex, yindex);
      this.ur = new CoordReturn(xintv.end(), yintv.end(), xindex, yindex);
      this.ul = new CoordReturn(xintv.start(), yintv.end(), xindex, yindex);
    }

    public CellBounds(CoordReturn ll, CoordReturn lr, CoordReturn ur, CoordReturn ul, int xindex, int yindex) {
      this.ll = ll;
      this.lr = lr;
      this.ur = ur;
      this.ul = ul;
      this.xindex = xindex;
      this.yindex = yindex;
    }

    @Override
    public String toString() {
      return String.format("CoordBounds{ (%d,%d): ll=%s lr=%s ur=%s ul=%s}", xindex, yindex, ll.toStringShort(),
          lr.toStringShort(), ur.toStringShort(), ul.toStringShort());
    }
  }

  /**
   * An iterator over each cell of the GridHorizCoordinateSystem.
   */
  public Iterable<CellBounds> cells() {
    return () -> new CellBoundsIterator(xaxis.getNominalSize(), yaxis.getNominalSize());
  }

  private class CellBoundsIterator extends AbstractIterator<CellBounds> {
    final int nx;
    final int ny;
    int xindex = 0;
    int yindex = 0;

    public CellBoundsIterator(int nx, int ny) {
      this.nx = nx;
      this.ny = ny;
    }

    @Override
    protected CellBounds computeNext() {
      if (xindex >= nx) {
        yindex++;
        xindex = 0;
      }
      if (yindex >= ny) {
        return endOfData();
      }
      CellBounds result = new CellBounds(xindex, yindex);
      xindex++;
      return result;
    }
  }

  /**
   * The cell index in which a grid coordinate is found. Return value from findXYindexFromCoord().
   */
  @Immutable
  public static class CoordReturn {
    /**
     * The cell index
     */
    public final int xindex, yindex;
    /**
     * The x,y projection grid coordinate.
     */
    public final double xcoord, ycoord;

    public CoordReturn(double xcoord, double ycoord, int xindex, int yindex) {
      this.xindex = xindex;
      this.yindex = yindex;
      this.xcoord = xcoord;
      this.ycoord = ycoord;
    }

    @Override
    public String toString() {
      return String.format("CoordReturn{xindex=%d, yindex=%d, xcoord=%f, ycoord=%f", xindex, yindex, xcoord, ycoord);
    }

    public String toStringShort() {
      return String.format("[%f,%f]", xcoord, ycoord);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      CoordReturn that = (CoordReturn) o;
      return xindex == that.xindex && yindex == that.yindex && Double.compare(that.xcoord, xcoord) == 0
          && Double.compare(that.ycoord, ycoord) == 0;
    }

    @Override
    public int hashCode() {
      return Objects.hash(xindex, yindex, xcoord, ycoord);
    }
  }

  /**
   * From the (x,y) projection point, find the indices and coordinates of the horizontal 2D grid.
   *
   * @param x x of point
   * @param y y of point
   * @return empty if not in the grid.
   */
  public Optional<CoordReturn> findXYindexFromCoord(double x, double y) {
    if (xaxis.getAxisType() == AxisType.Lon) {
      x = LatLonPoints.lonNormalFrom(x, xaxis.getCoordinate(0).doubleValue()); // LOOK ??
    }

    int xindex = SubsetHelpers.findCoordElement(xaxis, x, false);
    int yindex = SubsetHelpers.findCoordElement(yaxis, y, false);

    if (xindex >= 0 && xindex < xaxis.getNominalSize() && yindex >= 0 && yindex < yaxis.getNominalSize()) {
      double xcoord = xaxis.getCoordinate(xindex).doubleValue();
      double ycoord = yaxis.getCoordinate(yindex).doubleValue();
      return Optional.of(new CoordReturn(xcoord, ycoord, xindex, yindex));
    } else {
      return Optional.empty();
    }
  }

  /**
   * From the (x,y) projection point, find the indices and coordinates of the horizontal 2D grid, with an initial guess.
   * The initial guess is an optimization used for curvilinear grids, otherwise not needed.
   *
   * @param x x of point
   * @param y y of point
   * @param initial initial guess, used eg for mouse tracking in UI
   * @return empty if not in the grid.
   */
  public Optional<CoordReturn> findXYindexFromCoord(double x, double y, @Nullable int[] initial) {
    return findXYindexFromCoord(x, y);
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  final GridAxisPoint xaxis;
  final GridAxisPoint yaxis;
  private final Projection projection;
  private final ProjectionRect mapArea;
  private final LatLonRect llbb;

  public GridHorizCoordinateSystem(GridAxisPoint xaxis, GridAxisPoint yaxis, @Nullable Projection projection) {
    Preconditions.checkNotNull(xaxis);
    Preconditions.checkNotNull(yaxis);
    this.xaxis = convertUnits(xaxis);
    this.yaxis = convertUnits(yaxis);
    // TODO set the LatLon seam?
    this.projection = projection == null ? new LatLonProjection() : projection;
    this.mapArea = calcBoundingBox();
    this.llbb = calcLatLonBoundingBox();
  }

  // for GridHorizCurvilinear
  GridHorizCoordinateSystem(GridAxisPoint xaxis, GridAxisPoint yaxis, Projection projection, ProjectionRect mapArea,
      LatLonRect llbb) {
    Preconditions.checkNotNull(xaxis);
    Preconditions.checkNotNull(yaxis);
    this.xaxis = convertUnits(xaxis);
    this.yaxis = convertUnits(yaxis);
    this.projection = projection;
    this.mapArea = mapArea;
    this.llbb = llbb;
  }

  // convert projection units to km
  private GridAxisPoint convertUnits(GridAxisPoint axis) {
    String units = axis.getUnits();
    SimpleUnit axisUnit = SimpleUnit.factory(units);
    if (axisUnit == null) {
      return axis;
    }

    double factor;
    try {
      factor = axisUnit.convertTo(1.0, SimpleUnit.kmUnit);
      if (factor == 1.0) {
        return axis;
      }
    } catch (IllegalArgumentException e) {
      return axis;
    }

    GridAxisPoint.Builder<?> newAxisBuilder = axis.toBuilder();
    newAxisBuilder.changeUnits(factor);
    newAxisBuilder.setUnits("km");
    return newAxisBuilder.build();
  }

  private ProjectionRect calcBoundingBox() {
    double y1 = yaxis.getCoordInterval(0).start();
    double x1 = xaxis.getCoordInterval(0).start();

    double y2 = yaxis.getCoordInterval(yaxis.getNominalSize() - 1).end();
    double x2 = xaxis.getCoordInterval(xaxis.getNominalSize() - 1).end();

    return new ProjectionRect(x1, y1, x2, y2);
  }

  private LatLonRect calcLatLonBoundingBox() {
    if (isLatLon()) {
      double y1 = yaxis.getCoordInterval(0).start();
      double x1 = xaxis.getCoordInterval(0).start();

      double y2 = yaxis.getCoordInterval(yaxis.getNominalSize() - 1).end();
      double x2 = xaxis.getCoordInterval(xaxis.getNominalSize() - 1).end();

      LatLonPoint llpt = LatLonPoint.create(Math.min(y1, y2), Math.min(x1, x2));
      return new LatLonRect(llpt, Math.abs(y2 - y1), Math.abs(x2 - x1));

    } else {
      return projection.projToLatLonBB(getBoundingBox());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GridHorizCoordinateSystem that = (GridHorizCoordinateSystem) o;
    return Objects.equals(xaxis, that.xaxis) && Objects.equals(yaxis, that.yaxis)
        && Objects.equals(projection, that.projection);
  }

  @Override
  public int hashCode() {
    return Objects.hash(xaxis, yaxis, projection);
  }

  @Override
  public String toString() {
    return "GridHorizCoordinateSystem{" + "xaxis=" + xaxis + ", yaxis=" + yaxis + ", projection=" + projection + '}';
  }
}
