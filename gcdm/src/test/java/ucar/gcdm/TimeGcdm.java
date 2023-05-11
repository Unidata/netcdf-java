/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.gcdm.client.GcdmNetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.category.Slow;

/** Time {@link GcdmNetcdfFile} takes ~ 3 minutes */
@Category({NeedsCdmUnitTest.class, Slow.class})
public class TimeGcdm {
  String localFilename =
      TestDir.cdmUnitTestDir + "formats/netcdf4/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4";

  @Test
  public void readCmdrArray() throws IOException {
    String gcdmUrl = "gcdm://localhost:16111/" + localFilename;

    long total = 0;
    Stopwatch stopwatchAll = Stopwatch.createStarted();
    try (GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {
      System.out.println("Test input: " + gcdmFile.getLocation());
      boolean ok = true;
      for (Variable v : gcdmFile.getVariables()) {
        System.out.printf("  read variable though array : %s %s", v.getArrayType(), v.getShortName());
        Stopwatch stopwatch = Stopwatch.createStarted();
        Array<?> data = v.readArray();
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
