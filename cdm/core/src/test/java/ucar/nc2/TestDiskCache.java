package ucar.nc2;

import static ucar.nc2.internal.util.DiskCache.cleanCache;
import static ucar.nc2.internal.util.DiskCache.showCache;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import ucar.nc2.internal.util.DiskCache;
import ucar.nc2.time.CalendarDate;

/** Test DiskCache */
public class TestDiskCache {

  static void make(String filename) throws IOException {
    File want = DiskCache.getCacheFile(filename);
    System.out.println("make=" + want.getPath() + "; exists = " + want.exists());
    if (!want.exists()) {
      boolean ret = want.createNewFile();
      assert ret;
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

    showCache(System.out);
    StringBuilder sbuff = new StringBuilder();
    cleanCache(1000 * 1000 * 10, sbuff);
    System.out.println(sbuff);
  }

}
