/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import java.io.IOException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import static com.google.common.truth.Truth.assertThat;

/** Compare reading netcdf with Array */
@Category(NeedsCdmUnitTest.class)
public class TestReadArrayProblem {

  @Test
  public void testNc4Vlen2() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/vlen/fpcs_1dwave_2.nc"; // srate
    assertThat(CompareArrayToArray.compareNetcdfDataset(filename)).isTrue();
  }

  @Test
  public void testHdf4SpecialChunked() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/hdf4/MYD29.A2009152.0000.005.2009153124331.hdf";
    assertThat(CompareArrayToArray.compareNetcdfDataset(filename)).isTrue();
  }

  @Test
  public void testProblem() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf3/longOffset.nc";
    assertThat(CompareArrayToArray.compareNetcdfDataset(filename)).isTrue();
  }

  @Test
  public void testH5nestedEnumByteOrder() throws IOException {
    // bug failing to set byteorder correctly on nested enums
    String filename = TestDir.cdmUnitTestDir + "formats/hdf5/samples/enumcmpnd.h5";
    assertThat(CompareArrayToArray.compareNetcdfDataset(filename)).isTrue();
  }

  @Test
  public void testProblemEnhanceNestedVlen() throws IOException {
    // the ma2 version fails to convert sequence nested in a Structure.
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/vlen/cdm_sea_soundings.nc4";
    assertThat(CompareArrayToArray.compareNetcdfDataset(filename)).isTrue();
  }

  @Test
  public void testProblemEnhanceNestedVlen2() throws IOException {
    // the ma2 version fails to convert sequence nested in a Structure.
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/vlen/IntTimSciSamp.nc";
    assertThat(CompareArrayToArray.compareNetcdfDataset(filename)).isTrue();
  }

  @Test
  public void testBufrProblem2() throws IOException {
    String filename = TestDir.cdmTestDataDir + "ucar/nc2/bufr/IUPT02_KBBY_281400_522246081.bufr.2018032814";
    CompareArrayToArray.compareSequence(filename);
    assertThat(CompareArrayToArray.compareNetcdfDataset(filename)).isTrue();
  }

  @Test
  public void testBufrProblem() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/DART2.bufr";
    assertThat(CompareArrayToArray.compareSequence(filename)).isTrue();
  }

  @Test
  public void testBufrCompressedNestedStruct() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/problems/0-01-030_bitWidth-128.bufr";
    assertThat(CompareArrayToArray.compareSequence(filename)).isTrue();
  }

  @Test
  public void testBufrProblemMixed() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/mixed/TimeIncr.bufr";
    assertThat(CompareArrayToArray.compareSequence(filename)).isTrue();
  }

  @Test
  public void testGrib1() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/grib1/SST_Global_5x2p5deg_20071119_0000.grib1";
    assertThat(CompareArrayToArray.compareNetcdfFile(filename)).isTrue();
  }

  @Test
  public void testGrib2() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/ds.wdir.bin";
    assertThat(CompareArrayToArray.compareNetcdfFile(filename)).isTrue();
  }

  @Test
  public void testStructureNestedSequence() throws IOException {
    // problem is we are unwrapping scalar Vlens, different from ma2
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/vlen/IntTimSciSamp.nc";
    assertThat(CompareArrayToArray.compareNetcdfFile(filename)).isTrue();
  }

  @Test
  public void testOpaque() throws IOException {
    String filename = TestDir.cdmLocalTestDataDir + "hdf5/test_atomic_types.nc"; // opaque
    assertThat(CompareArrayToArray.compareNetcdfFile(filename)).isTrue();
  }

  @Test
  public void testHdf4() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/hdf4/TOVS_BROWSE_MONTHLY_AM_B861001.E861031_NF.HDF";
    assertThat(CompareArrayToArray.compareNetcdfFile(filename)).isTrue();
  }

  @Test
  public void testStructureWithChar() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/hdf5/support/cstr.h5";
    assertThat(CompareArrayToArray.compareNetcdfFile(filename)).isTrue();
  }

  @Test
  public void testStructure() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/hdf5/wrf/wrf_bdy_par.h5";
    assertThat(CompareArrayToArray.compareNetcdfFile(filename)).isTrue();
  }

  @Test
  public void testBufrUncompressed() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/MSSARG_00217064.bufr";
    assertThat(CompareArrayToArray.compareSequence(filename)).isTrue();
  }

  @Test
  public void testBufrUncompressed2() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/5900.20030601.rass";
    assertThat(CompareArrayToArray.compareSequence(filename)).isTrue();
  }

  @Test
  public void testBufrCompressed() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/WMO_v16_3-10-61.bufr";
    assertThat(CompareArrayToArray.compareSequence(filename)).isTrue();
  }

  @Test
  public void testBufrUncompressedNested() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/test1.bufr";
    assertThat(CompareArrayToArray.compareSequence(filename)).isTrue();
  }

  @Test
  public void testBufrCompressedNestedSequence() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/TimeIncr0.bufr";
    assertThat(CompareArrayToArray.compareSequence(filename)).isTrue();
  }

  @Test
  public void testNc4Vlen() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/vlen/cdm_sea_soundings.nc4";
    assertThat(CompareArrayToArray.compareNetcdfFile(filename)).isTrue();
  }

  @Test
  public void testNc4AttributeVlen() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/files/tst_opaque_data.nc4";
    assertThat(CompareArrayToArray.compareNetcdfFile(filename)).isTrue();
  }

  @Test
  public void testNc4() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/tst/c0_4.nc4";
    assertThat(CompareArrayToArray.compareNetcdfFile(filename)).isTrue();
  }

}

