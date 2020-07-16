/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Variable;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** Test opening nested NcML with and without use of NetcdfDataset.initNetcdfFileCache. */
@Category(NeedsCdmUnitTest.class)
@RunWith(JUnit4.class)
public class TestAggNested {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void TestNotCached() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ncml/nestedAgg/test.ncml";

    try (NetcdfDataset ncd = NetcdfDatasets.acquireDataset(DatasetUrl.findDatasetUrl(filename), true, null)) {
      Variable time = ncd.findVariable("time");
      assertThat(time.getSize()).isEqualTo(19723);
      // System.out.printf(" time array = %s%n", NCdumpW.toString(time.read()));
    }
  }

  @Test
  public void TestCached() throws IOException {
    try {
      NetcdfDatasets.initNetcdfFileCache(10, 20, -1);

      String filename = TestDir.cdmUnitTestDir + "ncml/nestedAgg/test.ncml";
      try (NetcdfDataset ncd = NetcdfDatasets.acquireDataset(DatasetUrl.findDatasetUrl(filename), true, null)) {
        Variable time = ncd.findVariable("time");
        assertThat(time.getSize()).isEqualTo(19723);
        // System.out.printf(" time array = %s%n", NCdumpW.toString(time.read()));
      }

      FileCacheIF cache = NetcdfDatasets.getNetcdfFileCache();
      cache.showCache();
    } finally {
      NetcdfDatasets.shutdown();
    }
  }

}
