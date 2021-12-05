package ucar.unidata.geoloc.projection.sat;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionPoint;

import static com.google.common.truth.Truth.assertThat;

public class TestMSGnavigation {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // Copied from MSGnavigation. I don't really care about their value, I just want them to be "reasonable".
  private static final double SAT_HEIGHT = 42164.0; // distance from Earth centre to satellite
  private static final double R_EQ = 6378.169; // radius from Earth centre to equator
  private static final double R_POL = 6356.5838; // radius from Earth centre to pol

  // Demonstrates the bug described in TDS-575.
  @Test
  public void testConstructCopy() {
    // These are the same as the values used in MSGnavigation() except for the 2nd, lon0. For the purposes of
    // this test, that's the only argument I care about.
    MSGnavigation msgNav = new MSGnavigation(0.0, 180, R_EQ, R_POL, SAT_HEIGHT, SAT_HEIGHT - R_EQ, SAT_HEIGHT - R_EQ);

    assertThat(msgNav.getLon0()).isWithin(1e-6).of(Math.PI); // 180° = π radians.

    // The 2 tests below failed prior to TDS-575 being fixed.

    MSGnavigation msgNavCopy1 = (MSGnavigation) msgNav.constructCopy();
    assertThat(msgNavCopy1.getLon0()).isWithin(1e-6).of(Math.PI); // 180° = π radians.

    MSGnavigation msgNavCopy2 = (MSGnavigation) msgNavCopy1.constructCopy();
    assertThat(msgNavCopy2.getLon0()).isWithin(1e-6).of(Math.PI); // 180° = π radians.
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////

  static void tryit(double want, double x) {
    System.out.printf("x = %f %f %f %n", x, x / want, want / x);
  }

  static private void doOne(Projection proj, double lat, double lon) {
    LatLonPoint startL = LatLonPoint.create(lat, lon);
    ProjectionPoint p = proj.latLonToProj(startL);
    LatLonPoint endL = proj.projToLatLon(p);

    System.out.println("start  = " + startL.toString());
    System.out.println("xy   = " + p.toString());
    System.out.println("end  = " + endL.toString());
  }

  static private void doTwo(Projection proj, double x, double y) {
    ProjectionPoint startL = ProjectionPoint.create(x, y);
    LatLonPoint p = proj.projToLatLon(startL);
    ProjectionPoint endL = proj.latLonToProj(p);

    System.out.println("start  = " + startL.toString());
    System.out.println("lat,lon   = " + p.toString());
    System.out.println("end  = " + endL.toString());
  }

  @Test
  public void testStuff() {
    double dx = 1207;
    double dy = 1189;
    double nr = 6610700.0;

    double scanx = 2 * Math.asin(1.e6 / nr) / dx;
    double scany = 2 * Math.asin(1.e6 / nr) / dy;
    System.out.printf("scanx = %g urad %n", scanx * 1e6);
    System.out.printf("scany = %g urad %n", scany * 1e6);

    double scan2 = 2 * Math.asin(1.e6 / nr) / 3566;
    System.out.printf("scan2 = %g urad %n", scan2 * 1e6);
  }
}


