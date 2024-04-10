package opendap.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.DapTestContainer;
import java.io.File;
import java.io.FileReader;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

public class TestSources extends TestFiles {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  //////////////////////////////////////////////////
  // Remote test info

  public static final String URL = "http://" + DapTestContainer.DTS_PATH;

  // These shorter tests are always run
  static final String[] TESTS_S1 =
      {"test.01", "test.02", "test.04", "test.05", "test.06a", "test.07a", "test.07", "test.21", "test.22", "test.23",
          "test.31", "test.50", "test.53", "test.55", "test.56", "test.57", "test.66", "test.67", "test.68", "test.69"};

  // Following tests are to check constraint handling
  static final String[] TESTS_C1 = {"test.02;1;b[1:2:10]", "test.03;1;i32[0:1][1:2][0:2]", "test.04;1;types.i32",
      "test.05;1;types.floats.f32", "test.06;1;ThreeD", "test.07;1;person.age", "test.07;2;person"};

  static enum TestSetEnum {
    Standard1, Constrained1
  }

  static enum TestPart {
    DAS, DDS, DATADDS;
  }

  static class TestSet {
    public String name;
    public String url;
    public String[] tests;
  }

  static Map<TestSetEnum, TestSet> TestSets;

  static String partext(TestPart part) {
    switch (part) {
      case DAS:
        return ".das";
      case DDS:
        return ".dds";
      case DATADDS:
        return ".dods";
      default:
        break;
    }
    return ".dds";
  }

  static String accessTestData(String testprefix, String basename, TestPart part) throws Exception {

    String fname = testprefix + File.separator + basename + partext(part);

    String result = null;
    try {
      File f = new File(fname);
      if (!f.canRead())
        return null;
      FileReader fr = new FileReader(fname);
      StringBuffer cbuf = new StringBuffer();
      int c;
      while ((c = fr.read()) != -1) {
        cbuf.append((char) c);
      }
      return cbuf.toString();
    } catch (Exception e) {
      System.err.println("File io failure: " + e.toString());
      e.printStackTrace();
      throw e;
    }

  }

  public TestSources() {
    super();
    setup();
  }

  protected void setup() {
    TestSets = new HashMap<TestSetEnum, TestSet>();
    TestSet set;
    set = new TestSet();
    set.name = "Standard1";
    set.url = URL;
    set.tests = TESTS_S1;
    TestSets.put(TestSetEnum.Standard1, set);
    set = new TestSet();
    set.name = "Constrained1";
    set.url = URL;
    set.tests = TESTS_C1;
    TestSets.put(TestSetEnum.Constrained1, set);
  }
}
