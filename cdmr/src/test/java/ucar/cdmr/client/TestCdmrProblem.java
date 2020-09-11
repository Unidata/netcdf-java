/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.cdmr.client;

import java.util.Formatter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.category.NotJenkins;

/** Test {@link CdmrNetcdfFile} */
@Category(NotJenkins.class) // Needs CmdrServer to be started up
public class TestCdmrProblem {

  private final String filename;
  private final String cdmrUrl;

  public TestCdmrProblem() {
    String localFilename = "dataset/SimpleGeos/hru_soil_moist_vlen_3hru_5timestep.nc";
    this.filename = TestDir.cdmLocalFromTestDataDir + localFilename;
    // LOOK kludge for now. Also, need to auto start up CmdrServer
    this.cdmrUrl = "cdmr://localhost:16111/" + TestDir.cdmLocalFromTop + localFilename;
  }

  @Test
  public void doOne() throws Exception {
    System.out.printf("TestCdmrProblem %s%n", filename);
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
