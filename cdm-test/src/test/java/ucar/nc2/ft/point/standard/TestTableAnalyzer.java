package ucar.nc2.ft.point.standard;

import java.io.IOException;
import java.util.Formatter;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@Category(NeedsCdmUnitTest.class)
public class TestTableAnalyzer {

  static void doit(String filename) throws IOException {
    filename = TestDir.cdmUnitTestDir + filename;
    System.out.println(filename);
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    TableAnalyzer csa = TableAnalyzer.factory(null, null, ncd);
    csa.getDetailInfo(new Formatter(System.out));
    System.out.println("%n-----------------");
  }

  @Test
  public void testStuff() throws IOException {
    doit("ncss/point_features/metars/Surface_METAR_20130823_0000.nc");
    doit("ncss/point_features/windProfilers/20131101/PROFILER_wind_06min_20131101_2348.nc");

    // LOOK add all in directory
    // doAll("cfPoint/");
  }


}
