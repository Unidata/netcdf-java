/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import thredds.featurecollection.FeatureCollectionType;
import thredds.inventory.CollectionUpdateType;
import ucar.ma2.ArrayDouble;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.*;
import ucar.nc2.grib.collection.*;
import ucar.nc2.util.DebugFlagsImpl;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Test GribCollection Coordinates
 *
 * @author caron
 * @since 3/2/2015
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribCollectionCoordinates {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static CollectionUpdateType updateMode = CollectionUpdateType.always;

  @BeforeClass
  public static void before() throws IOException {
    GribIosp.debugIndexOnlyCount = 0;
    GribCollectionImmutable.countGC = 0;
    PartitionCollectionImmutable.countPC = 0;
    RandomAccessFile.enableDefaultGlobalFileCache();
    RandomAccessFile.setDebugLeaks(true);
    Grib.setDebugFlags(new DebugFlagsImpl("Grib/indexOnly"));
    GribCdmIndex.setGribCollectionCache(new ucar.nc2.util.cache.FileCacheGuava("GribCollectionCacheGuava", 100));
    GribCdmIndex.gribCollectionCache.resetTracking();
  }

  @AfterClass
  public static void after() {
    Grib.setDebugFlags(new DebugFlagsImpl());
  }

  /////////////////////////////////////////////////////////

  // check that all time variables are coordinates (TwoD PofP was not eliminating unused coordinates after merging)
  @Test
  public void testExtraCoordinates() throws IOException {
    Grib.setDebugFlags(new DebugFlagsImpl("Grib/debugGbxIndexOnly"));
    FeatureCollectionConfig config =
        new FeatureCollectionConfig("namAlaska22", "test/namAlaska22", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "gribCollections/namAlaska22/.*gbx9", null, null, null, "file", null);
    config.gribConfig.setExcludeZero(true); // no longer the default

    boolean changed = GribCdmIndex.updateGribCollection(config, CollectionUpdateType.always, logger);
    String topLevelIndex = GribCdmIndex.getTopIndexFileFromConfig(config).getAbsolutePath();

    logger.debug("changed = {}", changed);

    boolean ok = true;

    try (NetcdfDataset ds = NetcdfDatasets.openDataset(topLevelIndex)) {
      for (Variable vds : ds.getVariables()) {
        String stdname = vds.findAttributeString("standard_name", "no");
        if (!stdname.equalsIgnoreCase("time"))
          continue;

        logger.debug(" {} == {}", vds.getFullName(), vds.getClass().getName());
        assertThat((Object) vds).isInstanceOf(CoordinateAxis.class);

        // test that zero Intervals are removed
        if (vds instanceof CoordinateAxis1D) {
          CoordinateAxis1D axis = (CoordinateAxis1D) vds;
          if (axis.isInterval()) {
            for (int i = 0; i < axis.getSize(); i++) {
              double[] bound = axis.getCoordBounds(i);
              if (bound[0] == bound[1]) {
                logger.debug("ERR1 {}({}) = [{},{}]", vds.getFullName(), i, bound[0], bound[1]);
                ok = false;
              }
            }
          }

        } else if (vds instanceof CoordinateAxis2D) {
          CoordinateAxis2D axis2 = (CoordinateAxis2D) vds;
          if (axis2.isInterval()) {
            ArrayDouble.D3 bounds = axis2.getCoordBoundsArray();
            for (int i = 0; i < axis2.getShape(0); i++)
              for (int j = 0; j < axis2.getShape(1); j++) {
                double start = bounds.get(i, j, 0);
                double end = bounds.get(i, j, 1);
                if (start == end) {
                  logger.debug("ERR2 {}({},{}) = [{},{}]", vds.getFullName(), i, j, start, end);
                  ok = false;
                }
              }
          }
        }
      }
    }

    assertThat(ok).isTrue();
  }

  // make sure Best reftimes always increase
  @Test
  public void testBestReftimeMonotonic() throws IOException {
    FeatureCollectionConfig config =
        new FeatureCollectionConfig("gfs_2p5deg", "test/gfs_2p5deg", FeatureCollectionType.GRIB2,
            TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/.*grib2", null, null, null, "file", null);

    boolean changed = GribCdmIndex.updateGribCollection(config, updateMode, logger);
    logger.debug("changed = {}", changed);
    String topLevelIndex = GribCdmIndex.getTopIndexFileFromConfig(config).getAbsolutePath();
    boolean ok = true;

    try (NetcdfDataset ds = NetcdfDatasets.openDataset(topLevelIndex)) {
      for (Variable vds : ds.getVariables()) {
        String stdname = vds.findAttributeString("standard_name", "no");
        if (!stdname.equalsIgnoreCase("forecast_reference_time"))
          continue;

        logger.debug(" {} == {}", vds.getFullName(), vds.getClass().getName());
        assertThat((Object) vds).isInstanceOf(CoordinateAxis.class);
        CoordinateAxis1D axis = (CoordinateAxis1D) vds;

        // test that values are monotonic
        double last = Double.NaN;
        for (int i = 0; i < axis.getSize(); i++) {
          double val = axis.getCoordValue(i);
          if (i > 0 && (val < last)) {
            logger.debug("  {}({}) == {} < {}", vds.getFullName(), i, val, last);
            ok = false;
          }
          last = val;
        }
      }
    }

    assertThat(ok).isTrue();
  }

  @Test
  public void shouldNotAddScalarReftimeDimension() throws IOException {
    final String path = TestDir.cdmUnitTestDir + "gribCollections/mrms/MRMS_CONUS_BaseReflectivity_20230918_1700.grib2";

    try (NetcdfDataset ds = NetcdfDatasets.openDataset(path)) {
      final List<Dimension> dimensions = ds.getRootGroup().getDimensions();
      for (Dimension dimension : dimensions) {
        assertThat(dimension.getName()).doesNotContain("reftime");
      }
    }
  }
}
