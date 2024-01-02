package ucar.nc2.grib;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.filesystem.MFileOS;
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MFile;
import ucar.nc2.util.DiskCache2;

@RunWith(Parameterized.class)
public class TestGribIndexLocation {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String DATA_DIR = "../grib/src/test/data/";
  private static final String INDEX_DIR = "../grib/src/test/data/index/";

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    return Arrays.asList(new Object[][] {

        {"radar_national.grib1"},

        {"cosmo-eu.grib2"},

    });
  }

  private final String filename;
  private final String indexFilename;
  private final boolean isGrib1;

  public TestGribIndexLocation(String filename) {
    this.filename = filename;
    this.indexFilename = filename + GribIndex.GBX9_IDX;
    this.isGrib1 = filename.endsWith(".grib1");
  }

  @Rule
  public final TemporaryFolder tempCacheFolder = new TemporaryFolder();

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Before
  public void setCacheLocation() {
    System.getProperties().setProperty("nj22.cache", tempCacheFolder.getRoot().getPath());
  }

  @After
  public void unsetCacheLocation() {
    System.clearProperty("nj22.cache");
  }

  @Test
  public void shouldCreateIndexInLocationOfDataFile() throws IOException {
    useCache(false);

    final MFile copiedFile = new MFileOS(copyToTempFolder(DATA_DIR + filename).getPath());

    final GribIndex index =
        GribIndex.readOrCreateIndexFromSingleFile(isGrib1, copiedFile, CollectionUpdateType.always, logger);
    assertThat(index).isNotNull();
    assertThat(index.getNRecords()).isNotEqualTo(0);

    assertTempFolderHasSize(2);
    assertTempFolderHas(copiedFile.getName() + GribIndex.GBX9_IDX);
  }

  @Test
  public void shouldUseIndexInLocationOfDataFile() throws IOException {
    useCache(false);

    final MFile copiedFile = new MFileOS(copyToTempFolder(DATA_DIR + filename).getPath());
    final File copiedIndexFile = copyToTempFolder(INDEX_DIR + indexFilename);
    assertThat(copiedIndexFile.setLastModified(0)).isTrue();

    final GribIndex index =
        GribIndex.readOrCreateIndexFromSingleFile(isGrib1, copiedFile, CollectionUpdateType.nocheck, logger);
    assertThat(index).isNotNull();
    assertThat(index.getNRecords()).isNotEqualTo(0);

    assertTempFolderHasSize(2);
    assertTempFolderHas(copiedFile.getName() + GribIndex.GBX9_IDX);
    assertThat(copiedIndexFile.lastModified()).isEqualTo(0);
  }

  @Test
  public void shouldCreateIndexInDefaultCache() throws IOException {
    final DiskCache2 diskCache = useCache(true);

    final MFile copiedFile = new MFileOS(copyToTempFolder(DATA_DIR + filename).getPath());

    final GribIndex index =
        GribIndex.readOrCreateIndexFromSingleFile(isGrib1, copiedFile, CollectionUpdateType.always, logger);
    assertThat(index).isNotNull();
    assertThat(index.getNRecords()).isNotEqualTo(0);

    assertTempFolderHasSize(1);
    assertThat(diskCache.getCacheFile(copiedFile.getPath() + GribIndex.GBX9_IDX).exists()).isTrue();
  }

  @Test
  public void shouldReuseCachedIndex() throws IOException {
    final DiskCache2 diskCache = useCache(true);

    final MFile copiedFile = new MFileOS(copyToTempFolder(DATA_DIR + filename).getPath());

    final GribIndex index =
        GribIndex.readOrCreateIndexFromSingleFile(isGrib1, copiedFile, CollectionUpdateType.always, logger);
    assertThat(index).isNotNull();
    assertThat(diskCache.getCacheFile(copiedFile.getPath() + GribIndex.GBX9_IDX).exists()).isTrue();
    final long cacheLastModified = diskCache.getCacheFile(copiedFile.getPath() + GribIndex.GBX9_IDX).lastModified();

    final GribIndex rereadIndex =
        GribIndex.readOrCreateIndexFromSingleFile(isGrib1, copiedFile, CollectionUpdateType.never, logger);
    assertThat(rereadIndex).isNotNull();
    final File rereadCachedIndex = diskCache.getCacheFile(copiedFile.getPath() + GribIndex.GBX9_IDX);
    assertThat(rereadCachedIndex.lastModified()).isEqualTo(cacheLastModified);
  }

  // Helper functions
  static DiskCache2 useCache(boolean useCache) {
    final DiskCache2 diskCache = GribIndexCache.getDiskCache2();
    diskCache.setNeverUseCache(!useCache);
    diskCache.setAlwaysUseCache(useCache);
    return diskCache;
  }

  private File copyToTempFolder(String filename) throws IOException {
    final File file = new File(filename);
    final File copiedFile = new File(tempFolder.getRoot(), file.getName());
    Files.copy(file.toPath(), copiedFile.toPath());
    assertTempFolderHas(copiedFile.getName());
    return copiedFile;
  }

  private void assertTempFolderHas(String filename) {
    final File[] filesInFolder = tempFolder.getRoot().listFiles();
    assertThat(filesInFolder).isNotNull();
    assertThat(Arrays.stream(filesInFolder).anyMatch(file -> file.getName().equals(filename))).isTrue();
  }

  private void assertTempFolderHasSize(int size) {
    final File[] filesInFolder = tempFolder.getRoot().listFiles();
    assertThat(filesInFolder).isNotNull();
    assertThat(filesInFolder.length).isEqualTo(size);
  }
}
