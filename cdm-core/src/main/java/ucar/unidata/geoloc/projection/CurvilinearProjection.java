/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.geoloc.projection;

import ucar.array.Array;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;

import java.util.Objects;
import java.util.Optional;

public class CurvilinearProjection extends AbstractProjection {
  Array<Double> latdata;
  Array<Double> londata;

  public CurvilinearProjection() {
    super("Curvilinear", false);
  }

  public CurvilinearProjection(Array<Double> latdata, Array<Double> londata) {
    super("Curvilinear", false);
    this.latdata = latdata;
    this.londata = londata;
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

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    CurvilinearProjection that = (CurvilinearProjection) o;
    boolean what = Objects.equals(latdata, that.latdata) && Objects.equals(londata, that.londata);
    return what; // LOOK fuzzy math needed
  }

  @Override
  public int hashCode() {
    return Objects.hash(latdata, londata);
  }
}
