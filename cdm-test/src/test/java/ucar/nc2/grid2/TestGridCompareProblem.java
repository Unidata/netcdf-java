package ucar.nc2.grid2;

import org.junit.Ignore;
import org.junit.Test;
import ucar.unidata.util.test.TestDir;

import static ucar.nc2.grid2.TestGridCompare.compareWithCoverage;
import static ucar.nc2.grid2.TestGridCompare.compareWithGrid1;

// @Ignore("not fixed yet")
public class TestGridCompareProblem {

  @Test
  public void testProblem() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/jonathan/fixed.fw0.0Sv.nc";
    compareWithCoverage(filename);
    compareWithGrid1(filename);
  }

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
  public void testProblem2() throws Exception {
    String filename = "/media/snake/Elements/data/grib/idd/namPolar90/NamPolar90.ncx4";
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
