package ucar.nc2.ncml;

import static com.google.common.truth.Truth.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.util.cache.FileCacheIF;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import java.util.List;

/**
 * Test caching of NetcdfDataset in context of aggregation
 *
 * @author John
 * @since 9/11/13
 */
@Category(NeedsCdmUnitTest.class)
public class TestAggDatasetIsCached {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String JOIN_EXISTING_AGGREGATION = TestDir.cdmUnitTestDir + "agg/caching/wqb.ncml";

  @BeforeClass
  public static void setupClass() {
    // All datasets, once opened, will be added to this cache.
    NetcdfDataset.initNetcdfFileCache(10, 20, -1);
  }

  @AfterClass
  public static void cleanupClass() {
    // Undo global changes we made in setupSpec() so that they do not affect subsequent test classes.
    NetcdfDataset.shutdown();
  }

  @Test
  public void testAggCached() throws IOException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(JOIN_EXISTING_AGGREGATION);

    for (int i = 0; i < 2; i++) {
      NetcdfDataset ncd = NetcdfDataset.acquireDataset(durl, true, null);
      NetcdfDataset ncd2 = NetcdfDataset.wrap(ncd, NetcdfDataset.getEnhanceAll());
      Formatter out = new Formatter();
      assertThat(CompareNetcdf2.compareFiles(ncd, ncd2, out, false, false, false)).isTrue();
      logger.debug(out.toString());

      Set<Enhance> modes = ncd2.getEnhanceMode();
      logger.debug(modes.toString());
      ncd2.close();
    }

    Formatter f = new Formatter();
    FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
    cache.showCache(f);
    logger.debug(f.toString());

    List<String> cacheFiles = cache.showCache();
    assertThat(cacheFiles.size()).isEqualTo(6);
    assertThat(cacheFiles.stream().filter(cacheFile -> cacheFile.endsWith("wqb.ncml")).collect(Collectors.toList()))
        .isNotEmpty();
  }

  @Test
  public void testAggCached2() throws IOException {
    DatasetUrl durl = DatasetUrl.findDatasetUrl(JOIN_EXISTING_AGGREGATION);

    for (int i = 0; i < 2; i++) {
      logger.debug("=====Iteration {} =====", i + 1);
      NetcdfDataset nc1 = NetcdfDataset.acquireDataset(durl, true, null); // put/get in the cache
      logger.debug("-----------------------nc object == {}", nc1.hashCode());

      NetcdfDataset nc2 = new NetcdfDataset(nc1);
      logger.debug("---new NetcdfDataset(nc1) object == {}", nc2.hashCode());
      FeatureDataset fd2 = ucar.nc2.ft.FeatureDatasetFactoryManager.wrap(ucar.nc2.constants.FeatureType.STATION, nc2,
          null, new Formatter(System.out));
      assertThat(fd2).isNotNull();
      logger.debug("---FeatureDataset not failed");

      Formatter out = new Formatter();
      assertThat(CompareNetcdf2.compareFiles(nc1, nc2, out, false, false, false)).isTrue();
      logger.debug(out.toString());

      NetcdfDataset nc3 = NetcdfDataset.wrap(nc1, NetcdfDataset.getEnhanceAll());
      logger.debug("---NetcdfDataset.wrap(nc1, enhance) object == {}", nc3.hashCode());
      FeatureDataset fd3 = ucar.nc2.ft.FeatureDatasetFactoryManager.wrap(ucar.nc2.constants.FeatureType.STATION, nc3,
          null, new Formatter(System.out));
      assertThat(fd3).isNotNull();
      logger.debug("---FeatureDataset not failed {}", i);

      NetcdfDataset nc4 = NetcdfDataset.wrap(nc1, null);
      logger.debug("---NetcdfDataset.wrap(nc1, null) object == {}", nc4.hashCode());
      FeatureDataset fd4 = ucar.nc2.ft.FeatureDatasetFactoryManager.wrap(ucar.nc2.constants.FeatureType.STATION, nc4,
          null, new Formatter(System.err));
      assertThat(fd4).isNotNull();
      logger.debug("---FeatureDataset not failed");

      nc1.close();
    }

    FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
    cache.showCache();
  }
}
