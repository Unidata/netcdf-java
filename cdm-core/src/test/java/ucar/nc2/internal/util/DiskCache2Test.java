/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.util;

import static com.google.common.truth.Truth.assertThat;
import java.util.ArrayList;
import java.util.List;
import org.junit.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author cwardgar
 * @since 2015/06/23
 */
public class DiskCache2Test {

  private static Path dirForStaticTests;
  private static DiskCache2 diskCache;

  private static final List<File> filesToDelete = new ArrayList<>();

  @BeforeClass
  public static void beforeClass() throws IOException {
    String cacheName = DiskCache2Test.class.getSimpleName();
    dirForStaticTests = Files.createTempDirectory(cacheName + "-static-tests");
    Path testCacheDir = Files.createTempDirectory(cacheName);

    try {
      Files.deleteIfExists(testCacheDir);
    } catch (IOException e) {
      throw new IOException(
          String.format("Unable to remove existing test cache directory %s - test setup failed.", testCacheDir), e);
    }
    assertThat(testCacheDir.toFile().exists());
    // create a disk cache for the test to use
    diskCache = new DiskCache2(testCacheDir.toString(), false, 1, 5);
  }

  @Test
  public void canWriteDir() {
    File file = dirForStaticTests.toFile();

    assertThat(file.exists());
    assertThat(DiskCache2.canWrite(file));
  }

  @Test
  public void canWriteFile() throws IOException {
    File file = Files.createTempFile(dirForStaticTests, "temp", null).toFile();
    assertThat(file.exists());
    filesToDelete.add(file);
    assertThat(DiskCache2.canWrite(file));
  }

  @Test
  public void cantWriteFile() throws IOException {
    File file = Files.createTempFile(dirForStaticTests, "temp", null).toFile();
    filesToDelete.add(file);

    Assert.assertTrue(file.exists());

    // make unwritable
    assertThat(file.setWritable(false)).isTrue();
    assertThat(DiskCache2.canWrite(file));

    // change back to writable so we can clean up
    assertThat(file.setWritable(true)).isTrue();
    assertThat(DiskCache2.canWrite(file));
  }

  @Test
  public void canWriteNonExistentFileWithExistentParent() {
    File file = Paths.get(dirForStaticTests.toString(), "non-existent-file.txt").toFile();

    assertThat(file.exists()).isFalse();
    assertThat(DiskCache2.canWrite(file));
  }

  @Test
  public void cantWriteNonExistentFileWithNonExistentParent() {
    File file = Paths.get(dirForStaticTests.toString(), "A", "B", "C", "non-existent-file.txt").toFile();

    assertThat(file.exists()).isFalse();
    assertThat(file.getParentFile().exists()).isFalse();
    assertThat(DiskCache2.canWrite(file)).isFalse();
  }

  @Test
  public void checkUniqueFileNames() throws IOException {
    final String prefix = "pre-";
    final String suffix = "-suf";
    final String lockFileExtention = ".reserve";

    // loop a few times to make sure it triggers.
    for (int i = 0; i < 10; i++) {
      File first = diskCache.createUniqueFile(prefix, suffix);
      File second = diskCache.createUniqueFile(prefix, suffix);

      // files do not exist yet
      assertThat(first.exists()).isFalse();
      assertThat(second.exists()).isFalse();

      // make the files
      assertThat(first.createNewFile());
      assertThat(second.createNewFile());

      // keep track of files that need to be cleaned up
      filesToDelete.add(first);
      filesToDelete.add(second);

      // files should exist now
      assertThat(first.exists());
      assertThat(second.exists());

      // lock files should exist as well
      File firstLockFile = Paths.get(first.getCanonicalFile().toString() + lockFileExtention).toFile();
      File secondLockFile = Paths.get(second.getCanonicalFile().toString() + lockFileExtention).toFile();
      Assert.assertTrue(firstLockFile.exists());
      Assert.assertTrue(secondLockFile.exists());
      filesToDelete.add(firstLockFile);
      filesToDelete.add(secondLockFile);

      // make sure they start with prefix
      assertThat(first.getName()).startsWith(prefix);
      assertThat(second.getName()).startsWith(prefix);

      // make sure they end with suffix
      assertThat(first.getName()).endsWith(suffix);
      assertThat(second.getName()).endsWith(suffix);

      // make sure they are different files
      assertThat(first.getAbsolutePath()).isNotEqualTo(second.getAbsolutePath());
    }
  }

  @AfterClass
  public static void afterClass() throws IOException {
    for (File f : filesToDelete) {
      Files.deleteIfExists(f.toPath());
    }
    Files.delete(dirForStaticTests);

    DiskCache2.exit();
    Files.deleteIfExists(Paths.get(diskCache.getRootDirectory()));
  }
}
