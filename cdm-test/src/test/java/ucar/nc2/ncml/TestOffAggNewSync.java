/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;

@Category(NeedsCdmUnitTest.class)
public class TestOffAggNewSync {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  String dataDir = TestDir.cdmUnitTestDir + "formats/gini/";
  int ntimes = 3;

  private String aggExistingSync = "<?xml version='1.0' encoding='UTF-8'?>\n"
      + "<netcdf xmlns='http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2'>\n"
      + "  <aggregation  dimName='time' type='joinExisting' recheckEvery='1 sec' >\n"
      + "    <variableAgg name='IR_WV'/>\n" + "    <scan location='" + dataDir
      + "' regExp='WEST-CONUS_4km_3.9.*\\.gini' dateFormatMark='WEST-CONUS_4km_3.9_#yyyyMMdd_HHmm'/>\n"
      + "  </aggregation>\n" + "</netcdf>";

  @Test
  @Ignore("file in use - testing artifact")
  public void testMove() throws IOException, InterruptedException {
    String fname = dataDir + "WEST-CONUS_4km_3.9_20050912_2130.gini";
    if (!TestOffAggUpdating.move(fname))
      System.out.printf("Move failed on %s%n", fname);
    System.out.printf("%s%n", aggExistingSync);
    try (NetcdfDataset ncfile =
        NetcdfDatasets.openNcmlDataset(new StringReader(aggExistingSync), "aggExistingSync", null)) {
      testAggCoordVar(ncfile, ntimes - 1);
    }

    if (!TestOffAggUpdating.moveBack(fname))
      System.out.printf("Move back failed on %s%n", fname);

    try (NetcdfDataset ncfile =
        NetcdfDatasets.openNcmlDataset(new StringReader(aggExistingSync), "aggExistingSync", null)) {
      testAggCoordVar(ncfile, ntimes);
    }
    System.out.printf("ok testMove%n");
  }

  @Test
  @Ignore("file in use - testing artifact")
  public void testRemove() throws IOException, InterruptedException {
    try (NetcdfDataset ncfile =
        NetcdfDatasets.openNcmlDataset(new StringReader(aggExistingSync), "aggExistingSync", null)) {
      testAggCoordVar(ncfile, ntimes);
      System.out.println("");
    }

    String fname = dataDir + "WEST-CONUS_4km_3.9_20050912_2130.gini";
    boolean ok = TestOffAggUpdating.move(fname);
    int nfiles = ok ? ntimes - 1 : ntimes; // sometimes fails

    try (NetcdfFile ncfile =
        NetcdfDatasets.openNcmlDataset(new StringReader(aggExistingSync), "aggExistingSync", null)) {
      testAggCoordVar(ncfile, nfiles);
    }

    TestOffAggUpdating.moveBack(fname);
    System.out.printf("ok testRemove%n");
  }

  private void testAggCoordVar(NetcdfFile ncfile, int n) throws IOException {
    Variable time = ncfile.findVariable("time");
    assert null != time;
    assert time.getShortName().equals("time");
    assert time.getRank() == 1;
    assert time.getSize() == n : time.getSize() + " != " + n;
    assert time.getShape()[0] == n;

    assert time.getDimension(0) == ncfile.findDimension("time");

    Array data = time.read();
    assert data.getRank() == 1;
    assert data.getSize() == n;
    assert data.getShape()[0] == n;
  }

}


