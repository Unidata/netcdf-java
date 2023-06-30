package ucar.nc2.ft.point.remote;

import java.util.Formatter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.point.FlattenedDatasetPointCollection;
import ucar.nc2.ft.point.PointTestUtil;
import ucar.unidata.util.test.TestDir;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(Parameterized.class)
public class TestPointStream {

  public static final String cfDocDsgExamplesDir = TestDir.cdmLocalTestDataDir + "cfDocDsgExamples/";
  public static final String pointDir = TestDir.cdmLocalTestDataDir + "point/";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[] {cfDocDsgExamplesDir + "H.1.1.ncml"});
    result.add(new Object[] {pointDir + "point.ncml"});
    result.add(new Object[] {pointDir + "pointMissing.ncml"});
    result.add(new Object[] {pointDir + "pointUnlimited.nc"});

    return result;
  }

  String location;

  public TestPointStream(String location) {
    this.location = location;
  }

  @Test
  public void roundTrip() throws IOException {
    File outFile = temporaryFolder.newFile();
    Formatter errlog = new Formatter();
    try (FeatureDatasetPoint fdPoint =
        (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(FeatureType.ANY_POINT, location, null, errlog)) {

      PointFeatureCollection origPointCol = new FlattenedDatasetPointCollection(fdPoint);
      PointStream.write(origPointCol, outFile);
      PointFeatureCollection roundTrippedPointCol = new PointCollectionStreamLocal(outFile);

      assertThat(PointTestUtil.equals(origPointCol, roundTrippedPointCol)).isTrue();
    }
  }
}
