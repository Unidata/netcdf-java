/*
 * Copyright (c) 1998-2022 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.util.cache;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CancelTask;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.*;
import ucar.unidata.util.test.category.NeedsUcarNetwork;
import ucar.unidata.util.test.category.NeedsExternalResource;

/** Test FileCache. */
@Category(NeedsExternalResource.class)
public class TestS3NetcdfFileCache {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static FileCache cache;
  private static FileFactory factory = new MyFileFactory();

  // Taken from TestS3Read
  private static final String COMMON_G16_KEY =
      "ABI-L1b-RadC/2017/242/00/OR_ABI-L1b-RadC-M3C01_G16_s20172420002168_e20172420004540_c20172420004583.nc";
  private static final String NCAR_PROFILE_NAME = "stratus-profile"; // generally not available
  private static final String NCAR_G16_S3_URI = "cdms3://" + NCAR_PROFILE_NAME
      + "@stratus.ucar.edu/unidata-netcdf-zarr-testing?netcdf-java/test/" + COMMON_G16_KEY;

  @BeforeClass
  static public void setUp() {
    cache = new FileCache(5, 100, 60 * 60);
  }

  @AfterClass
  static public void tearDown() {
    cache.clearCache(true);
  }

  static public class MyFileFactory implements FileFactory {
    public FileCacheable open(DatasetUrl location, int buffer_size, CancelTask cancelTask, Object iospMessage)
        throws IOException {
      return NetcdfDatasets.openFile(location, buffer_size, cancelTask, iospMessage);
    }
  }

  private int count = 0;

  private void loadFileIntoCache(String s3url, FileCache cache) {
    try {
      DatasetUrl durl = DatasetUrl.create(null, s3url);
      cache.acquire(factory, durl);
      count++;
    } catch (IOException e) {
      fail(" *** failed on " + s3url);
    }
  }

  @Test
  @Category(NeedsUcarNetwork.class)
  public void testS3NetcdfFileCache() throws IOException {
    loadFileIntoCache(NCAR_G16_S3_URI, cache);
    assertThat(cache.hits.get()).isEqualTo(0);
    assertThat(cache.miss.get()).isEqualTo(1);
    cache.showCache(new Formatter(System.out));

    // count cache size
    Map<Object, FileCache.CacheElement> map = cache.getCache();
    assertThat(map.values().size()).isEqualTo(count);

    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      assertThat(elem.list.size()).isEqualTo(1);
    }

    // load same file again - should be added to the list, rather than creating a new elem
    int saveCount = count;
    loadFileIntoCache(NCAR_G16_S3_URI, cache);

    map = cache.getCache();
    cache.showCache(new Formatter(System.out));
    // The file is currently locked, so no cache hit.
    assertThat(cache.hits.get()).isEqualTo(0);
    assertThat(cache.miss.get()).isEqualTo(2);
    assertThat(map.values().size()).isEqualTo(saveCount); // problem

    for (Object key : map.keySet()) {
      FileCache.CacheElement elem = map.get(key);
      assertThat(elem.list.size()).isEqualTo(2);
      checkAllSame(elem.list);
    }
    cache.clearCache(true);

    // TODO: Verify that hits actually happen
  }

  void checkAllSame(List<FileCache.CacheElement.CacheFile> list) {
    FileCache.CacheElement.CacheFile first = null;
    for (FileCache.CacheElement.CacheFile file : list) {
      assertThat(file.isLocked.get()).isTrue();
      assertThat(file.countAccessed).isEqualTo(0); // countAccessed not incremented until its closed, so == 0
      assertThat(file.lastAccessed).isNotEqualTo(0);

      if (first == null)
        first = file;
      else {
        assertThat(first.ncfile.getLocation()).isEqualTo(file.ncfile.getLocation());
        assertThat(first.lastAccessed).isLessThan(file.lastAccessed);
      }
    }
  }
}
