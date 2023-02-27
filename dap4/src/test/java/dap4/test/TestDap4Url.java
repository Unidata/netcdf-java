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
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This Test uses the JUNIT Version 4 parameterized test mechanism.
 * The set of arguments for each test is encapsulated in a class
 * called TestCase. This allows for code re-use and for extending
 * tests by adding fields to the TestCase object.
 */

/**
 * This specific set of tests verifies the legal URLs for accessing
 * a DAP4 server.
 */

@RunWith(Parameterized.class)
public class TestDap4Url extends DapTestCommon implements Dap4ManifestIF {

  //////////////////////////////////////////////////
  // Constants

  // Legal url formats
  static protected String[] urls = {"https://remotetest.unidata.ucar.edu/d4ts/testfiles/test_one_var.nc#dap4",
      "dap4://remotetest.unidata.ucar.edu/d4ts/testfiles/test_one_var.nc",
      "https://remotetest.unidata.ucar.edu/d4ts/testfiles/test_one_var.nc",
      // "https://remotetest.unidata.ucar.edu/thredds/dap4/testAll/H.1.1.nc",
  };

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

    public TestCase(String url) {
      this.url = url;
    }

    // This defines how the test is reported by JUNIT.
    public String toString() {
      return this.url;
    }
  }

  //////////////////////////////////////////////////
  // Test Generator

  @Parameterized.Parameters(name = "{index}: {0}")
  static public List<TestCaseCommon> defineTestCases() {
    List<TestCaseCommon> testcases = new ArrayList<>();
    for (String u : urls) {
      TestCase tc = new TestCase(u);
      testcases.add(tc);
    }
    return testcases;
  }

  //////////////////////////////////////////////////
  // Test Fields

  TestCase tc;

  //////////////////////////////////////////////////
  // Constructor(s)

  public TestDap4Url(TestCaseCommon tc) {
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
    System.err.println(">>> Test: " + tc.url);
    DatasetUrl durl;
    try {
      durl = DatasetUrl.findDatasetUrl(tc.url);
    } catch (Exception e) {
      e.printStackTrace();
      durl = null;
    }
    Assert.assertNotNull("*** FAIL: " + tc.name, durl);
    System.out.println("*** PASS: " + tc.name);
  }
}

