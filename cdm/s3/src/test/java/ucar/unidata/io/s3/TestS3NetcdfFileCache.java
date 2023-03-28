/*
 * Copyright (c) 1998-2023 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.io.s3;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.cache.FileCache;
import ucar.nc2.util.cache.FileCacheable;
import ucar.nc2.util.cache.FileFactory;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.*;

/** Test FileCache. */
public class TestS3NetcdfFileCache {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static FileCache cache;
  private static FileFactory factory = new MyFileFactory();

  private static final String FILENAME = "testData.nc";
  private static final String BUCKET = "thredds-test-data"; // public S3 bucket owned by Unidata
  private static final String S3_URI = "cdms3:" + BUCKET + "?" + FILENAME;

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

  private FileCacheable loadFileIntoCache(String s3url, FileCache cache) {
    FileCacheable handle = null;
    try {
      DatasetUrl durl = DatasetUrl.create(null, s3url);
      handle = cache.acquire(factory, durl);
    } catch (IOException e) {
      fail(" *** failed on " + s3url);
    }
    return handle;
  }

  @Test
  public void testS3NetcdfFileCache() throws IOException {
    FileCacheable handle = loadFileIntoCache(S3_URI, cache);
    Formatter formatter = new Formatter();
    cache.showStats(formatter);
    assertThat(formatter.toString()).contains("hits= 0 miss= 1 nfiles= 1 elems= 1");

    // load same file again - should be added to the list, rather than creating a new elem
    loadFileIntoCache(S3_URI, cache);
    formatter = new Formatter();
    cache.showStats(formatter);
    // The file is currently locked, so no cache hit.
    assertThat(formatter.toString()).contains("hits= 0 miss= 2 nfiles= 2 elems= 1");

    handle.close();
    loadFileIntoCache(S3_URI, cache);
    formatter = new Formatter();
    cache.showStats(formatter);
    // The file is now unlocked, so this should result in a hit.
    assertThat(formatter.toString()).contains("hits= 1 miss= 2 nfiles= 2 elems= 1");
  }
}
