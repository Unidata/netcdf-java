/* Copyright Unidata */
package ucar.nc2.iosp.hdf5;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import ucar.ma2.DataType;
import ucar.nc2.*;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.UnitTestCommon;

/** Test handling of enums in hdf5 / netcdf 4 files. */
// see Issue #126

/**
 * This Test uses the JUNIT Version 4 parameterized test mechanism.
 * The set of arguments for each test is encapsulated in a class
 * called TestCase. This allows for code re-use and for extending
 * tests by adding fields to the TestCase object.
 */

@RunWith(Parameterized.class)
public class TestEnumTypedef extends UnitTestCommon {

  //////////////////////////////////////////////////
  // Static Fields

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  //////////////////////////////////////////////////
  // Test Case Class

  // Encapulate the arguments for each test
  static class TestCase {
    public String file;
    public String fqn;
    public String enum_t;
    public DataType basetype;
    public String input;
    public String baseline;

    public TestCase(String file, String fqn, String enum_t, DataType basetype, String input, String baseline) {
      this.file = file;
      this.fqn = fqn;
      this.enum_t = enum_t;
      this.basetype = basetype;
      this.input = input;
      this.baseline = baseline;
    }

    // This defines how the test is reported by JUNIT.
    public String toString() {
      return this.file;
    }
  }

  //////////////////////////////////////////////////
  // Test Generator

  @Parameterized.Parameters(name = "{index}: {0}")
  static public List<TestCase> defineTestCases() {
    List<TestCase> testcases = new ArrayList<>();
    TestCase tc;
    String file;
    String fqn;
    String enum_t;
    DataType basetype;
    String input;
    String baseline;

    // Test case where:
    // * enum type is in same group to the variable using it
    // * Explicit enum type
    // * Anonymous variable enum type
    file = "test_atomic_types.nc";
    fqn = "/primary_cloud";
    enum_t = "cloud_class_t";
    basetype = DataType.ENUM1;
    input = TestDir.cdmLocalTestDataDir + "hdf5/" + file;
    baseline = null;
    tc = new TestCase(file, fqn, enum_t, basetype, input, baseline);
    // testcases.add(tc);

    // Test case where enum type is in a parent group to the variable using it.
    // Test case where:
    // * enum type is in parent group to the variable using it
    // * Explicit enum type
    // * Anonymous variable enum type
    // * Anonymous attribute enum type
    file = "test_enum_2.nc4";
    fqn = "/h/primary_cloud";
    enum_t = "cloud_class_t";
    basetype = DataType.ENUM1;
    input = TestDir.cdmLocalTestDataDir + "hdf5/" + file;
    baseline = null;
    tc = new TestCase(file, fqn, enum_t, basetype, input, baseline);
    testcases.add(tc);

    // Test case where enum type is anonymous
    // Test case where:
    // * enum type is in same group to the variable using it
    // * Explicit enum type
    // * Anonymous variable enum type
    file = "ref_anon_enum.h5";
    fqn = "/EnumTest";
    enum_t = "EnumTest_enum_t";
    basetype = DataType.ENUM4;
    input = TestDir.cdmLocalTestDataDir + "hdf5/" + file;
    baseline = "netcdf ref_anon_enum.h5 {\n" + "types:\n"
        + "enum EnumTest_enum_t { RED = 0, GREEN = 1, BLUE = 2, WHITE = 3, BLACK = 4};\n" + "variables:\n"
        + "enum EnumTest_enum_t EnumTest(10);\n" + "data:\n" + "EnumTest =\n" + "{0, 1, 2, 3, 4, 0, 1, 2, 3, 4}\n"
        + "\n}";
    tc = new TestCase(file, fqn, enum_t, basetype, input, baseline);
    // testcases.add(tc);

    // Test case where enum type exists
    // Test case where:
    // * enum type is in same group to the variable using it
    // * Explicit enum type
    // * Anonymous variable enum type
    file = "test_enum_type.nc";
    fqn = "/primary_cloud";
    enum_t = "cloud_class_t";
    basetype = DataType.ENUM1;
    input = TestDir.cdmLocalTestDataDir + "hdf5/" + file;
    baseline = null;
    tc = new TestCase(file, fqn, enum_t, basetype, input, baseline);
    // testcases.add(tc);

    return testcases;
  }

  //////////////////////////////////////////////////
  // Test Fields

  TestCase tc;

  //////////////////////////////////////////////////
  // Constructor(s)

  public TestEnumTypedef(TestCase tc) {
    super();
    this.tc = (TestCase) tc;
  }

  //////////////////////////////////////////////////
  // Junit test method(s)

  // Test case where enum type is in same group to the variable using it.
  @Test
  public void test() throws Exception {
    logger.info("TestEnumTypedef on {}%n", tc.input);
    try (NetcdfFile ncfile = NetcdfFiles.open(tc.input)) {
      String testresult = dumpdata(ncfile, tc.file);
      Variable var = getvar(ncfile, tc.fqn);
      Assert.assertNotNull((Object) var);
      Assert.assertTrue(var.getDataType().isEnum());
      Assert.assertTrue(var.getDataType() == tc.basetype);
      Assert.assertNotNull(var.getEnumTypedef());
      EnumTypedef typedef = var.getEnumTypedef();
      Assert.assertNotNull(typedef);
      logger.info("TestEnumTypedef typedef name {}%n", typedef.getShortName());
      Assert.assertTrue(typedef.getShortName().equals(tc.enum_t));
      if (tc.baseline != null) {
        Assert.assertTrue(compare(tc.file, ncfile, tc.baseline, testresult));
      }
    }
  }

  // Support functions

  protected boolean compare(String file, NetcdfFile ncfile, String baseline, String testresult) throws Exception {
    String diffs = UnitTestCommon.compare(file, baseline, testresult);
    if (diffs != null) {
      System.err.println(String.format("Test %s failed:\n%s", file, diffs));
    }
    return diffs == null;
  }

  protected Variable getvar(NetcdfFile ncfile, String fqn) {
    Variable v = null;
    v = ncfile.findVariable(fqn);
    return v;
  }

  protected String dumpdata(NetcdfFile ncfile, String datasetname) throws Exception {
    StringBuilder args = new StringBuilder("-strict -vall");
    if (datasetname != null) {
      args.append(" -datasetname ");
      args.append(datasetname);
    }
    StringWriter sw = new StringWriter();
    // Dump the databuffer
    sw = new StringWriter();
    try {
      if (!ucar.nc2.NCdumpW.print(ncfile, args.toString(), sw, null))
        throw new Exception("NCdumpW failed");
    } catch (IOException ioe) {
      ioe.printStackTrace();
      throw new Exception("NCdumpW failed", ioe);
    }
    sw.close();
    return sw.toString();
  }

}

