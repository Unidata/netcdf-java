/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ncml;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.write.NcmlWriter;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** TestWrite NcML, read back and compare with original. */
@Category(NeedsCdmUnitTest.class)
public class TestNcmlWriteAndCompareProblem {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  /////////////////////////////////////////////////////////////
  boolean showFiles = true;
  boolean compareData = false;
  String location = TestDir.cdmUnitTestDir + "/conventions/atd-radar/SPOL_3Volumes.nc";

  @Test
  public void compareProblemFile() throws IOException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(location);
    // compareNcML(durl,true, true, true);
    // compareNcML(durl,true, false, false);
    // compareNcML(durl,false, true, false);
    // compareNcML(durl,false, false, true);
    compareNcML(durl, false, false, false);
  }

  private void compareNcML(DatasetUrl durl, boolean useRecords, boolean explicit, boolean openDataset)
      throws IOException {
    if (compareData)
      useRecords = false;

    if (showFiles) {
      System.out.println("-----------");
      System.out.println("  input filename= " + durl.trueurl);
    }

    NetcdfFile org;
    Object iospMessage = useRecords ? NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE : null;
    if (openDataset)
      org = NetcdfDatasets.openDataset(durl, null, -1, null, iospMessage);
    else
      org = NetcdfDatasets.acquireFile(null, null, durl, -1, null, iospMessage);

    // create a file and write it out
    String ncmlOut = location + ".ncml"; // tempFolder.newFile().getAbsolutePath();
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
      boolean ok = mind.compare(org, copy, new CompareNetcdf2.Netcdf4ObjectFilter(), false, false, compareData);
      if (!ok) {
        System.out.printf("--Compare %s, useRecords=%s explicit=%s openDataset=%s compareData=%s %n", durl.trueurl,
            useRecords, explicit, openDataset, compareData);
        System.out.printf("  %s%n", f);
      } else {
        System.out.printf("--Compare %s is OK (useRecords=%s explicit=%s openDataset=%s compareData=%s)%n",
            durl.trueurl, useRecords, explicit, openDataset, compareData);
      }
      Assert.assertTrue(durl.trueurl, ok);
    } finally {
      org.close();
      copy.close();
    }
  }
}
