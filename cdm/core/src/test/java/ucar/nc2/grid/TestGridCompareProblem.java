package ucar.nc2.grid;

import org.junit.Test;
import ucar.unidata.util.test.TestDir;

import static ucar.nc2.grid.TestReadGridCompare.compareGridCoverage;
import static ucar.nc2.grid.TestReadGridCompare.compareGridDataset;

public class TestGridCompareProblem {

  @Test
  public void testProblem() throws Exception {
    String filename = TestDir.cdmLocalTestDataDir + "ncml/fmrc/GFS_Puerto_Rico_191km_20090729_0000.nc";
    compareGridDataset(filename);
    compareGridCoverage(filename);
  }
}
