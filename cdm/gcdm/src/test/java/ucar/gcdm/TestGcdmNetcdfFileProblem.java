/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import static com.google.common.truth.Truth.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.gcdm.client.GcdmNetcdfFile;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Test {@link GcdmNetcdfFile} problems */
public class TestGcdmNetcdfFileProblem {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String gcdmPrefix = "gcdm://localhost:16111/";

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testRequestTooBig() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path, "MODIS_SWATH_Type_L1B/Data_Fields/EV_1KM_RefSB");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testRequestNotTooBig() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path, "MODIS_SWATH_Type_L1B/Data_Fields/EV_500_Aggr1km_RefSB_Samples_Used");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testHdf4() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path, "MODIS_SWATH_Type_L1B/Data_Fields/EV_250_Aggr1km_RefSB");
  }

  @Ignore("This variable is too large and causes an out of memory exception unrelated to gcdm.")
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCombineStructure() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf5/IASI/IASI.h5";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path, "U-MARF/EPS/IASI_xxx_1C/DATA/MDR_1C_IASI_L1_ARRAY_000001");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCombineStructureMember() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf5/IASI/IASI.h5";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path, "U-MARF/EPS/IASI_xxx_1C/DATA/MDR_1C_IASI_L1_ARRAY_000001.GCcsRadAnalStd-value");
  }

  @Test
  public void testReadSection() throws Exception {
    String localFilename = "../../dap4/d4tests/src/test/data/resources/testfiles/test_atomic_array.nc";
    String varName = "v16";
    String gcdmUrl = gcdmPrefix + Paths.get(localFilename).toAbsolutePath();
    try (GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {
      Variable gcdmVar = gcdmFile.findVariable(varName);
      assertThat((Object) gcdmVar).isNotNull();

      Array array = gcdmVar.read();
      assertThat(array.getSize()).isEqualTo(4);

      Array subset1 = gcdmVar.read("0:1");
      assertThat(subset1.getSize()).isEqualTo(2);
      assertThat(subset1.getShort(0)).isEqualTo(array.getShort(0));
      assertThat(subset1.getShort(1)).isEqualTo(array.getShort(1));

      Array subset2 = gcdmVar.read("2:3");
      assertThat(subset2.getSize()).isEqualTo(2);
      assertThat(subset2.getShort(0)).isEqualTo(array.getShort(2));
      assertThat(subset2.getShort(1)).isEqualTo(array.getShort(3));
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testReadSectionOfArrayStructure() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf5/IASI/IASI.h5";
    String varName = "U-MARF/EPS/IASI_xxx_1C/DATA/MDR_1C_IASI_L1_ARRAY_000001";
    String gcdmUrl = gcdmPrefix + Paths.get(localFilename).toAbsolutePath();
    try (GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {
      Variable gcdmVar = gcdmFile.findVariable(varName);
      assertThat((Object) gcdmVar).isNotNull();

      assertThat(gcdmVar.read().getSize()).isEqualTo(720);
      assertThat(gcdmVar.read("0:130").getSize()).isEqualTo(131);
      assertThat(gcdmVar.read("718:719").getSize()).isEqualTo(2);
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testGcdmVlenCast() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/netcdf4/files/tst_opaque_data.nc4";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testGcdmProblemNeeds() throws Exception {
    String localFilename =
        TestDir.cdmUnitTestDir + "formats/netcdf4/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4";
    Path path = Paths.get(localFilename);
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
    compareArrayToArray(path, "observations");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testEnumProblem() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/netcdf4/tst/tst_enums.nc";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCharProblem() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/test1.bufr";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testChunkProblem() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/netcdf4/multiDimscale.nc4";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path);
  }

  @Test
  public void testOpaqueDataType() throws Exception {
    String localFilename = TestDir.cdmLocalTestDataDir + "hdf5/test_atomic_types.nc";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path);
  }

  @Test
  public void testGcdmProblem2() throws Exception {
    String localFilename = TestDir.cdmLocalTestDataDir + "dataset/SimpleGeos/hru_soil_moist_vlen_3hru_5timestep.nc";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path);
  }

  private static void compareArrayToArray(Path path) throws Exception {
    String gcdmUrl = gcdmPrefix + path.toAbsolutePath();
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(path.toString(), null);
        GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {

      Formatter formatter = new Formatter();
      boolean ok = CompareNetcdf2.compareFiles(ncfile, gcdmFile, formatter, true, false, true);
      logger.debug(formatter.toString());
      assertThat(ok).isTrue();
    }
  }

  private static void compareArrayToArray(Path path, String varName) throws Exception {
    String gcdmUrl = gcdmPrefix + path.toAbsolutePath();
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(path.toString(), null);
        GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {

      Variable ncVar = ncfile.findVariable(varName);
      Variable gcdmVar = gcdmFile.findVariable(varName);

      Formatter formatter = new Formatter();
      CompareNetcdf2 compareNetcdf2 = new CompareNetcdf2(formatter, false, true, true);
      boolean ok = compareNetcdf2.compareVariable(ncVar, gcdmVar, CompareNetcdf2.IDENTITY_FILTER);
      logger.debug(formatter.toString());
      assertThat(ok).isTrue();
    }
  }
}
