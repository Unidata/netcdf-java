package ucar.nc2.ft2.coverage.writer;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.ft2.coverage.TestHorizCoordSysCrossSeamBoundary;

public class TestCFGridCoverageWriter2 {
  private static final double TOL = 1e-5;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void shouldCalcOutputFileSizes() throws IOException, URISyntaxException, InvalidRangeException {
    // Lengths of the y and x axes in the dataset.
    double numY = 4;
    double numX = 4;

    File testFile = new File(TestHorizCoordSysCrossSeamBoundary.class.getResource("crossSeamProjection.ncml").toURI());
    try (FeatureDatasetCoverage featDsetCov = CoverageDatasetFactory.open(testFile.getAbsolutePath())) {
      assertThat(featDsetCov).isNotNull();
      assertThat(featDsetCov.getCoverageCollections().size()).isEqualTo(1);
      CoverageCollection covColl = featDsetCov.getCoverageCollections().get(0);

      // excluding 2D lat/lon vars
      double expectedSizeNoLatLon = numY * numX * DataType.FLOAT.getSize() + // Temperature_surface
          numY * DataType.FLOAT.getSize() + // y
          numX * DataType.FLOAT.getSize() + // x
          1 * DataType.INT.getSize(); // PolarStereographic_Projection

      double actualSizeNoLatLon = CFGridCoverageWriter2.getSizeOfOutput(
          // No subset; don't addLatLon; calc file size but don't write file.
          covColl, null, null, false).get();

      assertThat(actualSizeNoLatLon).isWithin(TOL).of(expectedSizeNoLatLon);

      // including 2D lat/lon vars
      double expectedSizeWithLatLon = expectedSizeNoLatLon + numY * numX * DataType.DOUBLE.getSize() + // lat
          numY * numX * DataType.DOUBLE.getSize(); // lon

      double actualSizeWithLatLon = CFGridCoverageWriter2.getSizeOfOutput(
          // No subset; do addLatLon; calc file size but don't write file.
          covColl, null, null, true).get();
      assertThat(actualSizeWithLatLon).isWithin(TOL).of(expectedSizeWithLatLon);
    }

  }

  @Test
  public void shouldAdd2DLatLonVariables() throws IOException, InvalidRangeException, URISyntaxException {
    int[] expectedShape = {4, 4};
    double[] expectedLatsList = { // These come from crossSeamLatLon2D.ncml
        48.771275207078986, 56.257940168398875, 63.23559652027781, 68.69641273007204, 51.52824383539942,
        59.91283563136657, 68.26407960692367, 75.7452461192097, 52.765818800755305, 61.615297053148296,
        70.80822358575152, 80.19456756234185, 52.28356434154232, 60.94659393490472, 69.78850194830888,
        78.27572828144659};
    Array expectedLats = Array.factory(DataType.DOUBLE, expectedShape, expectedLatsList);
    double[] expectedLonsList = { // These come from crossSeamLatLon2D.ncml
        -168.434948822922, -161.3099324740202, -150.0, -131.56505117707798, -179.6237487511738, -174.86369657175186,
        -166.1892062570269, -147.27368900609372, 167.86240522611175, 168.81407483429038, 170.71059313749964,
        176.3099324740202, 155.0737544933483, 151.8659776936037, 145.70995378081128, 130.00797980144134};
    Array expectedLons = Array.factory(DataType.DOUBLE, expectedShape, expectedLonsList);

    File testFile = new File(TestHorizCoordSysCrossSeamBoundary.class.getResource("crossSeamProjection.ncml").toURI());
    try (FeatureDatasetCoverage featDsetCov = CoverageDatasetFactory.open(testFile.getAbsolutePath())) {
      assertThat(featDsetCov).isNotNull();
      assertThat(featDsetCov.getCoverageCollections().size()).isEqualTo(1);
      CoverageCollection covColl = featDsetCov.getCoverageCollections().get(0);

      File outputFile = tempFolder.newFile();
      try (NetcdfFileWriter writer = NetcdfFileWriter.createNew(outputFile.getAbsolutePath(), false)) {
        CFGridCoverageWriter2.write(covColl, null, null, true, writer);

        try (NetcdfFile ncFile = NetcdfFiles.open(outputFile.getAbsolutePath())) {
          Array actualLats = ncFile.findVariable("lat").read();
          assertThat(actualLats.getShape()).isEqualTo(expectedLats.getShape());
          MAMath.nearlyEquals(expectedLats, actualLats);

          Array actualLons = ncFile.findVariable("lon").read();
          assertThat(actualLons.getShape()).isEqualTo(expectedLons.getShape());
          MAMath.nearlyEquals(expectedLons, actualLons);
        }
      }
    }
  }
}
