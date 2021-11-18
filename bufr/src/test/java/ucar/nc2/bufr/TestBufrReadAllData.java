/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.bufr;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Sequence;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Open Bufr and read through its data. */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestBufrReadAllData {
  static String bufrLocalFromTop = "src/test/data/";
  static boolean show = false;
  static boolean printFailures = false;

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    FileFilter ff = TestDir.FileFilterSkipSuffix(".cdl .ncml");
    List<Object[]> result = new ArrayList<>(500);
    try {
      TestDir.actOnAllParameterized(bufrLocalFromTop, ff, result, false);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/bufr/userExamples", ff, result, false);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/bufr/embeddedTable", ff, result, false);
    } catch (IOException e) {
      e.printStackTrace();
    }

    return result;
  }

  /////////////////////////////////////////////////////////////

  public TestBufrReadAllData(String filename) {
    this.filename = filename;
  }

  private final String filename;

  @Test
  public void doOne() throws Exception {
    readArrays(filename);
  }

  // compare to check complete data read. need seperate files, or else they interfere
  public static void readArrays(String filename) throws Exception {
    try (NetcdfFile arrayFile = NetcdfFiles.open(filename, "ucar.nc2.bufr.BufrIosp", -1, null, null);
        NetcdfFile arrayFile2 = NetcdfFiles.open(filename, "ucar.nc2.bufr.BufrIosp", -1, null, null)) {
      System.out.println("Test NetcdfFile: " + arrayFile.getLocation());

      boolean ok = CompareArrayToArray.compareFiles(arrayFile, arrayFile2);
      assertThat(ok).isTrue();
    }
  }

  public static void bitCount(String filename) throws IOException {
    System.out.printf("%n***bitCount bufr %s%n", filename);
    int bad = 0;
    int count = 0;
    boolean showFailures = true;
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
            if (counter.getCountedDataBytes() == 336) {
              System.out.printf(" %3d nobs = %4d (%s) center = %s table=%s cat=%s%n", count++, nobs, m.getHeader(),
                  m.getLookup().getCenterNo(), m.getLookup().getTableName(), m.getLookup().getCategoryNo());
              System.out.printf("  MessageBitCounter failed = %s%n", f);
              showFailures = false;
            }
          }
          assertThat(bad < 10).isTrue();
          // assertThat(counter.isBitCountOk()).isTrue();
        }
        if (show) {
          System.out.printf("%n");
        }
      }
    }
  }

}

