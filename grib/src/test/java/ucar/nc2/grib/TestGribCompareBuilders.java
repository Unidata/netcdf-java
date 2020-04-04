package ucar.nc2.grib;

import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Compare objects in Gribiosp with and without builders. */
@Category(NeedsCdmUnitTest.class)
@RunWith(Parameterized.class)
public class TestGribCompareBuilders {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static List<String> testDirs =
      ImmutableList.of("../grib/src/test/data/");
  // TODO refactor this framework: need a way to pass in compareData, these files are too big.
  // , TestDir.cdmUnitTestDir + "/formats/grib1", TestDir.cdmUnitTestDir + "/formats/grib2");

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> getTestParameters() {
    Collection<Object[]> filenames = new ArrayList<>();
    try {
      for (String dir : testDirs) {
        TestDir.actOnAllParameterized(dir,
            (file) -> file.getPath().endsWith(".grb") || file.getPath().endsWith(".grib1") ||
                file.getPath().endsWith(".grib2") || file.getPath().endsWith(".grb2"),
            filenames,
            true);
      }
    } catch (IOException e) {
      logger.warn("Failed to add directory", e);
    }
    return filenames;
  }

  private final String filename;
  private final boolean compareData = true;

  public TestGribCompareBuilders(String filename) {
    this.filename = filename;
  }

  @Test
  public void compareWithBuilder() throws IOException {
    System.out.printf("TestBuilders on %s%n", filename);
    try (NetcdfFile org = NetcdfFile.open(filename)) {
      try (NetcdfFile withBuilder = NetcdfFiles.open(filename)) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, compareData);
        if (!compare.compare(org, withBuilder, null)) {
          System.out.printf("Compare %s%n%s%n", filename, f);
          fail();
        }
      }
    }
  }
}

