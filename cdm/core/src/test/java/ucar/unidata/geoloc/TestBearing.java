package ucar.unidata.geoloc;

import static ucar.unidata.geoloc.Bearing.calculateBearing;
import static ucar.unidata.geoloc.Bearing.findPoint;
import org.junit.Test;

public class TestBearing {

  @Test
  public void testStuff() {
    // Bearing workBearing = new Bearing();
    LatLonPoint pt1 = LatLonPoint.create(40, -105);
    LatLonPoint pt2 = LatLonPoint.create(37.4, -118.4);
    Bearing b = calculateBearing(pt1, pt2, null);
    System.out.println("Bearing from " + pt1 + " to " + pt2 + " = \n\t" + b);
    LatLonPoint pt3 = findPoint(pt1, b.getAngle(), b.getDistance());
    System.out.println("using first point, angle and distance, found second point at " + pt3);
    pt3 = findPoint(pt2, b.getBackAzimuth(), b.getDistance());
    System.out.println("using second point, backazimuth and distance, found first point at " + pt3);
    /*
     * uncomment for timing tests
     * for(int j=0;j<10;j++) {
     * long t1 = System.currentTimeMillis();
     * for(int i=0;i<30000;i++) {
     * workBearing = Bearing.calculateBearing(42.5,-93.0,
     * 48.9,-117.09,workBearing);
     * }
     * long t2 = System.currentTimeMillis();
     * System.err.println ("time:" + (t2-t1));
     * }
     */
  }

}
