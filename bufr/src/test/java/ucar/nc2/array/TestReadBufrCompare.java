/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.array;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.internal.util.CompareArrayToMa2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Compare reading bufr with ma2.array and array.Array */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestReadBufrCompare {
  public static String bufrLocalFromTop = "src/test/data/";

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    FileFilter ff = TestDir.FileFilterSkipSuffix(".cdl .ncml RadiosondeStationData.bufr");
    List<Object[]> result = new ArrayList<>(500);
    try {
      TestDir.actOnAllParameterized(bufrLocalFromTop, ff, result, false);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;
  }

  /////////////////////////////////////////////////////////////

  public TestReadBufrCompare(String filename) {
    this.filename = filename;
  }

  private final String filename;

  @Test
  public void compareSequence() throws IOException {
    compareSequence(filename);
  }

  private void compareSequence(String filename) throws IOException {
    try (NetcdfFile org = NetcdfFiles.open(filename, -1, null);
        NetcdfFile copy = NetcdfFiles.open(filename, -1, null)) {
      System.out.println("Test NetcdfFile: " + org.getLocation());

      boolean ok = CompareArrayToMa2.compareFiles(org, copy);
      assertThat(ok).isTrue();
    }
  }

}

