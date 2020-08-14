/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 *  See LICENSE for license information.
 */
package ucar.nc2.ft.coverage;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Test GridCoverageDataset problems. */
@Category(NeedsCdmUnitTest.class)
public class TestCoverageProblems {

  @Test
  public void testFactory() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "formats/hdf4/AIRS.2003.01.24.116.L2.RetStd_H.v5.0.14.0.G07295101113.hdf";
    System.out.printf("TestCoverageProblems %s%n", endpoint);
    FeatureType expectType = FeatureType.SWATH;
    int ncoverages = 93;
    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assert cc != null;
      Assert.assertEquals(1, cc.getCoverageCollections().size());
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals("NGrids", ncoverages, gds.getCoverageCount());
      Assert.assertEquals(expectType, gds.getCoverageType());
    }
  }

}
