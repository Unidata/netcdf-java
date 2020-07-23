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
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.write.NcmlWriter;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.StringUtil2;

/**
 * TestWrite NcML, read back and compare with original.
 * This is identical to TestNcmlWriteAndCompareShared, except that we're using local datasets.
 */
@RunWith(Parameterized.class)
public class TestNcmlWriteAndCompareLocal {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(500);

    // try everything from these directories
    try {
      TestDir.actOnAllParameterized(TestDir.cdmLocalFromTestDataDir + "point/", new SuffixFileFilter(".ncml"), result);
      TestDir.actOnAllParameterized(TestDir.cdmLocalFromTestDataDir + "ncml/standalone/", new SuffixFileFilter(".ncml"),
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
    Object iospMessage = useRecords ? NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE : null;
    if (openDataset)
      org = NetcdfDatasets.openDataset(durl, null, -1, null, iospMessage);
    else
      org = NetcdfDatasets.acquireFile(null, null, durl, -1, null, iospMessage);

    // create a file and write it out
    int pos = location.lastIndexOf("/");
    String ncmlOut = tempFolder.newFile().getAbsolutePath();
    if (showFiles)
      System.out.println(" output filename= " + ncmlOut);

    try {
      NcmlWriter ncmlWriter = new NcmlWriter();
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
    DatasetUrl durlcopy = DatasetUrl.findDatasetUrl(ncmlOut);
    if (openDataset)
      copy = NetcdfDatasets.openDataset(durlcopy, null, -1, null, iospMessage);
    else
      copy = NetcdfDatasets.acquireFile(null, null, durlcopy, -1, null, iospMessage);

    try {
      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, false, false, compareData);
      boolean ok = mind.compare(org, copy, new CompareNetcdf2.Netcdf4ObjectFilter());
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
