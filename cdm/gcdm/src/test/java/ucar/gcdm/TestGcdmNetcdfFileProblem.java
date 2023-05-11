/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import static com.google.common.truth.Truth.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.gcdm.client.GcdmNetcdfFile;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Test {@link GcdmNetcdfFile} problems */
public class TestGcdmNetcdfFileProblem {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String gcdmPrefix = "gcdm://localhost:16111/";

  /*
   * ushort EV_1KM_RefSB(Band_1KM_RefSB=15, 10*nscans=2030, Max_EV_frames=1354);
   * 
   * GcdmServer getData
   * /media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/hdf4/MOD021KM.A2004328.
   * 1735.004.2004329164007.hdf
   * MODIS_SWATH_Type_L1B/Data_Fields/EV_1KM_RefSB(0:14, 0:2029, 0:1353)
   * Send one chunk MODIS_SWATH_Type_L1B/Data_Fields/EV_1KM_RefSB(0:8, 0:2029, 0:1353) size=49475160 bytes
   * Send one chunk MODIS_SWATH_Type_L1B/Data_Fields/EV_1KM_RefSB(9:14, 0:2029, 0:1353) size=32983440 bytes
   ** size=82,458,600 took=1.461 s
   * 
   * WARNING: readSection requestData failed failed:
   * io.grpc.StatusRuntimeException: RESOURCE_EXHAUSTED: gRPC message exceeds maximum size 51000000: 99265809
   * at io.grpc.Status.asRuntimeException(Status.java:535)
   * at io.grpc.stub.ClientCalls$BlockingResponseStream.hasNext(ClientCalls.java:648)
   * at ucar.gcdm.client.GcdmNetcdfFile.readArrayData(GcdmNetcdfFile.java:85)
   * at ucar.nc2.Variable.proxyReadArray(Variable.java:829)
   * at ucar.nc2.Variable.readArray(Variable.java:738)
   * ...
   * 
   * LOOK what is 99265809 here? This implies my calculation of the data size is seriously wrong.
   * Temp fix is to put MAX = 101 Mbytes.
   * Confirmed this is an artifact of unsigned short, which doesnt have a direct protobug type, so we use uint32.
   * Ratios are sometimes ~2. see GcdmConverter.debugSize.
   * GcdmNetcdfProto.Data nelems = 24737580 type=ushort expected size =49475160 actual = 99265523 ratio = 2.006371
   * 
   */
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

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCombineStructure() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf5/IASI/IASI.h5";
    Path path = Paths.get(localFilename);
    compareArrayToArray(path, "U-MARF/EPS/IASI_xxx_1C/DATA/MDR_1C_IASI_L1_ARRAY_000001");
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


  // media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/bufr/userExamples/test1.bufr
  // media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/bufr/userExamples/test1.bufr
  // media/snake/0B681ADF0B681ADF/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/multiDimscale.nc4

  // char variables from BUFR are incorrect
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCharProblem() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/test1.bufr";
    Path path = Paths.get(localFilename);
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

  ////////////////////////////////////////////////////////////////////////////

  public void compareArrayToArray(Path path) throws Exception {
    String gcdmUrl = gcdmPrefix + path.toAbsolutePath();
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(path.toString(), null);
        GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {

      Formatter formatter = new Formatter();
      boolean ok = CompareNetcdf2.compareFiles(ncfile, gcdmFile, formatter, true, false, true);
      logger.debug(formatter.toString());
      assertThat(ok).isTrue();
    }
  }


  public void compareArrayToArray(Path path, String varName) throws Exception {
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
