/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;

import static org.junit.Assert.assertThrows;

/** Test problems reading new and GridDataset. */
@Category(NeedsCdmUnitTest.class)
public class TestGridCompareProblem {

  @Test
  public void testProblem() throws IOException {
    TestGridCompareSlice tester = new TestGridCompareSlice(TestDir.cdmUnitTestDir + "conventions/cf/gomoos_cf.nc",
        FeatureType.GRID, "elev", null, null, "2003-03-30T03:00Z", 100000.0, null);
    tester.testReadGridSlice();
  }

  @Test
  public void testNonStandardCalendar() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/cf1.nc";
    TestGridCompareWithDt.testAll(filename, true);
  }

  @Test
  public void testNonMonotonic() {
    String filename = TestDir.cdmUnitTestDir + "ft/grid/cg/cg.ncml";
    assertThrows(IllegalArgumentException.class, () -> TestGridCompareWithDt.testAll(filename, true)).getMessage()
        .contains("time not monotonic");
  }

  @Test
  public void testScalarVertCoordinate() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/ipcc/tas_A1.nc";
    TestGridCompareWithDt.testAll(filename, true);
  }

  @Test
  public void testNominalPointAxis() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/ipcc/cl_A1.nc";
    TestGridCompareWithDt.testAll(filename, true);
  }

  @Test
  public void testProblem1() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_01_000000_0003.nc";
    TestGridCompareWithDt.testAll(filename, true);
  }

  @Ignore("GRID fails because not monotonic")
  @Test
  public void testProblem3() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/nuwg/2003021212_avn-x.nc";
    TestGridCompareWithDt.testAll(filename, true);
  }

  @Test
  public void testTime2DRegularOffsetSize() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "tds_index/NCEP/NAM/Polar_90km/NAM-Polar_90km.ncx4";
    TestGridCompareWithDt.testAll(filename, false);
  }

  @Test
  public void testProblem4() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/ifps/HUNGrids.netcdf";
    TestGridCompareWithDt.testAll(filename, true);
  }

  @Test
  public void testCurvilinear() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "ft/grid/stag/bora_feb-coord.ncml";
    TestGridCompareWithDt.testAll(filename, true);
  }

  @Test
  public void testCurvilinear2() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/bora_test_agg.ncml";
    TestGridCompareWithDt.testAll(filename, true);
  }

}
