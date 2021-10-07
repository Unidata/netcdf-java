/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.iosp.hdf4;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;

@Category(NeedsCdmUnitTest.class)
public class TestH4misc {
  static public String testDir = TestDir.cdmUnitTestDir + "formats/hdf4/";

  @Test
  public void testUnsigned() throws IOException, InvalidRangeException {
    String filename = testDir + "MOD021KM.A2004328.1735.004.2004329164007.hdf";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      String vname = "MODIS_SWATH_Type_L1B/Data_Fields/EV_250_Aggr1km_RefSB";
      Variable v = ncfile.findVariable(vname);
      assert v != null : filename + " " + vname;

      Array<?> data = v.readArray();
      System.out.printf(" sum =          %f%n", Arrays.sumDouble(data));

      double sum2 = 0;
      double sum3 = 0;
      int[] varShape = v.getShape();
      int[] origin = new int[3];
      int[] size = new int[] {1, varShape[1], varShape[2]};
      for (int i = 0; i < varShape[0]; i++) {
        origin[0] = i;
        Array<?> data2D = v.readArray(new Section(origin, size));

        double sum = Arrays.sumDouble(data2D);
        System.out.printf("  %d sum3D =        %f%n", i, sum);
        sum2 += sum;

        // assert data2D.getRank() == 2;
        sum = Arrays.sumDouble(Arrays.reduce(data2D, 0));
        System.out.printf("  %d sum2D =        %f%n", i, sum);
        sum3 += sum;

        CompareArrayToArray.compareData(v.getShortName(), data2D, Arrays.reduce(data2D, 0));
      }
      System.out.printf(" sum2D =        %f%n", sum2);
      System.out.printf(" sum2D.reduce = %f%n", sum3);
      assert sum2 == sum3;
    }
  }

  @Test
  public void readProblem() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/hdf4/eos/misr/MISR_AM1_AGP_P040_F01_24.subset.eos";
    TestDir.readAll(filename);
  }


}
