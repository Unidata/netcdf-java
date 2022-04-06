package ucar.nc2.ft2.coverage.writer;

import static com.google.common.truth.Truth.assertThat;

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

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Test CoverageAsPoint */
public class TestCoverageAsPoint {

  private static final String testFilePath = TestDir.cdmLocalTestDataDir + "rankTest.nc";

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
  public void testFileHash() throws IOException, NoSuchAlgorithmException {
    MessageDigest mdigest = MessageDigest.getInstance("MD5");
    // Get file input stream for reading the file
    // content
    FileInputStream fis = new FileInputStream(testFilePath);

    // Create byte array to read data in chunks
    byte[] byteArray = new byte[1024];
    int bytesCount = 0;

    // read the data from file and update that data in
    // the message digest
    while ((bytesCount = fis.read(byteArray)) != -1)
    {
      mdigest.update(byteArray, 0, bytesCount);
    };

    // close the input stream
    fis.close();

    // store the bytes returned by the digest() method
    byte[] bytes = mdigest.digest();

    // this array of bytes has bytes in decimal format
    // so we need to convert it into hexadecimal format

    // for this we create an object of StringBuilder
    // since it allows us to update the string i.e. its
    // mutable
    StringBuilder sb = new StringBuilder();

    // loop through the bytes array
    for (int i = 0; i < bytes.length; i++) {

      // the following line converts the decimal into
      // hexadecimal format and appends that to the
      // StringBuilder object
      sb.append(Integer
              .toString((bytes[i] & 0xff) + 0x100, 16)
              .substring(1));
    }

    // finally we return the complete hash
    assertThat(sb.toString()).isEqualTo("162f0df1be52baccff678a5622054b8a");
  }

  @Test
  public void testVarGroupByDimensions() throws IOException {
    List<String> varNames = new ArrayList<>();
    gds.getCoverages().forEach(cov -> {
      varNames.add(cov.getName());
    });

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
    params.setVariables(varNames);
    readCoverageAsPoint(varNames, params, alts[0], times, vals);
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
    FeatureDatasetPoint fdp = new CoverageAsPoint(gds, varNames, params).asFeatureDatasetPoint();
    assertThat(fdp.getFeatureType()).isEqualTo(FeatureType.STATION);
    final String varName = varNames.get(0);

    StationTimeSeriesFeatureCollection fc =
        (StationTimeSeriesFeatureCollection) fdp.getPointFeatureCollectionList().get(0);
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
    FeatureDatasetPoint fdp = new CoverageAsPoint(gds, varNames, params).asFeatureDatasetPoint();
    assertThat(fdp.getFeatureType()).isEqualTo(FeatureType.STATION_PROFILE);
    final String varName = varNames.get(0);

    StationTimeSeriesFeatureCollection fc =
        (StationTimeSeriesFeatureCollection) fdp.getPointFeatureCollectionList().get(0);
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
        assertThat(feat.getLocation().getAltitude()).isEqualTo(alts[i % alts.length]);
        assertThat(feat.getObservationTime()).isEqualTo(time[i / alts.length]);
        // verify point data

        Array data = feat.getDataAll().getArray(varName);
        assertThat(data.getDouble(0)).isEqualTo(expected[i]);
        i++;
      }
    }
    assertThat(i).isEqualTo(expected.length);
  }
}
