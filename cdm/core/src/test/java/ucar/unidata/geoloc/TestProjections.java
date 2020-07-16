
package ucar.unidata.geoloc;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.projection.*;
import ucar.unidata.geoloc.projection.proj4.CylindricalEqualAreaProjection;
import ucar.unidata.geoloc.projection.proj4.EquidistantAzimuthalProjection;
import ucar.unidata.geoloc.projection.proj4.PolyconicProjection;
import ucar.unidata.geoloc.projection.proj4.StereographicAzimuthalProjection;
import ucar.unidata.geoloc.projection.proj4.TransverseMercatorProjection;
import ucar.unidata.geoloc.projection.sat.Geostationary;
import ucar.unidata.geoloc.projection.sat.MSGnavigation;
import ucar.unidata.geoloc.projection.proj4.AlbersEqualAreaEllipse;
import ucar.unidata.geoloc.projection.proj4.LambertConformalConicEllipse;
import java.lang.invoke.MethodHandles;
import ucar.unidata.geoloc.projection.sat.VerticalPerspectiveView;

/** Test the standard methods of Projections. */
public class TestProjections {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final boolean show = false;
  private static final int NTRIALS = 10000;
  private static final double tolerence = 5.0e-4;

  private LatLonPoint doOne(Projection proj, double lat, double lon, boolean show) {
    LatLonPoint startL = LatLonPoint.create(lat, lon);
    ProjectionPoint p = proj.latLonToProj(startL);
    if (Double.isNaN(p.getX()) || Double.isNaN(p.getY()))
      return LatLonPoint.INVALID;
    if (Double.isInfinite(p.getX()) || Double.isInfinite(p.getY()))
      return LatLonPoint.INVALID;
    LatLonPoint endL = proj.projToLatLon(p);

    if (show) {
      System.out.println("start  = " + LatLonPoints.toString(startL, 8));
      System.out.println("projection point  = " + p.toString());
      System.out.println("end  = " + endL.toString());
    }
    return endL;
  }

  private void testProjection(Projection proj) {
    java.util.Random r = new java.util.Random(this.hashCode());

    int countT1 = 0;
    for (int i = 0; i < NTRIALS; i++) {
      // random latlon point
      LatLonPoint startL = LatLonPoint.create(180.0 * (r.nextDouble() - .5), 360.0 * (r.nextDouble() - .5));

      ProjectionPoint p = proj.latLonToProj(startL);
      if (Double.isNaN(p.getX()) || Double.isNaN(p.getY()))
        continue;
      LatLonPoint endL = proj.projToLatLon(p);
      if (Double.isNaN(endL.getLatitude()) || Double.isNaN(endL.getLongitude()) || endL.equals(LatLonPoint.INVALID))
        continue;

      Assert.assertEquals(LatLonPoints.toString(startL, 8), startL.getLatitude(), endL.getLatitude(), 1.0e-3);
      Assert.assertEquals(LatLonPoints.toString(startL, 8), startL.getLongitude(), endL.getLongitude(), 1.0e-3);
      countT1++;
    }

    int countT2 = 0;
    for (int i = 0; i < NTRIALS; i++) {
      ProjectionPoint startP = ProjectionPoint.create(10000.0 * (r.nextDouble() - .5), // random proj point
          10000.0 * (r.nextDouble() - .5));

      LatLonPoint ll = proj.projToLatLon(startP);
      if (Double.isNaN(ll.getLatitude()) || Double.isNaN(ll.getLongitude()))
        continue;
      ProjectionPoint endP = proj.latLonToProj(ll);
      if (Double.isNaN(endP.getX()) || Double.isNaN(endP.getY()))
        continue;

      Assert.assertEquals(startP.toString(), startP.getX(), endP.getX(), tolerence);
      Assert.assertEquals(startP.toString(), startP.getY(), endP.getY(), tolerence);
      countT2++;
    }
    if (show)
      System.out.printf("Tested %d, %d pts for projection %s %n", countT1, countT2, proj.getClassName());
  }

  // must have lon within +/- lonMax, lat within +/- latMax
  private void testProjectionLonMax(Projection proj, double lonMax, double latMax, boolean show) {
    java.util.Random r = new java.util.Random(this.hashCode());

    double minx = Double.MAX_VALUE;
    double maxx = -Double.MAX_VALUE;
    double miny = Double.MAX_VALUE;
    double maxy = -Double.MAX_VALUE;

    for (int i = 0; i < NTRIALS; i++) {
      // random latlon point
      LatLonPoint startL = LatLonPoint.create(latMax * (2 * r.nextDouble() - 1), lonMax * (2 * r.nextDouble() - 1));
      ProjectionPoint p = proj.latLonToProj(startL);
      LatLonPoint endL = proj.projToLatLon(p);

      if (show) {
        System.out.println("startL  = " + startL);
        System.out.println("inter  = " + p);
        System.out.println("endL  = " + endL);
      }

      Assert.assertEquals(startL.toString(), startL.getLatitude(), endL.getLatitude(), tolerence);
      Assert.assertEquals(startL.toString(), startL.getLongitude(), endL.getLongitude(), tolerence);

      minx = Math.min(minx, p.getX());
      maxx = Math.max(maxx, p.getX());
      miny = Math.min(miny, p.getY());
      maxy = Math.max(maxy, p.getY());
    }

    double rangex = maxx - minx;
    double rangey = maxy - miny;
    if (show) {
      System.out.printf("***************%n", minx, maxx);
      System.out.printf("rangex  = (%f,%f) %n", minx, maxx);
      System.out.printf("rangey  = (%f,%f) %n", miny, maxy);
    }

    for (int i = 0; i < NTRIALS; i++) {
      double x = minx + rangex * r.nextDouble();
      double y = miny + rangey * r.nextDouble();
      ProjectionPoint startP = ProjectionPoint.create(x, y);

      try {
        LatLonPoint ll = proj.projToLatLon(startP);
        ProjectionPoint endP = proj.latLonToProj(ll);

        if (show) {
          System.out.println("start  = " + startP);
          System.out.println("interL  = " + ll);
          System.out.println("end  = " + endP);
        }

        Assert.assertEquals(startP.toString(), startP.getX(), endP.getX(), tolerence);
        Assert.assertEquals(startP.toString(), startP.getY(), endP.getY(), tolerence);
      } catch (IllegalArgumentException e) {
        System.out.printf("IllegalArgumentException=%s%n", e.getMessage());
      }
    }

    if (show)
      System.out.println("Tested " + NTRIALS + " pts for projection " + proj.getClassName());
  }

  // must have x within +/- xMax, y within +/- yMax
  private void testProjectionProjMax(Projection proj, double xMax, double yMax) {
    java.util.Random r = new java.util.Random(this.hashCode());
    for (int i = 0; i < NTRIALS; i++) {
      double x = xMax * (2 * r.nextDouble() - 1);
      double y = yMax * (2 * r.nextDouble() - 1);
      ProjectionPoint startP = ProjectionPoint.create(x, y);
      try {
        LatLonPoint ll = proj.projToLatLon(startP);
        ProjectionPoint endP = proj.latLonToProj(ll);
        if (show) {
          System.out.println("start  = " + startP);
          System.out.println("interL  = " + ll);
          System.out.println("end  = " + endP);
        }

        Assert.assertEquals(startP.toString(), startP.getX(), endP.getX(), tolerence);
        Assert.assertEquals(startP.toString(), startP.getY(), endP.getY(), tolerence);
      } catch (IllegalArgumentException e) {
        System.out.printf("IllegalArgumentException=%s%n", e.getMessage());
      }
    }
    if (show)
      System.out.println("Tested " + NTRIALS + " pts for projection " + proj.getClassName());
  }

  @Test
  // java.lang.AssertionError: .072111263S 165.00490E expected:<-0.07211126381547306> but was:<39.99999999999999>
  public void testTMproblem() {
    double lat = -.072111263;
    double lon = 165.00490;
    LatLonPoint endL = doOne(new TransverseMercator(), lat, lon, true);
    if (endL.equals(LatLonPoint.INVALID))
      return;
    Assert.assertEquals(lat, endL.getLatitude(), tolerence);
    Assert.assertEquals(lon, endL.getLongitude(), tolerence);
  }

  @Test
  public void testLC() {
    testProjection(new LambertConformal());
    LambertConformal p = new LambertConformal();
    LambertConformal p2 = (LambertConformal) p.constructCopy();
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testLCseam() {
    // test seam crossing
    LambertConformal lc = new LambertConformal(40.0, 180.0, 20.0, 60.0);
    ProjectionPoint p1 = lc.latLonToProj(LatLonPoint.create(0.0, -1.0));
    ProjectionPoint p2 = lc.latLonToProj(LatLonPoint.create(0.0, 1.0));
    if (show) {
      System.out.printf(" p1= x=%f y=%f%n", p1.getX(), p1.getY());
      System.out.printf(" p2= x=%f y=%f%n", p2.getX(), p2.getY());
    }
    assert lc.crossSeam(p1, p2);
  }

  @Test
  public void testTM() {
    testProjection(new TransverseMercator());

    TransverseMercator p = new TransverseMercator();
    TransverseMercator p2 = (TransverseMercator) p.constructCopy();
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testStereo() {
    testProjection(new Stereographic());
    Stereographic p = new Stereographic();
    Stereographic p2 = (Stereographic) p.constructCopy();
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testLA() {
    testProjection(new LambertAzimuthalEqualArea());
    LambertAzimuthalEqualArea p = new LambertAzimuthalEqualArea();
    LambertAzimuthalEqualArea p2 = (LambertAzimuthalEqualArea) p.constructCopy();
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testOrtho() {
    testProjectionLonMax(new Orthographic(), 10, 10, false);
    Orthographic p = new Orthographic();
    Orthographic p2 = (Orthographic) p.constructCopy();
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testAEA() {
    testProjection(new AlbersEqualArea());
    AlbersEqualArea p = new AlbersEqualArea();
    AlbersEqualArea p2 = (AlbersEqualArea) p.constructCopy();
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testCEA() {
    testProjection(new CylindricalEqualAreaProjection());
    CylindricalEqualAreaProjection p = new CylindricalEqualAreaProjection();
    CylindricalEqualAreaProjection p2 = (CylindricalEqualAreaProjection) p.constructCopy();
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testEAP() {
    testProjection(new EquidistantAzimuthalProjection());
    EquidistantAzimuthalProjection p = new EquidistantAzimuthalProjection();
    EquidistantAzimuthalProjection p2 = (EquidistantAzimuthalProjection) p.constructCopy();
    assertThat(p).isEqualTo(p2);
  }

  // LOOK this fails
  public void testAEAE() {
    testProjectionLonMax(new AlbersEqualAreaEllipse(), 180, 80, true);
    AlbersEqualAreaEllipse p = new AlbersEqualAreaEllipse();
    AlbersEqualAreaEllipse p2 = (AlbersEqualAreaEllipse) p.constructCopy();
    assertThat(p).isEqualTo(p2);
  }

  // LOOK this fails
  public void testLCCE() {
    testProjectionLonMax(new LambertConformalConicEllipse(), 360, 80, true);
    LambertConformalConicEllipse p = new LambertConformalConicEllipse();
    LambertConformalConicEllipse p2 = (LambertConformalConicEllipse) p.constructCopy();
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testFlatEarth() {
    testProjectionProjMax(new FlatEarth(), 5000, 5000);
    FlatEarth p = new FlatEarth();
    FlatEarth p2 = (FlatEarth) p.constructCopy();
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testMercator() {
    testProjection(new Mercator());
    Mercator p = new Mercator();
    Mercator p2 = (Mercator) p.constructCopy();
    assertThat(p).isEqualTo(p2);
  }

  private void showProjVal(Projection proj, double lat, double lon) {
    LatLonPoint startL = LatLonPoint.create(lat, lon);
    ProjectionPoint p = proj.latLonToProj(startL);
    if (show)
      System.out.printf("lat,lon= (%f, %f) x, y= (%f, %f) %n", lat, lon, p.getX(), p.getY());
  }

  @Test
  public void testMSG() {
    doOne(new MSGnavigation(), 60, 60, true);
    testProjection(new MSGnavigation());

    MSGnavigation m = new MSGnavigation();
    showProjVal(m, 0, 0);
    showProjVal(m, 60, 0);
    showProjVal(m, -60, 0);
    showProjVal(m, 0, 60);
    showProjVal(m, 0, -60);
  }

  @Test
  public void testRotatedPole() {
    testProjectionLonMax(new RotatedPole(37, 177), 360, 88, false);
    RotatedPole p = new RotatedPole();
    RotatedPole p2 = (RotatedPole) p.constructCopy();
    assertThat(p).isEqualTo(p2);
  }

  /*
   * grid_south_pole_latitude = -30.000001907348633
   * grid_south_pole_longitude = -15.000000953674316
   * grid_south_pole_angle = 0.0
   */
  @Test
  public void testRotatedLatLon() {
    testProjectionLonMax(new RotatedLatLon(-30, -15, 0), 360, 88, false);
    RotatedLatLon p = new RotatedLatLon();
    RotatedLatLon p2 = (RotatedLatLon) p.constructCopy();
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testSinusoidal() {
    doOne(new Sinusoidal(0, 0, 0, 6371.007), 20, 40, true);
    testProjection(new Sinusoidal(0, 0, 0, 6371.007));
    Sinusoidal p = new Sinusoidal();
    Sinusoidal p2 = (Sinusoidal) p.constructCopy();
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testUTM() {
    // The central meridian = (zone * 6 - 183) degrees, where zone in [1,60].
    // zone = (lon + 183)/6
    // 33.75N 15.25E end = 90.0N 143.4W
    // doOne(new UtmProjection(10, true), 33.75, -122);
    testProjectionUTM(-12.89, .07996);

    testProjectionUTM(NTRIALS);

    UtmProjection p = new UtmProjection();
    UtmProjection p2 = (UtmProjection) p.constructCopy();
    assertThat(p).isEqualTo(p2); // */
  }

  private void testProjectionUTM(double lat, double lon) {
    LatLonPoint startL = LatLonPoint.create(lat, lon);
    int zone = (int) ((lon + 183) / 6);
    UtmProjection proj = new UtmProjection(zone, lat >= 0.0);

    ProjectionPoint p = proj.latLonToProj(startL);
    LatLonPoint endL = proj.projToLatLon(p);

    if (show) {
      System.out.println("startL  = " + startL);
      System.out.println("inter  = " + p);
      System.out.println("endL  = " + endL);
    }

    Assert.assertEquals(startL.toString(), startL.getLatitude(), endL.getLatitude(), 1.3e-4);
    Assert.assertEquals(startL.toString(), startL.getLongitude(), endL.getLongitude(), 1.3e-4);
  }

  private void testProjectionUTM(int n) {
    java.util.Random r = new java.util.Random((long) this.hashCode());
    for (int i = 0; i < n; i++) {
      // random latlon point
      LatLonPoint startL = LatLonPoint.create(180.0 * (r.nextDouble() - .5), 360.0 * (r.nextDouble() - .5));
      double lat = startL.getLatitude();
      double lon = startL.getLongitude();
      int zone = (int) ((lon + 183) / 6);
      UtmProjection proj = new UtmProjection(zone, lat >= 0.0);

      ProjectionPoint p = proj.latLonToProj(startL);
      LatLonPoint endL = proj.projToLatLon(p);

      if (show) {
        System.out.println("startL  = " + startL);
        System.out.println("inter  = " + p);
        System.out.println("endL  = " + endL);
      }

      Assert.assertEquals(startL.toString(), startL.getLatitude(), endL.getLatitude(), 1.0e-4);
      Assert.assertEquals(startL.toString(), startL.getLongitude(), endL.getLongitude(), .02);
    }

    if (show)
      System.out.println("Tested " + n + " pts for UTM projection ");
  }

  @Test
  // Test known values from the port to 6. It was sad how many mistakes I made without test failure.
  // These values are gotten from th5.X version, before porting.
  public void makeSanityTest() {
    ProjectionPoint ppt = ProjectionPoint.create(-4000, -2000);
    LatLonPoint lpt = LatLonPoint.create(11, -22);

    AlbersEqualArea albers = new AlbersEqualArea();
    test(albers, ppt, LatLonPoint.create(-2.99645329, -126.78340473));
    test(albers, lpt, ProjectionPoint.create(7858.99117, 1948.34216));

    FlatEarth flat = new FlatEarth();
    test(flat, ppt, LatLonPoint.create(-17.98578564, -37.81970091));
    test(flat, lpt, ProjectionPoint.create(-2401.42949, 1223.18815));

    LambertAzimuthalEqualArea lea = new LambertAzimuthalEqualArea();
    test(lea, ppt, LatLonPoint.create(15.02634094, 62.50447310));
    test(lea, lpt, ProjectionPoint.create(-8814.26563, 5087.96966));

    LambertConformal lc = new LambertConformal();
    test(lc, ppt, LatLonPoint.create(13.70213773, -141.55784873));
    test(lc, lpt, ProjectionPoint.create(8262.03211, 1089.40638));

    Mercator merc = new Mercator();
    test(merc, ppt, LatLonPoint.create(-18.79370670, -143.28014659));
    test(merc, lpt, ProjectionPoint.create(8672.90304, 1156.54768));

    Orthographic orth = new Orthographic();
    test(orth, ppt, LatLonPoint.create(-18.29509511, -41.39503231));
    test(orth, lpt, ProjectionPoint.create(-2342.85390, 1215.68780));

    RotatedLatLon rll = new RotatedLatLon();
    test(rll, ppt, LatLonPoint.create(-46.04179300, 119.52015163));
    test(rll, lpt, ProjectionPoint.create(-62.5755691, -65.5259331));

    RotatedPole rpole = new RotatedPole();
    test(rpole, ProjectionPoint.create(-105, -40), LatLonPoint.create(-11.43562982, 130.98084177));
    test(rpole, lpt, ProjectionPoint.create(62.5755691, 65.5259331));

    Sinusoidal ss = new Sinusoidal();
    test(ss, ppt, LatLonPoint.create(-17.98578564, -37.81970091));
    test(ss, lpt, ProjectionPoint.create(-2401.42949, 1223.18815));

    Stereographic stereo = new Stereographic(90.0, 255.0, 0.9330127018922193, 0, 0, 6371229.0);
    test(stereo, ppt, LatLonPoint.create(89.95689508, -168.43494882));
    test(stereo, lpt, ProjectionPoint.create(9727381.45, -1194372.26));

    UtmProjection utm = new UtmProjection();
    test(utm, ppt, LatLonPoint.create(-14.20675125, 168.02702665));
    test(utm, lpt, ProjectionPoint.create(39757.9030, 24224.2087));

    VerticalPerspectiveView vv = new VerticalPerspectiveView();
    test(vv, ppt, LatLonPoint.create(-19.41473686, -44.82061281));
    test(vv, lpt, ProjectionPoint.create(-2305.97999, 1196.55423));
  }

  @Test
  public void makeSatSanityTest() {
    ProjectionPoint ppt = ProjectionPoint.create(1000, -1000);
    LatLonPoint lpt = LatLonPoint.create(11, -22);

    Geostationary geo = new Geostationary();
    test(geo, ProjectionPoint.create(-.07, .04), LatLonPoint.create(13.36541946, -24.35896030));
    test(geo, lpt, ProjectionPoint.create(-.0644264897, .0331714453));

    MSGnavigation msg = new MSGnavigation();
    test(msg, ppt, LatLonPoint.create(9.12859974, 9.18008048));
    test(msg, lpt, ProjectionPoint.create(-2305.57189, -1187.00138));
  }

  @Test
  public void makeProj4SanityTest() {
    ProjectionPoint ppt = ProjectionPoint.create(999, 666);
    LatLonPoint lpt = LatLonPoint.create(11.1, -222);

    AlbersEqualAreaEllipse aea = new AlbersEqualAreaEllipse();
    test(aea, ppt, LatLonPoint.create(28.58205667, -85.79021175));
    test(aea, lpt, ProjectionPoint.create(-10854.6125, 7215.68490));

    AlbersEqualAreaEllipse aeaSpherical = new AlbersEqualAreaEllipse(23.0, -96.0, 29.5, 45.5, 0, 0, new Earth());
    test(aeaSpherical, ppt, LatLonPoint.create(28.56099715, -85.77391141));
    test(aeaSpherical, lpt, ProjectionPoint.create(-10846.3062, 7202.23178));

    CylindricalEqualAreaProjection cea = new CylindricalEqualAreaProjection();
    test(cea, ppt, LatLonPoint.create(6.03303479, 8.97552756));
    test(cea, lpt, ProjectionPoint.create(15359.7656, 1220.09762));

    CylindricalEqualAreaProjection ceaSpherical = new CylindricalEqualAreaProjection(0, 1, 0, 0, new Earth());
    test(ceaSpherical, ppt, LatLonPoint.create(5.99931086, 8.98526842));
    test(ceaSpherical, lpt, ProjectionPoint.create(15343.1142, 1226.78838));

    makeEquidistantAzimuthalProjectionTest(0, false, LatLonPoint.create(5.99817760, 9.00687785),
        ProjectionPoint.create(4612.89929, 1343.46918));
    makeEquidistantAzimuthalProjectionTest(45, false, LatLonPoint.create(50.18393050, 14.04526295),
        ProjectionPoint.create(5360.13920, 5340.66199));
    makeEquidistantAzimuthalProjectionTest(90, false, LatLonPoint.create(79.24928617, 123.69006753),
        ProjectionPoint.create(5871.24515, 6520.67834));
    makeEquidistantAzimuthalProjectionTest(-90, false, LatLonPoint.create(-79.24928617, 56.30993247),
        ProjectionPoint.create(7513.99763, -8345.13980));

    makeEquidistantAzimuthalProjectionTest(0, true, LatLonPoint.create(5.96464789, 9.01660332),
        ProjectionPoint.create(14599.9299, 4280.76724));
    makeEquidistantAzimuthalProjectionTest(45, true, LatLonPoint.create(50.18063010, 14.08788032),
        ProjectionPoint.create(8862.91638, 8797.76181));
    makeEquidistantAzimuthalProjectionTest(90, true, LatLonPoint.create(79.20269606, 123.69006753),
        ProjectionPoint.create(5870.68098, 6520.05176));
    makeEquidistantAzimuthalProjectionTest(-90, true, LatLonPoint.create(-79.20269606, 56.30993247),
        ProjectionPoint.create(7522.50757, -8354.59105));

    LambertConformalConicEllipse lcc = new LambertConformalConicEllipse();
    test(lcc, ppt, LatLonPoint.create(28.45959294, -85.80653902));
    test(lcc, lpt, ProjectionPoint.create(-10921.9485, 7293.53362));

    PolyconicProjection pc = new PolyconicProjection();
    test(pc, ppt, LatLonPoint.create(29.15559062, 86.84046566));
    test(pc, lpt, ProjectionPoint.create(6658.86657, -695.508749));

    makeStereographicAzimuthalProjectionTest(0, false, LatLonPoint.create(0, 0), ProjectionPoint.create(0, 2297.78968));
    makeStereographicAzimuthalProjectionTest(45, false, LatLonPoint.create(50.47225526, 15.09952044),
        ProjectionPoint.create(11084.6954, 2585.73594));
    makeStereographicAzimuthalProjectionTest(90, false, LatLonPoint.create(78.51651479, 123.69006753),
        ProjectionPoint.create(6540.11179, 7263.53001));
    makeStereographicAzimuthalProjectionTest(-90, false, LatLonPoint.create(-78.51651479, 56.30993247),
        ProjectionPoint.create(9633.88149, -10699.5093));

    makeStereographicAzimuthalProjectionTest(0, true, LatLonPoint.create(6.36756802, 9.63623417),
        ProjectionPoint.create(.0, 1155.24055));
    makeStereographicAzimuthalProjectionTest(45, true, LatLonPoint.create(50.46642756, 15.15040679),
        ProjectionPoint.create(11070.7987, 2591.21171));
    makeStereographicAzimuthalProjectionTest(90, true, LatLonPoint.create(78.46658754, 123.69006753),
        ProjectionPoint.create(6546.11795, 7270.20052));
    makeStereographicAzimuthalProjectionTest(-90, true, LatLonPoint.create(-78.46658754, 56.30993247),
        ProjectionPoint.create(9667.61835, -10736.9779));


    TransverseMercatorProjection tm = new TransverseMercatorProjection();
    test(tm, ppt, LatLonPoint.create(5.95132079, 8.98953399));
    test(tm, lpt, ProjectionPoint.create(68160.1123, 69441.2299));

    TransverseMercatorProjection tmSpherical = new TransverseMercatorProjection(new Earth(), 0, 0, 0.9996, 0, 0);
    test(tmSpherical, ppt, LatLonPoint.create(5.91843536, 8.99922690));
    test(tmSpherical, lpt, ProjectionPoint.create(5009.10324, 18363.9569));
  }

  private void makeEquidistantAzimuthalProjectionTest(double lat0, boolean isSpherical, LatLonPoint expectl,
      ProjectionPoint expectp) {
    ProjectionPoint ppt = ProjectionPoint.create(999, 666);
    LatLonPoint lpt = LatLonPoint.create(11.1, -222);

    EquidistantAzimuthalProjection ea =
        new EquidistantAzimuthalProjection(lat0, 0, 0, 0, isSpherical ? new Earth() : EarthEllipsoid.WGS84);
    test(ea, ppt, expectl);
    test(ea, lpt, expectp);
  }

  private void makeStereographicAzimuthalProjectionTest(double lat0, boolean isSpherical, LatLonPoint expectl,
      ProjectionPoint expectp) {
    ProjectionPoint ppt = ProjectionPoint.create(999, 666);
    LatLonPoint lpt = LatLonPoint.create(11.1, -222);

    StereographicAzimuthalProjection ea = new StereographicAzimuthalProjection(lat0, 0.0, 0.9330127018922193, 60., 0, 0,
        isSpherical ? new Earth() : EarthEllipsoid.WGS84);
    test(ea, ppt, expectl);
    test(ea, lpt, expectp);
  }

  private void test(Projection p, ProjectionPoint ppt, LatLonPoint expect) {
    System.out.printf("%s%n", p);
    LatLonPoint lpt = p.projToLatLon(ppt);
    System.out.printf("  projToLatLon %s -> %s%n", LatLonPoints.toString(ppt, 9), LatLonPoints.toString(lpt, 9));
    assertThat(Misc.nearlyEquals(lpt.getLatitude(), expect.getLatitude())).isTrue();
    assertThat(Misc.nearlyEquals(lpt.getLongitude(), expect.getLongitude())).isTrue();
  }

  private void test(Projection p, LatLonPoint lpt, ProjectionPoint expect) {
    ProjectionPoint ppt = p.latLonToProj(lpt);
    System.out.printf("  latLonToProj %s -> %s%n", LatLonPoints.toString(lpt, 9), LatLonPoints.toString(ppt, 9));
    assertThat(Misc.nearlyEquals(ppt.getX(), expect.getX())).isTrue();
    assertThat(Misc.nearlyEquals(ppt.getY(), expect.getY())).isTrue();
  }

}
