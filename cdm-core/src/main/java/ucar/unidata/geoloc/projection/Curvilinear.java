/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.geoloc.projection;

import ucar.array.Array;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;

import java.util.Optional;

public class Curvilinear extends AbstractProjection {
  Array<Double> latArray;
  Array<Double> lonArray;

  public Curvilinear() {
    super("Curvilinear", false);
  }

  @Override
  public ProjectionPoint latLonToProj(LatLonPoint latlon) {
    double lat = latlon.getLatitude();
    double lon = latlon.getLongitude();
    Optional<CoordReturn> coords = findXYindexFromCoord(lat, lon);
    return coords.map(c -> ProjectionPoint.create(c.yidx, c.xidx)).orElse(null);
  }

  @Override
  public LatLonPoint projToLatLon(ProjectionPoint ppt) {
    return null;
    /*
     * int xidx = (int) ppt.getX(); // LOOK 0 based
     * int yidx = (int) ppt.getY();
     * double lat = latArray.get(yidx, xidx);
     * double lon = lonArray.get(yidx, xidx);
     * return LatLonPoint.create(lat, lon);
     */
  }

  @Override
  public boolean crossSeam(ProjectionPoint pt1, ProjectionPoint pt2) {
    return false;
  }

  @Override
  public Projection constructCopy() {
    return null;
  }

  @Override
  public String paramsToString() {
    return "";
  }

  public static class CoordReturn {
    public double lat, lon;
    public int xidx, yidx;
  }

  private Optional<CoordReturn> findXYindexFromCoord(double lat, double lon) {
    return Optional.empty();
  }
}
