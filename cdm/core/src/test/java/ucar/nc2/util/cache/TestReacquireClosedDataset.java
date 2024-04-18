package ucar.nc2.util.cache;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.util.Formatter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.TestDir;

/**
 * Tests caching behavior when datasets are closed and then reacquired.
 */
public class TestReacquireClosedDataset {

  @BeforeClass
  public static void setup() {
    // All datasets, once opened, will be added to this cache. Config values copied from CdmInit.
    NetcdfDataset.initNetcdfFileCache(100, 150, 12 * 60);
  }

  @AfterClass
  public static void cleanup() {
    // Undo global changes we made in setup() so that they do not affect subsequent test classes.
    NetcdfDataset.shutdown();
  }

  @Test
  public void shouldReacquire() throws IOException {
    String location = TestDir.cdmLocalTestDataDir + "jan.nc";

    // Acquire and close dataset 4 times
    for (int i = 0; i < 4; i++) {
      NetcdfDataset.acquireDataset(DatasetUrl.findDatasetUrl(location), true, null).close();
    }

    Formatter formatter = new Formatter();
    NetcdfDataset.getNetcdfFileCache().showStats(formatter);

    // The cache will have recorded 1 miss (1st trial) and 3 hits (subsequent trials)
    // This is kludgy, but FileCache doesn't provide getHits() or getMisses() methods.
    assertThat(formatter.toString().trim()).contains("hits= 3 miss= 1");

    // Prior to 2016-03-09 bug fix in AbstractIOServiceProvider.getLastModified(),
    // this would record 0 hits and 4 misses.
  }
}
