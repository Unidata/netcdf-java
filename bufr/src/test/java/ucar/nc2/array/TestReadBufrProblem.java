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
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.nc2.internal.util.CompareArrayToMa2;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Compare reading bufr with old ma2.array and new array.Array */
@Category(NeedsCdmUnitTest.class)
public class TestReadBufrProblem {

  @Test
  public void testEmbeddedRecursionCompare() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/embeddedTable/gdas.adpsfc.t00z.20120603.bufr";
    TestReadBufrCompare.compareMa2Array(filename);
  }

  @Test
  public void testEmbeddedRecursionArrays() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/embeddedTable/gdas.adpsfc.t00z.20120603.bufr";
    TestReadBufrCompare.readArrays(filename);
  }

  @Test
  public void testEmbeddedRecursionOrg() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/embeddedTable/gdas.adpsfc.t00z.20120603.bufr";
    TestReadBufrCompare.readOrg(filename);
  }

  @Test
  public void testProblem() throws Exception {
    String filename = TestReadBufrCompare.bufrLocalFromTop + "RadiosondeStationData.bufr";
    TestReadBufrCompare.compareMa2Array(filename);
  }

}

