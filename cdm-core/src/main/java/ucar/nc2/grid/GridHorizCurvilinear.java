/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.MinMax;
import ucar.array.Range;
import ucar.array.Section;
import ucar.nc2.internal.grid.CurvilinearCoords;
import ucar.nc2.write.NcdumpArray;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPoints;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.CurvilinearProjection;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Formatter;
import java.util.Objects;
import java.util.Optional;

/**
 * A "Curvilinear" horizontal CoordinateSystem does not have a closed form projection function, nor orthogonal lat/lon
 * axes.
 * The x/y axes are typically nominal (eg just reflect the x,y index). The lat/lon values are stored in a 2D array.
 * To find a cell from a lat/lon value, we do a search in those 2D arrays.
 */
@Immutable
public class GridHorizCurvilinear extends GridHorizCoordinateSystem {

  /**
   * Create a GridHorizCurvilinear from the x/y axes and 2D lat/lon arrays.
   * 
   * @param xaxis the xaxis, may be nominal, as the projection is never used.
   * @param yaxis the yaxis, may be nominal, as the projection is never used.
   * @param latdata The latitude of the center points.
   * @param londata The longitude of the center points.
   * @return a new GridHorizCurvilinear
   */
  public static GridHorizCurvilinear create(GridAxisPoint xaxis, GridAxisPoint yaxis, Array<Number> latdata,
      Array<Number> londata) {

    Array<Double> latedge = CurvilinearCoords.makeEdges(latdata);
    Array<Double> lonedge = CurvilinearCoords.makeEdges(londata);

    return createFromEdges(xaxis, yaxis, latedge, lonedge);
  }

  public static GridHorizCurvilinear createFromEdges(GridAxisPoint xaxis, GridAxisPoint yaxis, Array<Double> latedge,
      Array<Double> lonedge) {
    Preconditions.checkNotNull(xaxis);
    Preconditions.checkNotNull(yaxis);
    Preconditions.checkNotNull(latedge);
    Preconditions.checkNotNull(lonedge);
    Preconditions.checkArgument(java.util.Arrays.equals(latedge.getShape(), lonedge.getShape()));
    Preconditions.checkArgument(latedge.getRank() == 2);

    MinMax latMinmax = Arrays.getMinMaxSkipMissingData(latedge, null);
    MinMax lonMinmax = Arrays.getMinMaxSkipMissingData(lonedge, null);

    LatLonPoint llpt = LatLonPoint.create(latMinmax.min(), lonMinmax.min());
    LatLonRect llbb = new LatLonRect(llpt, latMinmax.max() - latMinmax.min(), lonMinmax.max() - lonMinmax.min());

    return new GridHorizCurvilinear(xaxis, yaxis, new CurvilinearProjection(latedge, lonedge), calcBoundingBox(llbb),
        llbb, latMinmax, lonMinmax, latedge, lonedge);
  }

  /** Get horizontal bounding box in projection coordinates. */
  private static ProjectionRect calcBoundingBox(LatLonRect latlonRect) {
    double centerLon = latlonRect.getCenterLon();

    LatLonPoint ll = latlonRect.getLowerLeftPoint();
    LatLonPoint ur = latlonRect.getUpperRightPoint();
    ProjectionPoint w1 = latLonToProj(ll, centerLon);
    ProjectionPoint w2 = latLonToProj(ur, centerLon);

    // make bounding box out of those two corners
    ProjectionRect.Builder world = ProjectionRect.builder(w1.getX(), w1.getY(), w2.getX(), w2.getY());

    LatLonPoint la = LatLonPoint.create(ur.getLatitude(), ll.getLongitude());
    LatLonPoint lb = LatLonPoint.create(ll.getLatitude(), ur.getLongitude());

    // now extend if needed to the other two corners
    world.add(latLonToProj(la, centerLon));
    world.add(latLonToProj(lb, centerLon));

    return world.build();
  }

  // from LatLonProjection
  private static ProjectionPoint latLonToProj(LatLonPoint latlon, double centerLon) {
    return ProjectionPoint.create(LatLonPoints.lonNormal(latlon.getLongitude(), centerLon), latlon.getLatitude());
  }

  //////////////////////////////////////////////////////////////////////
  private final Array<Double> latedge;
  private final Array<Double> lonedge;
  private final CurvilinearCoords helper;

  private GridHorizCurvilinear(GridAxisPoint xaxis, GridAxisPoint yaxis, CurvilinearProjection projection,
      ProjectionRect mapArea, LatLonRect llbb, MinMax latMinmax, MinMax lonMinmax, Array<Double> latedge,
      Array<Double> lonedge) {
    super(xaxis, yaxis, projection, mapArea, llbb);
    this.latedge = latedge;
    this.lonedge = lonedge;
    this.helper = new CurvilinearCoords("GridHorizCurvilinear", latedge, lonedge, latMinmax, lonMinmax);
  }

  /** The latitude edge array. */
  public Array<Double> getLatEdges() {
    return helper.getLatEdges();
  }

  /** The longitude edge array. */
  public Array<Double> getLonEdges() {
    return helper.getLonEdges();
  }

  /** Always true for GridHorizCurvilinear. */
  @Override
  public boolean isLatLon() {
    return true;
  }

  @Override
  public Optional<CoordReturn> findXYindexFromCoord(double x, double y) {
    return helper.findIndexFromLatLon(y, x).map(cr -> new CoordReturn(cr.lon, cr.lat, cr.lonindex, cr.latindex));
  }

  @Override
  public Optional<CoordReturn> findXYindexFromCoord(double x, double y, @Nullable int[] initial) {
    return helper.findIndexFromLatLon(y, x, initial)
        .map(cr -> new CoordReturn(cr.lon, cr.lat, cr.lonindex, cr.latindex));
  }

  @Override
  public Iterable<CellBounds> cells() {
    return () -> new BoundsIterator(getXHorizAxis().getNominalSize(), getYHorizAxis().getNominalSize());
  }

  private class BoundsIterator extends AbstractIterator<CellBounds> {
    final int nx;
    final int ny;
    int xindex = 0;
    int yindex = 0;

    public BoundsIterator(int nx, int ny) {
      int[] shape = lonedge.getShape();
      Preconditions.checkArgument(nx == shape[1] - 1);
      Preconditions.checkArgument(ny == shape[0] - 1);
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

      CoordReturn ll = new CoordReturn(lonedge.get(yindex, xindex), latedge.get(yindex, xindex), xindex, yindex);
      CoordReturn lr =
          new CoordReturn(lonedge.get(yindex, xindex + 1), latedge.get(yindex, xindex + 1), xindex, yindex);
      CoordReturn ur =
          new CoordReturn(lonedge.get(yindex + 1, xindex + 1), latedge.get(yindex + 1, xindex + 1), xindex, yindex);
      CoordReturn ul =
          new CoordReturn(lonedge.get(yindex + 1, xindex), latedge.get(yindex + 1, xindex), xindex, yindex);

      CellBounds result = new CellBounds(ll, lr, ur, ul, xindex, yindex);
      xindex++;
      return result;
    }
  }

  void showEdges() {
    System.out.printf("latedge = %s%n", NcdumpArray.printArray(latedge));
    System.out.printf("lonedge = %s%n", NcdumpArray.printArray(lonedge));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    if (!super.equals(o))
      return false;
    GridHorizCurvilinear that = (GridHorizCurvilinear) o;
    return Arrays.equalDoubles(latedge, that.latedge) && Arrays.equalDoubles(lonedge, that.lonedge);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), latedge, lonedge);
  }

  ///////////////////////////////////////////////////////////////////////////

  @Override
  public LatLonPoint getLatLon(int xindex, int yindex) {
    CurvilinearCoords.CoordReturn cr = helper.midpoint(yindex, xindex);
    return LatLonPoint.create(cr.lat, cr.lon);
  }

  @Override
  Optional<GridHorizCoordinateSystem> subset(GridSubset params, MaterializedCoordinateSystem.Builder builder,
      Formatter errlog) {
    Preconditions.checkNotNull(params);
    Preconditions.checkNotNull(errlog);

    Integer horizStride = params.getHorizStride();
    if (horizStride == null || horizStride < 1) {
      horizStride = 1;
    }

    // point
    ProjectionPoint projCoord = params.getProjectionPoint();
    LatLonPoint latlon = params.getLatLonPoint();
    if (projCoord != null && latlon == null) {
      latlon = getProjection().projToLatLon(projCoord);
    }

    if (latlon != null) {
      // find the x,y index of the given latlon
      Optional<CurvilinearCoords.CoordReturn> resulto =
          helper.findIndexFromLatLon(latlon.getLatitude(), latlon.getLongitude());
      if (resulto.isEmpty()) {
        errlog.format("subset failed to find index for latlon.");
        return Optional.empty();
      }
      CurvilinearCoords.CoordReturn result = resulto.get();

      // use that index to create a subset of just that point.
      return subset(result.latindex, result.lonindex, result.latindex, result.lonindex, horizStride, errlog);
    }

    ProjectionRect projbb = params.getProjectionBoundingBox();
    LatLonRect llbb = params.getLatLonBoundingBox();
    if (projbb == null && llbb != null) {
      projbb = getProjection().latLonToProjBB(llbb);
    }

    if (projbb != null) {
      // find the min and max indices that are needed to cover as much of the LatLonRect as possible
      CurvilinearCoords.MinMaxIndices ret = helper.subsetProjectionRect(projbb);
      if (ret.isEmpty()) {
        errlog.format("ProjectionRect does not intersect the data");
        return Optional.empty();
      }
      // use those indices to create a rectangle of data.
      return subset(ret.minLat, ret.minLon, ret.maxLat, ret.maxLon, horizStride, errlog);

    } else if (horizStride > 1) {
      return subset(0, 0, yaxis.ncoords - 1, xaxis.ncoords - 1, horizStride, errlog);
    }

    // no subsetting
    return Optional.of(this);
  }

  private Optional<GridHorizCoordinateSystem> subset(int llLat, int llLon, int urLat, int urLon, int stride,
      Formatter errlog) {
    int latmin = Math.min(llLat, urLat);
    int latmax = Math.max(llLat, urLat);

    // use those indices to create a rectangle of data.
    Range latRange = Range.make(latmin, latmax, stride);
    GridAxisPoint lataxisSubset = yaxis.toBuilder().subsetWithRange(latRange).build();

    int lonmin = Math.min(llLon, urLon);
    int lonmax = Math.max(llLon, urLon);
    Range lonRange = Range.make(lonmin, lonmax, stride);
    GridAxisPoint lonaxisSubset = xaxis.toBuilder().subsetWithRange(lonRange).build();

    // Subset the edge arrays LOOK cant subset edge array if there is a stride; must recalculate
    Section section = Section.builder().appendRange(Range.make(latmin, latmax + 1, stride))
        .appendRange(Range.make(lonmin, lonmax + 1, stride)).build();
    try {
      Array<Double> latedgeSubset = Arrays.section(latedge, section);
      Array<Double> lonedgeSubset = Arrays.section(lonedge, section);
      return Optional
          .of(GridHorizCurvilinear.createFromEdges(lonaxisSubset, lataxisSubset, latedgeSubset, lonedgeSubset));

    } catch (InvalidRangeException e) {
      errlog.format("failed to create subsetted edge arrays");
      return Optional.empty();
    }
  }

}
