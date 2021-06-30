package ucar.nc2.grid2;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import static ucar.nc2.grid2.TestGridCompare.compareWithCoverage;
import static ucar.nc2.grid2.TestGridCompare.compareWithGrid1;

@Category(NeedsCdmUnitTest.class)
public class TestGridCompareProblem {

  @Test
  public void testProblem1() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_01_000000_0003.nc";
    compareWithCoverage(filename);
    compareWithGrid1(filename);
  }

  @Test
  public void testProblem3() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/nuwg/2003021212_avn-x.nc";
    compareWithCoverage(filename);
    compareWithGrid1(filename);
  }

  @Test
  public void testProblem4() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/ifps/HUNGrids.netcdf";
    compareWithCoverage(filename);
    compareWithGrid1(filename);
  }

}
