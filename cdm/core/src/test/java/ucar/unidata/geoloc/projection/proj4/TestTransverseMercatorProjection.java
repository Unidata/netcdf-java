package ucar.unidata.geoloc.projection.proj4;

import org.junit.Test;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionPointImpl;

public class TestTransverseMercatorProjection {


  static private void test(ProjectionImpl proj, double[] lat, double[] lon) {
    double[] x = new double[lat.length];
    double[] y = new double[lat.length];
    for (int i = 0; i < lat.length; ++i) {
      LatLonPoint lp = new LatLonPointImpl(lat[i], lon[i]);
      ProjectionPoint p = proj.latLonToProj(lp);
      System.out.println(lp.getLatitude() + ", " + lp.getLongitude() + ": " + p.getX() + ", " + p.getY());
      x[i] = p.getX();
      y[i] = p.getY();
    }
    for (int i = 0; i < lat.length; ++i) {
      ProjectionPointImpl p = new ProjectionPointImpl(x[i], y[i]);
      LatLonPointImpl lp = (LatLonPointImpl) proj.projToLatLon(p);
      if ((Math.abs(lp.getLatitude() - lat[i]) > 1e-5) || (Math.abs(lp.getLongitude() - lon[i]) > 1e-5)) {
        if (Math.abs(lp.getLatitude()) > 89.99 && (Math.abs(lp.getLatitude() - lat[i]) < 1e-5)) {
          // ignore longitude singularities at poles
        } else {
          System.err.print("ERROR:");
        }
      }
      System.out.println("reverse:" + p.getX() + ", " + p.getY() + ": " + lp.getLatitude() + ", " + lp.getLongitude());

    }

  }

  @Test
  public void testStuff() {
    // test-code
    Earth e = new Earth(6378.137, 6356.7523142, 0);
    ProjectionImpl proj = new TransverseMercatorProjection(e, 9., 0., 0.9996, 500.000, 0.);

    double[] lat = {60., 90., 60.};
    double[] lon = {0., 0., 10.};
    test(proj, lat, lon);

    proj = new TransverseMercatorProjection(e, 9., 0., 0.9996, 500., 0.);
    test(proj, lat, lon);
  }

}
