/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.test;

import dap4.dap4lib.HttpDSP;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.write.Ncdump;
import ucar.unidata.util.test.UnitTestCommon;

import java.io.File;
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

  static final String D4TESTDIRNAME = "";

  // Equivalent to the path to the webapp/d4ts for testing purposes
  protected static final String DFALTRESOURCEPATH = "/src/test/data/resources";

  //////////////////////////////////////////////////

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

  public static class TestCaseCommon {
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

  protected static String dap4root = null;
  protected static String dap4testroot = null;
  protected static String dap4resourcedir = null;

  protected static TestProperties props = null;

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

  protected static String getResourceRoot() {
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

  protected String title = "Dap4 Testing";

  public DapTestCommon() {
    this("DapTest");
  }

  public DapTestCommon(String name) {
    super(name);
    setSystemProperties();
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

  //////////////////////////////////////////////////

  @Override
  public String getResourceDir() {
    return this.dap4resourcedir;
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
  public static String[][] excludeNames(String[][] manifest, String[] exclusions) {
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
  public static boolean regexpFilter(StringBuilder document, String regexp, boolean repeat) {
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

  public static boolean regexpFilters(StringBuilder sbdoc, String[] regexps, boolean repeat) {
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
  public static boolean regexpFilterLine(StringBuilder document, String regexp, boolean repeat) {
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
