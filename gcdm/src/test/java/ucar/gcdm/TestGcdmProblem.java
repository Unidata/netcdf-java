/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.gcdm.client.GcdmNetcdfFile;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.nc2.internal.util.CompareArrayToMa2;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Test {@link GcdmNetcdfFile} */
public class TestGcdmProblem {

  // media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/bufr/userExamples/test1.bufr
  // media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/bufr/userExamples/test1.bufr
  // media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/multiDimscale.nc4

  // char variables from BUFR are incorrect
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCharProblem() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/test1.bufr";
    Path path = Paths.get(localFilename);
    doOne(path, "obs");
    doTwo(path);
  }

  // Send one chunk u(0:2, 0:39, 0:90997) size=43679040 bytes
  // Send one chunk u(3:5, 0:39, 0:90997) size=43679040 bytes
  // Send one chunk u(6:8, 0:39, 0:90997) size=43679040 bytes
  // Send one chunk u(0:0, 0:39, 0:90997) size=14559680 bytes
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testChunkProblem() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/netcdf4/multiDimscale.nc4";
    Path path = Paths.get(localFilename);
    doOne(path, "u");
    doTwo(path);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testShowClassPath() throws Exception {
    Misc.showClassPath();
  }

  @Test
  public void testOpaqueDataType() throws Exception {
    String localFilename = TestDir.cdmLocalTestDataDir + "hdf5/test_atomic_types.nc";
    Path path = Paths.get(localFilename);
    doOne(path);
    doTwo(path);
  }

  @Test
  public void testGcdmProblem2() throws Exception {
    String localFilename = TestDir.cdmLocalTestDataDir + "dataset/SimpleGeos/hru_soil_moist_vlen_3hru_5timestep.nc";
    Path path = Paths.get(localFilename);
    doOne(path);
    doTwo(path);
  }

  public void doOne(Path path) throws Exception {
    String gcdmUrl = "gcdm://localhost:16111/" + path.toAbsolutePath();
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(path.toString(), null);
        GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {

      boolean ok = CompareArrayToMa2.compareFiles(ncfile, gcdmFile);
      assertThat(ok).isTrue();
    }
  }

  public void doTwo(Path path) throws Exception {
    String gcdmUrl = "gcdm://localhost:16111/" + path.toAbsolutePath();
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(path.toString(), null);
        GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {

      boolean ok = CompareArrayToArray.compareFiles(ncfile, gcdmFile);
      assertThat(ok).isTrue();
    }
  }

  public void doOne(Path path, String varName) throws Exception {
    // LOOK kludge for now. Also, need to auto start up CmdrServer
    String gcdmUrl = "gcdm://localhost:16111/" + path.toAbsolutePath();
    try (NetcdfFile ma2File = NetcdfDatasets.openFile(path.toString(), null);
        GcdmNetcdfFile arrayFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {

      boolean ok = CompareArrayToMa2.compareVariable(ma2File, arrayFile, varName, true);
      assertThat(ok).isTrue();
    }
  }

}
