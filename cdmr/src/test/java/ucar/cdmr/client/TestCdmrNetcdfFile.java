/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.cdmr.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Formatter;
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
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.category.NotJenkins;

/** Test {@link CdmrNetcdfFile} */
@RunWith(Parameterized.class)
@Category({NeedsExternalResource.class, NotJenkins.class}) // Needs CmdrServer to be started up
public class TestCdmrNetcdfFile {
  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(500);
    try {
      TestDir.actOnAllParameterized(TestDir.cdmLocalFromTestDataDir, new SuffixFileFilter(".nc"), result, true);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  private final String filename;
  private final String cdmrUrl;

  public TestCdmrNetcdfFile(String filename) {
    this.filename = filename;
    // LOOK kludge for now. Also, need to auto start up CmdrServer
    this.cdmrUrl = "cdmr://localhost:16111/" + filename;
  }

  @Test
  public void doOne() throws Exception {
    System.out.printf("TestCdmrNetcdfFile %s%n", filename);
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
