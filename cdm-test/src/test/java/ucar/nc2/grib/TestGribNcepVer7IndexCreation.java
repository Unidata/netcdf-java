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
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.grib.collection.GribIosp;
import ucar.nc2.grib.collection.PartitionCollectionImmutable;
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

import static thredds.inventory.CollectionUpdateType.always;

/** CDM Index Creation for NCEP datasets */
@Category(NeedsCdmUnitTest.class)
public class TestGribNcepVer7IndexCreation {
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
  @Category(NeedsCdmUnitTest.class)
  public void createNBMAlaska() throws IOException { // TWOD
    Grib.setDebugFlags(DebugFlags.create("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config =
        new FeatureCollectionConfig("NCEP_NBM_ALASKA", "test/NCEP_NBM_ALASKA", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "tds_index/NCEP/NBM/Alaska/ver7/.*gbx9", null, null, null, "file", null);
    System.out.printf("Create %s %s index for = %s%n", config.collectionName, config.ptype, config.spec);

    GribCdmIndex.updateGribCollection(config, always, logger);
    Grib.setDebugFlags(DebugFlags.create(""));
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void createMRMS_NLDN_GC() throws IOException { // TWOD
    Grib.setDebugFlags(DebugFlags.create("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config =
        new FeatureCollectionConfig("MRMS_NLDN_ver7", "test/MRMS_NLDN_ver7", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "tds_index/NCEP/MRMS/NLDN/MRMS_NLDN_20201027_0000.grib2.gbx9", null, null, null,
            "none", null);
    System.out.printf("Create %s %s index for = %s%n", config.collectionName, config.ptype, config.spec);

    GribCdmIndex.updateGribCollection(config, always, logger);
    Grib.setDebugFlags(DebugFlags.create(""));
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void createMRMS_NLDN_PC() throws IOException { // TWOD
    Grib.setDebugFlags(DebugFlags.create("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config =
        new FeatureCollectionConfig("MRMS_NLDN_ver7", "test/MRMS_NLDN_ver7", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "tds_index/NCEP/MRMS/NLDN/.*gbx9", null, null, null, "file", null);
    System.out.printf("Create %s %s index for = %s%n", config.collectionName, config.ptype, config.spec);

    GribCdmIndex.updateGribCollection(config, always, logger);
    Grib.setDebugFlags(DebugFlags.create(""));
  }

}
