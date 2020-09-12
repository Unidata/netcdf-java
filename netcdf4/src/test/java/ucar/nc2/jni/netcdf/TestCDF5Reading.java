/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.jni.netcdf;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsContentRoot;
import java.io.IOException;

/** Test Reading of CDF-5 files using JNI netcdf-4 iosp */
@Category(NeedsContentRoot.class)
public class TestCDF5Reading {
  private static final ArrayFloat.D1 BASELINE;

  static {
    BASELINE = new ArrayFloat.D1(3);
    BASELINE.set(0, -3.4028235E38F);
    BASELINE.set(1, 3.4028235E38F);
    BASELINE.set(2, Float.NEGATIVE_INFINITY);
  }

  @Before
  public void setLibrary() {
    // Ignore this class's tests if NetCDF-4 isn't present.
    // We're using @Before because it shows these tests as being ignored.
    // @BeforeClass shows them as *non-existent*, which is not what we want.
    Assume.assumeTrue("NetCDF-4 C library not present.", NetcdfClibrary.isLibraryPresent());
  }

  @Test
  public void testReadSubsection() throws IOException, InvalidRangeException {
    String location = TestDir.cdmTestDataDir + "thredds/public/testdata/nc_test_cdf5.nc";
    try (RandomAccessFile raf = RandomAccessFile.acquire(location)) {
      // Verify that this is a netcdf-5 file
      NetcdfFileFormat format = NetcdfFileFormat.findNetcdfFormatType(raf);
      assertWithMessage("Fail: file format is not CDF-5").that(format).isEqualTo(NetcdfFileFormat.NETCDF3_64BIT_DATA);
    }
    try (NetcdfFile jni = TestNc4reader.openJni(location)) {
      Array data = read(jni, "f4", "0:2");
      assertThat(MAMath.nearlyEquals(data, BASELINE)).isTrue();
      System.err.println("***Pass");
    }
  }

  private Array read(NetcdfFile ncfile, String vname, String section) throws IOException, InvalidRangeException {
    Variable v = ncfile.findVariable(vname);
    assert v != null;
    return v.read(section);
  }

}
