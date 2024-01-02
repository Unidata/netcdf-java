/*
 * Copyright (c) 1998-2023 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.gcdm.client.GcdmNetcdfFile;
import ucar.ma2.Array;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.category.Slow;

/** Time {@link GcdmNetcdfFile} takes ~ 3 minutes */
@Ignore("TODO This test fails due to deadline exceeded after 30s")
@Category({NeedsCdmUnitTest.class, Slow.class})
public class TestGcdmTiming {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  String localFilename =
      TestDir.cdmUnitTestDir + "formats/netcdf4/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4";

  @Test
  public void readGcdmArray() throws IOException {
    String gcdmUrl = "gcdm://localhost:16111/" + localFilename;

    long total = 0;
    Stopwatch stopwatchAll = Stopwatch.createStarted();
    try (GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {
      logger.debug("Test input: " + gcdmFile.getLocation());
      boolean ok = true;
      for (Variable v : gcdmFile.getVariables()) {
        logger.debug("  read variable though array : {} {}", v.getDataType(), v.getShortName());
        Stopwatch stopwatch = Stopwatch.createStarted();
        Array data = v.read();
        stopwatch.stop();
        long size = data.getSize();
        double rate = ((double) size) / stopwatch.elapsed(TimeUnit.MICROSECONDS);
        logger.debug("    size = {}, time = {} rate = {} MB/sec", size, stopwatch, rate);
        total += size;
      }
      assertThat(ok).isTrue();
    }
    stopwatchAll.stop();
    double rate = ((double) total) / stopwatchAll.elapsed(TimeUnit.MICROSECONDS);
    logger.debug("*** {} bytes took {} = {} MB/sec", total, stopwatchAll, rate);
  }
}
