/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package opendap.test;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.io.LineReader;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Formatter;
import ucar.unidata.util.test.TestDir;

public abstract class UnitTestCommon {
  static final boolean DEBUG = false;

  // Look for these to verify we have found the thredds root
  static final String[] DEFAULTSUBDIRS = {"httpservices", "cdm", "opendap"};

  static String threddsroot;
  static String threddsServer;

  static {
    // Compute the root path
    threddsroot = locateThreddsRoot();
    assert threddsroot != null : "Cannot locate /thredds parent dir";
    threddsServer = TestDir.remoteTestServer;
    if (DEBUG)
      System.err.println("UnitTestCommon: threddsServer=" + threddsServer);
  }

  // Walk around the directory structure to locate
  // the path to the thredds root (which may not
  // be names "thredds").
  // Same as code in UnitTestCommon, but for
  // some reason, Intellij will not let me import it.
  static String locateThreddsRoot() {
    // Walk up the user.dir path looking for a node that has
    // all the directories in SUBROOTS.

    // It appears that under Jenkins, the Java property "user.dir" is
    // set incorrectly for our purposes. In this case, we want
    // to use the WORKSPACE environment variable set by Jenkins.
    String workspace = System.getenv("WORKSPACE");
    System.err.println("WORKSPACE=" + (workspace == null ? "null" : workspace));
    System.err.flush();

    String userdir = System.getProperty("user.dir");

    String path = (workspace != null ? workspace : userdir); // Pick one

    // clean up the path
    path = path.replace('\\', '/'); // only use forward slash
    if (path.endsWith("/"))
      path = path.substring(0, path.length() - 1);

    File prefix = new File(path);
    for (; prefix != null; prefix = prefix.getParentFile()) {// walk up the tree
      int found = 0;
      String[] subdirs = prefix.list();
      assertThat(subdirs).isNotNull();
      for (String dirname : subdirs) {
        for (String want : DEFAULTSUBDIRS) {
          if (dirname.equals(want)) {
            found++;
            break;
          }
        }
      }
      if (found == DEFAULTSUBDIRS.length) {
        try {// Assume this is it
          String root = prefix.getCanonicalPath();
          // clean up the root path
          root = root.replace('\\', '/'); // only use forward slash
          return root;
        } catch (IOException ioe) {
        }
      }
    }
    return null;
  }

  public static void clearDir(File dir, boolean clearsubdirs) {
    // wipe out the dir contents
    if (!dir.exists())
      return;
    for (File f : dir.listFiles()) {
      if (f.isDirectory()) {
        if (clearsubdirs) {
          clearDir(f, true); // clear subdirs
          f.delete();
        }
      } else
        f.delete();
    }
  }

  //////////////////////////////////////////////////
  // Instance variables

  // System properties
  protected boolean prop_ascii = true;
  protected boolean prop_diff = true;
  protected boolean prop_baseline = false;
  protected boolean prop_visual = false;
  protected boolean prop_debug = DEBUG;
  protected boolean prop_generate = true;
  protected String prop_controls = null;
  protected boolean prop_display = false;

  protected String title = "Testing";
  protected String name = "testcommon";

  public UnitTestCommon() {
    this("Testing");
  }

  public UnitTestCommon(String name) {
    this.title = name;
    setSystemProperties();
  }

  protected void setSystemProperties() {
    if (System.getProperty("nodiff") != null)
      prop_diff = false;
    if (System.getProperty("baseline") != null)
      prop_baseline = true;
    if (System.getProperty("nogenerate") != null)
      prop_generate = false;
    if (System.getProperty("debug") != null)
      prop_debug = true;
    if (System.getProperty("visual") != null)
      prop_visual = true;
    if (System.getProperty("ascii") != null)
      prop_ascii = true;
    if (System.getProperty("utf8") != null)
      prop_ascii = false;
    if (System.getProperty("hasdisplay") != null)
      prop_display = true;
    if (prop_baseline && prop_diff)
      prop_diff = false;
    prop_controls = System.getProperty("controls", "");
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTitle() {
    return this.title;
  }

  public String getThreddsroot() {
    return this.threddsroot;
  }

  public String getName() {
    return this.name;
  }

  public static String readfile(String filename) throws IOException {
    StringBuilder buf = new StringBuilder();
    Path file = Paths.get(filename);
    try (BufferedReader rdr = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      String line;
      while ((line = rdr.readLine()) != null) {
        if (line.startsWith("#"))
          continue;
        buf.append(line + "\n");
      }
      return buf.toString();
    }
  }

  // Collect testcases locally
  static public class Testcase {
    String title;
    String url;
    String cdl;

    public Testcase(String title, String url, String cdl) {
      this.title = title;
      this.url = url;
      this.cdl = cdl;
    }
  }

  // standardize line endings
  protected String baseline(Testcase testcase) throws IOException {
    File file;
    if (testcase.cdl.startsWith("file://")) {
      file = new File(testcase.cdl.substring("file://".length()));
    } else {
      return testcase.cdl;
    }

    Formatter result = new Formatter();
    String line;
    try (FileReader reader = new FileReader(file)) {
      LineReader lineReader = new LineReader(reader);
      while (null != (line = lineReader.readLine())) {
        result.format("%s%n", line);
      }
    }
    return result.toString();
  }
}
