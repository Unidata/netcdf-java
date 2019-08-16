package ucar.nc2.ft.point.standard;

import java.io.IOException;
import java.util.Formatter;
import org.junit.Test;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.TestDir;

public class TestTableAnalyzer {

  static void doit(String filename) throws IOException {
    filename = TestDir.cdmTestDataDir + filename;
    System.out.println(filename);
    NetcdfDataset ncd = ucar.nc2.dataset.NetcdfDataset.openDataset(filename);
    TableAnalyzer csa = TableAnalyzer.factory(null, null, ncd);
    csa.getDetailInfo(new Formatter(System.out));
    System.out.println("%n-----------------");
  }

  @Test
  public void testStuff() throws IOException {
    /* doit("C:/data/dt2/station/Surface_METAR_20080205_0000.nc");
    doit("C:/data/bufr/edition3/idd/profiler/PROFILER_3.bufr");
    doit("C:/data/bufr/edition3/idd/profiler/PROFILER_2.bufr");
    doit("C:/data/profile/PROFILER_wind_01hr_20080410_2300.nc"); */
    //doit("C:/data/test/20070301.nc");
    //doit("C:/data/dt2/profile/PROFILER_3.bufr");

    //doit("C:/data/dt2/station/ndbc.nc");
    //doit("C:/data/dt2/station/madis2.sao");
    // doit("C:/data/metars/Surface_METAR_20070326_0000.nc");  // ok
    //doit("C:/data/dt2/station/Sean_multidim_20070301.nc"); // ok
    //doit("Q:/cdmUnitTest/formats/gempak/surface/20090521_sao.gem");
    doit("datasets/metars/Surface_METAR_20070513_0000.nc");

    //doit("C:/data/profile/PROFILER_wind_01hr_20080410_2300.nc");
    //doit("C:/data/cadis/tempting");
  }


}
