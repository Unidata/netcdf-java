package ucar.nc2.ft2.coverage;

import static com.google.common.truth.Truth.assertThat;
import static ucar.unidata.geoloc.ProjectionPoint.create;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointNoNormalize;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionRect;

public class TestHorizCoordSysCrossSeamBoundary {
  private static final double TOL = 1e-4;

  @Test
  public void shouldCalcProjectionsBoundaryPoints() throws URISyntaxException, IOException {
    HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamProjection.ncml");
    List<ProjectionPoint> actualPoints = horizCoordSys.calcProjectionBoundaryPoints();

    List<ProjectionPoint> expectedPoints = Arrays.asList(
        // Bottom edge
        create(-4500, -2450), create(-3500, -2450), create(-2500, -2450), create(-1500, -2450),
        // Right edge
        create(-500, -2450), create(-500, -1550), create(-500, -650), create(-500, 250),
        // Top edge
        create(-500, 1150), create(-1500, 1150), create(-2500, 1150), create(-3500, 1150),
        // Left edge
        create(-4500, 1150), create(-4500, 250), create(-4500, -650), create(-4500, -1550));

    assertThat(actualPoints).isEqualTo(expectedPoints);
  }

  @Test
  public void shouldCalcProjectionBoundaryPoints2By2() throws URISyntaxException, IOException {
    HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamProjection.ncml");
    List<ProjectionPoint> actualPoints = horizCoordSys.calcProjectionBoundaryPoints(2, 2);

    List<ProjectionPoint> expectedPoints = Arrays.asList(create(-4500, -2450), create(-2500, -2450), // Bottom edge
        create(-500, -2450), create(-500, -650), // Right edge
        create(-500, 1150), create(-2500, 1150), // Top edge
        create(-4500, 1150), create(-4500, -650) // Left edge
    );

    assertThat(actualPoints).isEqualTo(expectedPoints);
  }

  @Test
  public void shouldCalcConnectedLatLonBoundaryPoints() throws URISyntaxException, IOException {
    HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamLatLon1D.ncml");
    List<LatLonPointNoNormalize> actualPoints = horizCoordSys.calcConnectedLatLonBoundaryPoints();

    List<LatLonPointNoNormalize> expectedPoints = Arrays.asList(createNoNorm(0, 130), createNoNorm(0, 150),
        createNoNorm(0, 170), createNoNorm(0, 190), createNoNorm(0, 210), // Bottom edge
        createNoNorm(0, 230), createNoNorm(10, 230), createNoNorm(20, 230), createNoNorm(30, 230),
        createNoNorm(40, 230), // Right edge
        createNoNorm(50, 230), createNoNorm(50, 210), createNoNorm(50, 190), createNoNorm(50, 170),
        createNoNorm(50, 150), // Top edge
        createNoNorm(50, 130), createNoNorm(40, 130), createNoNorm(30, 130), createNoNorm(20, 130),
        createNoNorm(10, 130) // Left edge
    );

    assertThat(actualPoints).isEqualTo(expectedPoints);
  }

  @Test
  public void shouldCalcConnectedLatLonBoundaryPoints2By3() throws URISyntaxException, IOException {
    HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamLatLon1D.ncml");
    List<LatLonPointNoNormalize> actualPoints = horizCoordSys.calcConnectedLatLonBoundaryPoints(2, 3);

    List<LatLonPointNoNormalize> expectedPoints =
        Arrays.asList(createNoNorm(0, 130), createNoNorm(0, 170), createNoNorm(0, 210), // Bottom edge
            createNoNorm(0, 230), createNoNorm(30, 230), // Right edge
            createNoNorm(50, 230), createNoNorm(50, 190), createNoNorm(50, 150), // Top edge
            createNoNorm(50, 130), createNoNorm(20, 130) // Left edge
        );

    assertThat(actualPoints).isEqualTo(expectedPoints);
  }

  @Test
  public void shouldCalcConnectedLatLonBoundaryPointsFromProjection() throws URISyntaxException, IOException {
    HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamProjection.ncml");
    List<LatLonPointNoNormalize> actualPoints = horizCoordSys.calcConnectedLatLonBoundaryPoints();

    List<LatLonPointNoNormalize> expectedPoints = Arrays.asList(
        // Bottom edge
        createNoNorm(43.3711, -166.4342), createNoNorm(50.4680, -160.0080), createNoNorm(57.1887, -150.5787),
        createNoNorm(62.8319, -136.4768),
        // Right edge
        createNoNorm(66.2450, -116.5346), createNoNorm(74.3993, -122.8787), createNoNorm(82.1083, -142.5686),
        createNoNorm(84.6159, -221.5651),
        // Top edge
        createNoNorm(77.9578, -261.5014), createNoNorm(71.9333, -232.4762), createNoNorm(63.9355, -219.7024),
        createNoNorm(55.5660, -213.1890),
        // Left edge
        createNoNorm(47.3219, -209.3354), createNoNorm(48.4777, -198.1798), createNoNorm(48.1430, -186.7808),
        createNoNorm(46.3647, -175.9940));

    assertThat(actualPoints.size()).isEqualTo(expectedPoints.size());
    for (int i = 0; i < actualPoints.size(); ++i) {
      assertThat(actualPoints.get(i).nearlyEquals(expectedPoints.get(i), TOL)).isTrue();
    }
  }

  @Test
  public void shouldCalcConnectedLatLonBoundaryPoints2D() throws URISyntaxException, IOException {
    HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamLatLon2D.ncml");
    List<LatLonPointNoNormalize> actualPoints = horizCoordSys.calcConnectedLatLonBoundaryPoints();

    List<LatLonPointNoNormalize> expectedPoints = Arrays.asList(
        // Verified by visually inspecting the coverage drawing in ToolsUI.
        // Note how these boundary points differ from the ones we calculated in the test above, even though
        // "crossSeamProjection.ncml" and "crossSeamLatLon2D.ncml" represent the same grid. That's because the
        // edges in shouldCalcConnectedLatLonBoundaryPointsFromProjection were calculated in projection coordinates
        // and THEN converted to lat/lon. THESE edges, on the other hand, were calculated from 2D lat/lon
        // midpoints generated from the projection. Taking that path, there's an unavoidable loss of precision.
        createNoNorm(44.8740, -169.5274), createNoNorm(51.7795, -157.6634), createNoNorm(58.6851, -145.7993),
        createNoNorm(64.2176, -125.9033), // Bottom edge
        createNoNorm(69.7501, -106.0074), createNoNorm(76.0530, -134.4232), createNoNorm(82.3559, -162.8391),
        createNoNorm(83.7438, -207.9060), // Right edge
        createNoNorm(85.1317, -252.9728), createNoNorm(75.7804, -237.0202), createNoNorm(66.4291, -221.0677),
        createNoNorm(57.3500, -213.7392), // Top edge
        createNoNorm(48.2709, -206.4107), createNoNorm(48.0159, -197.4671), createNoNorm(47.7609, -188.5235),
        createNoNorm(46.3175, -179.0254) // Left edge
    );

    assertThat(actualPoints.size()).isEqualTo(expectedPoints.size());
    for (int i = 0; i < actualPoints.size(); ++i) {
      assertThat(actualPoints.get(i).nearlyEquals(expectedPoints.get(i), TOL)).isTrue();
    }
  }

  @Test
  public void shouldCalcConnectedLatLonBoundaryPoints2D2By2() throws URISyntaxException, IOException {
    HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamLatLon2D.ncml");
    List<LatLonPointNoNormalize> actualPoints = horizCoordSys.calcConnectedLatLonBoundaryPoints(2, 2);

    List<LatLonPointNoNormalize> expectedPoints =
        Arrays.asList(createNoNorm(44.8740, -169.5274), createNoNorm(58.6851, -145.7993), // Bottom edge
            createNoNorm(69.7501, -106.0074), createNoNorm(82.3559, -162.8391), // Right edge
            createNoNorm(85.1317, -252.9728), createNoNorm(66.4291, -221.0677), // Top edge
            createNoNorm(48.2709, -206.4107), createNoNorm(47.7609, -188.5235) // Left edge
        );

    assertThat(actualPoints.size()).isEqualTo(expectedPoints.size());
    for (int i = 0; i < actualPoints.size(); ++i) {
      assertThat(actualPoints.get(i).nearlyEquals(expectedPoints.get(i), TOL)).isTrue();
    }
  }

  @Test
  public void shouldCalcProjectionBoundingBox() throws URISyntaxException, IOException {
    HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamProjection.ncml");
    ProjectionRect actualBB = horizCoordSys.calcProjectionBoundingBox();

    ProjectionRect expectedBB =
        new ProjectionRect(ProjectionPoint.create(-4500, -2450), ProjectionPoint.create(-500, 1150));

    assertThat(actualBB).isEqualTo(expectedBB);
  }

  @Test
  public void shouldCalcLatLonBoundingBox1D() throws URISyntaxException, IOException {
    HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamLatLon1D.ncml");
    LatLonRect actualBB = horizCoordSys.calcLatLonBoundingBox();

    // Derived by manually finding the minimum and maximum lat & lon values of the expected points in the
    // shouldCalcConnectedLatLonBoundaryPoints test.
    LatLonRect expectedBB = new LatLonRect(LatLonPoint.create(0, 130), LatLonPoint.create(50, 230));

    assertThat(actualBB).isEqualTo(expectedBB);
  }

  @Test
  public void shouldCalcLatLonBoundingBoxProjection() throws URISyntaxException, IOException {
    HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamProjection.ncml");
    LatLonRect actualBB = horizCoordSys.calcLatLonBoundingBox();

    // Derived by manually finding the minimum and maximum lat & lon values of the expected points in the
    // shouldCalcConnectedLatLonBoundaryPointsFromProjection test.
    LatLonRect expectedBB =
        new LatLonRect(LatLonPoint.create(43.3711, -261.5014), LatLonPoint.create(84.6159, -116.5346));

    assertThat(actualBB.nearlyEquals(expectedBB, TOL)).isTrue();
  }

  @Test
  public void shouldCalcLatLonBoundingBox2D() throws URISyntaxException, IOException {
    HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamLatLon2D.ncml");
    LatLonRect actualBB = horizCoordSys.calcLatLonBoundingBox();

    // Derived by manually finding the minimum and maximum lat & lon values of the expected points in the
    // shouldCalcConnectedLatLonBoundaryPoints2D test.
    LatLonRect expectedBB =
        new LatLonRect(LatLonPoint.create(44.8741, -252.9727), LatLonPoint.create(85.1318, -106.0074));

    assertThat(actualBB.nearlyEquals(expectedBB, TOL)).isTrue();
  }

  private HorizCoordSys getHorizCoordSysOfDataset(String resourceName) throws URISyntaxException, IOException {
    File file = new File(getClass().getResource(resourceName).toURI());

    try (FeatureDatasetCoverage featDsetCov = CoverageDatasetFactory.open(file.getAbsolutePath())) {
      assertThat(featDsetCov).isNotNull();
      assertThat(featDsetCov.getCoverageCollections().size()).isEqualTo(1);

      CoverageCollection covColl = featDsetCov.getCoverageCollections().get(0);
      return covColl.getHorizCoordSys();
    }
  }

  private static LatLonPointNoNormalize createNoNorm(double x, double y) {
    return new LatLonPointNoNormalize(x, y);
  }
}
