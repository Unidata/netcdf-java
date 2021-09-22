/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import java.util.Formatter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

/** Test things are ok when wrapping by a Dataset */
@Category(NeedsCdmUnitTest.class)
public class TestDatasetWrapProblem {

  @Test
  public void testDatasetWrap() throws Exception {
    doOne(TestDir.cdmUnitTestDir + "conventions/nuwg/eta.nc");
  }

  private void doOne(String filename) throws Exception {
    DatasetUrl durl = DatasetUrl.create(null, filename);
    try (NetcdfFile ncfile = NetcdfDatasets.acquireFile(durl, null);
        NetcdfDataset ncWrap = NetcdfDatasets.enhance(ncfile, NetcdfDataset.getDefaultEnhanceMode(), null)) {

      NetcdfDataset ncd = NetcdfDatasets.acquireDataset(durl, true, null);
      System.out.println(" dataset wraps= " + durl.getTrueurl());

      Formatter errlog = new Formatter();
      boolean ok = CompareNetcdf2.compareFiles(ncd, ncWrap, errlog);
      if (!ok) {
        System.out.printf("FAIL %s %s%n", durl, errlog);
      }
      Assert.assertTrue(ok);
    }
  }

  @Test
  public void testMissingDataReplaced() throws Exception {
    // this one has misssing longitude data, but not getting set to NaN
    String filename = TestDir.cdmUnitTestDir + "/ft/point/netcdf/Surface_Synoptic_20090921_0000.nc";
    System.out.println(" testMissingDataReplaced= " + filename);

    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      String varName = "Lon";
      Variable wrap = ds.findVariable(varName);
      assert wrap != null;
      Array data_wrap = wrap.read();
      double[] data_wrap_double = (double[]) data_wrap.get1DJavaArray(DataType.DOUBLE);

      assert wrap instanceof CoordinateAxis1D;
      CoordinateAxis1D axis = (CoordinateAxis1D) wrap;

      Assert.assertTrue(new CompareNetcdf2().compareData(varName, data_wrap_double, axis.getCoordValues()));
    }
  }

  @Test
  public void testLongitudeWrap() throws Exception {
    // this one was getting clobbered by longitude wrapping
    String filename = TestDir.cdmUnitTestDir + "/ft/profile/sonde/sgpsondewnpnC1.a1.20020507.112400.cdf";
    System.out.println(" testLongitudeWrap= " + filename);

    try (NetcdfFile ncfile = NetcdfFiles.open(filename); NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {

      String varName = "lon";
      Variable org = ncfile.findVariable(varName);
      Variable wrap = ds.findVariable(varName);

      Array data_org = org.read();
      Array data_wrap = wrap.read();

      boolean ok;
      ok = CompareNetcdf2.compareData(varName, data_org, data_wrap);

      assert wrap instanceof CoordinateAxis1D;
      CoordinateAxis1D axis = (CoordinateAxis1D) wrap;

      double[] data_org_double = (double[]) data_org.get1DJavaArray(DataType.DOUBLE);
      ok &= new CompareNetcdf2().compareData(varName, data_org_double, axis.getCoordValues());

      assert ok;
    }
  }
}
