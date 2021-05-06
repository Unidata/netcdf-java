/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Stopwatch;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.gcdm.client.GcdmNetcdfFile;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.util.CompareArrayToMa2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Test {@link GcdmNetcdfFile} */
@Category(NeedsCdmUnitTest.class)
public class TestGcdmProblemNeeds {
  private final String filename;
  private final String cdmrUrl;

  public TestGcdmProblemNeeds() {
    String localFilename = "formats/netcdf4/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4";
    this.filename = TestDir.cdmUnitTestDir + localFilename;
    // LOOK kludge for now. Also, need to auto start up CmdrServer
    this.cdmrUrl = "cdmr://localhost:16111/" + TestDir.cdmUnitTestDir + localFilename;
  }

  @Test
  public void doOne() throws Exception {
    System.out.printf("TestGcdmProblem %s%n", filename);
    Stopwatch stopwatch = Stopwatch.createStarted();
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null);
        GcdmNetcdfFile cdmrFile = GcdmNetcdfFile.builder().setRemoteURI(cdmrUrl).build()) {

      boolean ok = CompareArrayToMa2.compareFiles(ncfile, cdmrFile);
      assertThat(ok).isTrue();
    }
    System.out.printf("*** That took %s%n", stopwatch.stop());
  }
}
