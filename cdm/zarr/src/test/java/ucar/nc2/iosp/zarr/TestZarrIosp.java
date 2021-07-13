/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.zarr;

import org.junit.Test;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

import java.io.IOException;
import java.nio.ByteOrder;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test class for Zarr IOSP
 */
public class TestZarrIosp {

  // file names
  private static final String ZARR_FILENAME = "zarr_test_data.zarr/";
  private static final String COMPRESSED_ZARR_FILENAME = "zarr_test_data.zip";
  private static final String INVALID_ZARR_FILENAME = "zarr_invalid_data.zarr";

  // test store paths
  private static final String OBJECT_STORE_ZARR_URI = ZarrTestsCommon.S3_PREFIX + ZarrTestsCommon.AWS_BUCKET_NAME + "?"
      + ZARR_FILENAME + "#" + ZarrTestsCommon.S3_FRAGMENT;
  private static final String DIRECTORY_STORE_URI = ZarrTestsCommon.LOCAL_TEST_DATA_PATH + ZARR_FILENAME;
  private static final String ZIP_STORE_URI = ZarrTestsCommon.LOCAL_TEST_DATA_PATH + COMPRESSED_ZARR_FILENAME;

  // invalid zarr file
  private static final String INVALID_ZARR_DATA = ZarrTestsCommon.LOCAL_TEST_DATA_PATH + INVALID_ZARR_FILENAME;

  @Test
  public void testIsValidFile() throws IOException {
    ZarrIosp iosp = new ZarrIosp();
    // zarr stores
    assertThat(iosp.isValidFile(NetcdfFiles.getRaf(DIRECTORY_STORE_URI, -1))).isTrue();
    assertThat(iosp.isValidFile(NetcdfFiles.getRaf(ZIP_STORE_URI, -1))).isTrue();
    assertThat(iosp.isValidFile(NetcdfFiles.getRaf(OBJECT_STORE_ZARR_URI, -1))).isTrue();
    // not zarr stores
    assertThat(iosp.isValidFile(NetcdfFiles.getRaf(DIRECTORY_STORE_URI + "/.zgroup", -1))).isFalse();
  }

  @Test
  public void testBuildNcfile() throws IOException {
    _testBuildNcfile(DIRECTORY_STORE_URI);
    _testBuildNcfile(ZIP_STORE_URI);
    _testBuildNcfile(OBJECT_STORE_ZARR_URI);
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
    // TODO: update when compressors/filters are implemented
    assertThat(vinfo.getCompressor()).isNull();
    assertThat(vinfo.getFilters()).isNull();

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
  }

}
