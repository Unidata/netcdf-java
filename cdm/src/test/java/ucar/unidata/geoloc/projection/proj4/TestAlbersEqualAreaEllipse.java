package ucar.unidata.geoloc.projection.proj4;

import org.junit.Test;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPoint;

public class TestAlbersEqualAreaEllipse {



  /*
   proj +inv +proj=aea +lat_0=23.0 +lat_1=29.5 +lat_2=45.5 +a=6378137.0 +rf=298.257222101 +b=6356752.31414 +lon_0=-96.0

Input X,Y:     1730692.593817677      1970917.991173046

results in:

Output Lon,Lat:     -75.649278      39.089117
   *
   */

  private static void toProj(ProjectionImpl p, double lat, double lon) {
    System.out.printf("lon,lat = %f %f%n", lon, lat);
    ProjectionPoint pt = p.latLonToProj(lat, lon);
    System.out.printf("x,y     = %f %f%n", pt.getX(), pt.getY());
    LatLonPoint ll = p.projToLatLon(pt);
    System.out.printf("lon,lat = %f %f%n%n", ll.getLongitude(), ll.getLatitude());
  }

  private static void fromProj(ProjectionImpl p, double x, double y) {
    System.out.printf("x,y     = %f %f%n", x,y);
    LatLonPoint ll = p.projToLatLon(x, y);
    System.out.printf("lon,lat = %f %f%n", ll.getLongitude(), ll.getLatitude());
    ProjectionPoint pt = p.latLonToProj(ll);
    System.out.printf("x,y     = %f %f%n%n", pt.getX(), pt.getY());
  }

  @Test
  public void testStuff() {
    AlbersEqualAreaEllipse a = new AlbersEqualAreaEllipse(23.0, -96.0, 29.5, 45.5, 0, 0, new Earth(6378137.0, 0.0, 298.257222101));
    System.out.printf("proj = %s %s%n%n", a.getName(), a.paramsToString());
    //fromProj(a, 1730.692593817677, 1970.917991173046);
    //toProj(a, 39.089117, -75.649278);

    fromProj(a, 5747, 13470);

  }

}
