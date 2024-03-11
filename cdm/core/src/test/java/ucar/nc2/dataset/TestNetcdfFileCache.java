package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ncml.TestNcmlRead;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.util.test.TestDir;

public class TestNetcdfFileCache {
  @BeforeClass
  public static void setupCaches() {
    NetcdfDatasets.initNetcdfFileCache(1, 10, 15, -1);
  }

  @AfterClass
  public static void shutdownCaches() {
    NetcdfDatasets.shutdown();
  }

  @After
  public void cleanupAfterEach() {
    NetcdfDatasets.getNetcdfFileCache().clearCache(true);
  }

  @Test
  public void shouldReleaseLockOnNetcdfFileUsingBuilder() throws IOException {
    final String filename = "file:./" + TestDir.cdmLocalTestDataDir + "jan.nc";
    final DatasetUrl durl = DatasetUrl.findDatasetUrl(filename);

    final NetcdfFile netcdfFile = NetcdfDatasets.acquireFile(durl, null);
    final NetcdfDataset netcdfDatasetFromBuilder = NetcdfDataset.builder(netcdfFile).build();
    // Closing the builder NetcdfDataset should close the original NetcdfFile acquired from cache
    netcdfDatasetFromBuilder.close();

    assertNoFilesAreLocked();
  }

  @Test
  public void shouldReleaseLockOnNetcdfDatasetUsingBuilder() throws IOException {
    final String filename = "file:./" + TestDir.cdmLocalTestDataDir + "jan.nc";
    final DatasetUrl durl = DatasetUrl.findDatasetUrl(filename);

    final NetcdfDataset netcdfDataset = NetcdfDatasets.acquireDataset(durl, null);
    final NetcdfDataset netcdfDatasetFromBuilder = netcdfDataset.toBuilder().build();
    // Closing the builder NetcdfDataset should close the original NetcdfDataset acquired from cache
    netcdfDatasetFromBuilder.close();

    assertNoFilesAreLocked();
  }

  @Test
  public void shouldReleaseLockOnDataset() throws IOException {
    final String filename = "file:./" + TestDir.cdmLocalTestDataDir + "jan.nc";
    assertLockIsReleasedOnDataset(filename);
  }

  @Test
  public void shouldReleaseLockOnAggregation() throws IOException {
    final String filename = "file:./" + TestNcmlRead.topDir + "aggExisting.xml";
    assertLockIsReleasedOnDataset(filename);
  }

  private static void assertLockIsReleasedOnDataset(String filename) throws IOException {
    final DatasetUrl durl = DatasetUrl.findDatasetUrl(filename);
    final NetcdfFile netcdfFile = NetcdfDatasets.acquireFile(durl, null);
    final NetcdfDataset netcdfDataset = NetcdfDatasets.enhance(netcdfFile, NetcdfDataset.getDefaultEnhanceMode(), null);
    // Closing the netcdf dataset should close the "wrapped" NetcdfFile acquired from cache
    netcdfDataset.close();

    assertNoFilesAreLocked();
  }

  private static void assertNoFilesAreLocked() {
    FileCacheIF cache = NetcdfDatasets.getNetcdfFileCache();
    boolean isAnyFileLocked = cache.showCache().stream().anyMatch(entry -> entry.startsWith("true"));
    assertWithMessage(cache.showCache().toString()).that(isAnyFileLocked).isFalse();
  }
}
