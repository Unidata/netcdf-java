package ucar.nc2.grib;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;

/** Compare objects in Gribiosp with and without builders. */
@RunWith(Parameterized.class)
public class TestGribCompareBuilders {

  private static final Logger logger = LoggerFactory
      .getLogger(MethodHandles.lookup().lookupClass());
  private static String testDir = "../grib/src/test/data/";

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> getTestParameters() {
    Collection<Object[]> filenames = new ArrayList<>();
    try {
      TestDir.actOnAllParameterized(testDir,
          (file) -> file.getPath().endsWith(".grib1") || file.getPath().endsWith(".grib2"),
          filenames,
          true);
    } catch (IOException e) {
      filenames.add(new Object[]{e.getMessage()});
    }
    return filenames;
  }

  private String filename;

  public TestGribCompareBuilders(String filename) {
    this.filename = filename;
  }

  @Test
  public void compareWithBuilder() throws IOException {
    System.out.printf("TestBuilders on %s%n", filename);
    try (NetcdfFile org = NetcdfFile.open(filename)) {
      try (NetcdfFile withBuilder = NetcdfFiles.open(filename)) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, true);
        if (!compare.compare(org, withBuilder, null)) {
          System.out.printf("Compare %s%n%s%n", filename, f);
          fail();
        }
      }
    }
  }
}

