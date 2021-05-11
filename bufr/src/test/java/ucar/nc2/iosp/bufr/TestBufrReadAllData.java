package ucar.nc2.iosp.bufr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Read all data in a BUFR file. */
@Category(NeedsCdmUnitTest.class)
@RunWith(Parameterized.class)
public class TestBufrReadAllData {
  private static final String testDir = TestDir.cdmUnitTestDir + "/formats/bufr/userExamples";

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> getTestParameters() {
    Collection<Object[]> filenames = new ArrayList<>();
    try {
      TestDir.actOnAllParameterized(testDir, (file) -> true, filenames, true);
    } catch (IOException e) {
      filenames.add(new Object[] {e.getMessage()});
    }
    return filenames;
  }

  private final String filename;

  public TestBufrReadAllData(String filename) {
    this.filename = filename;
  }

  @Test
  public void compareWithBuilder() throws IOException {
    System.out.printf("Test read all variables for  on %s%n", filename);
    TestDir.readAll(filename);
  }
}


