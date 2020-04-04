/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 *  See LICENSE for license information.
 */
package ucar.nc2.grib.grib2;

import java.io.IOException;
import java.util.Formatter;
import org.junit.Ignore;
import org.junit.Test;
import ucar.nc2.grib.grib2.TestGrib2Records.Callback;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.TestDir;

/** Test problems reading data */
public class TestProblemDataReading {

  @Ignore
  public void compareProblemFile() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/gfs_4_20130830_1800_144.grb2";
    testRead(filename);
  }

  private void testRead(String filename) throws IOException {
    readFile(filename, (raf, gr) -> {
      Grib2SectionData ds = gr.getDataSection();
      System.out.printf("Grib2SectionData %s end %d%n", ds, ds.getEndingPosition());
      float[] data = gr.readData(raf);
      return true;
    });
  }

  private void readFile(String path, Callback callback) throws IOException {
    try (RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(path, "r")) {
      raf.order(ucar.unidata.io.RandomAccessFile.BIG_ENDIAN);
      raf.seek(0);
      System.out.printf("Filename %s len = %s%n", path, raf.length());

      Grib2RecordScanner reader = new Grib2RecordScanner(raf);
      while (reader.hasNext()) {
        Grib2Record gr = reader.next();
        if (gr == null)
          break;
        callback.call(raf, gr);
      }
    }
  }

}
