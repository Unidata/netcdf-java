package ucar.nc2.grib;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.CollectionUpdateType;
import ucar.nc2.grib.collection.*;
import ucar.nc2.internal.cache.FileCache;
import ucar.nc2.internal.cache.FileCacheIF;
import ucar.nc2.internal.util.DiskCache2;
import ucar.nc2.util.DebugFlags;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

/** CDM Index Creation for NCEP datasets */
@Category(NeedsCdmUnitTest.class)
public class TestGribNcepIndexCreation {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static final boolean show = false;
  private static CollectionUpdateType updateMode = CollectionUpdateType.always;

  @BeforeClass
  static public void before() {
    GribIosp.debugIndexOnlyCount = 0;
    GribCollectionImmutable.countGC = 0;
    PartitionCollectionImmutable.countPC = 0;
    RandomAccessFile.enableDefaultGlobalFileCache();
    RandomAccessFile.setDebugLeaks(true);
    // Grib.setDebugFlags(DebugFlags.create("Grib/indexOnly"));
    GribCdmIndex.setGribCollectionCache(new ucar.nc2.internal.cache.FileCacheGuava("GribCollectionCacheGuava", 100));
    GribCdmIndex.gribCollectionCache.resetTracking();

    // make sure that the indexes are created with the data files
    DiskCache2 diskCache = GribIndexCache.getDiskCache2();
    diskCache.setNeverUseCache(true);
    diskCache.setAlwaysUseCache(false);
  }

  @AfterClass
  static public void after() {
    Grib.setDebugFlags(DebugFlags.create(""));
    Formatter out = new Formatter(System.out);

    FileCacheIF cache = GribCdmIndex.gribCollectionCache;
    if (show && cache != null) {
      cache.showTracking(out);
      cache.showCache(out);
      cache.clearCache(false);
    }

    FileCacheIF rafCache = RandomAccessFile.getGlobalFileCache();
    if (show && rafCache != null) {
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
  public void createRUC2() throws IOException {
    FeatureCollectionConfig config = new FeatureCollectionConfig("NDFD-CONUS-RUC2_CONUS_40km",
        "test/NDFD-RUC2_CONUS_40km", FeatureCollectionType.GRIB1,
        TestDir.cdmUnitTestDir + "tds/ncep/RUC2_CONUS_40km.*grib1", null, null, null, "file", null);

    System.out.printf("===NDFD-RUC2_CONUS_20km_hybrid-5km%n");
    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    System.out.printf("changed = %s%n", changed);
  }

}
