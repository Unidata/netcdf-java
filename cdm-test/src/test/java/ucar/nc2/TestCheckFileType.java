/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsContentRoot;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/** Test that NetcdfFileFormat.findNetcdfFormatType can recognize various file types */
@Category(NeedsContentRoot.class)
@RunWith(Parameterized.class)
public class TestCheckFileType {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String PREFIX = "/thredds/public/testdata/";

  @Parameterized.Parameters(name = "{1}")
  static public List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();
    result.add(new Object[] {NetcdfFileFormat.NETCDF3, "testData.nc"});
    result.add(new Object[] {NetcdfFileFormat.NETCDF3_64BIT_OFFSET, "nc_test_cdf2.nc"});
    result.add(new Object[] {NetcdfFileFormat.NETCDF3_64BIT_DATA, "nc_test_cdf5.nc"});
    result.add(new Object[] {NetcdfFileFormat.NETCDF4, "group.test2.nc"}); // aka netcdf4
    return result;
  }

  @Parameterized.Parameter(0)
  public NetcdfFileFormat expected;

  @Parameterized.Parameter(1)
  public String filename;

  @Test
  public void testCheckFileType() throws Exception {
    String location = TestDir.cdmTestDataDir + PREFIX + filename;
    try (RandomAccessFile raf = RandomAccessFile.acquire(location)) {
      NetcdfFileFormat found = NetcdfFileFormat.findNetcdfFormatType(raf);
      assertThat(found).isEqualTo(expected);
    }
  }

}
