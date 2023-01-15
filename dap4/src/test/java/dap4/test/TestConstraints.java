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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This Test uses the JUNIT Version 4 parameterized test mechanism.
 * The set of arguments for each test is encapsulated in a class
 * called TestCase. This allows for code re-use and for extending
 * tests by adding fields to the TestCase object.
 */

@RunWith(Parameterized.class)
public class TestConstraints extends DapTestCommon implements Dap4ManifestIF {

  //////////////////////////////////////////////////
  // Constants

  // Define the server to use
  static protected final String SERVERNAME = "d4ts";
  static protected final String SERVER = "remotetest.unidata.ucar.edu";
  static protected final int SERVERPORT = -1;
  static protected final String SERVERPATH = "d4ts/testfiles";

  // Define the input set location(s)
  static protected final String INPUTEXT = ".nc"; // note that the .dap is deliberately left off
  static protected final String INPUTQUERY = "?" + DapConstants.CHECKSUMTAG + "=false&";
  static protected final String INPUTFRAG = "#dap4";

  static protected final String BASELINEDIR = "/baselineconstraints";
  static protected final String BASELINEEXT = ".nc.ncdump";

  // Following files cannot be tested because of flaws in sequence handling
  // by the CDM code in ucar.nc2.dataset.
  static protected String[] EXCLUSIONS =
      {"test_vlen2", "test_vlen3", "test_vlen4", "test_vlen5", "test_vlen6", "test_vlen7", "test_vlen8"};

  //////////////////////////////////////////////////
  // Static Fields

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static public String resourceroot;
  static public Dap4Server server;

  static {
    // This test uses remotetest
    server = new Dap4Server("remotetest", SERVER, SERVERPORT, SERVERPATH);
    Dap4Server.register(true, server);
    resourceroot = getResourceRoot();
  }

  //////////////////////////////////////////////////
  // Test Case Class

  // Encapulate the arguments for each test
  static class TestCase extends TestCaseCommon {
    public String url;
    public String baseline;
    public String ce; // for debugging

    public TestCase(String name, String url, String baseline, String ce) {
      super(name);
      this.url = url;
      this.baseline = baseline;
      this.ce = ce;
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
    assert (server != null);
    List<TestCaseCommon> testcases = new ArrayList<>();
    String[][] manifest = excludeNames(constraint_manifest, EXCLUSIONS);
    // Separate the manifest string into the file name and the index and the query parts
    for (String[] tuple : manifest) {
      String file = tuple[0];
      String index = tuple[1];
      String query = tuple[2]; // excluding leading '?'
      String url =
          server.getURL() + "/" + file + INPUTEXT + INPUTQUERY + DapConstants.CONSTRAINTTAG + query + INPUTFRAG;
      String baseline = resourceroot + BASELINEDIR + "/" + file + "." + index + BASELINEEXT;
      TestCase tc = new TestCase(file + "." + index, url, baseline, query);
      testcases.add(tc);
    }
    // singleTest(0,testcases); // choose single test for debugging
    return testcases;
  }

  //////////////////////////////////////////////////
  // Test Fields

  TestCase tc;

  //////////////////////////////////////////////////
  // Constructor(s)

  public TestConstraints(TestCaseCommon tc) {
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

    NetcdfDataset ncfile;
    try {
      ncfile = openDataset(tc.url);
    } catch (Exception e) {
      e.printStackTrace();
      throw new Exception("File open failed: " + tc.url, e);
    }
    assert ncfile != null;

    String datasetname = tc.name;
    String testresult = dumpdata(ncfile, datasetname);

    // Read the baseline file(s) if they exist
    String baselinecontent = null;
    if (props.prop_baseline) {
      writefile(tc.baseline, testresult);
    } else {
      try {
        baselinecontent = readfile(tc.baseline);
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
}


