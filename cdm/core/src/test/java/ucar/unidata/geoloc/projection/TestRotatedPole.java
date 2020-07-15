package ucar.unidata.geoloc.projection;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPoints;
import ucar.unidata.geoloc.ProjectionPoint;
import java.lang.invoke.MethodHandles;

/**
 * Tests for {@link RotatedPole}.
 * 
 * @author Ben Caradoc-Davies (Transient Software Limited)
 */
public class TestRotatedPole {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Tolerance for coordinate comparisons.
   */
  private static final double TOLERANCE = 1e-6;

  /**
   * A rotated lat/lon projection with origin at 54 degrees North, 254 degrees
   * East.
   */
  private RotatedPole proj = new RotatedPole(90 - 54, LatLonPoints.lonNormal(254 + 180));

  /**
   * Test that the unrotated centre lat/lon is the origin of the rotated
   * projection.
   */
  @Test
  public void testLatLonToProj() {
    LatLonPoint latlon = LatLonPoint.create(54, 254);
    ProjectionPoint result = proj.latLonToProj(latlon);
    Assert.assertEquals("Unexpected rotated longitude", 0, result.getX(), TOLERANCE);
    Assert.assertEquals("Unexpected rotated latitude", 0, result.getY(), TOLERANCE);
  }

  /**
   * Test that the origin of the rotated projection is the unrotated centre
   * lat/lon.
   */
  @Test
  public void testProjToLatLon() {
    ProjectionPoint p = ProjectionPoint.create(0, 0);
    LatLonPoint latlonResult = proj.projToLatLon(p);
    Assert.assertEquals("Unexpected longitude", LatLonPoints.lonNormal(254), latlonResult.getLongitude(), TOLERANCE);
    Assert.assertEquals("Unexpected latitude", 54, latlonResult.getLatitude(), TOLERANCE);
  }

}
