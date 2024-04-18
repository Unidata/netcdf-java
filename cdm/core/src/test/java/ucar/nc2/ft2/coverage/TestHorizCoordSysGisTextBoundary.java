package ucar.nc2.ft2.coverage;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.Test;

public class TestHorizCoordSysGisTextBoundary {

  // Builds on HorizCoordSysCrossSeamBoundarySpec."calcConnectedLatLonBoundaryPoints(2, 3) - lat/lon 1D"()
  @Test
  public void shouldGetLatLonBoundaryAsWKT() throws IOException, URISyntaxException {
    HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamLatLon1D.ncml");
    String actualWKT = horizCoordSys.getLatLonBoundaryAsWKT(2, 3);

    String expectedWKT = "POLYGON((" + "130.000 0.000, 170.000 0.000, 210.000 0.000, " + // Bottom edge
        "230.000 0.000, 230.000 30.000, " + // Right edge
        "230.000 50.000, 190.000 50.000, 150.000 50.000, " + // Top edge
        "130.000 50.000, 130.000 20.000" + // Left edge
        "))";

    assertThat(actualWKT).isEqualTo(expectedWKT);
  }

  // Builds on HorizCoordSysCrossSeamBoundarySpec."calcConnectedLatLonBoundaryPoints(2, 2) - lat/lon 2D"()
  @Test
  public void shouldGetLatLonBoundaryAsGeoJSON() throws IOException, URISyntaxException {
    HorizCoordSys horizCoordSys = getHorizCoordSysOfDataset("crossSeamLatLon2D.ncml");
    String actualGeoJSON = horizCoordSys.getLatLonBoundaryAsGeoJSON(2, 2);

    String expectedGeoJSON = "{ 'type': 'Polygon', 'coordinates': [ [ " + "[-169.527, 44.874], [-145.799, 58.685], " + // Bottom
                                                                                                                       // edge
        "[-106.007, 69.750], [-162.839, 82.356], " + // Right edge
        "[-252.973, 85.132], [-221.068, 66.429], " + // Top edge
        "[-206.411, 48.271], [-188.523, 47.761]" + // Left edge
        " ] ] }";

    assertThat(actualGeoJSON).isEqualTo(expectedGeoJSON);
  }

  private HorizCoordSys getHorizCoordSysOfDataset(String resourceName) throws IOException, URISyntaxException {
    File file = new File(getClass().getResource(resourceName).toURI());

    try (FeatureDatasetCoverage featDsetCov = CoverageDatasetFactory.open(file.getAbsolutePath())) {
      // Assert that featDsetCov was opened without failure and it contains 1 CoverageCollection.
      assertThat(featDsetCov).isNotNull();
      assertThat(featDsetCov.getCoverageCollections().size()).isEqualTo(1);

      // Return HorizCoordSys from single CoverageCollection.
      CoverageCollection covColl = featDsetCov.getCoverageCollections().get(0);
      return covColl.getHorizCoordSys();
    }
  }
}
