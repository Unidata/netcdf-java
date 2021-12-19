/*
 * Copyright (c) 1998-2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.bufr;

import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Sequence;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.TestDir;
import java.io.File;
import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/** Sanity check on reading bufr messages. */
public class TestBufrModuleRead {
  private static final boolean show = false;
  private static final String unitDir = "../bufr/src/test/data/";

  static class MyFileFilter implements java.io.FileFilter {
    public boolean accept(File pathname) {
      if (pathname.getPath().indexOf("exclude") > 0)
        return false;
      if (pathname.getName().endsWith(".bfx"))
        return false;
      if (pathname.getName().endsWith(".jpg"))
        return false;
      if (pathname.getName().endsWith("embedded.bufr")) // fails
        return false;
      if (pathname.getName().endsWith("temp_20210824133030_IUSK11_AMMC_241200.bufr")) // fails
        return false;
      return true;
    }
  }

  @Test
  public void bitCountAllInUnitTestDir() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(unitDir, new MyFileFilter(), filename -> bitCount(filename), true);
    System.out.println("***BitCount " + count + " records");
  }

  @Test
  public void openAllInUnitTestDir() throws IOException {
    int count = 0;
    count += TestDir.actOnAll(unitDir, new MyFileFilter(), filename -> {
      openNetcdf(filename);
      return 1;
    }, true);
    System.out.println("***Opened " + count + " files");
  }

  private int bitCount(String filename) throws IOException {
    System.out.printf("%n***bitCount bufr %s%n", filename);
    int count = 0;
    int totalObs = 0;
    try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
      MessageScanner scan = new MessageScanner(raf, 0, true);
      while (scan.hasNext()) {
        try {
          Message m = scan.next();
          if (m == null) {
            continue;
          }
          int nobs = m.getNumberDatasets();
          if (show) {
            System.out.printf(" %3d nobs = %4d (%s) center = %s table=%s cat=%s ", count++, nobs, m.getHeader(),
                m.getLookup().getCenterNo(), m.getLookup().getTableName(), m.getLookup().getCategoryNo());
          }
          assertThat(m.isTablesComplete()).isTrue();

          if (nobs > 0) {
            BufrSingleMessage bufr = new BufrSingleMessage();
            Sequence top = bufr.fromSingleMessage(m.raf(), m);
            Formatter f = new Formatter();
            MessageBitCounter counter = new MessageBitCounter(top, m, m, f);
            assertWithMessage("bit count wrong on " + filename).that(counter.isBitCountOk()).isTrue();
          }

          totalObs += nobs;
          if (show) {
            System.out.printf("%n");
          }

        } catch (Exception e) {
          e.printStackTrace();
        }

      }

    }

    return totalObs;
  }

  // just open and see if it barfs
  private void openNetcdf(String filename) throws IOException {
    System.out.printf("%n***openNetcdf bufr %s%n", filename);
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      if (show)
        System.out.printf("%s%n", ncfile);
    }
  }



}
