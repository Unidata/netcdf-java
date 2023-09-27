package ucar.nc2.ft2.coverage.writer;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.*;
import ucar.nc2.ft.point.StationPointFeature;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Station;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Test CoverageAsPoint */
public class TestCoverageAsPoint {

  private static final String testFilePath = TestDir.cdmLocalTestDataDir + "point/testGridAsPointAxes.ncml";

  private static CoverageCollection gds;

  // point coords
  private static final double lat = 3.0;
  private static final double lon = 5.0;
  private static final LatLonPoint latlon = LatLonPoint.create(lat, lon);
  private static final double[] alts = new double[] {0.0, 10.0, 20.0, 30.0};
  private static final double[] times = new double[] {0.0, 3600.0};
  // point data values
  private static final double[] expected = new double[] {11.0, 111.0, 211.0, 311.0, 1011.0, 1111.0, 1211.0, 1311.0};

  @BeforeClass
  public static void setupTest() throws IOException {
    // create coverage as point for all vars in test file
    gds = CoverageDatasetFactory.open(testFilePath).getCoverageCollections().get(0);
  }

  @Test
  public void testVarGroupByDimensions() throws IOException {
    List<String> varNames = new ArrayList<>();
    gds.getCoverages().forEach(cov -> varNames.add(cov.getName()));

    SubsetParams params = new SubsetParams();
    params.setVariables(varNames);
    params.setLatLonPoint(latlon);

    FeatureDatasetPoint fdp = new CoverageAsPoint(gds, varNames, params).asFeatureDatasetPoint();
    assertThat(fdp.getFeatureType()).isEqualTo(FeatureType.ANY_POINT);

    // assert all vars are present
    assertThat(fdp.getDataVariables().size()).isEqualTo(10);
    // assert vars are grouped by dimensions
    assertThat(fdp.getPointFeatureCollectionList().size()).isEqualTo(8);
  }

  @Test
  public void testVarFeatureTypes() throws IOException {
    // vars with no z (or z.len <=1) should be station
    List<String> stationVarNames = Arrays.asList(new String[] {"withZ1", "withT1Z1", "Z1noT", "T1noZ"});

    SubsetParams params = new SubsetParams();
    params.setVariables(stationVarNames);
    params.setLatLonPoint(latlon);
    params.setVertCoord(0);

    FeatureDatasetPoint fdp1 = new CoverageAsPoint(gds, stationVarNames, params).asFeatureDatasetPoint();
    assertThat(fdp1.getFeatureType()).isEqualTo(FeatureType.STATION);

    // vars with z should be station profile
    List<String> profileVarNames = Arrays.asList(new String[] {"full4", "withT1", "full3", "3D", "4D"});

    params = new SubsetParams();
    params.setLatLonPoint(latlon);
    params.setVariables(profileVarNames);

    FeatureDatasetPoint fdp2 = new CoverageAsPoint(gds, profileVarNames, params).asFeatureDatasetPoint();
    assertThat(fdp2.getFeatureType()).isEqualTo(FeatureType.STATION_PROFILE);
  }

  @Test
  public void testCoverageAsPoint() throws IOException {
    double[] vals = Arrays.copyOfRange(expected, 0, 1);
    // test single point (no time)
    List<String> varNames = new ArrayList<>();
    varNames.add("2D");
    SubsetParams params = new SubsetParams();
    params.setVariables(varNames);
    params.setLatLonPoint(latlon);
    readCoverageAsPoint(varNames, params, alts[0], times, vals);

    // test time series
    varNames = new ArrayList<>();
    varNames.add("T1noZ");
    vals = Arrays.copyOfRange(expected, 0, 2);
    params.setVariables(varNames);
    readCoverageAsPoint(varNames, params, alts[0], times, vals, 0, "time1");

    // test multiple time axes
    varNames = new ArrayList<>();
    varNames.add("full4");
    varNames.add("withT1");
    vals = new double[] {11.0, 1011.0};
    params.setVariables(varNames);
    params.setVertCoord(alts[0]);
    readCoverageAsPoint(varNames, params, alts[0], times, vals, 0, "time");
    readCoverageAsPoint(varNames, params, alts[0], times, vals, 1, "time1");
  }

  @Test
  public void testExceptionThrownForPointOutsideCoverage() {
    double[] vals = Arrays.copyOfRange(expected, 0, 1);
    // test single point (no time)
    List<String> varNames = new ArrayList<>();
    varNames.add("2D");
    SubsetParams params = new SubsetParams();
    params.setVariables(varNames);
    params.setLatLonPoint(LatLonPoint.create(100, 100));
    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> readCoverageAsPoint(varNames, params, alts[0], times, vals));
    assertThat(e).hasMessageThat()
        .contains("No coverage data found for parameters  latlonPoint == 90.0000N 100.0000E, var == [2D],");
  }

  @Test
  public void testCoverageAsProfile() throws IOException {
    // test single profile (no time)
    List<String> varNames = new ArrayList<>();
    varNames.add("3D");
    SubsetParams params = new SubsetParams();
    params.setVariables(varNames);
    params.setLatLonPoint(latlon);
    readCoverageAsProfile(varNames, params, alts, times, Arrays.copyOfRange(expected, 0, 4));

    // test time series of profiles
    varNames = new ArrayList<>();
    varNames.add("4D");
    params.setVariables(varNames);
    readCoverageAsProfile(varNames, params, alts, times, expected);

    // test two different time axes
    varNames = new ArrayList<>();
    varNames.add("full4");
    varNames.add("withT1");
    params = new SubsetParams();
    params.setVariables(varNames);
    params.setLatLonPoint(latlon);
    readCoverageAsProfile(varNames, params, alts, times, expected);
    readCoverageAsProfile(varNames, params, alts, times, expected, 1, "time1", "z");

    // test no time different z-axis names
    varNames = new ArrayList<>();
    varNames.add("full3");
    varNames.add("Z1noT");
    params = new SubsetParams();
    params.setVariables(varNames);
    params.setLatLonPoint(latlon);
    readCoverageAsProfile(varNames, params, alts, times, Arrays.copyOfRange(expected, 0, 4));
    readCoverageAsProfile(varNames, params, alts, times, Arrays.copyOfRange(expected, 0, 2), 1, "time", "z1");

    // test single timeseries different z-axis names
    varNames = new ArrayList<>();
    varNames.add("withT1");
    varNames.add("withT1Z1");
    params = new SubsetParams();
    params.setVariables(varNames);
    params.setLatLonPoint(latlon);
    readCoverageAsProfile(varNames, params, alts, times, expected, 0, "time1", "z");
    readCoverageAsProfile(varNames, params, Arrays.copyOfRange(alts, 0, 2), times, Arrays.copyOfRange(expected, 0, 4),
        1, "time1", "z1");

    // test different time-axis names and different z-axis names
    varNames = new ArrayList<>();
    varNames.add("full4");
    varNames.add("withT1Z1");
    params = new SubsetParams();
    params.setVariables(varNames);
    params.setLatLonPoint(latlon);
    readCoverageAsProfile(varNames, params, alts, times, expected);
    readCoverageAsProfile(varNames, params, Arrays.copyOfRange(alts, 0, 2), times, Arrays.copyOfRange(expected, 0, 4),
        1, "time1", "z1");
  }

  @Test
  public void testSubset() throws IOException {
    List<String> varNames = new ArrayList<>();
    varNames.add("4D");

    // subset by vertical level
    SubsetParams params = new SubsetParams();
    params.setVariables(varNames);
    params.setLatLonPoint(latlon);
    params.setVertCoord(alts[1]);
    readCoverageAsPoint(varNames, params, alts[1], times, new double[] {expected[1], expected[5]});

    // subset by time
    params = new SubsetParams();
    params.setVariables(varNames);
    params.setLatLonPoint(latlon);
    params.setTimeOffset(times[1]);
    double[] vals = Arrays.copyOfRange(expected, 4, 8);
    readCoverageAsProfile(varNames, params, alts, new double[] {times[1]}, vals);

    // subset on vertical and time
    params.setVertCoord(alts[1]);
    readCoverageAsPoint(varNames, params, alts[1], new double[] {times[1]}, new double[] {expected[5]});
  }

  private void readCoverageAsPoint(List<String> varNames, SubsetParams params, double alt, double[] time,
      double[] expected) throws IOException {
    readCoverageAsPoint(varNames, params, alt, time, expected, 0, "time");
  }

  private void readCoverageAsPoint(List<String> varNames, SubsetParams params, double alt, double[] time,
      double[] expected, int stationIndex, String timeName) throws IOException {
    FeatureDatasetPoint fdp = new CoverageAsPoint(gds, varNames, params).asFeatureDatasetPoint();
    assertThat(fdp.getFeatureType()).isEqualTo(FeatureType.STATION);
    final String varName = varNames.get(stationIndex);

    StationTimeSeriesFeatureCollection fc =
        (StationTimeSeriesFeatureCollection) fdp.getPointFeatureCollectionList().get(stationIndex);
    assertThat(fc).isNotNull();
    assertThat(fc.getCollectionFeatureType()).isEqualTo(FeatureType.STATION);

    StationTimeSeriesFeature stationFeature = (StationTimeSeriesFeature) fc.getStationFeatures().get(0);

    int i = 0;
    for (PointFeature feat : stationFeature) {
      assertThat(feat).isInstanceOf(StationPointFeature.class);

      // verify point coords
      Station station = ((StationPointFeature) feat).getStation();
      assertThat(station.getLatitude()).isEqualTo(lat);
      assertThat(station.getLongitude()).isEqualTo(lon);
      assertThat(station.getAltitude()).isEqualTo(alt);
      assertThat(((StationTimeSeriesFeature) station).getTimeName()).isEqualTo(timeName);
      assertThat(feat.getObservationTime()).isEqualTo(time[i]);

      // verify point data
      Array data = feat.getDataAll().getArray(varName);
      assertThat(data.getDouble(0)).isEqualTo(expected[i]);
      i++;
    }
    assertThat(i).isEqualTo(expected.length);
  }

  private void readCoverageAsProfile(List<String> varNames, SubsetParams params, double[] alt, double[] time,
      double[] expected) throws IOException {
    readCoverageAsProfile(varNames, params, alt, time, expected, 0, "time", "z");
  }

  private void readCoverageAsProfile(List<String> varNames, SubsetParams params, double[] alt, double[] time,
      double[] expected, int stationIndex, String timeName, String altName) throws IOException {
    FeatureDatasetPoint fdp = new CoverageAsPoint(gds, varNames, params).asFeatureDatasetPoint();
    assertThat(fdp.getFeatureType()).isEqualTo(FeatureType.STATION_PROFILE);
    final String varName = varNames.get(stationIndex);

    StationProfileFeatureCollection fc =
        (StationProfileFeatureCollection) fdp.getPointFeatureCollectionList().get(stationIndex);
    assertThat(fc).isNotNull();
    assertThat(fc.getCollectionFeatureType()).isEqualTo(FeatureType.STATION_PROFILE);

    StationProfileFeature stationFeature = (StationProfileFeature) fc.getStationFeatures().get(0);
    int i = 0;
    for (ProfileFeature profile : stationFeature) {
      assertThat(profile).isInstanceOf(ProfileFeature.class);
      for (PointFeature feat : profile) {
        assertThat(feat).isInstanceOf(StationPointFeature.class);
        // verify point coords
        Station station = ((StationPointFeature) feat).getStation();
        assertThat(station.getLatitude()).isEqualTo(lat);
        assertThat(station.getLongitude()).isEqualTo(lon);
        assertThat(feat.getLocation().getAltitude()).isEqualTo(alt[i % alt.length]);
        assertThat(((StationProfileFeature) station).getTimeName()).isEqualTo(timeName);
        assertThat(((StationProfileFeature) station).getAltName()).isEqualTo(altName);
        assertThat(feat.getObservationTime()).isEqualTo(time[i / alt.length]);
        // verify point data

        Array data = feat.getDataAll().getArray(varName);
        assertThat(data.getDouble(0)).isEqualTo(expected[i]);
        i++;
      }
    }
    assertThat(i).isEqualTo(expected.length);
  }
}
