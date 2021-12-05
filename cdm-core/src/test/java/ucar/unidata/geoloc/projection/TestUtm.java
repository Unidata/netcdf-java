/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.projection;

import org.junit.Test;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.*;

import static com.google.common.truth.Truth.assertThat;

/** Test basic projection methods */
public class TestUtm {

  static double maxx_all = 0.0;

  int REPEAT = 100;
  int NPTS = 10000;
  boolean checkit = false;
  boolean calcErrs = true;
  boolean show = false;
  double tolm = 10.0; // tolerence in meters

  long sumNormal = 0;

  java.util.Random r = new java.util.Random(System.currentTimeMillis());

  void doOne(double x, double y, int zone, boolean isNorth) {
    Projection proj = new UtmProjection(zone, isNorth);

    System.out.println("*** x=" + x + " y=" + y);
    LatLonPoint latlon = proj.projToLatLon(x, y);
    System.out.println("   lat=" + latlon.getLatitude() + " lon=" + latlon.getLongitude());
    ProjectionPoint endP = proj.latLonToProj(latlon);
    System.out.println("   x=" + endP.getX() + " y=" + endP.getY());
  }

  void run(int zone, boolean isNorth) {
    System.out.println("--------- zone= " + zone + " " + isNorth);

    double[][] from = new double[2][NPTS]; // random x, y
    for (int i = 0; i < NPTS; i++) {
      from[0][i] = 800 * r.nextDouble(); // random x point 400 km on either side of central meridian
      from[1][i] = isNorth ? 8000 * r.nextDouble() : 10000.0 - 8000 * r.nextDouble(); // random y point
    }

    int n = REPEAT * NPTS;

    Projection proj = new UtmProjection(zone, isNorth);
    double sumx = 0.0, sumy = 0.0, maxx = 0.0;
    long t1 = System.currentTimeMillis();
    for (int k = 0; k < REPEAT; k++) {
      for (int i = 0; i < NPTS; i++) {
        LatLonPoint latlon = proj.projToLatLon(from[0][i], from[1][i]);
        ProjectionPoint endP = proj.latLonToProj(latlon);

        if (calcErrs) {
          double errx = error(from[0][i], endP.getX());
          sumx += errx;
          sumy += error(from[1][i], endP.getY());
          maxx = Math.max(maxx, errx);
          maxx_all = Math.max(maxx, maxx_all);
        }

        if (checkit) {
          check_km("y", from[1][i], endP.getY());
          if (check_km("x", from[0][i], endP.getX()) || show)
            System.out.println("   x=" + from[0][i] + " y=" + from[1][i] + " lat=" + latlon.getLatitude() + " lon="
                + latlon.getLongitude());
        }
      }
    }
    long took = System.currentTimeMillis() - t1;
    sumNormal += took;
    System.out.println(" " + n + " normal " + proj.getClassName() + " took " + took + " msecs." + " avg error x= "
        + 1000 * sumx / n + " y=" + 1000 * sumy / n + " maxx err = " + 1000 * maxx + " m");
  }

  double error(double d1, double d2) {
    return Math.abs(d1 - d2);
  }

  boolean check_km(String what, double d1, double d2) {
    double err = 1000 * Math.abs(d1 - d2);
    if (err > tolm)
      System.out.println(" *" + what + ": " + d1 + "!=" + d2 + " err=" + err + " m");
    return (err > tolm);
  }

  boolean check_m(String what, double d1, double d2) {
    double err = d1 == 0.0 ? 0.0 : Math.abs(d1 - d2);
    if (err > tolm)
      System.out.println(" *" + what + ": " + d1 + "!=" + d2 + " err=" + err + " m");
    return (err > tolm);
  }

  boolean check_deg(String what, double d1, double d2) {
    double err = d1 == 0.0 ? 0.0 : Math.abs(d1 - d2);
    if (err > 10e-7)
      System.out.println(" *" + what + ": " + d1 + "!=" + d2 + " err=" + err + " degrees");
    return (err > 10e-7);
  }

  @Test
  public void testStuff2() {
    TestUtm r = new TestUtm();
    r.run(1, true);
    r.run(60, false);
    System.out.println("\nmaxx_all= " + 1000 * maxx_all + " m");
  }


  /*
   * roszelld@usgs.gov
   * 'm transforming coordinates (which are in UTM Zone 17N projection) to
   * lat/lon.
   * 
   * If I get the ProjectionImpl from the grid (stage) and use the projToLatLon
   * function with {{577.8000000000001}, {2951.8}} in kilometers for example, I
   * get {{26.553706785076937}, {-80.21754983617633}}, which is not very
   * accurate at all when I plot them.
   * 
   * If I use GeoTools to build a transform based on the same projection
   * parameters read from the projectionimpl, I get {{26.685132668190793},
   * {-80.21802662821469}} which appears to be MUCH more accurate when I plot
   * them on a map.
   */
  @Test
  public void testStuff() {
    UtmProjection utm = new UtmProjection(17, true);
    LatLonPoint ll = utm.projToLatLon(577.8000000000001, 2951.8);
    System.out.printf("%15.12f %15.12f%n", ll.getLatitude(), ll.getLongitude());
    assertThat(Misc.nearlyEquals(ll.getLongitude(), -80.21802662821469, 1.0e-8)).isTrue();
    assertThat(Misc.nearlyEquals(ll.getLatitude(), 26.685132668190793, 1.0e-8)).isTrue();
  }

}
