/*
 * Copyright (c) 1998-2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.bufr;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Sequence;
import ucar.nc2.util.IO;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/**
 * Sanity check on reading bufr messages
 */
@Category(NeedsCdmUnitTest.class)
public class TestBufrRead {

  static String unitDir = TestDir.cdmUnitTestDir + "formats/bufr";
  static boolean show = false;
  static boolean printFailures = false;

  static class MyFileFilter implements java.io.FileFilter {
    public boolean accept(File pathname) {
      if (pathname.getPath().indexOf("exclude") > 0)
        return false;
      if (pathname.getName().endsWith(".bfx"))
        return false;
      if (pathname.getName().endsWith(".jpg"))
        return false;
      // all embedded fails TODO
      if (pathname.getName().endsWith("gdas.adpsfc.t00z.20120603.bufr"))
        return false;
      if (pathname.getName().endsWith("gdas.adpupa.t00z.20120603.bufr"))
        return false;
      if (pathname.getName().endsWith("gdas1.t18z.osbuv8.tm00.bufr_d"))
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

  // @Test
  public void bitCountAllInIddDir() throws IOException {
    int count = 0;
    assert 13852 == (count = bitCount(TestDir.cdmUnitTestDir + "formats/bufr/exclude/uniqueIDD.bufr")) : count; // was
                                                                                                                // 12337
    assert 11249 == (count = bitCount(TestDir.cdmUnitTestDir + "formats/bufr/exclude/uniqueBrasil.bufr")) : count; // was
                                                                                                                   // 11533
    assert 22710 == (count = bitCount(TestDir.cdmUnitTestDir + "formats/bufr/exclude/uniqueExamples.bufr")) : count; // was
                                                                                                                     // 12727
    assert 9929 == (count = bitCount(TestDir.cdmUnitTestDir + "formats/bufr/exclude/uniqueFnmoc.bufr")) : count;
  }

  public void utestCountMessages() throws IOException {
    int count = 0;
    count += bitCount(TestDir.cdmUnitTestDir + "formats/bufr/uniqueIDD.bufr");
    // count += readBufr(TestAll.cdmUnitTestDir + "formats/bufr/uniqueBrasil.bufr");
    // count += readBufr(TestAll.cdmUnitTestDir + "formats/bufr/uniqueExamples.bufr");
    // count += readBufr(TestAll.cdmUnitTestDir + "formats/bufr/uniqueFnmoc.bufr");
    System.out.printf("total read ok = %d%n", count);
  }

  private int bitCount(String filename) throws IOException {
    System.out.printf("%n***bitCount bufr %s%n", filename);
    int bad = 0;
    int count = 0;
    int totalObs = 0;
    try (RandomAccessFile raf = new RandomAccessFile(filename, "r")) {
      MessageScanner scan = new MessageScanner(raf, 0, true);
      while (scan.hasNext()) {
        Message m = scan.next();
        if (m == null) {
          continue;
        }
        int nobs = m.getNumberDatasets();
        if (show) {
          System.out.printf(" %3d nobs = %4d (%s) center = %s table=%s cat=%s ", count++, nobs, m.getHeader(),
              m.getLookup().getCenterNo(), m.getLookup().getTableName(), m.getLookup().getCategoryNo());
        }
        assert m.isTablesComplete() : "incomplete tables";

        if (nobs > 0) {
          BufrSingleMessage bufr = new BufrSingleMessage();
          Sequence top = bufr.fromSingleMessage(m.raf(), m);
          Formatter f = new Formatter();
          MessageBitCounter counter = new MessageBitCounter(top, m, m, f);
          if (!counter.isBitCountOk()) {
            // System.out.printf(" MessageBitCounter failed = %s%n", f);
            // System.out.printf(" nbits = %d%n", counter.msg_nbits);
            bad++;
            if (printFailures) {
              try (FileOutputStream out = new FileOutputStream("/tmp/bitcount.txt")) {
                IO.writeContents(f.toString(), out);
              }
            }
          }
          assertThat(bad < 10).isTrue();
          // assertThat(counter.isBitCountOk()).isTrue();
        }

        totalObs += nobs;
        if (show) {
          System.out.printf("%n");
        }
      }
    }

    return totalObs;
  }

  // just open and see if it barfs
  private void openNetcdf(String filename) throws IOException {
    System.out.printf("%n***openNetcdf bufr %s%n", filename);
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      if (show) {
        System.out.printf("%s%n", ncfile);
      }
    }
  }

}
