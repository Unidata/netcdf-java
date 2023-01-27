package dap4.test;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.httpservices.HTTPException;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.UnitTestCommon;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Test OpenDap Server at the NetcdfDataset level
 */
public class TestDAP4Client extends DapTestCommon {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static final boolean DEBUG = false;

  static final boolean BROKEN = false; // on/off known broken tests

  static final boolean BUILDBASELINE = false;

  static final boolean NCDUMP = true; // Use NcDumpW instead of NCPrint

  static final String EXTENSION = (NCDUMP ? "ncdump" : "dmp");

  static final String TESTEXTENSION = "dmr";

  // Mnemonic
  static final boolean HEADERONLY = false;

  //////////////////////////////////////////////////
  // Constants

  static final String SERVLETPATH = "d4ts/testfiles";

  static final String DATADIR = "src/test/data"; // relative to dap4 root
  static final String BASELINEDIR = "TestDAP4Client/baseline";

  // Define the names of the xfail tests
  static final String[] XFAIL_TESTS = {};

  static boolean isXfailTest(String t) {
    for (String s : XFAIL_TESTS) {
      if (s.equals(t))
        return true;
    }
    return false;
  }

  //////////////////////////////////////////////////
  // Type Declarations

  static class Server {
    public static String SERVLET = "dts";
    public String ip;
    public String port;

    public Server(String ip, String port) {
      this.ip = ip;
      this.port = port;
    }

    public String getURL() {
      StringBuilder buf = new StringBuilder();
      buf.append("http://");
      buf.append(this.ip);
      if (port != null) {
        buf.append(":");
        buf.append(this.port);
      }
      return buf.toString();
    }

    // Return a URL for testing if server is up/down
    public String getTestURL() {
      StringBuilder baseurl = new StringBuilder().append(getURL());
      baseurl.append("/");
      baseurl.append(SERVLET);
      return baseurl.toString();
    }

  }

  static class ClientTest {
    static String root = null;
    static String server = null;
    static String servlet = null;
    static int counter = 0;

    boolean checksumming = true;
    boolean xfail = false;
    boolean headeronly = false;

    String title;
    String dataset; // path minus the server url part.
    String baselinepath;
    String constraint;
    int id;

    ClientTest(String dataset) {
      this(0, dataset, null);
    }

    ClientTest(int id, String datasetpath, String constraint) {
      if (constraint == null)
        constraint = "";
      // Break off the final file set name
      int index = datasetpath.lastIndexOf('/');
      this.dataset = datasetpath.substring(index + 1, datasetpath.length());
      this.title = this.dataset;
      this.id = id;
      this.constraint = (constraint.length() == 0 ? null : constraint);
      this.baselinepath = root + "/" + BASELINEDIR + "/" + dataset;
      if (this.constraint != null)
        this.baselinepath += ("." + String.valueOf(this.id));
    }

    public ClientTest nochecksum() {
      this.checksumming = false;
      return this;
    }

    public ClientTest xfail() {
      this.xfail = true;
      return this;
    }

    public ClientTest headeronly() {
      this.headeronly = true;
      return this;
    }

    String makeurl() {
      String url = this.server + "/" + this.servlet + "/" + this.dataset;
      if (constraint != null)
        url += ("?" + constraint);
      url += "#dap4";
      return url;
    }

    public String toString() {
      return dataset;
    }
  }

  //////////////////////////////////////////////////
  // Class variables

  // Order is important; testing reachability is in the order listed
  static List<Server> SERVERS;

  static {
    SERVERS = new ArrayList<>();
    SERVERS.add(new Server("149.165.169.123", "8080"));
    SERVERS.add(new Server("remotetest.unidata.ucar.edu", null));
  }

  //////////////////////////////////////////////////
  // Instance variables

  // Test cases

  List<ClientTest> alltestcases = new ArrayList<ClientTest>();
  List<ClientTest> chosentests = new ArrayList<ClientTest>();

  String datasetpath = null;

  Server server = null;

  //////////////////////////////////////////////////

  @Before
  public void setup() throws Exception {
    // Find the server to use
    this.server = null;
    for (Server svc : SERVERS) {
      String url = svc.getTestURL();
      try (HTTPMethod method = HTTPFactory.Get(url)) {
        try {
          int code = method.execute();
          if (code == 200) {
            this.server = svc;
            System.out.println("Using server url " + url);
            break;
          }
        } catch (HTTPException e) {
          this.server = null;
        }
      }
    }
    if (this.server == null)
      throw new Exception("Cannot locate server");
    defineAllTestcases(this.getResourceDir(), SERVLETPATH, this.server.getURL());
    chooseTestcases();
    if (BUILDBASELINE)
      prop_baseline = true;
  }

  //////////////////////////////////////////////////
  // Define test cases

  void chooseTestcases() {
    if (false) {
      chosentests = locate("test_atomic_types.nc");
      prop_baseline = true;
    } else {
      for (ClientTest tc : alltestcases) {
        chosentests.add(tc);
      }
    }
  }

  boolean defineAllTestcases(String root, String servlet, String server) {
    boolean what = HEADERONLY;
    ClientTest.root = root;
    ClientTest.server = server;
    ClientTest.servlet = servlet;
    alltestcases.add(new ClientTest("test_atomic_array.nc"));
    alltestcases.add(new ClientTest("test_atomic_types.nc"));
    alltestcases.add(new ClientTest("test_enum.nc"));
    alltestcases.add(new ClientTest("test_enum_2.nc"));
    alltestcases.add(new ClientTest("test_enum_array.nc"));
    alltestcases.add(new ClientTest("test_enum1.nc"));
    alltestcases.add(new ClientTest("test_fill.nc"));
    alltestcases.add(new ClientTest("test_groups1.nc"));
    if (BROKEN)
      alltestcases.add(new ClientTest("test_misc1.nc")); // 0 size unlimited
    if (BROKEN)
      alltestcases.add(new ClientTest("test_one_var.nc")); // 0 size unlimited
    alltestcases.add(new ClientTest("test_one_vararray.nc"));
    alltestcases.add(new ClientTest("test_opaque.nc"));
    alltestcases.add(new ClientTest("test_opaque_array.nc"));
    alltestcases.add(new ClientTest("test_struct_array.nc"));
    alltestcases.add(new ClientTest("test_struct_nested.nc"));
    alltestcases.add(new ClientTest("test_struct_nested3.nc"));
    alltestcases.add(new ClientTest("test_struct_type.nc"));
    alltestcases.add(new ClientTest("test_struct1.nc"));
    alltestcases.add(new ClientTest("test_test.nc"));
    if (BROKEN)
      alltestcases.add(new ClientTest("test_unlim.nc")); // ?
    if (BROKEN)
      alltestcases.add(new ClientTest("test_unlim1.nc")); // ?
    if (BROKEN)
      alltestcases.add(new ClientTest("test_utf8.nc")); // ?
    alltestcases.add(new ClientTest("test_vlen1.nc"));
    if (BROKEN)
      alltestcases.add(new ClientTest("test_vlen2.nc")); // non scalar vlen
    if (BROKEN)
      alltestcases.add(new ClientTest("test_vlen3.nc")); // non scalar vlen
    if (BROKEN)
      alltestcases.add(new ClientTest("test_vlen4.nc")); // non scalar vlen
    alltestcases.add(new ClientTest("test_vlen5.nc"));
    if (BROKEN)
      alltestcases.add(new ClientTest("test_vlen6.nc")); // non-scalar vlen
    if (BROKEN)
      alltestcases.add(new ClientTest("test_vlen7.nc")); // non-scalar vlen
    if (BROKEN)
      alltestcases.add(new ClientTest("test_vlen8.nc")); // non-scalar vlen
    if (BROKEN)
      alltestcases.add(new ClientTest("test_vlen9.nc")); // non-scalar
    if (BROKEN)
      alltestcases.add(new ClientTest("test_vlen10.nc")); // non-scalar
    if (BROKEN)
      alltestcases.add(new ClientTest("test_vlen11.nc")); // unknown failure
    if (BROKEN)
      alltestcases.add(new ClientTest("test_zerodim.nc")); // non-scalar seq
    alltestcases.add(new ClientTest("tst_fills.nc"));
    for (ClientTest test : alltestcases) {
      if (what == HEADERONLY)
        test.headeronly();
    }
    return true;
  }

  //////////////////////////////////////////////////
  // Junit test method

  @Test
  public void testDAP4Client() throws Exception {
    boolean pass = true;
    for (ClientTest testcase : chosentests) {
      if (!doOneTest(testcase))
        pass = false;
    }
    Assert.assertTrue("*** Fail: TestDAP4Client", pass);
  }

  //////////////////////////////////////////////////
  // Primary test method

  boolean doOneTest(ClientTest testcase) throws Exception {
    boolean pass = true;
    System.out.println("Testcase: " + testcase.dataset);
    String url = testcase.makeurl();
    NetcdfDataset ncfile = null;
    try {
      ncfile = openDataset(url);
    } catch (Exception e) {
      System.err.println(testcase.xfail ? "XFail" : "Fail");
      e.printStackTrace();
      return testcase.xfail;
    }
    String usethisname = UnitTestCommon.extractDatasetname(url, null);
    String metadata = (NCDUMP ? ncdumpmetadata(ncfile, usethisname) : null);
    if (prop_visual) {
      visual(testcase.title + ".dmr", metadata);
    }

    String data = null;
    if (!testcase.headeronly) {
      data = (NCDUMP ? ncdumpdata(ncfile, usethisname) : null);
      if (prop_visual) {
        visual(testcase.title + ".dap", data);
      }
    }

    String testoutput = (testcase.headeronly ? metadata : (NCDUMP ? data : metadata + data));

    String baselinefile = testcase.baselinepath + "." + EXTENSION;

    if (prop_baseline)
      writefile(baselinefile, testoutput);

    if (prop_diff) { // compare with baseline
      // Read the baseline file(s)
      String baselinecontent = readfile(baselinefile);
      System.out.println("Comparison: vs " + baselinefile);
      pass = pass && same(getTitle(), baselinecontent, testoutput);
      System.out.println(pass ? "Pass" : "Fail");
    }
    return pass;
  }


  String ncdumpmetadata(NetcdfDataset ncfile, String datasetname) throws Exception {
    StringWriter sw = new StringWriter();

    StringBuilder args = new StringBuilder("-strict");
    if (datasetname != null) {
      args.append(" -datasetname ");
      args.append(datasetname);
    }
    // Print the meta-databuffer using these args to NcdumpW
    try {
      if (!ucar.nc2.NCdumpW.print(ncfile, args.toString(), sw, null))
        throw new Exception("NcdumpW failed");
    } catch (IOException ioe) {
      throw new Exception("NcdumpW failed", ioe);
    }
    sw.close();
    return sw.toString();
  }

  String ncdumpdata(NetcdfDataset ncfile, String datasetname) throws Exception {
    StringWriter sw = new StringWriter();

    StringBuilder args = new StringBuilder("-strict -vall");
    if (datasetname != null) {
      args.append(" -datasetname ");
      args.append(datasetname);
    }

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

  //////////////////////////////////////////////////
  // Utility methods


  // Locate the test cases with given prefix
  List<ClientTest> locate(String prefix) {
    List<ClientTest> results = new ArrayList<ClientTest>();
    for (ClientTest ct : this.alltestcases) {
      if (!ct.dataset.startsWith(prefix))
        continue;
      results.add(ct);
    }
    return results;
  }

  static boolean report(String msg) {
    System.err.println(msg);
    return false;
  }

} // class TestDAP4Client

