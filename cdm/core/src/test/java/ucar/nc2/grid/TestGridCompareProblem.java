package ucar.nc2.grid;

import org.junit.Ignore;
import org.junit.Test;
import ucar.unidata.util.test.TestDir;

import static ucar.nc2.grid.TestReadGridCompare.compareGridCoverage;
import static ucar.nc2.grid.TestReadGridCompare.compareGridDataset;

// @Ignore("not fixed yet")
public class TestGridCompareProblem {

  @Test
  public void testProblem() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_01_000000_0003.nc";
    compareGridDataset(filename);
    compareGridCoverage(filename);
  }

  @Test
  public void testProblem3() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/nuwg/2003021212_avn-x.nc";
    compareGridDataset(filename);
    compareGridCoverage(filename);
  }

  @Test
  public void testProblem2() throws Exception {
    String filename = "/media/snake/Elements/data/grib/idd/namPolar90/NamPolar90.ncx4";
    compareGridDataset(filename);
    compareGridCoverage(filename);
  }

}
