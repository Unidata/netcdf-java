/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.stream;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.unidata.util.test.TestDir;

/** Test {@link ucar.nc2.stream.NcStreamWriter} */
public class TestNcStreamWriter {

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void writeNcStream() throws IOException, InvalidRangeException {
    String outFile = tempFolder.newFile().getAbsolutePath();

    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "testWrite.nc")) {
      NcStreamWriter writer = new NcStreamWriter(ncfile, null);
      try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(outFile), 50 * 1000)) {
        writer.streamAll(fos);
      }
    }
  }

}
