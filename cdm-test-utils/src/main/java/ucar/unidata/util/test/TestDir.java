/*
 * Copyright (c) 1998-2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.util.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Sequence;
import ucar.nc2.Variable;
import ucar.nc2.internal.util.AliasTranslator;
import ucar.unidata.io.RandomAccessFile;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.*;

import static com.google.common.truth.Truth.assertThat;

/**
 * Manage the test data directories and servers.
 *
 * @author caron
 * @since 3/23/12
 *
 *        <p>
 *        <table>
 *        <tr>
 *        <th colspan="3">-D Property Names
 *        <tr>
 *        <th>Static Variable
 *        <th>Property Name(s)
 *        <th>Description
 *        <tr>
 *        <td>testdataDirPropName
 *        <td>unidata.testdata.path
 *        <td>Property name for the path to the Unidata test data directory,
 *        e.g unidata.testdata.path=/share/testdata
 *        <tr>
 *        <td>remoteTestServerPropName
 *        <td>remotetest
 *        <td>Property name for the hostname of the C-library remote test server.
 *        </table>
 *        <p>
 *        <table>
 *        <tr>
 *        <th colspan="4">Computed Paths
 *        <tr>
 *        <th>Static Variable
 *        <th>Property Name(s) (-d)
 *        <th>Default Value
 *        <th>Description
 *        <tr>
 *        <td>cdmUnitTestDir
 *        <td>NA
 *        <td>NA
 *        <td>New test data directory. Do not put temporary files in here.
 *        Migrate all test data here eventually.
 *        <tr>
 *        <td>cdmLocalTestDataDir
 *        <td>NA
 *        <td>../cdm-core/src/test/data/
 *        <td>Level 1 test data directory (distributed with code and MAY be used in PR testing on GitHub).
 *        <tr>
 *        <td>remoteTestServer
 *        <td>remotetestserver
 *        <td>remotetest.unidata.ucar.edu
 *        <td>The hostname of the test server for doing C library remote tests.
 *        </table>
 *
 */
public class TestDir {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /** Property name for the path to the Unidata test data directory, e.g "unidata.testdata.path=/share/testdata". */
  private static final String testdataDirPropName = "unidata.testdata.path";

  /** Path to the Unidata test data. see "https://github.com/Unidata/thredds-test-data". */
  private static String testdataDir;

  /** Unidata test data for the CDM. testdataDir + CdmUnitTest. */
  public static String cdmUnitTestDir;

  /** The cdm-core local test data, use from any top level gradle subproject. */
  public static String cdmLocalTestDataDir = "../cdm-core/src/test/data/";

  /** The cdm-core local test data, use from any top level gradle subproject. */
  public static String bufrLocalTestDataDir = "../bufr/src/test/data/";

  /**
   * cdm-test data directory (distributed with code but can depend on data not in github (e.g. NcML files can reference
   * data not in github)
   */
  public static String cdmTestDataDir = "../cdm-test/src/test/data/";

  //////////////////////////////////////////////////////////////////////
  // Various Test Server machines
  //////////////////////////////////////////////////////////////////////

  // Remote Test server(s)
  private static String remoteTestServerPropName = "remotetestserver";
  public static String remoteTestServer = "localhost:8081";

  // DAP 2 Test server (for testing)
  public static String dap2TestServerPropName = "dts";
  public static String dap2TestServer = "localhost:8080";

  // DAP4 Test server (for testing)
  public static String dap4TestServerPropName = "d4ts";
  public static String dap4TestServer = "localhost:8080";

  static {
    testdataDir = System.getProperty(testdataDirPropName); // Check the system property.

    // Use default paths if needed.
    if (testdataDir == null) {
      testdataDir = "/share/testdata/";
      logger.warn("No '{}' property found; using default value '{}'.", testdataDirPropName, testdataDir);
    }

    // Make sure paths ends with a slash.
    testdataDir = testdataDir.replace('\\', '/'); // canonical
    if (!testdataDir.endsWith("/"))
      testdataDir += "/";

    cdmUnitTestDir = testdataDir + "cdmUnitTest/";

    File file = new File(cdmUnitTestDir);
    if (!file.exists() || !file.isDirectory()) {
      logger.warn("cdmUnitTest directory does not exist: {}", file.getAbsolutePath());
    }

    // Initialize various server values

    String rts = System.getProperty(remoteTestServerPropName);
    if (rts != null && rts.length() > 0)
      remoteTestServer = rts;

    String dts = System.getProperty(dap2TestServerPropName);
    if (dts != null && dts.length() > 0)
      dap2TestServer = dts;

    String d4ts = System.getProperty(dap4TestServerPropName);
    if (d4ts != null && d4ts.length() > 0)
      dap4TestServer = d4ts;

    AliasTranslator.addAlias("${cdmUnitTest}", cdmUnitTestDir);
  }

  public static NetcdfFile open(String filename) throws IOException {
    logger.debug("**** Open {}", filename);
    NetcdfFile ncfile = NetcdfFiles.open(filename, null);
    logger.debug("open {}", ncfile);

    return ncfile;
  }

  public static NetcdfFile openFileLocal(String filename) throws IOException {
    return open(TestDir.cdmLocalTestDataDir + filename);
  }

  public static long checkLeaks() {
    if (RandomAccessFile.getOpenFiles().size() > 0) {
      logger.debug("RandomAccessFile still open:");
      for (String filename : RandomAccessFile.getOpenFiles()) {
        logger.debug("  open= {}", filename);
      }
    } else {
      logger.debug("RandomAccessFile: no leaks");
    }

    logger.debug("RandomAccessFile: count open={}, max={}", RandomAccessFile.getOpenFileCount(),
        RandomAccessFile.getMaxOpenFileCount());
    return RandomAccessFile.getOpenFiles().size();
  }

  ////////////////////////////////////////////////

  // Calling routine passes in an action.
  public interface Act {
    /**
     * @param filename file to act on
     * @return count
     */
    int doAct(String filename) throws IOException;
  }

  public static class FileFilterFromSuffixes implements FileFilter {
    String[] suffixes;

    public FileFilterFromSuffixes(String suffixes) {
      this.suffixes = suffixes.split(" ");
    }

    @Override
    public boolean accept(File file) {
      for (String s : suffixes)
        if (file.getPath().endsWith(s))
          return true;
      return false;
    }
  }

  public static FileFilter FileFilterSkipSuffix(String suffixes) {
    return new FileFilterNoWant(suffixes);
  }

  private static class FileFilterNoWant implements FileFilter {
    String[] suffixes;

    FileFilterNoWant(String suffixes) {
      this.suffixes = suffixes.split(" ");
    }

    @Override
    public boolean accept(File file) {
      for (String s : suffixes) {
        if (file.getPath().endsWith(s)) {
          return false;
        }
      }
      return true;
    }
  }

  /** Call act.doAct() on each file in dirName that passes the file filter, recurse into subdirs. */
  public static int actOnAll(String dirName, FileFilter ff, Act act) throws IOException {
    return actOnAll(dirName, ff, act, true);
  }

  /**
   * Call act.doAct() on each file in dirName passing the file filter
   *
   * @param dirName recurse into this directory
   * @param ff for files that pass this filter, may be null
   * @param act perform this acction
   * @param recurse recurse into subdirectories
   * @return count
   * @throws IOException on IO error
   */
  public static int actOnAll(String dirName, FileFilter ff, Act act, boolean recurse) throws IOException {
    int count = 0;

    logger.debug("---------------Reading directory {}", dirName);
    File allDir = new File(dirName);
    File[] allFiles = allDir.listFiles();
    if (null == allFiles) {
      logger.debug("---------------INVALID {}", dirName);
      throw new FileNotFoundException("Cant open " + dirName);
    }

    List<File> flist = Arrays.asList(allFiles);
    Collections.sort(flist);

    for (File f : flist) {
      String name = f.getAbsolutePath();
      if (f.isDirectory()) {
        continue;
      }
      if (((ff == null) || ff.accept(f)) && !name.endsWith(".exclude")) {
        name = name.replace("\\", "/");
        logger.debug("----acting on file {}", name);
        count += act.doAct(name);
      }
    }

    if (!recurse) {
      return count;
    }

    for (File f : allFiles) {
      if (f.isDirectory() && !f.getName().equals("exclude") && !f.getName().equals("problem")) {
        count += actOnAll(f.getAbsolutePath(), ff, act);
      }
    }

    return count;
  }

  ////////////////////////////////////////////////////////////////////////////

  /** Make list of filenames that pass the file filter, recurse true. */
  public static int actOnAllParameterized(String dirName, FileFilter ff, Collection<Object[]> filenames)
      throws IOException {
    return actOnAll(dirName, ff, new ListAction(filenames), true);
  }

  /** Make list of filenames that pass the file filter, recurse set by user. */
  public static int actOnAllParameterized(String dirName, FileFilter ff, Collection<Object[]> filenames,
      boolean recurse) throws IOException {
    return actOnAll(dirName, ff, new ListAction(filenames), recurse);
  }

  private static class ListAction implements Act {
    Collection<Object[]> filenames;

    ListAction(Collection<Object[]> filenames) {
      this.filenames = filenames;
    }

    @Override
    public int doAct(String filename) {
      filenames.add(new Object[] {filename});
      return 0;
    }
  }

  ////////////////////////////////////////////////////////////////////////////

  public static void readAll(String filename) throws IOException {
    ReadAllVariables act = new ReadAllVariables();
    act.doAct(filename);
  }

  private static class ReadAllVariables implements Act {
    @Override
    public int doAct(String filename) throws IOException {
      try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
        return readAllData(ncfile);
      }
    }
  }

  private static int max_size = 1000 * 1000 * 10;

  static Section makeSubset(Variable v) {
    int[] shape = v.getShape();
    shape[0] = 1;
    Section s = new Section(shape);
    long size = s.computeSize();
    shape[0] = (int) Math.max(1, max_size / size);
    return new Section(shape);
  }

  public static int readAllData(NetcdfFile ncfile) throws IOException {
    logger.debug("------Reading ncfile {}", ncfile.getLocation());
    try {
      for (Variable v : ncfile.getAllVariables()) {
        if (v instanceof Sequence) {
          Sequence seq = (Sequence) v;
          int count = 0;
          for (ucar.array.StructureData sdata : seq) {
            count++;
          }
          System.out.printf("  Read %s StructureData count = %s%n", v.getNameAndDimensions(), count);
          continue;
        }

        if (v.getSize() > max_size) {
          Section s = makeSubset(v);
          logger.debug("  Try to read variable {} size={} section={}", v.getNameAndDimensions(), v.getSize(), s);
          v.readArray(s);
        } else {
          logger.debug("  Try to read variable {} size={}", v.getNameAndDimensions(), v.getSize());
          v.readArray();
        }
      }

      return 1;
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e);
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////
  // from UnitTestCommon
  // Look for these to verify we have found the thredds root
  static final String[] DEFAULTSUBDIRS = {"cdm-core", "opendap"};

  public static String locateThreddsRoot() {
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
      if (found == DEFAULTSUBDIRS.length)
        try {// Assume this is it
          String root = prefix.getCanonicalPath();
          // clean up the root path
          root = root.replace('\\', '/'); // only use forward slash
          return root;
        } catch (IOException ignored) {
        }
    }
    return null;
  }
}
