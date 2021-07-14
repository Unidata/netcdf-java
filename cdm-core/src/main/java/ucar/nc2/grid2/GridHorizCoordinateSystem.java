package ucar.nc2.grid2;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.nc2.constants.AxisType;
import ucar.nc2.grid.GridSubset;
import ucar.nc2.internal.grid2.SubsetHelpers;
import ucar.nc2.internal.grid2.SubsetPointHelper;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPoints;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionRect;
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
  @Nullable
  public GridAxisPoint getXHorizAxis() {
    return xaxis;
  }

  /** Get the 1D Y axis (either GeoY or Lat). */
  @Nullable
  public GridAxisPoint getYHorizAxis() {
    return yaxis;
  }

  /** Get the horizontal Projection. */
  public Projection getProjection() {
    return projection;
  }

  /** Does this use lat/lon horizontal axes? */
  public boolean isLatLon() {
    return projection.isLatLon();
  }

  /** Is this a global coverage over longitude ? */
  public boolean isGlobalLon() {
    if (!isLatLon()) {
      return false;
    }
    LatLonRect rect = getLatLonBoundingBox();
    return rect.getWidth() >= 360;
  }

  /** True if both X and Y axes are regularly spaced. */
  public boolean isRegular() {
    if (!isRegularSpatial(getXHorizAxis()))
      return false;
    return isRegularSpatial(getYHorizAxis());
  }

  private boolean isRegularSpatial(GridAxisPoint axis) {
    if (axis == null)
      return false;
    return axis.isRegular();
  }

  public List<Integer> getShape() {
    return ImmutableList.of(getYHorizAxis().getNominalSize(), getXHorizAxis().getNominalSize());
  }

  public List<ucar.array.Range> getSubsetRanges() {
    return ImmutableList.of(getYHorizAxis().getSubsetRange(), getXHorizAxis().getSubsetRange());
  }

  /** Return value from findXYindexFromCoord(). */
  public class CoordReturn {
    /** The data index */
    public int xindex, yindex;
    /** The x,y grid coordinate. */
    public double xcoord, ycoord;

    @Override
    public String toString() {
      return String.format("CoordReturn{xindex=%d, yindex=%d, xcoord=%f, ycoord=%f", xindex, yindex, xcoord, ycoord);
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  private final GridAxisPoint xaxis;
  private final GridAxisPoint yaxis;
  private final Projection projection;

  public GridHorizCoordinateSystem(GridAxisPoint xaxis, GridAxisPoint yaxis, Projection projection) {
    this.xaxis = xaxis;
    this.yaxis = yaxis;
    // TODO set the LatLon seam?
    this.projection = projection == null ? new LatLonProjection() : projection;
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
        Projection dataProjection = getProjection();
        ProjectionRect bb = getBoundingBox();
        if (dataProjection != null && bb != null) {
          llbb = dataProjection.projToLatLonBB(bb);
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
    Projection dataProjection = getProjection();
    return dataProjection.projToLatLon(ProjectionPoint.create(xcoord, ycoord));
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
    }

    return Optional.of(new GridHorizCoordinateSystem(xaxisSubset, yaxisSubset, this.projection));
  }

  /**
   * Get Index Ranges for the given lat, lon bounding box.
   * For projection, only an approximation based on latlon corners.
   * LOOK probabble needed by subset
   *
   * @param rect the requested lat/lon bounding box
   * @return list of 2 Range objects, first y then x.
   */
  List<Range> getRangesFromLatLonRect(LatLonRect rect) throws InvalidRangeException {
    double minx, maxx, miny, maxy;

    Projection proj = getProjection();
    if (proj != null && !(proj instanceof VerticalPerspectiveView) && !(proj instanceof MSGnavigation)
        && !(proj instanceof Geostationary)) { // LOOK kludge - how to do this generrally ??
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
      ProjectionRect prect = getProjection().latLonToProjBB(rect); // allow projection to override
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

}
