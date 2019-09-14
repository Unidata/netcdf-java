package ucar.unidata.geoloc.projection.sat;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionPointImpl;

public class TestMSGnavigation {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  // Copied from MSGnavigation. I don't really care about their value, I just want them to be "reasonable".
  private static final double SAT_HEIGHT = 42164.0; // distance from Earth centre to satellite
  private static final double R_EQ = 6378.169; // radius from Earth centre to equator
  private static final double R_POL = 6356.5838; // radius from Earth centre to pol

  // Demonstrates the bug described in TDS-575.
  @Test
  public void testConstructCopy() throws Exception {
    // These are the same as the values used in MSGnavigation() except for the 2nd, lon0. For the purposes of
    // this test, that's the only argument I care about.
    MSGnavigation msgNav = new MSGnavigation(0.0, 180, R_EQ, R_POL, SAT_HEIGHT, SAT_HEIGHT - R_EQ, SAT_HEIGHT - R_EQ);

    Assert.assertEquals(Math.PI, msgNav.getLon0(), 1e-6); // 180° = π radians.

    // The 2 tests below failed prior to TDS-575 being fixed.

    MSGnavigation msgNavCopy1 = (MSGnavigation) msgNav.constructCopy();
    Assert.assertEquals(Math.PI, msgNavCopy1.getLon0(), 1e-6); // 180° = π radians.

    MSGnavigation msgNavCopy2 = (MSGnavigation) msgNavCopy1.constructCopy();
    Assert.assertEquals(Math.PI, msgNavCopy2.getLon0(), 1e-6); // 180° = π radians.
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////

  static void tryit(double want, double x) {
    System.out.printf("x = %f %f %f %n", x, x / want, want / x);
  }

  static private void doOne(ProjectionImpl proj, double lat, double lon) {
    LatLonPointImpl startL = new LatLonPointImpl(lat, lon);
    ProjectionPoint p = proj.latLonToProj(startL);
    LatLonPointImpl endL = (LatLonPointImpl) proj.projToLatLon(p);

    System.out.println("start  = " + startL.toString(8));
    System.out.println("xy   = " + p.toString());
    System.out.println("end  = " + endL.toString(8));

  }

  static private void doTwo(ProjectionImpl proj, double x, double y) {
    ProjectionPointImpl startL = new ProjectionPointImpl(x, y);
    LatLonPoint p = proj.projToLatLon(startL);
    ProjectionPointImpl endL = (ProjectionPointImpl) proj.latLonToProj(p);

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


    /*
     * MSGnavigation msg = new MSGnavigation();
     * System.out.printf("const1 = %f %n",msg.const1);
     * System.out.printf("1/const1 = %f %n",1.0/msg.const1);
     * System.out.printf("const2 = %10.8f %n",msg.const2);
     * System.out.printf("1/const) = %f %n",1.0/msg.const2);
     * System.out.printf("1/(1+const2) = %f %n",1.0/(1+msg.const2));
     * System.out.printf("1/(1-const2) = %f %n",1.0/(1-msg.const2));
     * 
     * double s = Math.sqrt(1737121856.);
     * System.out.printf("sqrt = %f %n",s);
     * System.out.printf("try = %f %n",SAT_HEIGHT*SAT_HEIGHT - R_EQ * R_EQ);
     * 
     * /* tryit(s, SAT_HEIGHT/msg.const1);
     * tryit(s, SAT_HEIGHT/(1+msg.const2));
     * tryit(s, SAT_HEIGHT/(1+msg.const2*msg.const2));
     * tryit(s, SAT_HEIGHT/msg.const1*msg.const1);
     * tryit(s, SAT_HEIGHT/msg.const1*(1+msg.const2));
     * 
     * tryit(s, SAT_HEIGHT*(1+msg.const2));
     * 
     * /*
     * :grid_mapping_name = "MSGnavigation";
     * :longitude_of_projection_origin = 0.0; // double
     * :latitude_of_projection_origin = 0.0; // double
     * :semi_major_axis = 6378.14013671875; // double
     * :semi_minor_axis = 6356.75537109375; // double
     * :height_from_earth_center_km = 42163.97100180664; // double
     * :dx = 1207.0; // double
     * :dy = 1189.0; // double
     */

    // MSGnavigation m = new MSGnavigation(0, 0, 6378.14013671875, 6356.75537109375, 42163.97100180664);

    // doOne(m, 11, 51);
    // doOne(m, -34, 18);
    /*
     * doTwo(m, 100, 100);
     * doTwo(m, 5000, 5000);
     * 
     * m = new MSGnavigation(0, 0, 6378.14013671875, 6356.75537109375, 42163.97100180664, 0, 0);
     * System.out.printf("%ncfac = %f r = %f %n", m.cfac, CFAC/m.cfac);
     * System.out.printf("lfac = %f r = %f %n", m.lfac, CFAC/m.lfac);
     * 
     * doTwo(m, 100, 100);
     * doTwo(m, 5000, 5000);
     */

  }
}


