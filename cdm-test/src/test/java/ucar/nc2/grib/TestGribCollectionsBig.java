/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.grib.collection.*;
import ucar.nc2.util.DebugFlags;
import ucar.nc2.internal.cache.FileCache;
import ucar.nc2.internal.cache.FileCacheIF;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.util.Formatter;

/**
 * Look for missing data in large Grib Collections.
 * These numbers will be different if we index with unionRuntimeCoords
 * Indicates that coordinates are not matching,
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribCollectionsBig {
  String topdir = TestDir.cdmUnitTestDir + "gribCollections/rdavm";

  @BeforeClass
  static public void before() {
    GribIosp.debugIndexOnlyCount = 0;
    GribCollectionImmutable.countGC = 0;
    PartitionCollectionImmutable.countPC = 0;
    RandomAccessFile.enableDefaultGlobalFileCache();
    RandomAccessFile.setDebugLeaks(true);
    Grib.setDebugFlags(DebugFlags.create("Grib/indexOnly"));
    GribCdmIndex.setGribCollectionCache(new ucar.nc2.internal.cache.FileCacheGuava("GribCollectionCacheGuava", 100));
    GribCdmIndex.gribCollectionCache.resetTracking();
  }

  @AfterClass
  static public void after() {
    Grib.setDebugFlags(DebugFlags.create(""));
    Formatter out = new Formatter(System.out);

    FileCacheIF cache = GribCdmIndex.gribCollectionCache;
    if (cache != null) {
      cache.showTracking(out);
      cache.showCache(out);
      cache.clearCache(false);
    }

    FileCacheIF rafCache = RandomAccessFile.getGlobalFileCache();
    if (rafCache != null) {
      rafCache.showCache(out);
    }

    System.out.printf("            countGC=%7d%n", GribCollectionImmutable.countGC);
    System.out.printf("            countPC=%7d%n", PartitionCollectionImmutable.countPC);
    System.out.printf("    countDataAccess=%7d%n", GribIosp.debugIndexOnlyCount);
    System.out.printf(" total files needed=%7d%n",
        GribCollectionImmutable.countGC + PartitionCollectionImmutable.countPC + GribIosp.debugIndexOnlyCount);

    FileCache.shutdown();
    RandomAccessFile.setGlobalFileCache(null);
    TestDir.checkLeaks();
    RandomAccessFile.setDebugLeaks(false);
  }


  @Test
  public void testSRC() {
    GribCollectionMissing.Count count =
        GribCollectionMissing.read(topdir + "/ds083.2/grib1/2008/2008.10/ds083.2_Aggregation-2008.10.ncx4");
    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);

    assert count.nread == 35464 : count.nread;
    assert count.nmiss == 0;
  }

  @Test
  public void testTP() {
    GribCollectionMissing.Count count =
        GribCollectionMissing.read(topdir + "/ds083.2/grib1/2008/ds083.2_Aggregation-2008.ncx4");

    // local: 104 secs
    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);

    assert count.nread == 418704 : count.nread;
    assert count.nmiss == 0;
  }

  @Ignore("takes too long")
  @Test
  public void testPofP() {
    RandomAccessFile.setDebugLeaks(true);
    GribCollectionMissing.Count count = GribCollectionMissing.read(topdir + "/ds083.2/grib1/ds083.2_Aggregation.ncx4");

    System.out.printf("%n%50s == %d/%d/%d%n", "total", count.nerrs, count.nmiss, count.nread);

    assert count.nerrs == 0;
    assert count.nmiss == 492158;
    assert count.nread == 7038851;
  }

}
