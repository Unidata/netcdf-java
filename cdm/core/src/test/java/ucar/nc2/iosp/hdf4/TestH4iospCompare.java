/* Copyright Unidata */
package ucar.nc2.iosp.hdf4;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
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

/**
 * Compare objects in original N3iosp vs N3iospNew using builders.
 */
@Category(NeedsCdmUnitTest.class)
@RunWith(Parameterized.class)
public class TestH4iospCompare {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static String testDir = TestDir.cdmUnitTestDir + "/formats/hdf4";

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> getTestParameters() {
    Collection<Object[]> filenames = new ArrayList<>();
    try {
      TestDir.actOnAllParameterized(testDir, (file) -> !file.getPath().endsWith(".txt"), filenames);
    } catch (IOException e) {
      filenames.add(new Object[] {e.getMessage()});
    }
    return filenames;
  }

  private String filename;

  public TestH4iospCompare(String filename) {
    this.filename = filename;
  }

  @Test
  public void compareWithBuilder() throws IOException {
    System.out.printf("TestBuilders on %s%n", filename);
    try (NetcdfFile org = NetcdfFile.open(filename)) {
      try (NetcdfFile withBuilder = NetcdfFiles.open(filename)) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, true);
        if (!compare.compare(org, withBuilder)) {
          System.out.printf("Compare %s%n%s%n", filename, f);
          fail();
        }
      }
    }
  }
}
