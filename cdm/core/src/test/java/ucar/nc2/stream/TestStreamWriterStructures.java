/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.stream;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Formatter;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;

/** Test {@link ucar.nc2.stream.NcStreamWriter} for Structure data. */
public class TestStreamWriterStructures {

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  @Ignore("CdmRemote doesnt work reading member data")
  public void writeNcStream() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmLocalTestDataDir + "testStructures.nc";
    // String outFile = "C:/temp/testStructures.ncs"; // tempFolder.newFile().getAbsolutePath();
    String outFile = tempFolder.newFile().getAbsolutePath();
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, -1, null, NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      NcStreamWriter writer = new NcStreamWriter(ncfile, null);

      Variable record = ncfile.findVariable("record");
      assertThat(record).isNotNull();
      assertThat(record).isInstanceOf(Structure.class);

      try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(outFile), 50 * 1000)) {
        writer.streamAll(fos);
      }
      // read it back in and compare to original.
      try (NetcdfFile copy = NetcdfDatasets.openFile(outFile, null)) {
        Formatter errs = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(errs, true, true, true);
        boolean ok = compare.compare(ncfile, copy);
        if (!ok) {
          System.out.printf("FAIL %s%n", errs);
          fail();
        }
      }
    }
  }

}
