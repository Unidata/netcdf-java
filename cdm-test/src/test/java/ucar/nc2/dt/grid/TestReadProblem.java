/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Open specific problem dataset with GridDataset.open(). */
public class TestReadProblem {

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void doOne() throws Exception {
    TestReadandCount.doOne(TestDir.cdmUnitTestDir + "formats/grib2/", "eta218.wmo", 57, 16, 20, 11);
  }

}
