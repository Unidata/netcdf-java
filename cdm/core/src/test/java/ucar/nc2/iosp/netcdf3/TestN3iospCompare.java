/* Copyright Unidata */
package ucar.nc2.iosp.netcdf3;

import static org.junit.Assert.fail;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;

/**
 * Compare objects in original N3iosp vs N3iospNew using builders.
 */
@RunWith(Parameterized.class)
public class TestN3iospCompare {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static String testDir = TestDir.cdmLocalTestDataDir;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> getTestParameters() {
    Collection<Object[]> filenames = new ArrayList<>();
    try {
      TestDir.actOnAllParameterized(testDir, new SuffixFileFilter(".nc"), filenames);
    } catch (IOException e) {
      filenames.add(new Object[] {e.getMessage()});
    }
    return filenames;
  }

  private String filename;

  public TestN3iospCompare(String filename) {
    this.filename = filename;
  }

  @Test
  public void compareWithBuilder()
      throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
    logger.info("TestBuilders on {}%n", filename);
    SPFactory.setServiceProvider("ucar.nc2.iosp.netcdf3.N3raf");
    try (NetcdfFile org = NetcdfFile.open(filename)) {
      SPFactory.setServiceProvider("ucar.nc2.internal.iosp.netcdf3.N3iospNew");
      try (NetcdfFile withBuilder = NetcdfFiles.open(filename)) {
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
