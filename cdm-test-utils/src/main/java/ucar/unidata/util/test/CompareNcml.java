/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.util.test;

import java.io.File;
import java.io.IOException;
import java.util.Formatter;
import org.jdom2.Element;
import org.junit.rules.TemporaryFolder;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.nc2.write.NcmlWriter;

import static com.google.common.truth.Truth.assertWithMessage;

/** Read dataset, write NcML, compare results. */
public class CompareNcml {
  private final TemporaryFolder tempFolder;
  private final boolean showFiles;
  private final boolean compareData;
  private final DatasetUrl durl;

  public CompareNcml(TemporaryFolder tempFolder, DatasetUrl durl, boolean compareData, boolean showFiles)
      throws IOException {
    this.tempFolder = tempFolder;
    this.durl = durl;
    this.compareData = compareData;
    this.showFiles = showFiles;

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
      System.out.println("  input filename= " + durl.getTrueurl());
    }

    NetcdfFile org;
    Object iospMessage = useRecords ? NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE : null;
    if (openDataset)
      org = NetcdfDatasets.openDataset(durl, null, -1, null, iospMessage);
    else
      org = NetcdfDatasets.acquireFile(null, null, durl, -1, null, iospMessage);

    // create a file and write it out
    String ncmlOut = tempFolder.newFile().getAbsolutePath();
    if (showFiles) {
      System.out.println(" output filename= " + ncmlOut);
    }
    NcmlWriter ncmlWriter = new NcmlWriter();
    Element netcdfElement;
    if (explicit) {
      netcdfElement = ncmlWriter.makeExplicitNetcdfElement(org, null);
    } else {
      netcdfElement = ncmlWriter.makeNetcdfElement(org, null);
    }
    ncmlWriter.writeToFile(netcdfElement, new File(ncmlOut));

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
        System.out.printf("--Compare %s, useRecords=%s explicit=%s openDataset=%s compareData=%s %n", durl.getTrueurl(),
            useRecords, explicit, openDataset, compareData);
        System.out.printf("  %s%n", f);
      } else {
        System.out.printf("--Compare %s is OK (useRecords=%s explicit=%s openDataset=%s compareData=%s)%n",
            durl.getTrueurl(), useRecords, explicit, openDataset, compareData);
      }
      assertWithMessage(durl.getTrueurl()).that(ok).isTrue();
    } finally {
      org.close();
      copy.close();
    }
  }

}
