/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.bufr;

import org.junit.Test;
import ucar.array.Array;
import ucar.array.StructureData;
import ucar.nc2.Sequence;
import ucar.nc2.write.NcdumpArray;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class TestBufrSingleMessage {
  private static boolean show = false;

  @Test
  public void test() throws IOException {
    String filename = TestBufrReadAllData.bufrLocalFromTop + "embedded.bufr";
    readMessages(filename);
  }

  public static void readMessages(String filename) throws IOException {
    System.out.printf("%n***bitCount bufr %s%n", filename);
    int count = 0;
    try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
      MessageScanner scan = new MessageScanner(raf, 0, true);
      while (scan.hasNext()) {
        Message m = scan.next();
        if (m == null) {
          continue;
        }
        int nobs = m.getNumberDatasets();
        String hash = Integer.toHexString(m.hashCode());
        String ddshash = Integer.toHexString(m.dds.getDataDescriptors().hashCode());
        System.out.printf(" %3d nobs = %4d hash=%s ddsHash= %s %n", count++, nobs, hash, ddshash);
        assertThat(m.isTablesComplete()).isTrue();

        if (nobs > 0) {
          BufrSingleMessage bufr = new BufrSingleMessage();
          Sequence top = bufr.fromSingleMessage(m.raf(), m);

          Array<StructureData> as = bufr.iosp.readMessage(m);
          int obs = 0;
          for (StructureData sdata : as) {
            if (show)
              System.out.printf("   %3d = %s %n", obs, NcdumpArray.printStructureData(sdata));
            obs++;
          }
        }
      }
    }
  }
}
