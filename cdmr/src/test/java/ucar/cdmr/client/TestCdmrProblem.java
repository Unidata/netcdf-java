/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.cdmr.client;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.util.CompareArrayToMa2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Test {@link CdmrNetcdfFile} */
public class TestCdmrProblem {

  // Send one chunk u(0:2, 0:39, 0:90997) size=43679040 bytes
  // Send one chunk u(3:5, 0:39, 0:90997) size=43679040 bytes
  // Send one chunk u(6:8, 0:39, 0:90997) size=43679040 bytes
  // Send one chunk u(0:0, 0:39, 0:90997) size=14559680 bytes
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testChunkProblem() throws Exception {
    String localFilename = "formats/netcdf4/multiDimscale.nc4";
    doOne(TestDir.cdmUnitTestDir + localFilename, "u");
  }

  @Test
  public void testOpaqueDataType() throws Exception {
    String localFilename = "hdf5/test_atomic_types.nc";
    doOne(TestDir.cdmLocalFromTestDataDir + localFilename);
  }

  @Test
  public void testCdmrProblem2() throws Exception {
    String localFilename = "dataset/SimpleGeos/hru_soil_moist_vlen_3hru_5timestep.nc";
    doOne(TestDir.cdmLocalFromTestDataDir + localFilename);
  }

  public void doOne(String filename) throws Exception {
    // LOOK kludge for now. Also, need to auto start up CmdrServer
    String cdmrUrl = "cdmr://localhost:16111/" + filename;
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null);
        CdmrNetcdfFile cdmrFile = CdmrNetcdfFile.builder().setRemoteURI(cdmrUrl).build()) {

      boolean ok = CompareArrayToMa2.compareFiles(ncfile, cdmrFile);
      assertThat(ok).isTrue();
    }
  }

  public void doOne(String filename, String varName) throws Exception {
    // LOOK kludge for now. Also, need to auto start up CmdrServer
    String cdmrUrl = "cdmr://localhost:16111/" + filename;
    try (NetcdfFile ma2File = NetcdfDatasets.openFile(filename, null);
        CdmrNetcdfFile arrayFile = CdmrNetcdfFile.builder().setRemoteURI(cdmrUrl).build()) {

      boolean ok = CompareArrayToMa2.compareVariable(ma2File, arrayFile, varName, true);
      assertThat(ok).isTrue();
    }
  }

}
