/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.iosp;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.grib.GribIndexCache;
import ucar.nc2.internal.util.DiskCache2;
import ucar.unidata.util.StringUtil2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.File;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test GRIB disk caching
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribDiskCache {
  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testDiskCache() throws Exception {
    String cacheDirName = tempFolder.newFolder().getAbsolutePath() + "/";
    cacheDirName = StringUtil2.replace(cacheDirName, '\\', "/"); // no nasty backslash

    DiskCache2 cache = new DiskCache2(cacheDirName, false, 0, 0);
    cache.setAlwaysUseCache(true);
    assertThat(cacheDirName).isEqualTo(cache.getRootDirectory());
    assertThat(new File(cache.getRootDirectory()).exists()).isTrue();
    GribIndexCache.setDiskCache2(cache);

    String dataDir = TestDir.cdmUnitTestDir + "testCache";
    File dd = new File(dataDir);

    for (File data : dd.listFiles()) {
      String name = data.getName();
      if (name.contains(".gbx"))
        data.delete();
      if (name.contains(".ncx"))
        data.delete();
    }

    for (File data : dd.listFiles()) {
      System.out.printf("Open %s%n", data.getPath());
      NetcdfFile ncfile = NetcdfFiles.open(data.getPath());
      ncfile.close();
    }

    for (File data : dd.listFiles()) {
      String name = data.getName();
      assertThat(name.contains(".gbx")).isFalse();
      assertThat(name.contains(".ncx")).isFalse();
      if (data.getName().endsWith(".grib1") || data.getName().endsWith(".grib2")) {
        String index = data.getPath() + ".ncx4";
        File indexFile = cache.getCacheFile(index);
        assertThat(indexFile).isNotNull();
        assertThat(indexFile.exists()).isTrue();
      }
    }
  }
}
