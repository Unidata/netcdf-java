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
  public void TestAggCached() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "agg/caching/wqb.ncml";
    DatasetUrl durl = DatasetUrl.findDatasetUrl(filename);

    System.out.printf("==========%n");
    for (int i = 0; i < 2; i++) {
      NetcdfDataset ncd = NetcdfDataset.acquireDataset(durl, true, null);
      NetcdfDataset ncd2 = NetcdfDataset.wrap(ncd, NetcdfDataset.getEnhanceAll());
      Formatter out = new Formatter();
      assertThat(CompareNetcdf2.compareFiles(ncd, ncd2, out, false, false, false)).isTrue();
      System.out.printf("----------------%nfile=%s%n%s%n", filename, out);

      Set<Enhance> modes = ncd2.getEnhanceMode();
      showModes(modes);
      ncd2.close();
      System.out.printf("==========%n");
    }

    Formatter f = new Formatter();
    FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
    cache.showCache(f);
    System.out.printf("%s%n", f);

    List<String> cacheFiles = cache.showCache();
    assertThat(cacheFiles.size()).isEqualTo(6);
    assertThat(cacheFiles.stream().filter(cacheFile -> cacheFile.endsWith("wqb.ncml")).collect(Collectors.toList())).isNotEmpty();
  }

  private void showModes(Set<NetcdfDataset.Enhance> modes) {
    for (NetcdfDataset.Enhance mode : modes) {
      System.out.printf("%s,", mode);
    }
    System.out.printf("%n");
  }

  @Test
  // check if caching works
  public void TestAggCached2() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "agg/caching/wqb.ncml"; // joinExisting
    DatasetUrl durl = DatasetUrl.findDatasetUrl(filename);

    for (int i = 0; i < 2; i++) {
      System.out.printf("%n=====Iteration %d =====%n", i + 1);
      NetcdfDataset nc1 = NetcdfDataset.acquireDataset(durl, true, null); // put/get in the cache
      System.out.printf("-----------------------nc object == %d%n", nc1.hashCode());

      NetcdfDataset nc2 = new NetcdfDataset(nc1);
      System.out.printf("---new NetcdfDataset(nc1) object == %d%n", nc2.hashCode());
      FeatureDataset fd2 = ucar.nc2.ft.FeatureDatasetFactoryManager.wrap(ucar.nc2.constants.FeatureType.STATION, nc2,
          null, new Formatter(System.out));
      assertThat(fd2).isNotNull();
      System.out.printf("---FeatureDataset not failed%n");

      Formatter out = new Formatter();
      boolean ok = CompareNetcdf2.compareFiles(nc1, nc2, out, false, false, false);
      System.out.printf("---fd compare ok %s%n%s%n", ok, out);

      NetcdfDataset nc3 = NetcdfDataset.wrap(nc1, NetcdfDataset.getEnhanceAll());
      System.out.printf("---NetcdfDataset.wrap(nc1, enhance) object == %d%n", nc3.hashCode());
      FeatureDataset fd3 = ucar.nc2.ft.FeatureDatasetFactoryManager.wrap(ucar.nc2.constants.FeatureType.STATION, nc3,
          null, new Formatter(System.out));
      assertThat(fd3).isNotNull();
      System.out.printf("---FeatureDataset not failed %d%n", i);

      NetcdfDataset nc4 = NetcdfDataset.wrap(nc1, null);
      System.out.printf("---NetcdfDataset.wrap(nc1, null) object == %d%n", nc4.hashCode());
      FeatureDataset fd4 = ucar.nc2.ft.FeatureDatasetFactoryManager.wrap(ucar.nc2.constants.FeatureType.STATION, nc4,
          null, new Formatter(System.err));
      assertThat(fd4).isNotNull();
      System.out.printf("---FeatureDataset not failed%n");

      nc1.close();
    }

    FileCacheIF cache = NetcdfDataset.getNetcdfFileCache();
    cache.showCache();
  }
}
