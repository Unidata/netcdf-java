package ucar.nc2.grid2;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Test problems reading new and old GridDataset. */
@Category(NeedsCdmUnitTest.class)
public class TestGridCompareProblem {

  @Test
  public void testScalarVertCoordinate() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/ipcc/tas_A1.nc";
    new TestGridCompareCoverage(filename).compareWithCoverage(true);
  }

  @Test
  public void testNominalPointAxis() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/ipcc/cl_A1.nc";
    new TestGridCompareCoverage(filename).compareWithCoverage(true);
  }

  @Test
  public void testProblem1() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_01_000000_0003.nc";
    new TestGridCompareCoverage(filename).compareWithCoverage(true);
  }

  @Test
  public void testProblem3() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/nuwg/2003021212_avn-x.nc";
    new TestGridCompareCoverage(filename).compareWithCoverage(true);
  }

  @Test
  public void testTime2DRegularOffsetSize() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "tds_index/NCEP/NAM/Polar_90km/NAM-Polar_90km.ncx4";
    new TestGridCompareCoverage(filename).compareWithCoverage(false);
  }

  @Test
  public void testProblem4() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/ifps/HUNGrids.netcdf";
    new TestGridCompareCoverage(filename).compareWithCoverage(true);
  }

  // @Test
  public void testCurvilinear() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "ft/grid/stag/bora_feb-coord.ncml";
    new TestGridCompareCoverage(filename).compareWithCoverage(true);
  }

  // @Test
  public void testCurvilinear2() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "cf/bora_test_agg.ncml";
    new TestGridCompareCoverage(filename).compareWithCoverage(true);
  }

}
