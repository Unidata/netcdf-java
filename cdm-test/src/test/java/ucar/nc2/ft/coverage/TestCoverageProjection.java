/* Copyright Unidata */
package ucar.nc2.ft.coverage;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.Array;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.util.Optional;
import ucar.unidata.geoloc.*;

@RunWith(Parameterized.class)
public class TestCoverageProjection {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    return Arrays.asList(new Object[] {"km", 1.0}, new Object[] {"m", 1000.0});
  }

  private static final String DATA_DIR = "src/test/data/ucar/nc2/ft/coverage/";
  private static final double TOLERANCE = 1.0e-10;

  private final String unit;
  private final double unitScaleFactor;

  public TestCoverageProjection(String unit, double unitScaleFactor) {
    this.unit = unit;
    this.unitScaleFactor = unitScaleFactor;
  }

  @Test
  public void shouldGetBoundingBox() throws IOException {
    final String filename = DATA_DIR + "coverageWithLambertConformalConic_" + unit + ".nc";

    try (final FeatureDatasetCoverage featureDatasetCoverage = CoverageDatasetFactory.open(filename)) {
      assertThat(featureDatasetCoverage).isNotNull();

      final CoverageCollection coverageCollection = featureDatasetCoverage.findCoverageDataset(FeatureType.GRID);
      assertThat(coverageCollection).isNotNull();

      final Coverage coverage = coverageCollection.findCoverage("temp");
      assertThat(coverage).isNotNull();

      final CoverageCoordSys coverageCoordSys = coverage.getCoordSys();
      assertThat(coverageCoordSys).isNotNull();
      assertThat(coverageCoordSys.getShape().length).isEqualTo(3);

      final HorizCoordSys horizCoordSys = coverageCoordSys.getHorizCoordSys();
      assertThat(horizCoordSys).isNotNull();
      assertThat(horizCoordSys.isProjection()).isTrue();
      assertThat(horizCoordSys.getXAxis().getUnits()).isEqualTo(unit);

      final ProjectionRect expectedProjectionRect = new ProjectionRect(-3975 * unitScaleFactor, -1500 * unitScaleFactor,
          1325 * unitScaleFactor, 500 * unitScaleFactor);
      final ProjectionRect projectionRect = horizCoordSys.calcProjectionBoundingBox();
      assertWithMessage("actual: " + projectionRect + ", expected: " + expectedProjectionRect)
          .that(projectionRect.nearlyEquals(expectedProjectionRect)).isTrue();

      final LatLonRect expectedLatLonRect =
          new LatLonRect(LatLonPoint.create(18.4184, -143.143), LatLonPoint.create(43.3274, -79.9189));
      final LatLonRect latLonRect = horizCoordSys.calcLatLonBoundingBox();
      assertWithMessage("actual: " + latLonRect + ", expected: " + expectedLatLonRect)
          .that(latLonRect.nearlyEquals(expectedLatLonRect)).isTrue();
    }
  }

  @Test
  public void shouldGetLatLonByIndex() throws IOException {
    final String filename = DATA_DIR + "coverageWithLambertConformalConic_" + unit + ".nc";

    try (final FeatureDatasetCoverage featureDatasetCoverage = CoverageDatasetFactory.open(filename)) {
      assertThat(featureDatasetCoverage).isNotNull();
      final Coverage coverage = featureDatasetCoverage.findCoverageDataset(FeatureType.GRID).findCoverage("temp");

      final LatLonPoint latLon = coverage.getCoordSys().getHorizCoordSys().getLatLon(0, 0);
      assertThat(latLon.getLatitude()).isWithin(TOLERANCE).of(40.0);
      assertThat(latLon.getLongitude()).isWithin(TOLERANCE).of(-97.0);
    }
  }

  @Test
  public void shouldGetSubsetFromLatLonPoint() throws IOException {
    final String filename = DATA_DIR + "coverageWithLambertConformalConic_" + unit + ".nc";

    try (final FeatureDatasetCoverage featureDatasetCoverage = CoverageDatasetFactory.open(filename)) {
      assertThat(featureDatasetCoverage).isNotNull();
      final Coverage coverage = featureDatasetCoverage.findCoverageDataset(FeatureType.GRID).findCoverage("temp");

      final HorizCoordSys horizCoordSys = coverage.getCoordSys().getHorizCoordSys();
      final Array xCoords = horizCoordSys.getXAxis().getCoordsAsArray();
      assertThat(xCoords.getSize()).isEqualTo(2);
      final Array yCoords = horizCoordSys.getYAxis().getCoordsAsArray();
      assertThat(yCoords.getSize()).isEqualTo(2);

      final LatLonPoint latLon = horizCoordSys.getLatLon(1, 1);
      final SubsetParams subsetParams = new SubsetParams().setLatLonPoint(latLon);
      final Optional<HorizCoordSys> subsetHorizCoordSys = horizCoordSys.subset(subsetParams);
      assertThat(subsetHorizCoordSys.isPresent()).isTrue();

      final LatLonPoint subsetLatLon = subsetHorizCoordSys.get().getLatLon(0, 0);
      assertThat(subsetLatLon.getLatitude()).isWithin(TOLERANCE).of(latLon.getLatitude());
      assertThat(subsetLatLon.getLongitude()).isWithin(TOLERANCE).of(latLon.getLongitude());

      assertThat(subsetHorizCoordSys.get().getXAxis().getUnits()).isEqualTo(horizCoordSys.getXAxis().getUnits());
      final Array subsetXCoords = subsetHorizCoordSys.get().getXAxis().getCoordsAsArray();
      assertThat(subsetXCoords.getSize()).isEqualTo(1);
      assertThat(subsetXCoords.getDouble(0)).isEqualTo(xCoords.getDouble(1));

      assertThat(subsetHorizCoordSys.get().getYAxis().getUnits()).isEqualTo(horizCoordSys.getYAxis().getUnits());
      final Array subsetYCoords = subsetHorizCoordSys.get().getYAxis().getCoordsAsArray();
      assertThat(subsetYCoords.getSize()).isEqualTo(1);
      assertThat(subsetYCoords.getDouble(0)).isEqualTo(yCoords.getDouble(1));
    }
  }

  @Test
  public void shouldGetSubsetFromLatLonRect() throws IOException {
    final String filename = DATA_DIR + "coverageWithLambertConformalConic_" + unit + ".nc";

    try (final FeatureDatasetCoverage featureDatasetCoverage = CoverageDatasetFactory.open(filename)) {
      assertThat(featureDatasetCoverage).isNotNull();
      final Coverage coverage = featureDatasetCoverage.findCoverageDataset(FeatureType.GRID).findCoverage("temp");

      final HorizCoordSys horizCoordSys = coverage.getCoordSys().getHorizCoordSys();
      final Array xCoords = horizCoordSys.getXAxis().getCoordsAsArray();
      final Array yCoords = horizCoordSys.getYAxis().getCoordsAsArray();

      final double delta = 1.0;
      final LatLonRect latLon = new LatLonRect(horizCoordSys.getLatLon(1, 1), delta, delta);
      final SubsetParams subsetParams = new SubsetParams().setLatLonBoundingBox(latLon);
      final Optional<HorizCoordSys> subsetHorizCoordSys = horizCoordSys.subset(subsetParams);
      assertThat(subsetHorizCoordSys.isPresent()).isTrue();

      final LatLonPoint subsetLatLon = subsetHorizCoordSys.get().getLatLon(0, 0);
      assertThat(subsetLatLon.getLatitude()).isWithin(TOLERANCE).of(latLon.getLatMin());
      assertThat(subsetLatLon.getLongitude()).isWithin(TOLERANCE).of(latLon.getLonMin());

      assertThat(subsetHorizCoordSys.get().getXAxis().getUnits()).isEqualTo(horizCoordSys.getXAxis().getUnits());
      final Array xCoordsSubset = subsetHorizCoordSys.get().getXAxis().getCoordsAsArray();
      assertThat(xCoordsSubset.getSize()).isEqualTo(1);
      assertThat(xCoordsSubset.getDouble(0)).isEqualTo(xCoords.getDouble(1));

      assertThat(subsetHorizCoordSys.get().getYAxis().getUnits()).isEqualTo(horizCoordSys.getYAxis().getUnits());
      final Array yCoordsSubset = subsetHorizCoordSys.get().getYAxis().getCoordsAsArray();
      assertThat(yCoordsSubset.getSize()).isEqualTo(1);
      assertThat(yCoordsSubset.getDouble(0)).isEqualTo(yCoords.getDouble(1));
    }
  }
}
