package ucar.nc2.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.nc2.constants.AxisType;
import ucar.nc2.internal.grid.CylindricalCoord;
import ucar.nc2.internal.grid.SubsetHelpers;
import ucar.nc2.internal.grid.SubsetPointHelper;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPoints;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.CurvilinearProjection;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.geoloc.projection.sat.Geostationary;
import ucar.unidata.geoloc.projection.sat.MSGnavigation;
import ucar.unidata.geoloc.projection.sat.VerticalPerspectiveView;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Manages or Projection GeoX/GeoY or Lat/Lon horizontal CoordinateSystem, with orthogonal axes. */
@Immutable
public class GridHorizCoordinateSystem {

  /** Get the 1D X axis (either GeoX or Lon). */
  public GridAxisPoint getXHorizAxis() {
    return xaxis;
  }

  /** Get the 1D Y axis (either GeoY or Lat). */
  public GridAxisPoint getYHorizAxis() {
    return yaxis;
  }

  /** Get the horizontal Projection. */
  public Projection getProjection() {
    return projection;
  }

  /** Does this use lat/lon horizontal axes? */
  public boolean isLatLon() {
    return projection instanceof LatLonProjection;
  }

  /** If its curvilinear (will be instance of HorizCurviliner) */
  public boolean isCurvilinear() {
    return projection instanceof CurvilinearProjection;
  }

  /** Is this a global coverage over longitude ? */
  public boolean isGlobalLon() {
    if (!isLatLon()) {
      return false;
    }
    LatLonRect rect = getLatLonBoundingBox();
    return rect.getWidth() >= 360;
  }

  public List<Integer> getShape() {
    return ImmutableList.of(getYHorizAxis().getNominalSize(), getXHorizAxis().getNominalSize());
  }

  public List<ucar.array.Range> getSubsetRanges() {
    return ImmutableList.of(getYHorizAxis().getSubsetRange(), getXHorizAxis().getSubsetRange());
  }

  /**
   * Get the horizontal coordinate units, null for latlon. Needed to convert projection units.
   * TODO add to projection directly ??
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

  /** Get horizontal bounding box in projection coordinates. */
  public ProjectionRect getBoundingBox() {
    return mapArea;
  }

  /**
   * Get the Lat/Lon coordinates of the midpoint of a grid cell, using the x,y indices
   * LOOK needed?
   * 
   * @param xindex x index
   * @param yindex y index
   * @return lat/lon coordinate of the midpoint of the cell
   */
  public LatLonPoint getLatLon(int xindex, int yindex) {
    double x = xaxis.getCoordinate(xindex).doubleValue();
    double y = yaxis.getCoordinate(yindex).doubleValue();
    return isLatLon() ? LatLonPoint.create(y, x) : getLatLon(x, y);
  }

  private LatLonPoint getLatLon(double xcoord, double ycoord) {
    return projection.projToLatLon(ProjectionPoint.create(xcoord, ycoord));
  }

  public Optional<GridHorizCoordinateSystem> subset(GridSubset params, Formatter errlog) {
    return subset(params, null, errlog);
  }

  /** Subset both x and y axis based on the given parameters. */
  // see ucar.nc2.ft2.coverage.HorizCoordSys.subset()
  public Optional<GridHorizCoordinateSystem> subset(GridSubset params, MaterializedCoordinateSystem.Builder builder,
      Formatter errlog) {
    Preconditions.checkNotNull(params);
    Preconditions.checkNotNull(errlog);
    Integer horizStride = params.getHorizStride();
    if (horizStride == null || horizStride < 1) {
      horizStride = 1;
    }

    GridAxisPoint xaxisSubset;
    GridAxisPoint yaxisSubset;
    LatLonRect llbb = params.getLatLonBoundingBox();
    ProjectionRect projbb = params.getProjectionBoundingBox();

    if (projbb == null && llbb != null && !isLatLon()) {
      projbb = getProjection().latLonToProjBB(llbb);
      llbb = null;
    }

    if (projbb != null && llbb == null && isLatLon()) {
      llbb = getProjection().projToLatLonBB(projbb);
      projbb = null;
    }

    // LOOK not done: GridSubset.latlonPoint

    if (projbb != null) {
      SubsetPointHelper yhelper = new SubsetPointHelper(yaxis);
      Optional<GridAxisPoint.Builder<?>> ybo =
          yhelper.subsetRange(projbb.getMinY(), projbb.getMaxY(), horizStride, errlog);
      if (ybo.isEmpty()) {
        return Optional.empty();
      }
      yaxisSubset = ybo.get().build();

      SubsetPointHelper xhelper = new SubsetPointHelper(xaxis);
      Optional<GridAxisPoint.Builder<?>> xbo =
          xhelper.subsetRange(projbb.getMinX(), projbb.getMaxX(), horizStride, errlog);
      if (xbo.isEmpty()) {
        return Optional.empty();
      }
      xaxisSubset = xbo.get().build();

    } else if (llbb != null) {
      SubsetPointHelper yhelper = new SubsetPointHelper(yaxis);
      Optional<GridAxisPoint.Builder<?>> ybo =
          yhelper.subsetRange(llbb.getLatMin(), llbb.getLatMax(), horizStride, errlog);
      if (ybo.isEmpty()) {
        return Optional.empty();
      }
      yaxisSubset = ybo.get().build();

      if (builder != null && CylindricalCoord.isCylindrical(this)) {
        // possible subset across the longitude cylinder seam
        CylindricalCoord lonCoord = new CylindricalCoord(this);
        Optional<GridAxisPoint> axisO = lonCoord.subsetLon(llbb, errlog);
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
            xhelper.subsetRange(llbb.getLonMin(), llbb.getLonMax(), horizStride, errlog);
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

  /** The bounds of a particular grid cell. Return value from cells(). */
  @Immutable
  public class CellBounds {
    public final CoordReturn ll;
    public final CoordReturn lr;
    public final CoordReturn ur;
    public final CoordReturn ul;
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

  /** An iterator over each cell of the GridHorizCoordinateSystem. */
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

  /** Return value from findXYindexFromCoord(). */
  @Immutable
  public static class CoordReturn {
    /** The data index */
    public final int xindex, yindex;
    /** The x,y (or lon, lat) grid coordinate. */
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
   * From the (x,y) projection point, find the indices and coordinates of the horizontal 2D grid.
   *
   * @param x x of point
   * @param y y of point
   * @param initial initial guess, used eg for mouse tracking in UI
   * @return empty if not in the grid.
   */
  public Optional<CoordReturn> findXYindexFromCoord(double x, double y, @Nullable int[] initial) {
    return findXYindexFromCoord(x, y);
  }

  /**
   * Get Index Ranges for the given lat, lon bounding box.
   * For projection, only an approximation based on latlon corners.
   * From dt.grid.GridCoordSys, used in subsetting the csys
   *
   * @param rect the requested lat/lon bounding box
   * @return list of 2 Range objects, first y then x.
   */
  List<Range> getRangesFromLatLonRect(LatLonRect rect) throws InvalidRangeException {
    double minx, maxx, miny, maxy;

    if (projection != null && !(projection instanceof VerticalPerspectiveView) && !(projection instanceof MSGnavigation)
        && !(projection instanceof Geostationary)) { // LOOK kludge - how to do this generrally ??
      // first clip the request rectangle to the bounding box of the grid
      LatLonRect bb = getLatLonBoundingBox();
      LatLonRect rect2 = bb.intersect(rect);
      if (null == rect2)
        throw new InvalidRangeException("Request Bounding box does not intersect Grid ");
      rect = rect2;
    }

    if (isLatLon()) {
      LatLonPoint llpt = rect.getLowerLeftPoint();
      LatLonPoint urpt = rect.getUpperRightPoint();
      LatLonPoint lrpt = rect.getLowerRightPoint();
      LatLonPoint ulpt = rect.getUpperLeftPoint();

      minx = getMinOrMaxLon(llpt.getLongitude(), ulpt.getLongitude(), true);
      miny = Math.min(llpt.getLatitude(), lrpt.getLatitude());
      maxx = getMinOrMaxLon(urpt.getLongitude(), lrpt.getLongitude(), false);
      maxy = Math.min(ulpt.getLatitude(), urpt.getLatitude());

      // LOOK ?? normalize to [minLon,minLon+360]
      /*
       * MinMax minmaxLon = xaxis.getCoordEdgeMinMax();
       * minx = LatLonPoints.lonNormalFrom(minx, minmaxLon.min());
       * maxx = LatLonPoints.lonNormalFrom(maxx, minmaxLon.min());
       */

    } else {
      ProjectionRect prect = projection.latLonToProjBB(rect); // allow projection to override
      minx = prect.getMinPoint().getX();
      miny = prect.getMinPoint().getY();
      maxx = prect.getMaxPoint().getX();
      maxy = prect.getMaxPoint().getY();
    }

    int minxIndex = SubsetHelpers.findCoordElement(xaxis, minx, true);
    int minyIndex = SubsetHelpers.findCoordElement(yaxis, miny, true);

    int maxxIndex = SubsetHelpers.findCoordElement(xaxis, maxx, true);
    int maxyIndex = SubsetHelpers.findCoordElement(yaxis, maxy, true);

    List<Range> list = new ArrayList<>();
    list.add(new Range(Math.min(minyIndex, maxyIndex), Math.max(minyIndex, maxyIndex)));
    list.add(new Range(Math.min(minxIndex, maxxIndex), Math.max(minxIndex, maxxIndex)));
    return list;
  }

  double getMinOrMaxLon(double lon1, double lon2, boolean wantMin) {
    double midpoint = (lon1 + lon2) / 2;
    lon1 = LatLonPoints.lonNormal(lon1, midpoint);
    lon2 = LatLonPoints.lonNormal(lon2, midpoint);

    return wantMin ? Math.min(lon1, lon2) : Math.max(lon1, lon2);
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
    this.xaxis = xaxis;
    this.yaxis = yaxis;
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
    this.xaxis = xaxis;
    this.yaxis = yaxis;
    this.projection = projection;
    this.mapArea = mapArea;
    this.llbb = llbb;
  }

  private ProjectionRect calcBoundingBox() {
    double startY = yaxis.getCoordInterval(0).start();
    double startX = xaxis.getCoordInterval(0).start();

    double endY = yaxis.getCoordInterval(yaxis.getNominalSize() - 1).end();
    double endX = xaxis.getCoordInterval(xaxis.getNominalSize() - 1).end();

    return new ProjectionRect(startX, startY, endX, endY);
  }

  private LatLonRect calcLatLonBoundingBox() {
    if (isLatLon()) {
      double startLat = yaxis.getCoordInterval(0).start();
      double startLon = xaxis.getCoordInterval(0).start();

      double endLat = yaxis.getCoordInterval(yaxis.getNominalSize() - 1).end();
      double endLon = xaxis.getCoordInterval(xaxis.getNominalSize() - 1).end();

      LatLonPoint llpt = LatLonPoint.create(startLat, startLon);
      return new LatLonRect(llpt, endLat - startLat, endLon - startLon);

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
