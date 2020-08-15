/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.util.Formatter;
import org.junit.Ignore;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;

/** Compare problem grib file builder */
public class TestGribCompareProblem {

  @Ignore
  public void compareProblemFile() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/gfs_4_20130830_1800_144.grb2";
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
