/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.test;

import dap4.core.util.DapConstants;
import dap4.core.util.DapException;
import dap4.core.util.DapUtil;
import dap4.dap4lib.HttpDSP;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.write.Ncdump;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.UnitTestCommon;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract public class DapTestCommon extends UnitTestCommon {

  //////////////////////////////////////////////////
  // Constants

  static final String DEFAULTTREEROOT = "dap4";

  static public final String FILESERVER = "file://localhost:8080";

  static public final String ORDERTAG = "ucar.littleendian";
  static public final String TRANSLATETAG = "ucar.translate";
  static public final String TESTTAG = "ucar.testing";

  static final String D4TESTDIRNAME = "";

  // Equivalent to the path to the webapp/d4ts for testing purposes
  static protected final String DFALTRESOURCEPATH = "/src/test/data/resources";

  static protected final String[] LEGALEXTENSIONS = {".dap", ".dmr", ".nc", "dmp", ".ncdump", ".dds", ".das", ".dods"};

  //////////////////////////////////////////////////

  static class TestFilter implements FileFilter {
    boolean debug;
    boolean strip;
    String[] extensions;

    public TestFilter(boolean debug, String[] extensions) {
      this.debug = debug;
      this.strip = strip;
      this.extensions = extensions;
    }

    public boolean accept(File file) {
      boolean ok = false;
      if (file.isFile() && file.canRead()) {
        // Check for proper extension
        String name = file.getName();
        if (name != null) {
          for (String ext : extensions) {
            if (name.endsWith(ext))
              ok = true;
          }
        }
        if (!ok && debug)
          System.err.println("Ignoring: " + file.toString());
      }
      return ok;
    }

    static void filterfiles(String path, List<String> matches, String... extensions) {
      File testdirf = new File(path);
      assert (testdirf.canRead());
      TestFilter tf = new TestFilter(DEBUG, extensions);
      File[] filelist = testdirf.listFiles(tf);
      for (int i = 0; i < filelist.length; i++) {
        File file = filelist[i];
        if (file.isDirectory())
          continue;
        String fname = DapUtil.canonicalpath(file.getAbsolutePath());
        matches.add(fname);
      }
    }
  }

  // Test properties
  static class TestProperties {
    boolean prop_diff; // Do comparison with baseline files
    boolean prop_baseline; // Generate the baseline files
    boolean prop_visual; // Debug output: display the test output and the baseline output
    boolean prop_debug; // General debug flag
    boolean prop_http_debug; // Print request and response headers

    public TestProperties() {
      prop_diff = true;
      prop_baseline = false;
      prop_visual = false;
      prop_debug = DEBUG;
      prop_http_debug = false;
    }
  }

  static public class TestCaseCommon {
    public String name;

    public TestCaseCommon() {
      this(null);
    }

    public TestCaseCommon(String name) {
      this.name = name;
    }
  }

  //////////////////////////////////////////////////
  // Static variables

  static protected String dap4root = null;
  static protected String dap4testroot = null;
  static protected String dap4resourcedir = null;

  static protected TestProperties props = null;

  static {
    dap4root = locateDAP4Root(threddsroot);
    if (dap4root == null)
      System.err.println("Cannot locate /dap4 parent dir");
    dap4testroot = canonjoin(dap4root, D4TESTDIRNAME);
    dap4resourcedir = canonjoin(dap4testroot, DFALTRESOURCEPATH);
    props = new TestProperties();
  }

  //////////////////////////////////////////////////
  // Static methods

  static protected String getD4TestsRoot() {
    return dap4testroot;
  }

  static protected String getResourceRoot() {
    return dap4resourcedir;
  }

  static String locateDAP4Root(String threddsroot) {
    String root = threddsroot;
    if (root != null)
      root = root + "/" + DEFAULTTREEROOT;
    // See if it exists
    File f = new File(root);
    if (!f.exists() || !f.isDirectory())
      root = null;
    return root;
  }

  //////////////////////////////////////////////////
  // Instance variables

  protected String d4tsserver = null;

  protected String title = "Dap4 Testing";

  public DapTestCommon() {
    this("DapTest");
  }

  public DapTestCommon(String name) {
    super(name);
    setSystemProperties();
    this.d4tsserver = TestDir.dap4TestServer;
    if (DEBUG)
      System.err.println("DapTestCommon: d4tsServer=" + d4tsserver);
  }

  /**
   * Try to get the system properties
   */
  protected void setSystemProperties() {
    if (System.getProperty("nodiff") != null)
      props.prop_diff = false;
    if (System.getProperty("baseline") != null)
      props.prop_baseline = true;
    if (System.getProperty("debug") != null)
      props.prop_debug = true;
    if (System.getProperty("visual") != null)
      props.prop_visual = true;
    if (props.prop_baseline && props.prop_diff)
      props.prop_diff = false;
  }

  public void setup() {
    if (props.prop_http_debug)
      HttpDSP.setHttpDebug();
  }

  //////////////////////////////////////////////////
  // Accessor
  public void setTitle(String title) {
    this.title = title;
  }

  public String getTitle() {
    return this.title;
  }

  //////////////////////////////////////////////////
  // Instance Utilities

  public void visual(String header, String captured) {
    if (!captured.endsWith("\n"))
      captured = captured + "\n";
    // Dump the output for visual comparison
    System.err.println("\n" + header + ":");
    System.err.println("---------------");
    System.err.print(captured);
    System.err.println("---------------");
    System.err.flush();
  }

  protected void findServer(String path) throws DapException {
    String svc = DapConstants.HTTPSCHEME + "//" + this.d4tsserver + "/d4ts";
    if (!checkServer(svc))
      System.err.println("D4TS Server not reachable: " + svc);
    // Since we will be accessing it thru NetcdfDataset, we need to change the schema.
    d4tsserver = "dap4://" + d4tsserver + "/d4ts";
  }

  //////////////////////////////////////////////////

  public String getDAP4Root() {
    return this.dap4root;
  }

  @Override
  public String getResourceDir() {
    return this.dap4resourcedir;
  }

  static void printDir(String path) {
    File testdirf = new File(path);
    assert (testdirf.canRead());
    File[] filelist = testdirf.listFiles();
    System.err.println("\n*******************");
    System.err.printf("Contents of %s:%n", path);
    for (int i = 0; i < filelist.length; i++) {
      File file = filelist[i];
      String fname = file.getName();
      System.err.printf("\t%s%s%n", fname, (file.isDirectory() ? "/" : ""));
    }
    System.err.println("*******************");
    System.err.flush();
  }

  //////////////////////////////////////////////////
  // Filename processing utilities

  /**
   * Given a List of file names, return a list of those names except
   * for any in the array of filenames to be excluded.
   *
   * @param manifest The list of file names
   * @param exclusions The array of excluded names
   * @return manifest with excluded names removed
   */
  static public String[][] excludeNames(String[][] manifest, String[] exclusions) {
    List<String[]> xlist = new ArrayList<>(manifest.length);
    for (int i = 0; i < manifest.length; i++) {
      String name = manifest[i][0]; // Assume tuple element 0 is always the name
      boolean matched = false;
      for (String excluded : exclusions) {
        if (excluded.equals(name))
          matched = true;
      }
      if (!matched)
        xlist.add(manifest[i]);
    }
    return xlist.toArray(new String[0][]);
  }

  // Filter a document with respect to a set of regular expressions
  // Used to remove e.g. unwanted attributes
  static public boolean regexpFilter(StringBuilder document, String regexp, boolean repeat) {
    boolean changed = false;
    String doc = document.toString();
    Pattern p = Pattern.compile(regexp, Pattern.DOTALL);
    Matcher m = p.matcher(doc);
    if (m.find()) {
      changed = true;
      String newdoc = (repeat ? m.replaceAll("") : m.replaceFirst(""));
      doc = newdoc;
      document.setLength(0);
      document.append(doc);
    }
    return changed;
  }

  static public boolean regexpFilters(StringBuilder sbdoc, String[] regexps, boolean repeat) {
    boolean changed = false;
    for (String re : regexps) {
      if (regexpFilter(sbdoc, re, repeat))
        changed = true;
      else
        break;
    }
    return changed;
  }

  // Filter a document with respect to a set of regular expressions
  // on a per-line basis
  static public boolean regexpFilterLine(StringBuilder document, String regexp, boolean repeat) {
    boolean changed = false;
    Pattern p = Pattern.compile(regexp, Pattern.DOTALL);
    String[] lines = document.toString().split("\r?\n");
    document.setLength(0);
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      Matcher m = p.matcher(line);
      if (!m.find()) {
        document.append(line);
        document.append("\n");
        changed = true;
        if (!repeat)
          break;
      }
    }
    return changed;
  }


  /**
   * Choose a test case based on its name and return its index
   *
   * @param name to search for
   * @param testcases set of testcases to search
   */
  static void singleTest(String name, List<TestCaseCommon> testcases) {
    for (int i = 0; i < testcases.size(); i++) {
      TestCaseCommon tc = testcases.get(i);
      if (tc.name.equalsIgnoreCase(name)) {
        testcases.clear();
        testcases.add(tc);
      }
    }
    return;
  }

  /**
   * Choose a test case based on its name and return its index
   *
   * @param index to search for
   * @param testcases set of testcases to search
   */
  static void singleTest(int index, List<TestCaseCommon> testcases) {
    TestCaseCommon tc = testcases.get(index);
    testcases.clear();
    testcases.add(tc);
    return;
  }

  protected String dumpmetadata(NetcdfDataset ncfile, String datasetname) throws Exception {
    StringWriter sw = new StringWriter();
    StringBuilder args = new StringBuilder("-strict");
    if (datasetname != null) {
      args.append(" -datasetname ");
      args.append(datasetname);
    }
    // Print the meta-databuffer using these args to NcdumpW
    try {
      Ncdump.ncdump(ncfile, args.toString(), sw, null);
      // if (!ucar.nc2.NCdumpW.print(ncfile, args.toString(), sw, null))
      // throw new Exception("NcdumpW failed");
    } catch (IOException ioe) {
      throw new Exception("Ncdump failed", ioe);
    }
    sw.close();
    return sw.toString();
  }

  protected String dumpdata(NetcdfDataset ncfile, String datasetname) throws Exception {
    StringBuilder args = new StringBuilder("-strict -vall");
    if (datasetname != null) {
      args.append(" -datasetname ");
      args.append(datasetname);
    }
    StringWriter sw = new StringWriter();
    // Dump the databuffer
    try {
      Ncdump.ncdump(ncfile, args.toString(), sw, null);
      // if (!ucar.nc2.NCdumpW.print(ncfile, args.toString(), sw, null))
      // throw new Exception("NCdumpW failed");
    } catch (IOException ioe) {
      ioe.printStackTrace();
      throw new Exception("NCdump failed", ioe);
    }
    sw.close();
    return sw.toString();
  }


}
