/*
 * Copyright (c) 2023-2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.grib.TestGribIndexLocation.useCache;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
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
import thredds.inventory.CollectionUpdateType;
import thredds.inventory.MFile;
import thredds.inventory.s3.MFileS3;
import ucar.nc2.util.DiskCache2;

@RunWith(Parameterized.class)
public class TestGribIndexLocationS3 {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final String BUCKET = "cdms3:thredds-test-data";
  private static final String S3_DIR_WITHOUT_INDEX = BUCKET + "?" + "test-grib-without-index/";
  private static final String FRAGMENT = "#delimiter=/";

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    return Arrays.asList(new Object[][] {

        {"radar_national.grib1", ""},

        {"radar_national.grib1", FRAGMENT},

        {"cosmo-eu.grib2", ""},

        {"cosmo-eu.grib2", FRAGMENT},

    });
  }

  private final String filename;
  private final String indexFilename;
  private final boolean isGrib1;

  public TestGribIndexLocationS3(String filename, String fragment) {
    this.filename = filename + fragment;
    this.indexFilename = GribIndex.makeIndexFileName(filename);
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
  public void shouldCreateIndexInDefaultCache() throws IOException {
    final DiskCache2 diskCache = useCache(true);

    // Check that index file does not exist
    final MFile s3FileIndex = new MFileS3(S3_DIR_WITHOUT_INDEX + indexFilename);
    assertThat(s3FileIndex.exists()).isFalse();

    final MFile s3File = new MFileS3(S3_DIR_WITHOUT_INDEX + filename);

    final GribIndex index =
        GribIndex.readOrCreateIndexFromSingleFile(isGrib1, s3File, CollectionUpdateType.always, logger);
    assertThat(index).isNotNull();
    assertThat(index.getNRecords()).isNotEqualTo(0);

    assertThat(diskCache.getCacheFile(GribIndex.makeIndexFileName(s3File.getPath())).exists()).isTrue();
  }

  @Test
  public void shouldReuseCachedIndex() throws IOException {
    final DiskCache2 diskCache = useCache(true);

    final MFile s3File = new MFileS3(S3_DIR_WITHOUT_INDEX + filename);

    final GribIndex index =
        GribIndex.readOrCreateIndexFromSingleFile(isGrib1, s3File, CollectionUpdateType.always, logger);
    assertThat(index).isNotNull();
    assertThat(diskCache.getCacheFile(GribIndex.makeIndexFileName(s3File.getPath())).exists()).isTrue();
    final long cacheLastModified = diskCache.getCacheFile(GribIndex.makeIndexFileName(s3File.getPath())).lastModified();

    final GribIndex rereadIndex =
        GribIndex.readOrCreateIndexFromSingleFile(isGrib1, s3File, CollectionUpdateType.never, logger);
    assertThat(rereadIndex).isNotNull();
    final File rereadCachedIndex = diskCache.getCacheFile(GribIndex.makeIndexFileName(s3File.getPath()));
    assertThat(rereadCachedIndex.lastModified()).isEqualTo(cacheLastModified);
  }
}
