/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.zarr;

import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.filter.Filters;

import java.io.IOException;
import java.nio.ByteOrder;
import java.util.*;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test class for Zarr IOSP
 */
public class TestZarrIosp {

  // file names
  private static final String ZARR_FILENAME = "zarr_test_data.zarr/";
  private static final String ZARR_ZIP_NAME = "zarr_test_data.zip";
  private static final String INVALID_ZARR_FILENAME = "zarr_invalid_data.zarr";
  private static final String NON_ZARR_FILENAME = "nonZarrTestData.nc.zip";
  private static final String FILL_VALUES_FILENAME = "fill_values.zarr";

  // test store paths
  private static final String OBJECT_STORE_ZARR_URI = ZarrTestsCommon.S3_PREFIX + ZarrTestsCommon.AWS_BUCKET_NAME + "?"
      + ZARR_FILENAME + "#" + ZarrTestsCommon.S3_FRAGMENT;
  private static final String DIRECTORY_STORE_URI = ZarrTestsCommon.LOCAL_TEST_DATA_PATH + ZARR_FILENAME;
  private static final String ZIP_STORE_URI = ZarrTestsCommon.LOCAL_TEST_DATA_PATH + ZARR_ZIP_NAME;

  // invalid zarr file
  private static final String INVALID_ZARR_DATA = ZarrTestsCommon.LOCAL_TEST_DATA_PATH + INVALID_ZARR_FILENAME;

  // Non zarr zipped file
  private static final String NON_ZARR_DATA = ZarrTestsCommon.LOCAL_TEST_DATA_PATH + NON_ZARR_FILENAME;

  // fill values file
  private static final String FILL_VALUES_DATA = ZarrTestsCommon.LOCAL_TEST_DATA_PATH + FILL_VALUES_FILENAME;

  private static List<String> stores;

  @BeforeClass
  public static void setUpTests() {
    stores = new ArrayList<>();
    stores.add(DIRECTORY_STORE_URI);
    stores.add(ZIP_STORE_URI);
    stores.add(OBJECT_STORE_ZARR_URI);
  }

  @Test
  public void testIsValidFile() throws IOException {
    ZarrIosp iosp = new ZarrIosp();
    for (String uri : stores) {
      assertThat(iosp.isValidFile(NetcdfFiles.getRaf(uri, -1))).isTrue();
    }
    assertThat(iosp.isValidFile(NetcdfFiles.getRaf(DIRECTORY_STORE_URI + "/.zgroup", -1))).isFalse();
  }

  @Test
  public void testNonZarrZipIsNotValid() throws IOException {
    ZarrIosp iosp = new ZarrIosp();
    assertThat(iosp.isValidFile(NetcdfFiles.getRaf(NON_ZARR_DATA, -1))).isFalse();
  }

  //////////////////////////////////////////////////////
  // test open and build ncfile from Zarr files

  @Test
  public void testBuildNcfile() throws IOException {
    for (String uri : stores) {
      _testBuildNcfile(uri);
    }
  }

  private void _testBuildNcfile(String location) throws IOException {
    NetcdfFile ncfile = NetcdfFiles.open(location);

    // check for groups
    Group attrs_grp = ncfile.findGroup("group_with_attrs");
    assertThat(attrs_grp).isNotNull();
    Group dims_grp = ncfile.findGroup("group_with_dims");
    assertThat(dims_grp).isNotNull();

    // check group attributes
    assertThat(attrs_grp.hasAttribute("group_attr")).isTrue();
    assertThat(attrs_grp.findAttribute("group_attr").getStringValue()).isEqualTo("foo");

    // check for variables
    Variable F_order = attrs_grp.findVariableLocal("F_order_array");
    assertThat((Object) F_order).isNotNull();
    Variable nested = attrs_grp.findVariableLocal("nested");
    assertThat((Object) nested).isNotNull();
    Variable partial_fill1 = attrs_grp.findVariableLocal("partial_fill1");
    assertThat((Object) partial_fill1).isNotNull();
    Variable partial_fill2 = attrs_grp.findVariableLocal("partial_fill2");
    assertThat((Object) partial_fill2).isNotNull();
    Variable uninitialized = attrs_grp.findVariableLocal("uninitialized");
    assertThat((Object) uninitialized).isNotNull();

    // check array attributes
    assertThat(F_order.hasAttribute("bar")).isTrue();
    assertThat(F_order.findAttribute("bar").getStringValue()).isEqualTo("apples");
    assertThat(F_order.hasAttribute("baz")).isTrue();
    assertThat(F_order.findAttribute("baz").getLength()).isEqualTo(4);
    assertThat(F_order.findAttribute("baz").getNumericValue(2)).isEqualTo(3);
    assertThat(F_order.hasAttribute("foo")).isTrue();
    assertThat(F_order.findAttribute("foo").getNumericValue()).isEqualTo(42);

    // check VInfo
    ZarrHeader.VInfo vinfo = (ZarrHeader.VInfo) F_order.getSPobject();
    assertThat(vinfo.getChunks()).isEqualTo(new int[] {4, 5});
    assertThat(vinfo.getFillValue()).isEqualTo(0);
    assertThat(vinfo.getByteOrder()).isEqualTo(ByteOrder.LITTLE_ENDIAN);
    assertThat(vinfo.getOrder()).isEqualTo(ZArray.Order.F);
    assertThat(vinfo.getSeparator()).isEqualTo(ZArray.DEFAULT_SEPARATOR);

    // check variables and dimensions
    Variable var1D = dims_grp.findVariableLocal("var1D");
    assertThat((Object) var1D).isNotNull();
    assertThat(var1D.getDimensions().size()).isEqualTo(1);
    Variable var2D = dims_grp.findVariableLocal("var2D");
    assertThat((Object) var2D).isNotNull();
    assertThat(var2D.getDimensions().size()).isEqualTo(2);
    Variable var3D = dims_grp.findVariableLocal("var3D");
    assertThat((Object) var3D).isNotNull();
    assertThat(var3D.getDimensions().size()).isEqualTo(3);
    Variable var4D = dims_grp.findVariableLocal("var4D");
    assertThat((Object) var4D).isNotNull();
    assertThat(var4D.getDimensions().size()).isEqualTo(4);

    ncfile.close();
  }

  @Test
  public void testBuildInvalidFormat() throws IOException {
    NetcdfFile ncfile = NetcdfFiles.open(INVALID_ZARR_DATA);

    // check orphaned group is not added
    assertThat(ncfile.findGroup("orphaned_group")).isNull();

    // check orphaned variable is not added
    assertThat((Object) ncfile.findVariable("not_group/orphaned_array")).isNull();

    // check valid group does exist
    Group grp = ncfile.findGroup("valid_group");
    assertThat(grp).isNotNull();

    // check invalid vars are not added
    assertThat((Object) grp.findVariableLocal("bad_dtype")).isNull(); // .zarray with bad 'dtype'
    assertThat((Object) grp.findVariableLocal("bad_order")).isNull(); // .zarray with bad 'order'
    assertThat((Object) grp.findVariableLocal("missing_param")).isNull(); // .zarray missing 'shape'

    // check valid data is added
    assertThat((Object) grp.findVariableLocal("valid_data")).isNotNull();

    ncfile.close();
  }

  ////////////////////////////////////////////////////////
  // test reads for Zarr formats

  @Test
  public void testRead() throws IOException, InvalidRangeException {
    // test reading sections
    for (String uri : stores) {
      _testRead(uri);
    }
  }

  private void _testRead(String location) throws IOException, InvalidRangeException {
    NetcdfFile ncfile = NetcdfFiles.open(location);
    Array data;
    int[] expected;
    // 1D
    data = ncfile.findVariable("group_with_dims/var1D").read(new Section(new int[] {0}, new int[] {20}, new int[] {2}));
    expected = new int[] {0, 2, 4, 6, 8, 10, 12, 14, 16, 18};
    assertThat(data.get1DJavaArray(DataType.INT)).isEqualTo(expected);
    // 2D
    data = ncfile.findVariable("group_with_dims/var2D")
        .read(new Section(new int[] {15, 1}, new int[] {5, 10}, new int[] {2, 2}));
    expected = new int[] {1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9};
    assertThat(data.get1DJavaArray(DataType.INT)).isEqualTo(expected);
    // 3D
    data = ncfile.findVariable("group_with_dims/var3D")
        .read(new Section(new int[] {0, 15, 1}, new int[] {4, 5, 10}, new int[] {1, 2, 2}));
    expected = new int[] {1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1,
        3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9};
    assertThat(data.get1DJavaArray(DataType.INT)).isEqualTo(expected);
    // 4D
    data = ncfile.findVariable("group_with_dims/var4D")
        .read(new Section(new int[] {0, 0, 15, 1}, new int[] {20, 4, 5, 10}, new int[] {10, 1, 2, 2}));
    expected = new int[] {1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1,
        3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5,
        7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9,
        1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9};
    assertThat(data.get1DJavaArray(DataType.INT)).isEqualTo(expected);
    ncfile.close();
  }

  @Test
  public void testReadUninitialized() throws IOException, InvalidRangeException {
    for (String uri : stores) {
      _testUninitialized(uri);
    }
  }

  private void _testUninitialized(String location) throws IOException, InvalidRangeException {
    NetcdfFile ncfile = NetcdfFiles.open(location);

    Array data;
    int[] expected;
    int fill_value = 999;
    int assigned_value = 0;

    // fully uninitialized
    data = ncfile.findVariable("group_with_attrs/uninitialized").read();
    expected = new int[400];
    Arrays.fill(expected, fill_value);
    assertThat(data.get1DJavaArray(DataType.INT)).isEqualTo(expected);

    // partially uninitialized (one full block)
    data = ncfile.findVariable("group_with_attrs/partial_fill1").read();
    expected = new int[100];
    Arrays.fill(expected, assigned_value);
    assertThat(data.section(new int[] {0, 0}, new int[] {10, 10}).get1DJavaArray(DataType.INT)).isEqualTo(expected);
    expected = new int[100];
    Arrays.fill(expected, fill_value);
    assertThat(data.section(new int[] {0, 10}, new int[] {10, 10}).get1DJavaArray(DataType.INT)).isEqualTo(expected);
    assertThat(data.section(new int[] {10, 0}, new int[] {10, 10}).get1DJavaArray(DataType.INT)).isEqualTo(expected);
    assertThat(data.section(new int[] {10, 10}, new int[] {10, 10}).get1DJavaArray(DataType.INT)).isEqualTo(expected);

    // partially uninitialized (one partially full block)
    data = ncfile.findVariable("group_with_attrs/partial_fill2").read();
    expected = new int[50];
    Arrays.fill(expected, assigned_value);
    assertThat(data.section(new int[] {15, 10}, new int[] {5, 10}).get1DJavaArray(DataType.INT)).isEqualTo(expected);
    expected = new int[150];
    Arrays.fill(expected, fill_value);
    assertThat(data.section(new int[] {0, 0}, new int[] {15, 10}).get1DJavaArray(DataType.INT)).isEqualTo(expected);
  }

  @Test
  public void testReadNonZarrZipFile() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(NON_ZARR_DATA)) {
      assertThat(ncfile).isNotNull();
      assertThat(ncfile.findDimension("x")).isNotNull();
    }
  }

  @Test
  public void testFOrder() throws IOException, InvalidRangeException {
    // test reading F order stored array
    for (String uri : stores) {
      _testFOrder(uri);
    }
  }

  private void _testFOrder(String location) throws IOException, InvalidRangeException {
    NetcdfFile ncfile = NetcdfFiles.open(location);
    Array data;
    int[] expected;
    data = ncfile.findVariable("group_with_attrs/F_order_array")
        .read(new Section(new int[] {15, 1}, new int[] {5, 10}, new int[] {2, 2}));
    expected = new int[] {1, 3, 5, 7, 9, 1, 3, 5, 7, 9, 1, 3, 5, 7, 9};
    assertThat(data.get1DJavaArray(DataType.INT)).isEqualTo(expected);
  }

  @Test
  public void testNested() throws IOException {
    // test read nested store
    for (String uri : stores) {
      _testNested(uri);
    }
  }

  private void _testNested(String location) throws IOException {
    NetcdfFile ncfile = NetcdfFiles.open(location);

    Array data = ncfile.findVariable("group_with_attrs/nested").read();
    short[] expected = new short[400];
    Arrays.fill(expected, (short) 0);
    assertThat(data.get1DJavaArray(DataType.SHORT)).isEqualTo(expected);
  }

  @Test
  public void testFillValues() throws IOException {
    NetcdfFile ncfile = NetcdfFiles.open(FILL_VALUES_DATA);

    Array float_nan = ncfile.findVariable("float_nan").read();
    assertThat(float_nan.getFloat(0)).isEqualTo(Float.NaN);
    Array float_inf = ncfile.findVariable("float_inf").read();
    assertThat(float_inf.getFloat(0)).isEqualTo(Float.POSITIVE_INFINITY);
    Array float_ninf = ncfile.findVariable("float_ninf").read();
    assertThat(float_ninf.getFloat(0)).isEqualTo(Float.NEGATIVE_INFINITY);

    Array double_nan = ncfile.findVariable("float_nan").read();
    assertThat(double_nan.getDouble(0)).isEqualTo(Double.NaN);
    Array double_inf = ncfile.findVariable("float_inf").read();
    assertThat(double_inf.getDouble(0)).isEqualTo(Double.POSITIVE_INFINITY);
    Array double_ninf = ncfile.findVariable("float_ninf").read();
    assertThat(double_ninf.getDouble(0)).isEqualTo(Double.NEGATIVE_INFINITY);
  }

  @Test
  public void testLastModified() throws IOException {
    for (String uri : stores) {
      try (NetcdfFile ncfile = NetcdfFiles.open(uri)) {
        assertThat(ncfile.getLastModified()).isNotEqualTo(0);
      }
    }
  }
}
