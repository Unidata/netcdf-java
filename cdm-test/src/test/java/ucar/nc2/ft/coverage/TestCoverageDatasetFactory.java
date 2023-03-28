package ucar.nc2.ft.coverage;

import static com.google.common.truth.Truth.assertThat;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.unidata.io.RandomAccessFile;

public class TestCoverageDatasetFactory {

  @BeforeClass
  public static void setupCaches() {
    RandomAccessFile.enableDefaultGlobalFileCache();
    NetcdfDatasets.initNetcdfFileCache(1, 10, 15, 200);
  }

  @AfterClass
  public static void shutdownCaches() {
    NetcdfDatasets.shutdown();
    RandomAccessFile.setGlobalFileCache(null);
  }

  @After
  public void cleanupAfterEach() {
    NetcdfDatasets.getNetcdfFileCache().clearCache(true);
    RandomAccessFile.getGlobalFileCache().clearCache(true);
  }

  @Test
  public void shouldReleaseCacheResources() throws Exception {
    final String filename = "src/test/data/ucar/nc2/ft/coverage/coverageWithLambertConformalConic_m.nc";

    try (FeatureDatasetCoverage featureDatasetCoverage = CoverageDatasetFactory.open(filename)) {
      assertThat(featureDatasetCoverage).isNotNull();
    }
    assertThatCacheFileIsNotLocked();
  }

  @Test
  public void shouldReleaseCacheResourcesWhenGridsAreEmpty() throws Exception {
    final String filename = "../cdm/core/src/test/data/pointPre1.6/pointUnlimited.nc";

    try (FeatureDatasetCoverage featureDatasetCoverage = CoverageDatasetFactory.open(filename)) {
      assertThat(featureDatasetCoverage).isNull();
    }
    assertThatCacheFileIsNotLocked();
  }

  private static void assertThatCacheFileIsNotLocked() {
    assertThat(RandomAccessFile.getGlobalFileCache().showCache().size()).isEqualTo(1);
    assertThat(RandomAccessFile.getGlobalFileCache().showCache().get(0)).startsWith("false");

    assertThat(NetcdfDatasets.getNetcdfFileCache().showCache().size()).isEqualTo(1);
    assertThat(NetcdfDatasets.getNetcdfFileCache().showCache().get(0)).startsWith("false");
  }
}
