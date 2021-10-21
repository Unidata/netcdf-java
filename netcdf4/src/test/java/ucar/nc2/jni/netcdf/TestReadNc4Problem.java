/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.jni.netcdf;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import static org.junit.Assert.assertThrows;

/** Compare reading netcdf4 with old ma2.Array and new array.Array IOSPs for problem datasets. */
@Category(NeedsCdmUnitTest.class)
public class TestReadNc4Problem {

  @Test
  public void testVlenInt() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/vlen/vlenInt.nc";
    TestReadNc4Compare.compareMa2Array(filename);
  }

  @Test
  public void testVlenHeap() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/vlen/IntTimSciSamp.nc";
    TestReadNc4Compare.compareMa2Array(filename);
  }

  @Test
  public void testEnum() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/tst/test_enum_type.nc";
    TestReadNc4Compare.compareMa2Array(filename);
  }

  @Test
  public void testTooBig() {
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/UpperDeschutes_t4p10_swemelt.nc";
    assertThrows(IllegalArgumentException.class, () -> TestReadNc4Compare.compareMa2Array(filename));
  }

  @Test
  public void testOpaque() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/files/tst_opaque_data.nc4";
    TestReadNc4Compare.compareMa2Array(filename);
  }

  @Test
  public void testCompoundAtrString() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/compound-attribute-test.nc";
    TestReadNc4Compare.compareMa2Array(filename);
  }

  @Test
  public void testCompound() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/attributeStruct.nc";
    TestReadNc4Compare.compareMa2Array(filename);
  }

  @Test
  public void testVlen() throws Exception {
    String filename = TestDir.cdmUnitTestDir
        + "formats/netcdf4/SGA1-RO_-00-SRC_C_EUMT_20200704124757_G_O_20200704124200_20200704124459_O_N____.nc";
    TestReadNc4Compare.compareMa2Array(filename);
  }

}

