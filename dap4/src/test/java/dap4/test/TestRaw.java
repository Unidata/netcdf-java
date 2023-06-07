/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.test;

import dap4.core.util.DapConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static dap4.core.util.DapConstants.CHECKSUMATTRNAME;

/**
 * This Test uses the JUNIT Version 4 parameterized test mechanism.
 * The set of arguments for each test is encapsulated in a class
 * called TestCase. This allows for code re-use and for extending
 * tests by adding fields to the TestCase object.
 */

@RunWith(Parameterized.class)
public class TestRaw extends DapTestCommon implements Dap4ManifestIF {

  //////////////////////////////////////////////////
  // Constants

  // Define the input set location(s)
  static protected final String INPUTDIR = "/rawtestfiles";
  static protected final String INPUTEXT = ".nc.dap";
  static protected final String INPUTQUERY = "?" + DapConstants.CHECKSUMTAG + "=false";
  static protected final String INPUTFRAG = "";
  static protected final String BASELINEDIR = "/baselineraw";
  static protected final String BASELINEEXT = ".nc.ncdump";


  // Following files cannot be tested because of flaws in sequence handling
  // by the CDM code in ucar.nc2.dataset.
  static protected String[] EXCLUSIONS =
      {"test_vlen2", "test_vlen3", "test_vlen4", "test_vlen5", "test_vlen6", "test_vlen7", "test_vlen8"};

  // Attribute suppression
  static String RE_ENDIAN = "\n[ \t]*<Attribute[ \t]+name=[\"]_DAP4_Little_Endian[\"].*?</Attribute>[ \t]*";
  static String RE_CHECKSUM = ":" + DapConstants.CHECKSUMATTRNAME;
  static String RE_DAP4_ENDIAN = ":" + DapConstants.LITTLEENDIANATTRNAME;
  static String RE_DAP4_CE = ":" + DapConstants.CEATTRNAME;

  //////////////////////////////////////////////////
  // Static Fields

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static public String resourceroot;

  static {
    resourceroot = getResourceRoot();
  }

  //////////////////////////////////////////////////
  // Test Case Class

  // Encapulate the arguments for each test
  static class TestCase extends TestCaseCommon {
    public String url;
    public String baseline;

    public TestCase(String name, String url, String baseline) {
      super(name);
      this.url = url;
      this.baseline = baseline;
    }

    // This defines how the test is reported by JUNIT.
    public String toString() {
      return this.name;
    }
  }

  //////////////////////////////////////////////////
  // Test Generator

  @Parameterized.Parameters(name = "{index}: {0}")
  static public List<TestCaseCommon> defineTestCases() {
    List<TestCaseCommon> testcases = new ArrayList<>();
    String[][] manifest = excludeNames(dap4_manifest, EXCLUSIONS);
    for (String[] tuple : manifest) {
      String name = tuple[0];
      String url = buildURL(name);
      String baseline = resourceroot + BASELINEDIR + "/" + name + BASELINEEXT;
      TestCase tc = new TestCase(name, url, baseline);
      testcases.add(tc);
    }
    // singleTest("test_vlen3", testcases); // choose single test for debugging
    return testcases;
  }

  //////////////////////////////////////////////////
  // Test Fields

  TestCase tc;

  //////////////////////////////////////////////////
  // Constructor(s)

  public TestRaw(TestCaseCommon tc) {
    super();
    this.tc = (TestCase) tc;
  }

  //////////////////////////////////////////////////
  // Junit test method(s)

  @Before
  public void setup() {
    // Set any properties
    // props.prop_visual = true;
    // props.prop_baseline = true;
    super.setup();
  }

  @Test
  public void test() throws Exception {
    int i, c;
    StringBuilder sb = new StringBuilder();
    // String url = buildURL(resourceroot + INPUTDIR, tc.name + INPUTEXT);
    String url = tc.url;
    NetcdfDataset ncfile;
    try {
      ncfile = openDataset(url);
    } catch (Exception e) {
      e.printStackTrace();
      throw new Exception("File open failed: " + url, e);
    }
    assert ncfile != null;

    String testresult = dumpdata(ncfile, tc.name); // print data section
    ncfile.close();

    // Remove unused text
    sb.setLength(0);
    sb.append(testresult);
    regexpFilterLine(sb, RE_DAP4_ENDIAN, true);
    regexpFilterLine(sb, RE_DAP4_CE, true);
    testresult = sb.toString();

    // Read the baseline file(s) if they exist
    String baselinecontent = null;
    if (props.prop_baseline) {
      writefile(tc.baseline, testresult);
    } else {
      try {
        baselinecontent = readfile(tc.baseline);
        // Remove unused text
        sb.setLength(0);
        sb.append(baselinecontent);
        regexpFilterLine(sb, RE_CHECKSUM, true);
        baselinecontent = sb.toString();
      } catch (NoSuchFileException nsf) {
        Assert.fail(tc.name + ": ***Fail: test comparison file not found: " + tc.baseline);
      }
    }
    if (props.prop_visual) {
      if (baselinecontent != null)
        visual("Input", baselinecontent);
      visual("Output", testresult);
    }
    if (!props.prop_baseline && props.prop_diff) { // compare with baseline
      System.err.println("Comparison: vs " + tc.baseline);
      Assert.assertTrue("*** FAIL", same(getTitle(), baselinecontent, testresult));
      System.out.println(tc.name + ": Passed");
    }
  }

  //////////////////////////////////////////////////
  // Support Methods

  static protected String buildURL(String name) {
    StringBuilder url = new StringBuilder();
    url.append("file://");
    url.append(resourceroot);
    url.append(INPUTDIR);
    url.append("/");
    url.append(name);
    url.append(INPUTEXT);
    url.append(INPUTQUERY);
    url.append(INPUTFRAG);
    return url.toString();
  }

}


