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

/** Test {@link GcdmNetcdfFile} problems */
public class TestGcdmNetcdfFileProblem {

  // @Test
  @Category(NeedsCdmUnitTest.class)
  public void testHdf4() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf";
    Path path = Paths.get(localFilename);
    compareArrayToMa2(path);
    compareArrayToMa2(path, "MODIS_SWATH_Type_L1B/Data_Fields/EV_250_Aggr1km_RefSB");
    compareArrayToArray(path, "MODIS_SWATH_Type_L1B/Data_Fields/EV_250_Aggr1km_RefSB");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCombineStructure() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf5/IASI/IASI.h5";
    Path path = Paths.get(localFilename);
    compareArrayToMa2(path, "U-MARF/EPS/IASI_xxx_1C/DATA/MDR_1C_IASI_L1_ARRAY_000001");
    compareArrayToArray(path, "U-MARF/EPS/IASI_xxx_1C/DATA/MDR_1C_IASI_L1_ARRAY_000001");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testGcdmVlenCast() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/netcdf4/files/tst_opaque_data.nc4";
    Path path = Paths.get(localFilename);
    compareArrayToMa2(path);
    compareArrayToArray(path);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testGcdmProblemNeeds() throws Exception {
    String localFilename =
        TestDir.cdmUnitTestDir + "formats/netcdf4/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4";
    Path path = Paths.get(localFilename);
    compareArrayToMa2(path, "T");
    compareArrayToArray(path, "O3");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testDataTooLarge() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/netcdf4/UpperDeschutes_t4p10_swemelt.nc";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testAttributeStruct() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/netcdf4/attributeStruct.nc";
    Path path = Paths.get(localFilename);
    compareArrayToMa2(path);
    compareArrayToArray(path, "observations");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testEnumProblem() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/netcdf4/tst/tst_enums.nc";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path);
  }


  // media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/bufr/userExamples/test1.bufr
  // media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/bufr/userExamples/test1.bufr
  // media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/multiDimscale.nc4

  // char variables from BUFR are incorrect
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCharProblem() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/test1.bufr";
    Path path = Paths.get(localFilename);
    compareArrayToMa2(path, "obs");
    compareArrayToArray(path);
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
    compareArrayToMa2(path, "u");
    compareArrayToArray(path);
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
    compareArrayToMa2(path);
    compareArrayToArray(path);
  }

  @Test
  public void testGcdmProblem2() throws Exception {
    String localFilename = TestDir.cdmLocalTestDataDir + "dataset/SimpleGeos/hru_soil_moist_vlen_3hru_5timestep.nc";
    Path path = Paths.get(localFilename);
    compareArrayToMa2(path);
    compareArrayToArray(path);
  }

  ////////////////////////////////////////////////////////////////////////////

  public void compareArrayToMa2(Path path) throws Exception {
    String gcdmUrl = "gcdm://localhost:16111/" + path.toAbsolutePath();
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(path.toString(), null);
        GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {

      boolean ok = CompareArrayToMa2.compareFiles(ncfile, gcdmFile);
      assertThat(ok).isTrue();
    }
  }

  public void compareArrayToMa2(Path path, String varName) throws Exception {
    // LOOK kludge for now. Also, need to auto start up CmdrServer
    String gcdmUrl = "gcdm://localhost:16111/" + path.toAbsolutePath();
    try (NetcdfFile ma2File = NetcdfDatasets.openFile(path.toString(), null);
        GcdmNetcdfFile arrayFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {

      boolean ok = CompareArrayToMa2.compareVariable(ma2File, arrayFile, varName, true);
      assertThat(ok).isTrue();
    }
  }

  public void compareArrayToArray(Path path) throws Exception {
    String gcdmUrl = "gcdm://localhost:16111/" + path.toAbsolutePath();
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(path.toString(), null);
        GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {

      boolean ok = CompareArrayToArray.compareFiles(ncfile, gcdmFile);
      assertThat(ok).isTrue();
    }
  }


  public void compareArrayToArray(Path path, String varName) throws Exception {
    String gcdmUrl = "gcdm://localhost:16111/" + path.toAbsolutePath();
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(path.toString(), null);
        GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {

      boolean ok = CompareArrayToArray.compareVariable(ncfile, gcdmFile, varName, true);
      assertThat(ok).isTrue();
    }
  }

}
