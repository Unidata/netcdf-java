package ucar.nc2.ncml;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * Tests acquiring aggregated datasets from a file cache.
 */
public class TestCacheAggregation {
  private static final float TOLERANCE = 1.0e-4f;

  @BeforeClass
  public static void setupSpec() {
    // All datasets, once opened, will be added to this cache.
    // Config values copied from CdmInit.
    NetcdfDataset.initNetcdfFileCache(100, 150, 12 * 60);

    // Force NetcdfDataset to reacquire underlying file in order to read a Variable's data, instead of being
    // able to retrieve that data from the Variable's internal cache. OPV-470285 does not manifest on variables
    // with cached data.
    Variable.permitCaching = false;
  }

  @AfterClass
  public static void cleanupSpec() {
    // Undo global changes we made in setupSpec() so that they do not affect subsequent test classes.
    NetcdfDataset.shutdown();
    Variable.permitCaching = true;
  }

  // The number of times each dataset will be acquired.
  // Failure, if it occurs, is expected to start happening on the 2nd trial.
  int numTrials = 2;

  // Demonstrates eSupport ticket OPV-470285.
  @Test
  public void testUnion() throws IOException, InvalidRangeException {
    String filename = "file:./" + TestNcmlRead.topDir + "aggUnion.xml";
    double[] expecteds = new double[] {5.0, 10.0, 15.0, 20.0};

    for (int i = 0; i < numTrials; i++) {
      try (NetcdfDataset ncd = NetcdfDataset.acquireDataset(DatasetUrl.findDatasetUrl(filename), false, null)) {
        Variable var = ncd.findVariable("Temperature");
        Array array = var.read("1,1,:"); // Prior to fix, failure happened here on 2nd trial.
        double[] actuals = (double[]) array.getStorage();
        assertThat(expecteds).isEqualTo(actuals);
      }
    }
  }

  @Test
  public void testJoinExisting() throws IOException, InvalidRangeException {
    String filename = "file:./" + TestNcmlRead.topDir + "aggExisting.xml";
    double[] expecteds = new double[] {8420.0, 8422.0, 8424.0, 8426.0};

    for (int i = 0; i < numTrials; i++) {
      try (NetcdfDataset ncd = NetcdfDataset.acquireDataset(DatasetUrl.findDatasetUrl(filename), false, null)) {
        Variable var = ncd.findVariable("P");
        Array array = var.read("42,1,:");
        double[] actuals = (double[]) array.getStorage();
        assertThat(expecteds).isEqualTo(actuals);
      }
    }
  }

  @Test
  public void testJoinNew() throws IOException, InvalidRangeException {
    String filename = "file:./" + TestNcmlRead.topDir + "aggSynthetic.xml";
    double[] expecteds = new double[] {110.0, 111.0, 112.0, 113.0};

    for (int i = 0; i < numTrials; i++) {
      try (NetcdfDataset ncd = NetcdfDataset.acquireDataset(DatasetUrl.findDatasetUrl(filename), false, null)) {
        Variable var = ncd.findVariable("T");
        Array array = var.read("1,1,:");
        double[] actuals = (double[]) array.getStorage();
        assertThat(expecteds).isEqualTo(actuals);
      }
    }
  }

  @Test
  public void testFmrc() throws IOException, InvalidRangeException {
    String filename = "file:./" + TestNcmlRead.topDir + "fmrc/testAggFmrcScan.ncml";
    float[] expecteds = new float[] {232.6f, 232.4f, 233.0f};

    for (int i = 0; i < numTrials; i++) {
      try (NetcdfDataset ncd = NetcdfDataset.acquireDataset(DatasetUrl.findDatasetUrl(filename), false, null)) {
        Variable var = ncd.findVariable("Temperature_isobaric");
        Array array = var.read(":, 11, 0, 0, 0");
        float[] actuals = (float[]) array.getStorage();
        assertArrayEquals(expecteds, actuals, TOLERANCE);
      }
    }
  }
}
