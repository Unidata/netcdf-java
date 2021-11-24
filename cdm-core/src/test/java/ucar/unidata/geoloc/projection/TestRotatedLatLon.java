package ucar.unidata.geoloc.projection;

import java.io.PrintStream;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;

/**
 * Tests for {@link RotatedLatLon}.
 * 
 * @author Ben Caradoc-Davies (Transient Software Limited)
 */
public class TestRotatedLatLon {

  /** Tolerance for coordinate comparisons. */
  private static final double TOLERANCE = 1e-6;

  /** A rotated lat/lon projection with origin at 54 degrees North, 254 degrees East. */
  private Projection proj = new RotatedLatLon(-36, 254, 0);

  /** Test that the unrotated centre lat/lon is the origin of the rotated projection. */
  @Test
  public void testLatLonToProj() {
    LatLonPoint latlon = LatLonPoint.create(54, 254);
    ProjectionPoint result = proj.latLonToProj(latlon);
    Assert.assertEquals("Unexpected rotated longitude", 0, result.getX(), TOLERANCE);
    Assert.assertEquals("Unexpected rotated latitude", 0, result.getY(), TOLERANCE);
  }

  /** Test that the origin of the rotated projection is the unrotated centre lat/lon. */
  @Test
  public void testProjToLatLon() {
    ProjectionPoint p = ProjectionPoint.create(0, 0);
    LatLonPoint latlonResult = proj.projToLatLon(p);
    Assert.assertEquals("Unexpected longitude", 254 - 360, latlonResult.getLongitude(), TOLERANCE);
    Assert.assertEquals("Unexpected latitude", 54, latlonResult.getLatitude(), TOLERANCE);
  }

  private static class Testing {
    RotatedLatLon rll;
    static PrintStream ps = System.out;

    public Testing(double lo, double la, double rot) {
      rll = new RotatedLatLon(la, lo, rot);
    }

    void pr(double[] pos, double[] pos2, double[] pos3) {
      ps.println(" " + pos[0] + "   " + pos[1]);
      ps.println("    fwd: " + pos2[0] + "   " + pos2[1]);
      ps.println("    inv: " + pos3[0] + "   " + pos3[1]);
    }

    final static double err = 0.0001;

    private double[] test(float lon, float lat) {
      double[] p = {lon, lat};
      double[] p2 = rll.rotate(p, rll.getLonPole(), rll.getPoleRotate(), rll.getSinDlat());
      double[] p3 = rll.rotate(p2, -rll.getPoleRotate(), -rll.getLonPole(), -rll.getSinDlat());
      assert Math.abs(p[0] - p3[0]) < err;
      assert Math.abs(p[1] - p3[1]) < err;
      pr(p, p2, p3);
      return p2;
    }

    double[] proj(double lon, double lat, boolean fwd) {
      double[] pos = {lon, lat};
      double[] pos2 = fwd ? rll.rotate(pos, rll.getLonPole(), rll.getPoleRotate(), rll.getSinDlat())
          : rll.rotate(pos, -rll.getPoleRotate(), -rll.getLonPole(), -rll.getSinDlat());
      ps.println((fwd ? " fwd" : " inv") + " [" + lon + ", " + lat + "] -> " + Arrays.toString(pos2));
      return pos2;
    }
  }

  @Test
  public void testStuff() {
    Testing tst0 = new Testing(0, -25, 0);
    tst0.proj(0, -25, true);

    Testing t = new Testing(0, 90, 0);
    t.test(0, 0);
    t.test(90, 0);
    t.test(0, 30);
    t = new Testing(0, 0, 0);
    t.test(0, 0);
    t.test(90, 0);
    t.test(0, 30);
    t = new Testing(10, 50, 25);
    t.test(0, 0);
    t.test(90, 0);
    t.test(0, 30);
    RotatedLatLon rll = new RotatedLatLon(-50, 10, 20);
    long t0 = System.currentTimeMillis();
    long dt = 0;
    double[] p = {12., 60.};
    int i = 0;
    while (dt < 1000) {
      rll.rotate(p, rll.getLonPole(), rll.getPoleRotate(), rll.getSinDlat());
      rll.rotate(p, rll.getLonPole(), rll.getPoleRotate(), rll.getSinDlat());
      rll.rotate(p, rll.getLonPole(), rll.getPoleRotate(), rll.getSinDlat());
      rll.rotate(p, rll.getLonPole(), rll.getPoleRotate(), rll.getSinDlat());
      rll.rotate(p, rll.getLonPole(), rll.getPoleRotate(), rll.getSinDlat());
      rll.rotate(p, rll.getLonPole(), rll.getPoleRotate(), rll.getSinDlat());
      rll.rotate(p, rll.getLonPole(), rll.getPoleRotate(), rll.getSinDlat());
      rll.rotate(p, rll.getLonPole(), rll.getPoleRotate(), rll.getSinDlat());
      rll.rotate(p, rll.getLonPole(), rll.getPoleRotate(), rll.getSinDlat());
      rll.rotate(p, rll.getLonPole(), rll.getPoleRotate(), rll.getSinDlat());
      i++;
      dt = System.currentTimeMillis() - t0;
    }
    System.out.println("fwd/sec: " + i * 10);
  }

}
