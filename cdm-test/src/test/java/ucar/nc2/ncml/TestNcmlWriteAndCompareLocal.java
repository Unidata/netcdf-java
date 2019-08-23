package ucar.nc2.ncml;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.jdom2.Element;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.jni.netcdf.Nc4Iosp;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.StringUtil2;

/**
 * TestWrite NcML, read back and compare with original.
 *
 * This is identical to TestNcmlWriteAndCompareShared, except that we're using local datasets.
 *
 * @author caron
 * @since 11/2/13
 */
@RunWith(Parameterized.class)
public class TestNcmlWriteAndCompareLocal {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Before
  public void setLibrary() {
    // Ignore this class's tests if NetCDF-4 isn't present.
    // We're using @Before because it shows these tests as being ignored.
    // @BeforeClass shows them as *non-existent*, which is not what we want.
    Assume.assumeTrue("NetCDF-4 C library not present.", Nc4Iosp.isClibraryPresent());
  }

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(500);

    // try everything from these directories
    try {
      TestDir.actOnAllParameterized(TestDir.cdmLocalTestDataDir + "point/", new SuffixFileFilter(".ncml"), result);
      TestDir.actOnAllParameterized(TestDir.cdmLocalTestDataDir + "ncml/standalone/", new SuffixFileFilter(".ncml"),
          result);

    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;
  }

  /////////////////////////////////////////////////////////////
  boolean showFiles = true;
  boolean compareData = false;

  public TestNcmlWriteAndCompareLocal(String location) throws IOException {
    this.location = StringUtil2.replace(location, '\\', "/");
    this.durl = DatasetUrl.findDatasetUrl(location);
  }

  String location;
  DatasetUrl durl;

  int fail = 0;
  int success = 0;

  @Test
  public void compareNcML() throws IOException {
    compareNcML(true, true, true);
    compareNcML(true, false, false);
    compareNcML(false, true, false);
    compareNcML(false, false, true);
    compareNcML(false, false, false);
  }

  private void compareNcML(boolean useRecords, boolean explicit, boolean openDataset) throws IOException {
    if (compareData)
      useRecords = false;

    if (showFiles) {
      System.out.println("-----------");
      System.out.println("  input filename= " + location);
    }

    NetcdfFile org;
    if (openDataset)
      org = NetcdfDataset.openDataset(location, false, null);
    else
      org = NetcdfDataset.acquireFile(durl, null);

    if (useRecords)
      org.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    // create a file and write it out
    int pos = location.lastIndexOf("/");
    String ncmlOut = tempFolder.newFile().getAbsolutePath();
    if (showFiles)
      System.out.println(" output filename= " + ncmlOut);

    try {
      NcMLWriter ncmlWriter = new NcMLWriter();
      Element netcdfElement;

      if (explicit) {
        netcdfElement = ncmlWriter.makeExplicitNetcdfElement(org, null);
      } else {
        netcdfElement = ncmlWriter.makeNetcdfElement(org, null);
      }

      ncmlWriter.writeToFile(netcdfElement, new File(ncmlOut));
    } catch (IOException ioe) {
      // ioe.printStackTrace();
      assert false : ioe.getMessage();
    }

    // read it back in
    NetcdfFile copy;
    if (openDataset)
      copy = NetcdfDataset.openDataset(ncmlOut, false, null);
    else
      copy = NetcdfDataset.acquireFile(durl, null);

    if (useRecords)
      copy.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

    try {
      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, false, false, compareData);
      boolean ok = mind.compare(org, copy, new CompareNetcdf2.Netcdf4ObjectFilter(), false, false, compareData);
      if (!ok) {
        fail++;
        System.out.printf("--Compare %s, useRecords=%s explicit=%s openDataset=%s compareData=%s %n", location,
            useRecords, explicit, openDataset, compareData);
        System.out.printf("  %s%n", f);
      } else {
        System.out.printf("--Compare %s is OK (useRecords=%s explicit=%s openDataset=%s compareData=%s)%n", location,
            useRecords, explicit, openDataset, compareData);
        success++;
      }
      Assert.assertTrue(location, ok);
    } finally {
      org.close();
      copy.close();
    }
  }

}
