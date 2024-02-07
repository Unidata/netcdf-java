package ucar.nc2.util.cache;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.unidata.io.RandomAccessFile;

public class TestRandomAccessFileCacheCleanup {

  @ClassRule
  public static final TemporaryFolder tempFolder = new TemporaryFolder();

  private static FileCache cache;

  @BeforeClass
  public static void enableCache() {
    RandomAccessFile.shutdown();
    cache = new FileCache("RandomAccessFile", 0, 1, 1, 0, true);
    RandomAccessFile.setGlobalFileCache(cache);
    assertThat(cache.showCache().size()).isEqualTo(0);
  }

  @AfterClass
  public static void shutdownCache() {
    RandomAccessFile.shutdown();
    RandomAccessFile.setGlobalFileCache(null);
  }

  @Test
  public void shouldReleaseDeletedFileDuringCleanup() throws IOException {
    final File toDelete = tempFolder.newFile();
    RandomAccessFile.acquire(toDelete.getAbsolutePath());
    RandomAccessFile.acquire(tempFolder.newFile().getAbsolutePath());
    assertThat(cache.showCache().size()).isEqualTo(2);

    assertThat(toDelete.delete()).isTrue();
    cache.cleanup(1);
    assertThat(cache.showCache().size()).isEqualTo(1);
  }
}
