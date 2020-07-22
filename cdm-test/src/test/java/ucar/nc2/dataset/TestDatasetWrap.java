/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import java.util.Formatter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/** Compare acquireDataset with enhance(acquireFile) */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestDatasetWrap {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(500);
    try {
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "ft/grid", new SuffixFileFilter(".nc"), result);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  private DatasetUrl durl;

  public TestDatasetWrap(String filename) {
    durl = DatasetUrl.create(null, filename);
  }

  @Test
  public void doOne() throws Exception {
    try (NetcdfFile ncfile = NetcdfDatasets.acquireFile(durl, null);
        NetcdfDataset ncWrap = NetcdfDatasets.enhance(ncfile, NetcdfDataset.getDefaultEnhanceMode(), null)) {

      NetcdfDataset ncd = NetcdfDatasets.acquireDataset(durl, true, null);
      System.out.println(" dataset wraps= " + durl.getTrueurl());

      Formatter errlog = new Formatter();
      boolean ok = CompareNetcdf2.compareFiles(ncd, ncWrap, errlog);
      if (!ok) {
        System.out.printf("FAIL %s %s%n", durl, errlog);
      }
      Assert.assertTrue(ok);
    }
  }
}
