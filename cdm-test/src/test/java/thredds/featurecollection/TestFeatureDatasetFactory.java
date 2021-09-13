/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.featurecollection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Test open FeatureDatasets through FeatureDatasetFactoryManager */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestFeatureDatasetFactory {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[] {TestDir.cdmUnitTestDir + "formats/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf",
        FeatureType.CURVILINEAR});
    // coverage would give FeatureType.CURVILINEAR, but we have reverted
    result.add(new Object[] {TestDir.cdmUnitTestDir + "formats/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf",
        FeatureType.CURVILINEAR});

    return result;
  }

  String ds;
  FeatureType what;

  public TestFeatureDatasetFactory(String ds, FeatureType what) {
    this.ds = ds;
    this.what = what;
  }

  @Test
  public void testOpen() throws IOException, InvalidRangeException {
    System.out.printf("FeatureDatasetFactoryManager.open %s%n", ds);
    Formatter errlog = new Formatter();
    try (FeatureDataset fd = FeatureDatasetFactoryManager.open(what, ds, null, errlog)) {
      Assert.assertNotNull(errlog.toString() + " " + ds, fd);
      if (fd.getFeatureType().isCoverageFeatureType()) {
        testCoverage(ds);
      }
    }
  }

  void testCoverage(String endpoint) throws IOException, InvalidRangeException {
    System.out.printf("open CoverageDatasetFactory %s%n", endpoint);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(endpoint, errlog)) {
      assertThat(gds).isNotNull();
      assertThat(gds.getFeatureType()).isEqualTo(what);
    }
  }

}
