/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.ncml.Aggregation;
import ucar.nc2.internal.util.DiskCache2;
import ucar.unidata.util.StringUtil2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

/** Test aggregation cache is getting used */
@Category(NeedsCdmUnitTest.class)
public class TestAggExistingCache {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  String ncml = "<?xml version='1.0' encoding='UTF-8'?>\n"
      + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n"
      + "    <aggregation dimName='time' type='joinExisting' recheckEvery='15 min'>\n"
      + "      <variableAgg name='ATssta' />\n" + "      <scan dateFormatMark='AT#yyyyDDD_HHmmss' location='"
      + TestDir.cdmUnitTestDir + "ncml/nc/pfeg/' suffix='.nc' />\n" + "    </aggregation>\n" + "</netcdf>";

  @Test
  public void testCacheIsUsed() throws IOException, InvalidRangeException {
    String filename = "file:TestAggExistingCache.xml";
    System.out.printf("%s%n", filename);

    String cacheDirName = tempFolder.newFolder().getAbsolutePath() + "/";
    cacheDirName = StringUtil2.replace(cacheDirName, '\\', "/"); // no nasty backslash
    System.out.printf("cacheDir=%s%n", cacheDirName);
    File cacheDir = new File(cacheDirName);
    FileUtils.deleteDirectory(cacheDir); // from commons-io
    assert !cacheDir.exists();

    DiskCache2 cache = new DiskCache2(cacheDirName, false, 0, 0);
    cache.setAlwaysUseCache(true);
    Assert.assertEquals(cacheDirName, cache.getRootDirectory());
    assert new File(cache.getRootDirectory()).exists();

    Aggregation.setPersistenceCache(cache);
    Aggregation.countCacheUse = 0;

    try (NetcdfDataset ncfile = NetcdfDatasets.openNcmlDataset(new StringReader(ncml), filename, null)) {
      System.out.println(" TestNcmlAggExisting.open " + filename);
      Array ATssta = ncfile.readSectionArray("ATssta(:,0,0,0)");
      Assert.assertEquals(4, ATssta.getSize());
    }
    Assert.assertEquals(0, Aggregation.countCacheUse);
    Aggregation.countCacheUse = 0;

    try (NetcdfDataset ncfile = NetcdfDatasets.openNcmlDataset(new StringReader(ncml), filename, null)) {
      System.out.println(" TestNcmlAggExisting.open " + filename);
      Array ATssta = ncfile.readSectionArray("ATssta(:,0,0,0)");
      Assert.assertEquals(4, ATssta.getSize());
    }
    Assert.assertEquals(8, Aggregation.countCacheUse);
  }

  private String ncml2 = "<?xml version='1.0' encoding='UTF-8'?>\n"
      + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n"
      + "    <aggregation dimName='time' type='joinExisting' timeUnitsChange='true'>\n"
      + "      <scan location='B:/CM2.1R' suffix='.nc' />\n" + "    </aggregation>\n" + "</netcdf>";

  @Ignore("files not available")
  @Test
  public void testCacheTiming() throws IOException {
    String filename = "file:testCacheTiming.xml";
    System.out.printf("%s%n", filename);

    String cacheDirName = tempFolder.newFolder().getAbsolutePath();
    System.out.printf("cacheDir=%s%n", cacheDirName);
    File cacheDir = new File(cacheDirName);
    FileUtils.deleteDirectory(cacheDir); // from commons-io
    assert !cacheDir.exists();

    DiskCache2 cache = new DiskCache2(cacheDirName, false, 0, 0);
    cache.setAlwaysUseCache(true);
    Assert.assertEquals(cache.getRootDirectory(), cacheDirName);
    assert new File(cache.getRootDirectory()).exists();

    Aggregation.setPersistenceCache(cache);
    Aggregation.countCacheUse = 0;

    long start = System.currentTimeMillis();

    try (NetcdfDataset ncfile = NetcdfDatasets.openNcmlDataset(new StringReader(ncml2), filename, null)) {
      System.out.printf("%nTestNcmlAggExisting.open %s%n", filename);
      Variable time = ncfile.findVariable("time");
      System.out.printf(" Variable %s%n", time.getNameAndDimensions());
      time.read();
    }
    System.out.printf(" countCacheUse = %d%n", Aggregation.countCacheUse);

    long took = System.currentTimeMillis() - start;
    System.out.printf(" first took %d msecs%n", took);

    Aggregation.countCacheUse = 0;
    start = System.currentTimeMillis();

    try (NetcdfDataset ncfile = NetcdfDatasets.openNcmlDataset(new StringReader(ncml2), filename, null)) {
      System.out.printf("%nTestNcmlAggExisting.open %s%n", filename);
      Variable time = ncfile.findVariable("time");
      System.out.printf(" Variable %s%n", time.getNameAndDimensions());
      time.read();
    }
    System.out.printf(" countCacheUse = %d%n", Aggregation.countCacheUse);
    took = System.currentTimeMillis() - start;
    System.out.printf(" second took %d msecs%n", took);
  }


}
