/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.array;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;


/** Compare reading bufr with old ma2.array and new array.Array */
@Category(NeedsCdmUnitTest.class)
public class TestReadBufrProblem {

  @Test
  public void testEmbeddedRecursionArrays() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/embeddedTable/gdas.adpsfc.t00z.20120603.bufr";
    TestReadBufrCompare.readArrays(filename);
  }

  @Test
  public void testProblem() throws Exception {
    String filename = TestReadBufrCompare.bufrLocalFromTop + "RadiosondeStationData.bufr";
    TestReadBufrCompare.readArrays(filename);
  }

}

