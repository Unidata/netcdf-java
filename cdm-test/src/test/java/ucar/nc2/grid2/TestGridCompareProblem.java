package ucar.nc2.grid2;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import static ucar.nc2.grid2.TestGridCompareData1.compareWithGrid1;

@Category(NeedsCdmUnitTest.class)
public class TestGridCompareProblem {

  @Test
  public void testScalarVertCoordinate() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/ipcc/tas_A1.nc";
    compareWithGrid1(filename);
  }

  @Test
  public void testNominalPointAxis() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/ipcc/cl_A1.nc";
    compareWithGrid1(filename);
  }

  @Test
  public void testProblem1() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_01_000000_0003.nc";
    compareWithGrid1(filename);
  }

  @Test
  public void testProblem3() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/nuwg/2003021212_avn-x.nc";
    compareWithGrid1(filename);
  }

  // This is getting the ncoords wrong for Time2DRegular. I think validtime3 should be 4, somehow 52.
  // We see the problem because we are comparing to earlier versions. So probably internally consistent??
  // reproduced in TestGribGridCompare
  // @Test LOOK GRIB not ready yet
  public void testTime2DRegularOffsetSize() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "tds_index/NCEP/NAM/Polar_90km/NAM-Polar_90km.ncx4";
    compareWithGrid1(filename);
  }

  @Test
  public void testProblem4() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/ifps/HUNGrids.netcdf";
    compareWithGrid1(filename);
  }

  // @Test
  public void testCurvilinear() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "ft/grid/stag/bora_feb-coord.ncml";
    compareWithGrid1(filename);
  }

  // @Test
  public void testCurvilinear2() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "cf/bora_test_agg.ncml";
    compareWithGrid1(filename);
  }

}
