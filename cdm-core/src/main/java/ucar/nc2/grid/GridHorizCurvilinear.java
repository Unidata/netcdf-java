/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.MinMax;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPoints;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.Curvilinear;

public class GridHorizCurvilinear extends GridHorizCoordinateSystem {
  private final Array<Double> latedge;
  private final Array<Double> lonedge;
  private LatLonRect llbb;
  private ProjectionRect mapArea;

  public static GridHorizCurvilinear create(GridAxisPoint xaxis, GridAxisPoint yaxis, Array<Number> latdata,
      Array<Number> londata) {
    Preconditions.checkNotNull(xaxis);
    Preconditions.checkNotNull(yaxis);
    Preconditions.checkNotNull(latdata);
    Preconditions.checkNotNull(londata);
    Preconditions.checkArgument(latdata.getRank() == 2);
    Preconditions.checkArgument(londata.getRank() == 2);

    return new GridHorizCurvilinear(xaxis, yaxis, new Curvilinear(), latdata, londata);
  }

  private GridHorizCurvilinear(GridAxisPoint xaxis, GridAxisPoint yaxis, Projection projection, Array<Number> latdata,
      Array<Number> londata) {
    super(xaxis, yaxis, projection);
    this.latedge = makeEdges(latdata);
    this.lonedge = makeEdges(londata);
  }

  /** Get horizontal bounding box in lat, lon coordinates. Uses min/max of lat and lon arrays */
  // LOOK maybe need to use normalized lon ??
  public LatLonRect getLatLonBoundingBox() {
    if (llbb == null) {
      MinMax latMinmax = Arrays.getMinMaxSkipMissingData(latedge, null);
      MinMax lonMinmax = Arrays.getMinMaxSkipMissingData(lonedge, null);

      LatLonPoint llpt = LatLonPoint.create(latMinmax.min(), lonMinmax.min());
      llbb = new LatLonRect(llpt, latMinmax.max() - latMinmax.min(), lonMinmax.max() - lonMinmax.min());
    }
    return llbb;
  }

  /** Get horizontal bounding box in projection coordinates. */
  public ProjectionRect getBoundingBox() {
    if (mapArea == null) {
      LatLonRect latlonRect = getLatLonBoundingBox();
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

      mapArea = world.build();
    }
    return mapArea;
  }

  // from LatLonProjection
  private ProjectionPoint latLonToProj(LatLonPoint latlon, double centerLon) {
    return ProjectionPoint.create(LatLonPoints.lonNormal(latlon.getLongitude(), centerLon), latlon.getLatitude());
  }

  @Override
  public Iterable<CoordBounds> bounds() {
    return () -> new BoundsIterator(getXHorizAxis().getNominalSize(), getYHorizAxis().getNominalSize());
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

      CoordReturn ll = new CoordReturn(lonedge.get(yindex, xindex), latedge.get(yindex, xindex));
      CoordReturn lr = new CoordReturn(lonedge.get(yindex, xindex + 1), latedge.get(yindex, xindex + 1));
      CoordReturn ur = new CoordReturn(lonedge.get(yindex + 1, xindex + 1), latedge.get(yindex + 1, xindex + 1));
      CoordReturn ul = new CoordReturn(lonedge.get(yindex + 1, xindex), latedge.get(yindex + 1, xindex));

      CoordBounds result = new CoordBounds(ll, lr, ur, ul, xindex, yindex);
      xindex++;
      return result;
    }
  }

  /*
   * CoordInterval getCoordIntervalLon() {
   * double edge1, edge2;
   * 
   * double valuei = londata.get(yindex, xindex).doubleValue();
   * double valueim1 = xindex > 0 ? londata.get(yindex, xindex - 1).doubleValue() : Double.NaN;
   * 
   * if (xindex > 0) {
   * edge1 = (valueim1 + valuei) / 2;
   * } else {
   * double value0 = londata.get(yindex, 0).doubleValue();
   * double value1 = londata.get(yindex, 1).doubleValue();
   * edge1 = value0 - (value1 - value0) / 2;
   * }
   * 
   * if (xindex < nx - 1) {
   * double valueip1 = londata.get(yindex, xindex + 1).doubleValue();
   * edge2 = (valuei + valueip1) / 2;
   * } else {
   * edge2 = valuei - (valuei - valueim1) / 2;
   * }
   * 
   * return CoordInterval.create(edge1, edge2);
   * }
   * 
   * CoordInterval getCoordIntervalLat() {
   * double edge1, edge2;
   * 
   * double valuei = latdata.get(yindex, xindex).doubleValue();
   * double valueim1 = yindex > 0 ? latdata.get(yindex - 1, xindex).doubleValue() : Double.NaN;
   * 
   * if (yindex > 0) {
   * edge1 = (valueim1 + valuei) / 2;
   * } else {
   * double value0 = latdata.get(0, xindex).doubleValue();
   * double value1 = latdata.get(1, xindex).doubleValue();
   * edge1 = value0 - (value1 - value0) / 2;
   * }
   * 
   * if (yindex < ny - 1) {
   * double valueip1 = latdata.get(yindex + 1, xindex).doubleValue();
   * edge2 = (valuei + valueip1) / 2;
   * } else {
   * edge2 = valuei - (valuei - valueim1) / 2;
   * }
   * 
   * return CoordInterval.create(edge1, edge2);
   * }
   * }
   */

  private Array<Double> makeEdges(Array<Number> midpoints) {
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

}
