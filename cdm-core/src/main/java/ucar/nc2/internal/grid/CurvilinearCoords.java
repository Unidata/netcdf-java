/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.grid;

import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.MinMax;
import ucar.nc2.write.NcdumpArray;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.CurvilinearProjection;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Helper class for curvilinear lat, lon coordinates. The longitude values must use projection coordinates,
 * i.e. continuous values that are not normalized to a 360 cylinder.
 * originally from ucar.nc2.ft2.coverage.adapter.GeoGridCoordinate2D and CoordinateAxis2D.makeEdges()
 */
public class CurvilinearCoords {

  public static Array<Double> makeEdges(Array<Number> midpoints) {
    int[] shape = midpoints.getShape();
    int ny = shape[0];
    int nx = shape[1];
    int[] edgeShape = new int[] {ny + 1, nx + 1};
    Awrap edge = new Awrap(ny + 1, nx + 1);

    for (int y = 0; y < ny - 1; y++) {
      for (int x = 0; x < nx - 1; x++) {
        // the interior edges are the average of the 4 surrounding midpoints
        double xval = (midpoints.get(y, x).doubleValue() + midpoints.get(y, x + 1).doubleValue()
            + midpoints.get(y + 1, x).doubleValue() + midpoints.get(y + 1, x + 1).doubleValue()) / 4;
        edge.set(y + 1, x + 1, xval);
      }
      // extrapolate to exterior points
      edge.set(y + 1, 0, edge.get(y + 1, 1) - (edge.get(y + 1, 2) - edge.get(y + 1, 1)));
      edge.set(y + 1, nx, edge.get(y + 1, nx - 1) + (edge.get(y + 1, nx - 1) - edge.get(y + 1, nx - 2)));
    }

    // extrapolate to the first and last row
    for (int x = 0; x < nx + 1; x++) {
      edge.set(0, x, edge.get(1, x) - (edge.get(2, x) - edge.get(1, x)));
      edge.set(ny, x, edge.get(ny - 1, x) + (edge.get(ny - 1, x) - edge.get(ny - 2, x)));
    }

    return Arrays.factory(ArrayType.DOUBLE, edgeShape, edge.arr);
  }

  private static class Awrap {
    final int stride;
    final double[] arr;

    Awrap(int ny, int nx) {
      this.stride = nx;
      this.arr = new double[ny * nx];
    }

    double get(int y, int x) {
      return arr[y * stride + x];
    }

    void set(int y, int x, double val) {
      arr[y * stride + x] = val;
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CurvilinearCoords.class);
  private static final boolean debug = false;

  private final String name;
  private final Array<Double> latEdge;
  private final Array<Double> lonEdge;
  private final MinMax latMinMax;
  private final MinMax lonMinMax;
  private final int ncols;
  private final int nrows;

  public CurvilinearCoords(String name, Array<Number> lat, Array<Number> lon) {
    this.name = name;
    this.latEdge = makeEdges(lat);
    this.lonEdge = makeEdges(lon);
    this.latMinMax = Arrays.getMinMaxSkipMissingData(this.latEdge, null);
    this.lonMinMax = Arrays.getMinMaxSkipMissingData(this.lonEdge, null);

    int[] shape = lat.getShape();
    // midpoints, not edges
    this.nrows = shape[0];
    this.ncols = shape[1];
  }

  public CurvilinearCoords(String name, Array<Double> latedge, Array<Double> lonedge, MinMax latMinmax,
      MinMax lonMinmax) {
    this.name = name;
    this.latEdge = latedge;
    this.lonEdge = lonedge;
    this.latMinMax = latMinmax;
    this.lonMinMax = lonMinmax;

    int[] shape = latedge.getShape();
    // midpoints, not edges
    this.nrows = shape[0] - 1;
    this.ncols = shape[1] - 1;
  }

  public Array<Double> getLatEdges() {
    return latEdge;
  }

  public Array<Double> getLonEdges() {
    return lonEdge;
  }

  /** Calculate the midpoint from average of the 4 surrounding edges */
  public CoordReturn midpoint(int latidx, int lonidx) {
    int row = Math.max(Math.min(latidx, nrows - 1), 0);
    int col = Math.max(Math.min(lonidx, ncols - 1), 0);
    // midpoint is the average of the 4 surrounding edges
    double lat =
        (latEdge.get(row, col) + latEdge.get(row + 1, col) + latEdge.get(row, col + 1) + latEdge.get(row + 1, col + 1))
            / 4;
    double lon =
        (lonEdge.get(row, col) + lonEdge.get(row + 1, col) + lonEdge.get(row, col + 1) + lonEdge.get(row + 1, col + 1))
            / 4;
    return new CoordReturn(lat, lon, row, col);
  }

  /** Return value from findIndexFromLatLon(). */
  @Immutable
  public static class CoordReturn {
    /** The data index */
    public final int latindex, lonindex;
    /** The lat, lon grid coordinate. */
    public final double lat, lon;

    public CoordReturn(double lat, double lon, int latindex, int lonindex) {
      this.lat = lat;
      this.lon = lon;
      this.latindex = latindex;
      this.lonindex = lonindex;
    }

    @Override
    public String toString() {
      return String.format("CoordReturn{lat=%f, lon=%f, latindex=%d, lonindex=%d", lat, lon, latindex, lonindex);
    }

    public String toStringShort() {
      return String.format("[%f,%f]", latindex, lonindex);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      CoordReturn that = (CoordReturn) o;
      return latindex == that.latindex && lonindex == that.lonindex && Double.compare(that.lat, lat) == 0
          && Double.compare(that.lon, lon) == 0;
    }

    @Override
    public int hashCode() {
      return Objects.hash(latindex, lonindex, lat, lon);
    }
  }

  /**
   * Find the best index for the given lat,lon point.
   *
   * @param wantLon lon of point
   * @param wantLat lat of point
   * @return empty if not in the grid.
   */
  public Optional<CoordReturn> findIndexFromLatLon(double wantLat, double wantLon) {
    return findIndexFromLatLon(wantLat, wantLon, null);
  }

  /**
   * Find the best index for the given lat,lon point.
   *
   * @param wantLon lon of point
   * @param wantLat lat of point
   * @param initial initial guess, used eg for mouse tracking in UI
   * @return empty if not in the grid.
   */
  public Optional<CoordReturn> findIndexFromLatLon(double wantLat, double wantLon, @Nullable int[] initial) {
    if (wantLat < latMinMax.min()) {
      return Optional.empty();
    }
    if (wantLat > latMinMax.max()) {
      return Optional.empty();
    }
    if (wantLon < lonMinMax.min()) {
      return Optional.empty();
    }
    if (wantLon > lonMinMax.max()) {
      return Optional.empty();
    }

    double gradientLat = (latMinMax.max() - latMinMax.min()) / nrows;
    double gradientLon = (lonMinMax.max() - lonMinMax.min()) / ncols;

    double diffLat = wantLat - latMinMax.min();
    double diffLon = wantLon - lonMinMax.min();

    // initial guess
    int[] rectIndex = initial;
    if (rectIndex == null) {
      rectIndex = new int[2];
      rectIndex[0] = (int) (Math.round(diffLat / gradientLat)); // row
      rectIndex[1] = (int) (Math.round(diffLon / gradientLon)); // col
    }

    if (debug) {
      System.out.printf("%nfindIndexFromLatLon [%f,%f]%n", wantLat, wantLon);
    }

    Set<Integer> tried = new HashSet<>();
    int count = 0;
    while (true) {
      count++;
      if (debug) {
        System.out.printf("  Iteration %d start index (%d, %d) == %s%n", count, rectIndex[0], rectIndex[1],
            showBox(rectIndex));
      }
      if (contains(wantLat, wantLon, rectIndex)) {
        if (debug) {
          System.out.printf("    contains (%d, %d) %n", rectIndex[0], rectIndex[1]);
        }
        return Optional.of(new CoordReturn(wantLat, wantLon, rectIndex[0], rectIndex[1]));
      }

      if (!jump(wantLat, wantLon, rectIndex)) {
        return Optional.empty();
      }

      // bouncing around
      if (count > 10 || tried.contains(elem(rectIndex))) {
        // last ditch attempt
        if (incr(wantLat, wantLon, rectIndex)) {
          if (debug) {
            System.out.printf("    incr (%d, %d) %n", rectIndex[0], rectIndex[1]);
          }
          return Optional.of(new CoordReturn(wantLat, wantLon, rectIndex[0], rectIndex[1]));
        } else {
          return Optional.empty();
        }
      }
      tried.add(elem(rectIndex));
    }
  }

  // unique value for this index, to keep track of whats already been tried
  private int elem(int[] idx) {
    return idx[0] * nrows + idx[1];
  }

  /**
   * Is the point (lat,lon) contained in the (row, col) rectangle ?
   *
   * @param wantLat lat of point
   * @param wantLon lon of point
   * @param rectIndex rectangle row, col, will be clipped to [0, nrows), [0, ncols)
   * @return true if contained
   */

  /*
   * http://mathforum.org/library/drmath/view/54386.html
   *
   * Given any three points on the plane (x0,y0), (x1,y1), and
   * (x2,y2), the area of the triangle determined by them is
   * given by the following formula:
   *
   * 1 | x0 y0 1 |
   * A = - | x1 y1 1 |,
   * 2 | x2 y2 1 |
   *
   * where the vertical bars represent the determinant.
   * the value of the expression above is:
   *
   * (.5)(x1*y2 - y1*x2 -x0*y2 + y0*x2 + x0*y1 - y0*x1)
   *
   * The amazing thing is that A is positive if the three points are
   * taken in a counter-clockwise orientation, and negative otherwise.
   *
   * To be inside a rectangle (or any convex body), as you trace
   * around in a clockwise direction from p1 to p2 to p3 to p4 and
   * back to p1, the "areas" of triangles p1 p2 p, p2 p3 p, p3 p4 p,
   * and p4 p1 p must all be positive. If you don't know the
   * orientation of your rectangle, then they must either all be
   * positive or all be negative.
   *
   * this method works for arbitrary convex regions oo the plane.
   */
  private boolean contains(double wantLat, double wantLon, int[] rectIndex) {
    rectIndex[0] = Math.max(Math.min(rectIndex[0], nrows - 1), 0);
    rectIndex[1] = Math.max(Math.min(rectIndex[1], ncols - 1), 0);

    int row = rectIndex[0];
    int col = rectIndex[1];

    double x1 = lonEdge.get(row, col);
    double y1 = latEdge.get(row, col);

    double x2 = lonEdge.get(row, col + 1);
    double y2 = latEdge.get(row, col + 1);

    double x3 = lonEdge.get(row + 1, col + 1);
    double y3 = latEdge.get(row + 1, col + 1);

    double x4 = lonEdge.get(row + 1, col);
    double y4 = latEdge.get(row + 1, col);

    // must all have same determinate sign
    boolean sign = detIsPositive(x1, y1, x2, y2, wantLon, wantLat);
    boolean result = (sign == detIsPositive(x2, y2, x3, y3, wantLon, wantLat))
        && (sign == detIsPositive(x3, y3, x4, y4, wantLon, wantLat))
        && (sign == detIsPositive(x4, y4, x1, y1, wantLon, wantLat));
    return result;
  }

  private boolean detIsPositive(double x0, double y0, double x1, double y1, double x2, double y2) {
    double det = (x1 * y2 - y1 * x2 - x0 * y2 + y0 * x2 + x0 * y1 - y0 * x1);
    if (det == 0) {
      log.warn("determinate = 0 on lat/lon=" + name);
    }
    return det > 0;
  }

  /**
   * jump to a new row,col
   *
   * @param wantLat want this lat
   * @param wantLon want this lon
   * @param rectIndex starting row, col and replaced by new guess
   * @return true if new guess, false if failure
   */
  /*
   * choose x, y such that (matrix multiply) :
   *
   * (wantx) = (fxx fxy) (x)
   * (wanty) (fyx fyy) (y)
   *
   * where fxx = d(fx)/dx ~= delta lon in lon direction
   * where fxy = d(fx)/dy ~= delta lon in lat direction
   * where fyx = d(fy)/dx ~= delta lat in lon direction
   * where fyy = d(fy)/dy ~= delta lat in lat direction
   *
   * 2 linear equations in 2 unknowns, solve in usual way
   */
  private boolean jump(double wantLat, double wantLon, int[] rectIndex) {
    int row = Math.max(Math.min(rectIndex[0], nrows - 1), 0);
    int col = Math.max(Math.min(rectIndex[1], ncols - 1), 0);

    double lat = latEdge.get(row, col);
    double lon = lonEdge.get(row, col);
    double diffLat = wantLat - lat;
    double diffLon = wantLon - lon;

    double fxx = lonEdge.get(row, col + 1) - lon; // dlondx
    double fxy = lonEdge.get(row + 1, col) - lon; // dlondy
    double fyy = latEdge.get(row + 1, col) - lat; // dlatdy
    double fyx = latEdge.get(row, col + 1) - lat; // dlatdx

    double x;
    double y;
    double det = (fyy * fxx - fxy * fyx);
    if (det == 0) {
      // this could happen if the shape happens to be just so.
      // just make a guess using simpler assumption of uniform cells.
      if (debug) {
        System.out.printf("    jump found colinear points (%d, %d) == %s%n", rectIndex[0], rectIndex[1],
            showBox(rectIndex));
      }
      x = diffLon / fxx;
      y = diffLat / fyy;
    } else {
      x = (fyy * diffLon - fxy * diffLat) / det;
      if (fyy == 0) {
        // should not happen, means that the cell has no height
        throw new IllegalArgumentException(String.format("cell height == 0 at index %d %d", row, col));
      }
      y = (diffLat - fyx * x) / fyy;
    }

    int drow = (int) Math.round(y);
    int dcol = (int) Math.round(x);
    rectIndex[0] = Math.max(Math.min(row + drow, nrows - 1), 0);
    rectIndex[1] = Math.max(Math.min(col + dcol, ncols - 1), 0);

    if ((row == rectIndex[0]) && (col == rectIndex[1])) {
      // try this before giving up
      return incr(wantLat, wantLon, rectIndex);
    } else {
      if (debug) {
        System.out.printf("    jump to (%d %d)%n", rectIndex[0], rectIndex[1]);
      }
      return true;
    }
  }

  // tries incrementing/decrementing the lat or lon by one.
  // return true if found, else call box9
  private boolean incr(double wantLat, double wantLon, int[] rectIndex) {
    int row = rectIndex[0];
    int col = rectIndex[1];
    double diffLat = wantLat - latEdge.get(row, col);
    double diffLon = wantLon - lonEdge.get(row, col);
    if (debug) {
      System.out.printf("    try incr%n");
    }

    if (Math.abs(diffLat) > Math.abs(diffLon)) { // try lat first
      rectIndex[0] = row + ((diffLat > 0) ? 1 : -1);
      if (contains(wantLat, wantLon, rectIndex)) {
        return true;
      }
      rectIndex[1] = col + ((diffLon > 0) ? 1 : -1);
      if (contains(wantLat, wantLon, rectIndex))
        return true;
    } else {
      rectIndex[1] = col + ((diffLon > 0) ? 1 : -1);
      if (contains(wantLat, wantLon, rectIndex)) {
        return true;
      }
      rectIndex[0] = row + ((diffLat > 0) ? 1 : -1);
      if (contains(wantLat, wantLon, rectIndex)) {
        return true;
      }
    }

    // back to original, do box search
    rectIndex[0] = row;
    rectIndex[1] = col;
    return box9(wantLat, wantLon, rectIndex);
  }

  // we think its got to be in one of the 9 boxes around rectIndex
  // return true if found, else false
  private boolean box9(double wantLat, double wantLon, int[] rectIndex) {
    int row = rectIndex[0];
    int minrow = Math.max(row - 1, 0);
    int maxrow = Math.min(row + 1, nrows);

    int col = rectIndex[1];
    int mincol = Math.max(col - 1, 0);
    int maxcol = Math.min(col + 1, ncols);

    if (debug) {
      System.out.printf("    try box9%n");
    }
    for (int i = minrow; i <= maxrow; i++)
      for (int j = mincol; j <= maxcol; j++) {
        rectIndex[0] = i;
        rectIndex[1] = j;
        if (contains(wantLat, wantLon, rectIndex)) {
          return true;
        }
      }
    return false;
  }

  String showBox(int... idx) {
    int row = idx[0];
    int col = idx[1];

    double x1 = lonEdge.get(row, col);
    double y1 = latEdge.get(row, col);

    double x2 = lonEdge.get(row, col + 1);
    double y2 = latEdge.get(row, col + 1);

    double x3 = lonEdge.get(row + 1, col + 1);
    double y3 = latEdge.get(row + 1, col + 1);

    double x4 = lonEdge.get(row + 1, col);
    double y4 = latEdge.get(row + 1, col);

    return String.format("lat1=[%f, %f] lat2=[%f, %f] lon1=[%f, %f] lon2=[%f, %f]", y1, y2, y4, y3, x1, x2, x4, x3);
  }

  void showEdges() {
    System.out.printf("%s%n", NcdumpArray.printArray(this.latEdge, name + " lat", null));
    System.out.printf("%s%n", NcdumpArray.printArray(this.lonEdge, name + " lon", null));
  }

  void check() {
    boolean ascend = (lonEdge.get(0, 0) >= lonEdge.get(0, 1));
    for (int i = 0; i < nrows + 1; i++) {
      for (int j = 0; j < ncols; j++) {
        if (ascend != (lonEdge.get(i, j) >= lonEdge.get(i, j + 1))) {
          System.out.printf("HEY %d %d%n", i, j);
        }
      }
    }

    boolean ascend2 = (latEdge.get(0, 0) >= latEdge.get(1, 0));
    for (int i = 0; i < nrows; i++) {
      for (int j = 0; j < ncols + 1; j++) {
        if (ascend2 != (latEdge.get(i, j) >= latEdge.get(i + 1, j))) {
          System.out.printf("HEY2 %d %d%n", i, j);
        }
      }
    }
  }

  ////////////////////////////////////////////////////////

  public MinMaxIndices create(int minLat, int minLon, int maxLat, int maxLon) {
    MinMaxIndices result = new MinMaxIndices();
    result.minLat = minLat;
    result.minLon = minLon;
    result.maxLat = maxLat;
    result.maxLon = maxLon;
    return result;
  }

  public class MinMaxIndices {
    public int minLat = nrows - 1;
    public int minLon = ncols - 1;
    public int maxLat = 0;
    public int maxLon = 0;

    void addPoint(int latidx, int lonidx) {
      minLat = Math.min(minLat, latidx);
      minLon = Math.min(minLon, lonidx);
      maxLat = Math.max(maxLat, latidx);
      maxLon = Math.max(maxLon, lonidx);
    }

    public boolean isEmpty() {
      return (minLat > maxLat) || (minLon > maxLon);
    }

    @Override
    public String toString() {
      return "MinMaxIndices{" + "minLat=" + minLat + ", minLon=" + minLon + ", maxLat=" + maxLat + ", maxLon=" + maxLon
          + "isEmpty=" + isEmpty() + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      MinMaxIndices that = (MinMaxIndices) o;
      return minLat == that.minLat && minLon == that.minLon && maxLat == that.maxLat && maxLon == that.maxLon;
    }

    @Override
    public int hashCode() {
      return Objects.hash(minLat, minLon, maxLat, maxLon);
    }
  }

  // LOOK what about no intersection?
  public MinMaxIndices subsetProjectionRect(ProjectionRect projbb) {
    MinMaxIndices result = new MinMaxIndices();
    boolean needsEdges = false;

    // find the x,y index of the starting point and ending points
    ProjectionPoint ll = projbb.getLowerLeftPoint();
    Optional<CurvilinearCoords.CoordReturn> resulto = findIndexFromLatLon(ll.getY(), ll.getX());
    if (resulto.isEmpty()) { // LOOK what to do?
      needsEdges = true;
    } else {
      CurvilinearCoords.CoordReturn index = resulto.get();
      result.addPoint(index.latindex, index.lonindex);
    }

    ProjectionPoint ur = projbb.getUpperRightPoint();
    resulto = findIndexFromLatLon(ur.getY(), ur.getX());
    if (resulto.isEmpty()) {
      needsEdges = true;
    } else {
      CurvilinearCoords.CoordReturn index = resulto.get();
      result.addPoint(index.latindex, index.lonindex);
    }

    ProjectionPoint lr = projbb.getLowerRightPoint();
    resulto = findIndexFromLatLon(lr.getY(), lr.getX());
    if (resulto.isEmpty()) {
      needsEdges = true;
    } else {
      CurvilinearCoords.CoordReturn index = resulto.get();
      result.addPoint(index.latindex, index.lonindex);
    }

    ProjectionPoint ul = projbb.getUpperLeftPoint();
    resulto = findIndexFromLatLon(ul.getY(), ul.getX());
    if (resulto.isEmpty()) {
      needsEdges = true;
    } else {
      CurvilinearCoords.CoordReturn index = resulto.get();
      result.addPoint(index.latindex, index.lonindex);
    }

    if (needsEdges) {
      addLatLonEdges(projbb, result);
    }

    return result;
  }

  private void addLatLonEdges(ProjectionRect projbb, MinMaxIndices result) {
    CurvilinearProjection proj = new CurvilinearProjection();
    // go along the perimeter of the edge arrays
    for (int y = 0; y < nrows; y++) {
      // top and bottom
      if (y == 0 || y == nrows - 1) {
        for (int x = 0; x < ncols; x++) {
          CoordReturn midpoint = midpoint(y, x);
          if (projbb.contains(ProjectionPoint.create(midpoint.lon, midpoint.lat))) {
            result.addPoint(y, x);
          }
        }
      } else {
        // internal rows
        CoordReturn midpoint = midpoint(y, 0);
        if (projbb.contains(ProjectionPoint.create(midpoint.lon, midpoint.lat))) {
          result.addPoint(y, 0);
        }
        midpoint = midpoint(y, ncols - 1);
        if (projbb.contains(ProjectionPoint.create(midpoint.lon, midpoint.lat))) {
          result.addPoint(y, ncols - 1);
        }
      }
    }
  }

}
