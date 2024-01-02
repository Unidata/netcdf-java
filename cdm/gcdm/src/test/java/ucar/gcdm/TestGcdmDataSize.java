/*
 * Copyright (c) 1998-2023 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import static ucar.gcdm.TestUtils.*;

import java.lang.invoke.MethodHandles;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

public class TestGcdmDataSize {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testRequestTooBig() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf";
    compareVariables(localFilename, "MODIS_SWATH_Type_L1B/Data_Fields/EV_1KM_RefSB");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testRequestNotTooBig() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf";
    compareVariables(localFilename, "MODIS_SWATH_Type_L1B/Data_Fields/EV_500_Aggr1km_RefSB_Samples_Used");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testLargeArray() throws Exception {
    String localFilename =
        TestDir.cdmUnitTestDir + "formats/netcdf4/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4";
    compareVariables(localFilename, "O3");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testDataTooLarge() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/netcdf4/UpperDeschutes_t4p10_swemelt.nc";
    compareFiles(localFilename);
  }
}
