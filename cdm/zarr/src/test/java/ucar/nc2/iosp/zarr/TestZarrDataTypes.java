/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.zarr;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class TestZarrDataTypes {
  private static final Logger logger = LoggerFactory.getLogger(TestZarrDataTypes.class);

  private static final String FILENAME = ZarrTestsCommon.LOCAL_TEST_DATA_PATH + "test_dtypes.zarr";

  // big endian variable names
  private static final String BE_DOUBLE = "/byte_ordered_group/big_endian/double_data";
  private static final String BE_FLOAT = "/byte_ordered_group/big_endian/float_data";
  private static final String BE_INT = "/byte_ordered_group/big_endian/int_data";
  private static final String BE_LONG = "/byte_ordered_group/big_endian/long_data";
  private static final String BE_SHORT = "/byte_ordered_group/big_endian/short_data";
  private static final String BE_UINT = "/byte_ordered_group/big_endian/uint_data";
  private static final String BE_ULONG = "/byte_ordered_group/big_endian/ulong_data";
  private static final String BE_USHORT = "/byte_ordered_group/big_endian/ushort_data";

  // little endian variable names
  private static final String LE_DOUBLE = "/byte_ordered_group/little_endian/double_data";
  private static final String LE_FLOAT = "/byte_ordered_group/little_endian/float_data";
  private static final String LE_INT = "/byte_ordered_group/little_endian/int_data";
  private static final String LE_LONG = "/byte_ordered_group/little_endian/long_data";
  private static final String LE_SHORT = "/byte_ordered_group/little_endian/short_data";
  private static final String LE_UINT = "/byte_ordered_group/little_endian/uint_data";
  private static final String LE_ULONG = "/byte_ordered_group/little_endian/ulong_data";
  private static final String LE_USHORT = "/byte_ordered_group/little_endian/ushort_data";

  // unordered variable names
  private static final String BOOLEAN = "/unordered_group/boolean_data";
  private static final String BYTE = "/unordered_group/byte_data";
  private static final String UBYTE = "/unordered_group/ubyte_data";

  // string variable names
  private static final String CHAR = "/string_types/char_data";
  private static final String STRING = "/string_types/str_data";
  private static final String UNICODE = "/string_types/unicode_data";

  private static NetcdfFile ncfile;

  @BeforeClass
  public static void setUpTests() throws IOException {
    ncfile = NetcdfFiles.open(FILENAME);
  }

  @AfterClass
  public static void cleanUpTests() throws IOException {
    ncfile.close();
  }

  @Test
  public void testBigEndianDataTypeMapping() {
    Variable var = ncfile.findVariable(BE_DOUBLE);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.DOUBLE);
    var = ncfile.findVariable(BE_FLOAT);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.FLOAT);
    var = ncfile.findVariable(BE_INT);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.INT);
    var = ncfile.findVariable(BE_LONG);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.LONG);
    var = ncfile.findVariable(BE_SHORT);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.SHORT);
    var = ncfile.findVariable(BE_UINT);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.UINT);
    var = ncfile.findVariable(BE_ULONG);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.ULONG);
    var = ncfile.findVariable(BE_USHORT);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.USHORT);
  }

  @Test
  public void testLittleEndianDataTypeMapping() {
    Variable var = ncfile.findVariable(LE_DOUBLE);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.DOUBLE);
    var = ncfile.findVariable(LE_FLOAT);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.FLOAT);
    var = ncfile.findVariable(LE_INT);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.INT);
    var = ncfile.findVariable(LE_LONG);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.LONG);
    var = ncfile.findVariable(LE_SHORT);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.SHORT);
    var = ncfile.findVariable(LE_UINT);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.UINT);
    var = ncfile.findVariable(LE_ULONG);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.ULONG);
    var = ncfile.findVariable(LE_USHORT);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.USHORT);
  }

  @Test
  public void testUnorderedDataTypeMapping() {
    Variable var = ncfile.findVariable(BOOLEAN);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.BOOLEAN);
    var = ncfile.findVariable(BYTE);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.BYTE);
    var = ncfile.findVariable(UBYTE);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.UBYTE);
  }

  @Test
  public void testStringDataTypeMapping() {
    Variable var = ncfile.findVariable(CHAR);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.CHAR);
    var = ncfile.findVariable(STRING);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.STRING);
    var = ncfile.findVariable(UNICODE);
    assertThat((Object) var).isNotNull();
    assertThat(var.getDataType()).isEqualTo(DataType.STRING);
  }
}
