package ucar.nc2.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.nc2.constants.AxisType;
import ucar.nc2.internal.grid.SubsetHelpers;
import ucar.nc2.internal.grid.SubsetPointHelper;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPoints;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.Curvilinear;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.geoloc.projection.sat.Geostationary;
import ucar.unidata.geoloc.projection.sat.MSGnavigation;
import ucar.unidata.geoloc.projection.sat.VerticalPerspectiveView;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Manages Projection GeoX/GeoY or Lat/Lon CoordinateSystem. */
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

  /** Does this use lat/lon horizontal axes? */
  public boolean isCurvilinear() {
    return projection instanceof Curvilinear;
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

  private LatLonRect llbb; // lazy

  /** Get horizontal bounding box in lat, lon coordinates. For projection, only an approximation based on corners. */
  public LatLonRect getLatLonBoundingBox() {
    if (llbb == null) {
      if (isLatLon()) {
        double startLat = yaxis.getCoordInterval(0).start();
        double startLon = xaxis.getCoordInterval(0).start();

        double endLat = yaxis.getCoordInterval(yaxis.getNominalSize() - 1).end();
        double endLon = xaxis.getCoordInterval(xaxis.getNominalSize() - 1).end();

        LatLonPoint llpt = LatLonPoint.create(startLat, startLon);
        llbb = new LatLonRect(llpt, endLat - startLat, endLon - startLon);

      } else {
        ProjectionRect bb = getBoundingBox();
        if (projection != null && bb != null) {
          llbb = projection.projToLatLonBB(bb);
        }
      }
    }
    return llbb;
  }

  private ProjectionRect mapArea; // lazy

  /** Get horizontal bounding box in projection coordinates. */
  public ProjectionRect getBoundingBox() {
    if (mapArea == null) {
      double startY = yaxis.getCoordInterval(0).start();
      double startX = xaxis.getCoordInterval(0).start();

      double endY = yaxis.getCoordInterval(yaxis.getNominalSize() - 1).end();
      double endX = xaxis.getCoordInterval(xaxis.getNominalSize() - 1).end();

      mapArea = new ProjectionRect(startX, startY, endX, endY);
    }
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

  /** Subset both x and y axis based on the given parameters. */
  public Optional<GridHorizCoordinateSystem> subset(GridSubset params, Formatter errlog) {
    Integer horizStride = params.getHorizStride();
    if (horizStride == null || horizStride < 1) {
      horizStride = 1;
    }

    GridAxisPoint xaxisSubset = xaxis;
    GridAxisPoint yaxisSubset = yaxis;
    LatLonRect llbb = params.getLatLonBoundingBox();
    ProjectionRect projbb = params.getProjectionBoundingBox();

    // TODO GridSubset.latlonPoint
    if (projbb != null) { // TODO ProjectionRect ok for isLatlon = true?
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

    } else if (llbb != null && isLatLon()) { // TODO LatLonRect only used for isLatlon = true?
      SubsetPointHelper yhelper = new SubsetPointHelper(yaxis);
      Optional<GridAxisPoint.Builder<?>> ybo =
          yhelper.subsetRange(llbb.getLatMin(), llbb.getLatMax(), horizStride, errlog);
      if (ybo.isEmpty()) {
        return Optional.empty();
      }
      yaxisSubset = ybo.get().build();

      // TODO longitude wrapping
      SubsetPointHelper xhelper = new SubsetPointHelper(xaxis);
      Optional<GridAxisPoint.Builder<?>> xbo =
          xhelper.subsetRange(llbb.getLonMin(), llbb.getLonMax(), horizStride, errlog);
      if (xbo.isEmpty()) {
        return Optional.empty();
      }
      xaxisSubset = xbo.get().build();

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

  /** Return value from bounds(). */
  public class CoordBounds {
    public final CoordReturn ll;
    public final CoordReturn lr;
    public final CoordReturn ur;
    public final CoordReturn ul;
    public final int xindex, yindex;

    public CoordBounds(int xindex, int yindex) {
      this.xindex = xindex;
      this.yindex = yindex;
      CoordInterval xintv = xaxis.getCoordInterval(xindex);
      CoordInterval yintv = yaxis.getCoordInterval(yindex);
      this.ll = new CoordReturn(xintv.start(), yintv.start());
      this.lr = new CoordReturn(xintv.end(), yintv.start());
      this.ur = new CoordReturn(xintv.end(), yintv.end());
      this.ul = new CoordReturn(xintv.start(), yintv.end());
    }

    public CoordBounds(CoordReturn ll, CoordReturn lr, CoordReturn ur, CoordReturn ul, int xindex, int yindex) {
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

  public Iterable<CoordBounds> bounds() {
    return () -> new BoundsIterator(xaxis.getNominalSize(), yaxis.getNominalSize());
  }

  private class BoundsIterator extends AbstractIterator<CoordBounds> {
    final int nx;
    final int ny;
    int xindex = 0;
    int yindex = 0;

    public BoundsIterator(int nx, int ny) {
      this.nx = nx;
      this.ny = ny;
    }

    @Override
    protected CoordBounds computeNext() {
      if (xindex >= nx) {
        yindex++;
        xindex = 0;
      }
      if (yindex >= ny) {
        return endOfData();
      }
      CoordBounds result = new CoordBounds(xindex, yindex);
      xindex++;
      return result;
    }
  }

  /** Return value from findXYindexFromCoord(). */
  public static class CoordReturn {
    /** The data index */
    public int xindex, yindex;
    /** The x,y grid coordinate. */
    public double xcoord, ycoord;

    public CoordReturn() {}

    public CoordReturn(double xcoord, double ycoord) {
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

  // LOOK needed?
  /** From the (x,y) projection point, find the indices and coordinates of the horizontal 2D grid. */
  public Optional<CoordReturn> findXYindexFromCoord(double x, double y) {
    CoordReturn result = new CoordReturn();

    if (xaxis.getAxisType() == AxisType.Lon) {
      x = LatLonPoints.lonNormalFrom(x, xaxis.getCoordinate(0).doubleValue()); // LOOK ??
    }

    result.xindex = SubsetHelpers.findCoordElement(xaxis, x, false);
    result.yindex = SubsetHelpers.findCoordElement(yaxis, y, false);

    if (result.xindex >= 0 && result.xindex < xaxis.getNominalSize() && result.yindex >= 0
        && result.yindex < yaxis.getNominalSize()) {
      result.xcoord = xaxis.getCoordinate(result.xindex).doubleValue();
      result.ycoord = yaxis.getCoordinate(result.yindex).doubleValue();
      return Optional.of(result);
    } else {
      return Optional.empty();
    }
  }

  /**
   * Get Index Ranges for the given lat, lon bounding box.
   * For projection, only an approximation based on latlon corners.
   * LOOK maybe needed by subset
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
  private final GridAxisPoint xaxis;
  private final GridAxisPoint yaxis;
  private final Projection projection;

  public GridHorizCoordinateSystem(GridAxisPoint xaxis, GridAxisPoint yaxis, @Nullable Projection projection) {
    this.xaxis = xaxis;
    this.yaxis = yaxis;
    // TODO set the LatLon seam?
    this.projection = projection == null ? new LatLonProjection() : projection;
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
