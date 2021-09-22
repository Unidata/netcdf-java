/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import java.util.Formatter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.NetcdfFile;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Compare acquireDataset with enhance(acquireFile) */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestDatasetWrap {

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

  private final DatasetUrl durl;

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
      assertThat(ok).isTrue();
    }
  }
}
