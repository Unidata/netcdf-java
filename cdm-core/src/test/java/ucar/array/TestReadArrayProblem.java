/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.array;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Compare reading netcdf with Array */
@Category(NeedsCdmUnitTest.class)
public class TestReadArrayProblem {

  @Test
  public void testProblem() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf3/longOffset.nc";
    TestReadArrayCompare.compareNetcdfDataset(filename);
  }

  @Test
  public void testH5nestedEnumByteOrder() throws IOException {
    // bug failing to set byteorder correctly on nested enums
    String filename = TestDir.cdmUnitTestDir + "formats/hdf5/samples/enumcmpnd.h5";
    TestReadArrayCompare.compareNetcdfDataset(filename);
  }

  // @Test
  public void testProblemEnhanceNestedVlen() throws IOException {
    // the ma2 version fails to convert sequence nested in a Structure.
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/vlen/cdm_sea_soundings.nc4";
    TestReadArrayCompare.compareNetcdfDataset(filename);
  }

  // @Test
  public void testProblemEnhanceNestedVlen2() throws IOException {
    // the ma2 version fails to convert sequence nested in a Structure.
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/vlen/IntTimSciSamp.nc";
    TestReadArrayCompare.compareNetcdfDataset(filename);
  }

  @Test
  public void testBufrProblem2() throws IOException {
    String filename = TestDir.cdmTestDataDir + "ucar/nc2/bufr/IUPT02_KBBY_281400_522246081.bufr.2018032814";
    TestReadSequenceCompare.compareSequence(filename);
    TestReadSequenceCompare.compareDataset(filename);
  }

  @Test
  public void testBufrProblem() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/DART2.bufr";
    TestReadSequenceCompare.compareSequence(filename);
  }

  @Test
  public void testBufrCompressedNestedStruct() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/problems/0-01-030_bitWidth-128.bufr";
    TestReadSequenceCompare.compareSequence(filename);
  }

  @Test
  public void testBufrProblemMixed() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/mixed/TimeIncr.bufr";
    TestReadSequenceCompare.compareSequence(filename);
  }

  @Test
  public void testGrib1() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/grib1/SST_Global_5x2p5deg_20071119_0000.grib1";
    TestReadArrayCompare.compareNetcdfFile(filename);
  }

  @Test
  public void testGrib2() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/ds.wdir.bin";
    TestReadArrayCompare.compareNetcdfFile(filename);
  }

  @Test
  public void testStructureNestedSequence() throws IOException {
    // problem is we are unwrapping scalar Vlens, different from ma2
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/vlen/IntTimSciSamp.nc";
    TestReadArrayCompare.compareNetcdfFile(filename);
  }

  @Test
  public void testOpaque() throws IOException {
    String filename = TestDir.cdmLocalTestDataDir + "hdf5/test_atomic_types.nc"; // opaque
    TestReadArrayCompare.compareNetcdfFile(filename);
  }

  @Test
  public void testHdf4() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/hdf4/TOVS_BROWSE_MONTHLY_AM_B861001.E861031_NF.HDF";
    TestReadArrayCompare.compareNetcdfFile(filename);
  }

  @Test
  public void testStructureWithChar() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/hdf5/support/cstr.h5";
    TestReadArrayCompare.compareNetcdfFile(filename);
  }

  @Test
  public void testStructure() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/hdf5/wrf/wrf_bdy_par.h5";
    TestReadArrayCompare.compareNetcdfFile(filename);
  }

  @Test
  public void testBufrUncompressed() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/MSSARG_00217064.bufr";
    TestReadSequenceCompare.compareSequence(filename);
  }

  @Test
  public void testBufrUncompressed2() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/5900.20030601.rass";
    TestReadSequenceCompare.compareSequence(filename);
  }

  @Test
  public void testBufrCompressed() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/WMO_v16_3-10-61.bufr";
    TestReadSequenceCompare.compareSequence(filename);
  }

  @Test
  public void testBufrUncompressedNested() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/test1.bufr";
    TestReadSequenceCompare.compareSequence(filename);
  }

  @Test
  public void testBufrCompressedNestedSequence() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/TimeIncr0.bufr";
    TestReadSequenceCompare.compareSequence(filename);
  }

  @Test
  @Ignore("needs fix vlen length")
  public void testNc4Vlen() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/vlen/cdm_sea_soundings.nc4";
    TestReadArrayCompare.compareNetcdfFile(filename);
  }

}

