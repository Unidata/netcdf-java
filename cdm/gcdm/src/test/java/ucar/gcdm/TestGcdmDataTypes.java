/*
 * Copyright (c) 1998-2023 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import static com.google.common.truth.Truth.assertThat;
import static ucar.gcdm.TestUtils.*;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.gcdm.client.GcdmNetcdfFile;
import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;
import ucar.nc2.Sequence;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

public class TestGcdmDataTypes {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testAttributeStruct() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/netcdf4/attributeStruct.nc";
    compareVariables(localFilename, "observations");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testEnumProblem() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/netcdf4/tst/tst_enums.nc";
    compareFiles(localFilename);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCharProblem() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/test1.bufr";
    compareFiles(localFilename);
  }

  @Test
  public void testOpaqueDataType() throws Exception {
    String localFilename = TestDir.cdmLocalTestDataDir + "hdf5/test_atomic_types.nc";
    compareFiles(localFilename);
  }

  @Ignore("This variable is too large and causes an out of memory exception unrelated to gcdm.")
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCombineStructure() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf5/IASI/IASI.h5";
    compareVariables(localFilename, "U-MARF/EPS/IASI_xxx_1C/DATA/MDR_1C_IASI_L1_ARRAY_000001");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCombineStructureMember() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf5/IASI/IASI.h5";
    compareVariables(localFilename, "U-MARF/EPS/IASI_xxx_1C/DATA/MDR_1C_IASI_L1_ARRAY_000001.GCcsRadAnalStd-value");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testGcdmVlenCast() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/netcdf4/files/tst_opaque_data.nc4";
    compareFiles(localFilename);
  }

  @Test
  public void testGcdmVlenProblem() throws Exception {
    String localFilename = TestDir.cdmLocalTestDataDir + "dataset/SimpleGeos/hru_soil_moist_vlen_3hru_5timestep.nc";
    compareFiles(localFilename);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testSequenceData() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/test1.bufr";
    String gcdmUrl = gcdmPrefix + filename;

    try (GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {
      Sequence sequence = (Sequence) gcdmFile.findVariable("obs");
      assertThat((Object) sequence).isNotNull();
      assertThat(sequence.getNumberOfMemberVariables()).isEqualTo(78);

      Variable variable = sequence.findVariable("Station_or_site_name");
      assertThat((Object) variable).isNotNull();
      assertThat(variable.getSize()).isEqualTo(20);
      assertThat(variable.getDataType()).isEqualTo(DataType.CHAR);

      ArrayChar data = (ArrayChar) variable.read();
      assertThat(data.getString(0)).isEqualTo("WOLLOGORANG        ");
    }
  }
}
