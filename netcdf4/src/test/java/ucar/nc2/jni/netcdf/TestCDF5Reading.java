/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.jni.netcdf;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;

/** Test Reading of CDF-5 files using JNI netcdf-4 iosp */
public class TestCDF5Reading {
  private static final Array<Float> BASELINE;

  static {
    BASELINE = Arrays.factory(ArrayType.FLOAT, new int[] {3},
        new float[] {-3.4028235E38F, 3.4028235E38F, Float.NEGATIVE_INFINITY});
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
      Variable v = jni.findVariable("f4");
      assertThat(v).isNotNull();
      Array<?> data = v.readArray(new Section("0:2"));
      assertThat(CompareArrayToArray.compareData("f4", data, BASELINE)).isTrue();
      System.err.println("***Pass");
    }
  }

}
