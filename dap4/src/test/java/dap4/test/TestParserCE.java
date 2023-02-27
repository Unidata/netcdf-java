/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.test;

import dap4.core.ce.CECompiler;
import dap4.core.ce.CEConstraint;
import dap4.core.ce.parser.CEParserImpl;
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
import java.util.regex.Pattern;

/**
 * This Test uses the JUNIT Version 4 parameterized test mechanism.
 * The set of arguments for each test is encapsulated in a class
 * called TestCase. This allows for code re-use and for extending
 * tests by adding fields to the TestCase object.
 */

/**
 * This tests the Constraint Expression (CE) Parser;
 * it is completely self-contained.
 */

@RunWith(Parameterized.class)
public class TestParserCE extends DapTestCommon implements Dap4ManifestIF {

  //////////////////////////////////////////////////
  // Constants

  static final boolean DMRPARSEDEBUG = false;
  static final boolean CEPARSEDEBUG = false;

  // DMR Constants
  static final String CE1_DMR = "<Dataset\n" + "    name=\"ce1\"\n" + "    dapVersion=\"4.0\"\n"
      + "    dmrVersion=\"1.0\"\n" + "    ns=\"http://xml.opendap.org/ns/DAP/4.0#\">\n"
      + "  <Dimension name=\"d10\" size=\"10\"/>\n" + "  <Dimension name=\"d17\" size=\"17\"/>\n"
      + "  <Int32 name=\"a\">\n" + "    <Dim name=\"/d17\"/>\n" + "  </Int32>\n" + "  <Int32 name=\"b\">\n"
      + "    <Dim name=\"/d17\"/>\n" + "  </Int32>\n" + "  <Int32 name=\"c\">\n" + "    <Dim name=\"/d17\"/>\n"
      + "  </Int32>\n" + "  <Int32 name=\"d\">\n" + "    <Dim name=\"/d10\"/>\n" + "    <Dim name=\"/d17\"/>\n"
      + "  </Int32>\n" + "  <Int32 name=\"e\">\n" + "    <Dim name=\"/d10\"/>\n" + "    <Dim name=\"/d17\"/>\n"
      + "  </Int32>\n" + "  <Int32 name=\"f\">\n" + "    <Dim name=\"/d10\"/>\n" + "    <Dim name=\"/d17\"/>\n"
      + "  </Int32>\n" + "  <Structure name=\"s\">\n" + "    <Int32 name=\"x\"/>\n" + "    <Int32 name=\"y\"/>\n"
      + "    <Dim name=\"/d10\"/>\n" + "    <Dim name=\"/d10\"/>\n" + "  </Structure>\n" + "  <Sequence name=\"seq\">\n"
      + "    <Int32 name=\"i1\"/>\n" + "    <Int16 name=\"sh1\"/>\n" + "  </Sequence>\n" + "</Dataset>";

  static final String CE2_DMR = "<Dataset" + "    name=\"ce2\"" + "    dapVersion=\"4.0\"" + "    dmrVersion=\"1.0\""
      + "    ns=\"http://xml.opendap.org/ns/DAP/4.0#\">" + "  <Dimension name=\"d2\" size=\"2\"/>"
      + "  <Opaque name=\"vo\">" + "    <Dim name=\"/d2\"/>" + "    <Dim name=\"/d2\"/>" + "  </Opaque>" + "</Dataset>";

  static final String[][] testinputs =
      {{"/a[1]", null, CE1_DMR}, {"/b[10:16]", null, CE1_DMR}, {"/c[8:2:15]", null, CE1_DMR},
          {"/a[1];/b[10:16];/c[8:2:15]", null, CE1_DMR}, {"/d[1][0:2:2];/a[1];/e[1][0];/f[0][1]", null, CE1_DMR},
          {"/s[0:3][0:2].x;/s[0:3][0:2].y", "/s[0:3][0:2]", CE1_DMR}, {"/seq|i1<0", null, CE1_DMR},
          {"/seq|0<i1<10", "/seq|i1>0,i1<10", CE1_DMR}, {"vo[1:1][0,0]", "/vo[1][0,0]", CE2_DMR}};

  //////////////////////////////////////////////////
  // Static Fields

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  //////////////////////////////////////////////////
  // Test Case Class

  // Encapulate the arguments for each test
  static class TestCase extends TestCaseCommon {
    public String ce;
    public String expected; // null => same as ce
    public String dmr; // DMR against which the CE is defined

    public TestCase(String ce, String expected, String dmr) {
      super();
      this.ce = ce;
      this.expected = expected;
      this.dmr = dmr;
      if (this.expected == null)
        this.expected = ce;
    }

    // This defines how the test is reported by JUNIT.
    public String toString() {
      return this.ce;
    }
  }

  //////////////////////////////////////////////////
  // Test Generator

  @Parameterized.Parameters(name = "{index}: {0}")
  static public List<TestCaseCommon> defineTestCases() {
    List<TestCaseCommon> testcases = new ArrayList<>();
    for (String[] triple : testinputs) {
      TestCase tc = new TestCase(triple[0], triple[1], triple[2]);
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

  public TestParserCE(TestCaseCommon tc) {
    super();
    this.tc = (TestCase) tc;
  }

  //////////////////////////////////////////////////
  // Junit test method(s)

  @Before
  public void setup() {
    // Set any properties
    super.setup();
  }

  @Test
  public void test() throws Exception {
    int i, c;

    // Create the DMR tree
    Dap4Parser parser = new DOM4Parser(new DMRFactory());
    if (DMRPARSEDEBUG)
      parser.setDebugLevel(1);
    // Parse document
    Assert.assertTrue("DMR Parse failed", parser.parse(tc.dmr));
    Assert.assertNull("DMR Parser returned Error response", parser.getErrorResponse()); // Check result
    DapDataset dmr = parser.getDMR();
    Assert.assertNotNull("No DMR created", dmr);

    // Parse the constraint expression against the DMR
    CEParserImpl ceparser = new CEParserImpl(dmr);
    if (CEPARSEDEBUG)
      ceparser.setDebugLevel(1);
    Assert.assertTrue("CE Parse failed", ceparser.parse(tc.ce));
    CECompiler compiler = new CECompiler();
    CEConstraint ceroot = compiler.compile(dmr, ceparser.getCEAST());
    Assert.assertNotNull("No CEConstraint created", ceroot);

    // Dump the parsed CE for comparison purposes
    String cedump = ceroot.toConstraintString();
    if (props.prop_visual)
      visual("|" + tc.ce + "|", cedump);
    Assert.assertTrue("expected :: CE mismatch", same(getTitle(), tc.expected, cedump));
    System.out.println(tc.ce + ": Passed");
  }

}
