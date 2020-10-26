package ucar.nc2.grid;

import org.junit.Test;
import ucar.unidata.util.test.TestDir;

import static ucar.nc2.grid.TestReadGridCompare.compareGridCoverage;
import static ucar.nc2.grid.TestReadGridCompare.compareGridDataset;

public class TestGridCompareProblem {

  @Test
  public void testProblem() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "ft/grid/gfs_crossPM_contiguous.nc";
    compareGridDataset(filename);
    compareGridCoverage(filename);
  }
}
