/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.util.Formatter;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Time {@link Arrays} */
@Category({NeedsCdmUnitTest.class})
public class TimeArrays {
  String filename =
      TestDir.cdmUnitTestDir + "formats/netcdf4/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4";

  @Test
  public void testNc4Array() throws IOException {
    Stopwatch stopwatch = Stopwatch.createStarted();
    long total = TestReadArrayProblem.compareArrays(filename);
    stopwatch.stop();
    double rate = ((double) total) / stopwatch.elapsed(TimeUnit.MICROSECONDS);
    System.out.printf("*** %d bytes took %s = %10.4f MB/sec%n", total, stopwatch, rate);
  }

  @Test
  public void readMa2() throws IOException {
    long total = 0;
    Stopwatch stopwatchAll = Stopwatch.createStarted();
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, -1, null, NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      System.out.println("Test input: " + ncfile.getLocation());
      boolean ok = true;
      for (Variable v : ncfile.getVariables()) {
        System.out.printf("  read variable though ma2 : %s %s", v.getDataType(), v.getShortName());
        com.google.common.base.Stopwatch stopwatch = Stopwatch.createStarted();
        ucar.ma2.Array data = v.read();
        stopwatch.stop();
        long size = data.getSize();
        double rate = ((double) size) / stopwatch.elapsed(TimeUnit.MICROSECONDS);
        System.out.printf("    size = %d, time = %s rate = %10.4f MB/sec%n", size, stopwatch, rate);
        total += size;
      }
      assertThat(ok).isTrue();
    }
    stopwatchAll.stop();
    double rate = ((double) total) / stopwatchAll.elapsed(TimeUnit.MICROSECONDS);
    System.out.printf("*** %d bytes took %s = %10.4f MB/sec%n", total, stopwatchAll, rate);
  }

  @Test
  public void readArray() throws IOException {
    long total = 0;
    Stopwatch stopwatchAll = Stopwatch.createStarted();
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, -1, null, NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {
      System.out.println("Test input: " + ncfile.getLocation());
      boolean ok = true;
      for (Variable v : ncfile.getVariables()) {
        System.out.printf("  read variable though array : %s %s", v.getDataType(), v.getShortName());
        com.google.common.base.Stopwatch stopwatch = Stopwatch.createStarted();
        ucar.array.Array<?> data = v.readArray();
        stopwatch.stop();
        long size = data.length();
        double rate = ((double) size) / stopwatch.elapsed(TimeUnit.MICROSECONDS);
        System.out.printf("    size = %d, time = %s rate = %10.4f MB/sec%n", size, stopwatch, rate);
        total += size;
      }
      assertThat(ok).isTrue();
    }
    stopwatchAll.stop();
    double rate = ((double) total) / stopwatchAll.elapsed(TimeUnit.MICROSECONDS);
    System.out.printf("*** %d bytes took %s = %10.4f MB/sec%n", total, stopwatchAll, rate);
  }
}
