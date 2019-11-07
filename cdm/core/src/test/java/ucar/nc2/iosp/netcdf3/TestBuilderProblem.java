package ucar.nc2.iosp.netcdf3;

import static com.google.common.truth.Truth.assertThat;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;

public class TestBuilderProblem {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void problem() throws Exception {
    compareWithBuilder(TestDir.cdmLocalTestDataDir + "example1.nc");
  }

  private void compareWithBuilder(String filename) throws Exception {
    logger.info("TestBuilders on {}%n", filename);
    SPFactory.setServiceProvider("ucar.nc2.iosp.netcdf3.N3raf");
    try (NetcdfFile org = NetcdfFile.open(filename)) {
      TestDir.readAllData(org);
      SPFactory.setServiceProvider("ucar.nc2.iosp.netcdf3.N3iospNew");
      try (NetcdfFile withBuilder = NetcdfFile.open(filename)) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, true, true, true);
        boolean ok = compare.compare(org, withBuilder);
        System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", f);
        assertThat(ok).isTrue();
      }
    } finally {
      SPFactory.setServiceProvider("ucar.nc2.iosp.netcdf3.N3raf");
    }
  }
}
