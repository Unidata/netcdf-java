/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.cdmr.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.category.NotJenkins;

/** Test {@link CdmrNetcdfFile} */
@RunWith(Parameterized.class)
@Category({NotJenkins.class, NeedsCdmUnitTest.class}) // Needs CmdrServer to be started up
public class TestCdmrNetcdf4 {
  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(500);
    try {
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/netcdf4", new SuffixFileFilter(".nc4"), result,
          false);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  private final String filename;
  private final String cdmrUrl;

  public TestCdmrNetcdf4(String filename) {
    this.filename = filename;
    // LOOK kludge for now. Also, need to auto start up CmdrServer
    this.cdmrUrl = "cdmr://localhost:16111/" + filename;
  }

  @Test
  public void doOne() throws Exception {
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null);
        CdmrNetcdfFile cdmrFile = CdmrNetcdfFile.builder().setRemoteURI(cdmrUrl).build()) {

      Formatter errlog = new Formatter();
      boolean ok = CompareNetcdf2.compareFiles(ncfile, cdmrFile, errlog, true, false, false);
      if (!ok) {
        System.out.printf("FAIL %s %s%n", cdmrUrl, errlog);
      }
      Assert.assertTrue(ok);
    }
  }
}
