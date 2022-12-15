package ucar.unidata.io;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestRandomAccessFileCache {

  @ClassRule
  public static final TemporaryFolder tempFolder = new TemporaryFolder();

  private static RandomAccessFile testFile;
  private static final String TEST_FILE_PATH = "src/test/data/preserveLineEndings/testUTF8.txt";

  @BeforeClass
  public static void enableCache() {
    RandomAccessFile.enableDefaultGlobalFileCache();
  }

  @AfterClass
  public static void shutdownCache() {
    RandomAccessFile.setGlobalFileCache(null);
  }

  @Before
  public void setUpTestFile() throws IOException {
    testFile = RandomAccessFile.acquire(TEST_FILE_PATH);
  }

  @After
  public void cleanUpTestFile() throws IOException {
    testFile.close();
  }

  @Test
  public void shouldReturnInUseWhenFileIsAcquired() {
    assertThat(testFile.getCacheState()).isEqualTo(1);
  }

  @Test
  public void shouldReturnNotInUseWhenFileIsClosed() throws IOException {
    testFile.close();
    assertThat(testFile.getCacheState()).isEqualTo(2);
  }

  @Test
  public void shouldReturnNotInCacheWhenFileNotInCacheIsClosed() throws IOException {
    testFile.setFileCache(null);
    testFile.close();
    assertThat(testFile.getCacheState()).isEqualTo(0);
  }

  @Test
  public void shouldReturnNotInUseWhenFileIsClosedTwice() throws IOException {
    testFile.close();
    testFile.close();
    assertThat(testFile.getCacheState()).isEqualTo(2);
  }

  @Test
  public void shouldReturnNotInUseWhenFileIsReleased() {
    testFile.release();
    assertThat(testFile.getCacheState()).isEqualTo(2);
  }

  @Test
  public void shouldReturnInUseWhenFileIsReacquired() {
    testFile.reacquire();
    assertThat(testFile.getCacheState()).isEqualTo(1);
  }

  @Test
  public void shouldReturnNotInCacheWhenFileCacheIsReset() {
    testFile.setFileCache(null);
    assertThat(testFile.getCacheState()).isEqualTo(0);
  }
}
