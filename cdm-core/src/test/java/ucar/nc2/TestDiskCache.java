/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import ucar.nc2.internal.util.DiskCache;
import ucar.nc2.calendar.CalendarDate;

/** Test DiskCache */
public class TestDiskCache {

  static void make(String filename) throws IOException {
    File want = DiskCache.getCacheFile(filename);
    System.out.println("make=" + want.getPath() + "; exists = " + want.exists());
    if (!want.exists()) {
      boolean ret = want.createNewFile();
      assertThat(ret).isTrue();
    }
    System.out.println(" canRead= " + want.canRead() + " canWrite = " + want.canWrite() + " lastMod = "
        + CalendarDate.of(want.lastModified()));
    System.out.println(" original=" + filename);
  }

  @Test
  public void testDiskCacheCreateNewFile() throws IOException {
    Path tempDirWithPrefix = Files.createTempDirectory("TestDiskCache");
    DiskCache.setRootDirectory(tempDirWithPrefix.toString());
    make("C:/junk.txt");
    make("C:/some/enchanted/evening/joots+3478.txt");
    make("http://www.unidata.ucar.edu/some/enc hanted/eve'ning/nowrite.gibberish");

    DiskCache.showCache(System.out);
    StringBuilder sbuff = new StringBuilder();
    DiskCache.cleanCache(1000 * 1000 * 10, sbuff);
    System.out.println(sbuff);
    assertThat(DiskCache.getCacheFile("any")).isNotNull();
  }

}
