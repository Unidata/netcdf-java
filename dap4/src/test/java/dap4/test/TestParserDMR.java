/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.test;

import dap4.core.dmr.DMRFactory;
import dap4.core.dmr.DapDataset;
import dap4.core.dmr.ErrorResponse;
import dap4.core.dmr.parser.DOM4Parser;
import dap4.core.dmr.parser.Dap4Parser;
import dap4.core.dmr.parser.ParseUtil;
import dap4.core.dmr.DMRPrinter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
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
public class TestParserDMR extends DapTestCommon implements Dap4ManifestIF {

  //////////////////////////////////////////////////
  // Constants

  static final boolean PARSEDEBUG = false;

  // Define the input set location(s)
  static protected final String INPUTDIR = "/rawtestfiles";
  static protected final String INPUTEXT = ".nc.dmr";

  // Define some common DMR filter regular expression
  static String RE_ENDIAN = "\n[ \t]*<Attribute[ \t]+name=[\"]_DAP4_Little_Endian[\"].*?</Attribute>[ \t]*";
  static String RE_NCPROPS = "\n[ \t]*<Attribute[ \t]+name=[\"]_NCProperties[\"].*?</Attribute>[ \t]*";
  static protected final String RE_UNLIMITED = "[ \t]+_edu.ucar.isunlimited=\"1\"";

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
    public String name; // Name from manifest
    public String input; // Full path or URL for the input file

    public TestCase(String filename, String input) {
      super(filename);
      this.input = input;
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
    for (String[] tuple : dap4_manifest) {
      String name = tuple[0];
      String path = resourceroot + INPUTDIR + "/" + name + INPUTEXT;
      TestCase tc = new TestCase(name, path);
      testcases.add(tc);
    }
    // Include the constraint tests also
    for (String[] triple : constraint_manifest) {
      String file = triple[0]; // unpack
      String index = triple[1];
      String ce = triple[2]; // unused
      String path = resourceroot + INPUTDIR + "/" + file + "." + index + INPUTEXT;
      TestCase tc = new TestCase(file + "." + index, path);
      testcases.add(tc);
    }
    // singleTest(0, testcases); // choose single test for debugging
    return testcases;
  }

  //////////////////////////////////////////////////
  // Test Fields

  TestCase tc;

  //////////////////////////////////////////////////
  // Constructor(s)

  public TestParserDMR(TestCaseCommon tc) {
    super();
    this.tc = (TestCase) tc;
  }

  //////////////////////////////////////////////////
  // Junit test method(s)

  @Before
  public void setup() {
    // Set any properties
    // props.prop_visual = true;
    super.setup();
  }

  @Test
  public void test() throws Exception {
    String document;
    int i, c;

    document = readfile(tc.input);

    // Remove unneeded attributes
    StringBuilder sb = new StringBuilder();
    sb.append(document);
    regexpFilters(sb, new String[] {RE_ENDIAN, RE_NCPROPS}, false);
    regexpFilters(sb, new String[] {RE_UNLIMITED}, true);
    document = sb.toString();

    Dap4Parser parser = new DOM4Parser(new DMRFactory());
    if (PARSEDEBUG)
      parser.setDebugLevel(1);
    // Parse document
    Assert.assertTrue("Parse failed", parser.parse(document));

    // Check result
    ErrorResponse err = parser.getErrorResponse();
    if (err != null)
      Assert.fail("Error response:\n" + err.buildXML());

    DapDataset dmr = parser.getDMR();
    Assert.assertNotNull("No dataset created", dmr);

    // Dump the parsed DMR for comparison purposes
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    DMRPrinter dapprinter = new DMRPrinter(dmr, pw);
    dapprinter.print();
    pw.close();
    sw.close();

    sb = new StringBuilder(sw.toString());
    // Remove irrelevant dependencies
    regexpFilters(sb, new String[] {RE_ENDIAN}, false);
    String testresult = sb.toString();

    // Use the original DMR as the baseline
    String baselinecontent = document;

    if (props.prop_visual) {
      visual("Baseline", baselinecontent);
      visual("Output", testresult);
    }
    Assert.assertTrue("Files are different", same(getTitle(), baselinecontent, testresult));
    System.out.println(tc.name + ": Passed");
  }

}
