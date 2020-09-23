/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.array;

import java.io.IOException;
import java.util.Formatter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Compare reading netcdf with Array */
@Category(NeedsCdmUnitTest.class)
public class TestReadArrayProblem {

  // Opaque not implemented yet
  private final String filename = TestDir.cdmLocalTestDataDir + "hdf5/test_atomic_types.nc";
  // private final String filename = TestDir.cdmUnitTestDir + "formats/netcdf3/files/nctest_64bit_offset.nc";

  @Test
  public void compareArrays() throws IOException, InvalidRangeException {
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, -1, null, NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      System.out.println("Test input: " + ncfile.getLocation());

      boolean ok = true;
      for (Variable v : ncfile.getVariables()) {
        System.out.printf("  read variable %s %s", v.getDataType(), v.getShortName());
        ucar.ma2.Array org = v.read();
        try {
          Array<?> array = v.readArray();
          if (array != null) {
            System.out.printf("  COMPARE%n");
            Formatter f = new Formatter();
            boolean ok1 = TestReadArrayCompare.compareData(f, v.getShortName(), org, array, false, true);
            if (!ok1) {
              System.out.printf("%s%n", f);
            }
            ok &= ok1;
          } else {
            System.out.printf("%n");
          }
        } catch (Exception e) {
          System.out.printf(" BAD%n");
          e.printStackTrace();
        }
      }
      Assert.assertTrue(filename, ok);
    }
  }
}

