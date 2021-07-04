/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.util.Formatter;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Compare problem grib file builder */
@Category(NeedsCdmUnitTest.class)
public class TestGribCompareProblem {

  @Test
  public void compareProblemFile() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/GFS_Global_2p5deg_20101023_0600.grib2";
    compare(filename);
  }

  private void compare(String filename) throws IOException {
    System.out.printf("TestBuilders on %s%n", filename);
    try (NetcdfFile org = NetcdfFiles.open(filename)) {
      try (NetcdfFile withBuilder = NetcdfFiles.open(filename)) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, true);
        if (!compare.compare(org, withBuilder, null)) {
          System.out.printf("Compare %s%n%s%n", filename, f);
          fail();
        }
      }
    }
  }

}
