/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.DatasetUrl;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import ucar.unidata.util.test.DapTestContainer;

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
  protected static String[] urls = {"http://" + DapTestContainer.D4TS_PATH + "test_one_var.nc#dap4",
      "dap4://" + DapTestContainer.D4TS_PATH + "test_one_var.nc",
      "http://" + DapTestContainer.D4TS_PATH + "test_one_var.nc",};

  //////////////////////////////////////////////////
  // Static Fields

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static String resourceroot;

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
  public static List<TestCaseCommon> defineTestCases() {
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

