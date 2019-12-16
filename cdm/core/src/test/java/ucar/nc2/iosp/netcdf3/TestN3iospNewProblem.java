package ucar.nc2.iosp.netcdf3;

import static org.junit.Assert.fail;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;

public class TestN3iospNewProblem {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void problem() throws Exception {
    compareWithBuilder(TestDir.cdmLocalTestDataDir + "example1.nc");
  }

  private void compareWithBuilder(String filename) throws Exception {
    logger.info("TestBuilders on {}%n", filename);
    SPFactory.setServiceProvider("ucar.nc2.iosp.netcdf3.N3raf");
    try (NetcdfFile org = NetcdfFile.open(filename)) {
      SPFactory.setServiceProvider("ucar.nc2.internal.iosp.netcdf3.N3iospNew");
      try (NetcdfFile withBuilder = NetcdfFile.open(filename)) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, true);
        if (!compare.compare(org, withBuilder)) {
          System.out.printf("Compare %s%n%s%n", filename, f);
          fail();
        }
      }
    } finally {
      SPFactory.setServiceProvider("ucar.nc2.iosp.netcdf3.N3raf");
    }
  }
}
