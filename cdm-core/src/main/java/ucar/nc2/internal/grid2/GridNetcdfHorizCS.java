package ucar.nc2.internal.grid2;

import com.google.common.base.Preconditions;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.GridHorizCoordinateSystem;
import ucar.nc2.grid.GridSubset;
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

/** HorizCS with 1D x,y axes. */
public class GridNetcdfHorizCS implements GridHorizCoordinateSystem {

  public static GridNetcdfHorizCS create(GridAxis<?> xaxis, GridAxis<?> yaxis, @Nullable Projection projection) {
    Preconditions.checkArgument(xaxis instanceof GridAxisPoint);
    Preconditions.checkArgument(yaxis instanceof GridAxisPoint);
    // WRF NMM
    String horizStaggerType = xaxis.attributes().findAttributeString(_Coordinate.Stagger, null);
    return new GridNetcdfHorizCS((GridAxisPoint) xaxis, (GridAxisPoint) yaxis, projection, horizStaggerType);
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  private final GridAxisPoint xaxis;
  private final GridAxisPoint yaxis;
  private final Projection projection;
  private final @Nullable String horizStaggerType;

  GridNetcdfHorizCS(GridAxisPoint xaxis, GridAxisPoint yaxis, Projection projection,
      @Nullable String horizStaggerType) {
    this.xaxis = xaxis;
    this.yaxis = yaxis;
    // TODO set the LatLon seam?
    this.projection = projection == null ? new LatLonProjection() : projection;
    this.horizStaggerType = horizStaggerType;
  }

  @Override
  @Nullable
  public GridAxisPoint getXHorizAxis() {
    return xaxis;
  }

  @Override
  @Nullable
  public GridAxisPoint getYHorizAxis() {
    return yaxis;
  }

  @Override
  public Projection getProjection() {
    return projection;
  }

  @Override
  public boolean isLatLon() {
    return projection.isLatLon();
  }

  @Override
  public boolean isGlobalLon() {
    if (!isLatLon()) {
      return false;
    }
    LatLonRect rect = getLatLonBoundingBox();
    return rect.getWidth() >= 360;
  }

  @Override
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

  @Override
  @Nullable
  public String getGeoUnits() {
    return isLatLon() ? null : xaxis.getUnits();
  }

  @Override
  @Nullable
  public String getHorizStaggerType() {
    return horizStaggerType;
  }

  private LatLonRect llbb; // lazy

  @Override
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

  ProjectionRect mapArea; // lazy

  @Override
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
   *
   * @param xindex x index
   * @param yindex y index
   * @return lat/lon coordinate of the midpoint of the cell
   */
  public LatLonPoint getLatLon(int xindex, int yindex) {
    double x = xaxis.getCoordinate(xindex).doubleValue();
    double y = yaxis.getCoordinate(xindex).doubleValue();
    return isLatLon() ? LatLonPoint.create(y, x) : getLatLon(x, y);
  }

  private LatLonPoint getLatLon(double xcoord, double ycoord) {
    Projection dataProjection = getProjection();
    return dataProjection.projToLatLon(ProjectionPoint.create(xcoord, ycoord));
  }

  @Override
  public Optional<CoordReturn> findXYindexFromCoord(double x, double y) {
    SubsetPointHelper xhelper = new SubsetPointHelper(xaxis);
    SubsetPointHelper yhelper = new SubsetPointHelper(yaxis);
    CoordReturn result = new CoordReturn();

    if (xaxis.getAxisType() == AxisType.Lon) {
      x = LatLonPoints.lonNormalFrom(x, xaxis.getCoordinate(0).doubleValue()); // LOOK ??
    }

    result.xindex = xhelper.findCoordElement(x, false);
    result.yindex = yhelper.findCoordElement(y, false);

    if (result.xindex >= 0 && result.xindex < xaxis.getNominalSize() && result.yindex >= 0
        && result.yindex < yaxis.getNominalSize()) {
      result.xcoord = xaxis.getCoordinate(result.xindex).doubleValue();
      result.ycoord = yaxis.getCoordinate(result.yindex).doubleValue();
      return Optional.of(result);
    } else {
      return Optional.empty();
    }
  }

  @Override
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
      Optional<GridAxisPoint.Builder<?>> ybo = yhelper.subset(projbb.getMinY(), projbb.getMaxY(), horizStride, errlog);
      if (ybo.isEmpty()) {
        return Optional.empty();
      }
      yaxisSubset = ybo.get().build();

      SubsetPointHelper xhelper = new SubsetPointHelper(xaxis);
      Optional<GridAxisPoint.Builder<?>> xbo = xhelper.subset(projbb.getMinY(), projbb.getMaxY(), horizStride, errlog);
      if (xbo.isEmpty()) {
        return Optional.empty();
      }
      xaxisSubset = xbo.get().build();

    } else if (llbb != null && isLatLon()) { // TODO LatLonRect only used for isLatlon = true?
      SubsetPointHelper yhelper = new SubsetPointHelper(yaxis);
      Optional<GridAxisPoint.Builder<?>> ybo = yhelper.subset(llbb.getLatMin(), llbb.getLatMax(), horizStride, errlog);
      if (ybo.isEmpty()) {
        return Optional.empty();
      }
      yaxisSubset = ybo.get().build();

      // TODO longitude wrapping
      SubsetPointHelper xhelper = new SubsetPointHelper(xaxis);
      Optional<GridAxisPoint.Builder<?>> xbo = xhelper.subset(llbb.getLonMin(), llbb.getLonMax(), horizStride, errlog);
      if (xbo.isEmpty()) {
        return Optional.empty();
      }
      xaxisSubset = xbo.get().build();

    } else if (horizStride > 1) { // no bounding box, just horiz stride
      Preconditions.checkNotNull(yaxis);
      Preconditions.checkNotNull(xaxis);
      try {
        Range yRange = yaxis.getSubsetRange().copyWithStride(horizStride);
        yaxisSubset = yaxis.toBuilder().setRange(yRange).build();

        Range xRange = xaxis.getSubsetRange().copyWithStride(horizStride);
        xaxisSubset = xaxis.toBuilder().setRange(xRange).build();
      } catch (InvalidRangeException e) {
        errlog.format(e.getMessage());
      }
    }

    // GridAxisPoint xaxis, GridAxisPoint yaxis, Projection projection, @Nullable String horizStaggerType
    return Optional.of(new GridNetcdfHorizCS(xaxisSubset, yaxisSubset, this.projection, this.horizStaggerType));
  }

  /**
   * Get Index Ranges for the given lat, lon bounding box.
   * For projection, only an approximation based on latlon corners.
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

    SubsetPointHelper xhelper = new SubsetPointHelper(xaxis);
    SubsetPointHelper yhelper = new SubsetPointHelper(yaxis);
    int minxIndex = xhelper.findCoordElement(minx, true);
    int minyIndex = yhelper.findCoordElement(miny, true);

    int maxxIndex = xhelper.findCoordElement(maxx, true);
    int maxyIndex = yhelper.findCoordElement(maxy, true);

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
    GridNetcdfHorizCS that = (GridNetcdfHorizCS) o;
    return Objects.equals(xaxis, that.xaxis) && Objects.equals(yaxis, that.yaxis)
        && Objects.equals(projection, that.projection) && Objects.equals(horizStaggerType, that.horizStaggerType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(xaxis, yaxis, projection, horizStaggerType);
  }
}
