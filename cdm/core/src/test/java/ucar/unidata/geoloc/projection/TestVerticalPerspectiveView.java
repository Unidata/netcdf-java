package ucar.unidata.geoloc.projection;

import org.junit.Test;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionRect;

public class TestVerticalPerspectiveView {

  private static void test(double lat, double lon) {
    double radius = 6371.0;

    VerticalPerspectiveView a = new VerticalPerspectiveView(0, 0, radius, 5.62 * radius);
    ProjectionPoint p = a.latLonToProj(lat, lon);
    System.out.println("-----\nproj point = " + p);
    System.out.println("x/r = " + p.getX() / radius); // see snyder p 174
    System.out.println("y/r = " + p.getY() / radius);

    LatLonPoint ll = a.projToLatLon(p);
    System.out.println(" lat = " + ll.getLatitude() + " should be= " + lat);
    System.out.println(" lon = " + ll.getLongitude() + " should be= " + lon);
  }

  @Test
  public void testStuff() {
    double radius = 6371.0;
    double height = 35747.0;

    VerticalPerspectiveView a = new VerticalPerspectiveView(0, 0, radius, height);

    double limit = .99 * Math.sqrt((a.getP() - 1) / (a.getP() + 1));
    System.out.println(" limit = " + limit);
    System.out.println(" limit*90 = " + limit * 90);

    /*
     * whats the min and max lat/lon ?
     * double theta = 0;
     * while (theta <= 360) {
     * double x = limit*radius * Math.cos(Math.toRadians(theta));
     * double y = limit*radius * Math.sin(Math.toRadians(theta));
     * LatLonPointImpl llpt = (LatLonPointImpl) a.projToLatLon( ProjectionPoint.create(x, y));
     * System.out.println(theta+" = "+llpt.toString());
     * theta += 15;
     * }
     */

    LatLonRect rect = new LatLonRect(LatLonPoint.create(-45.0, -45.0), -45.0, -45.0);
    ProjectionRect r = a.latLonToProjBB(rect);
    System.out.println(" ProjectionRect result = " + r);


    /*
     * double minx, maxx, miny, maxy;
     * 
     * // first clip the request rectangle to the bounding box of the grid
     * LatLonRect bb = getLatLonBoundingBox();
     * rect = bb.intersect( rect);
     * if (null == rect)
     * throw new InvalidRangeException("Request Bounding box does not intersect Grid");
     * 
     * LatLonPointImpl llpt = rect.getLowerLeftPoint();
     * LatLonPointImpl urpt = rect.getUpperRightPoint();
     * LatLonPointImpl lrpt = rect.getLowerRightPoint();
     * LatLonPointImpl ulpt = rect.getUpperLeftPoint();
     * 
     * if (isLatLon()) {
     * minx = getMinOrMaxLon(llpt.getLongitude(), ulpt.getLongitude(), true);
     * miny = Math.min(llpt.getLatitude(), lrpt.getLatitude());
     * maxx = getMinOrMaxLon(urpt.getLongitude(), lrpt.getLongitude(), false);
     * maxy = Math.min(ulpt.getLatitude(), urpt.getLatitude());
     * 
     * } else {
     * Projection dataProjection = getProjection();
     * ProjectionPoint ll = dataProjection.latLonToProj(llpt, ProjectionPoint.create());
     * ProjectionPoint ur = dataProjection.latLonToProj(urpt, ProjectionPoint.create());
     * ProjectionPoint lr = dataProjection.latLonToProj(lrpt, ProjectionPoint.create());
     * ProjectionPoint ul = dataProjection.latLonToProj(ulpt, ProjectionPoint.create());
     * 
     * minx = Math.min(ll.getX(), ul.getX());
     * miny = Math.min(ll.getY(), lr.getY());
     * maxx = Math.max(ur.getX(), lr.getX());
     * maxy = Math.max(ul.getY(), ur.getY());
     * }
     */
  }

}
