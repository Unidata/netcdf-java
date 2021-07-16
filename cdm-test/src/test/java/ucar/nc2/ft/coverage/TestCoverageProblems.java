/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.coverage;

import java.io.IOException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import static com.google.common.truth.Truth.assertThat;

/** Test GridCoverageDataset problems. */
@Category(NeedsCdmUnitTest.class)
public class TestCoverageProblems {

  @Test
  public void testFactory() throws IOException {
    String endpoint =
        TestDir.cdmUnitTestDir + "formats/hdf4/AIRS.2003.01.24.116.L2.RetStd_H.v5.0.14.0.G07295101113.hdf";
    System.out.printf("TestCoverageProblems %s%n", endpoint);
    FeatureType expectType = FeatureType.SWATH;
    int ncoverages = 93;
    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(endpoint)) {
      assertThat(cc).isNotNull();
      assertThat(cc.getCoverageCollections()).hasSize(1);
      CoverageCollection gds = cc.getCoverageCollections().get(0);
      assertThat(gds).isNotNull();
      assertThat(gds.getCoverageCount()).isEqualTo(ncoverages);
      assertThat(gds.getCoverageType()).isEqualTo(expectType);
    }
  }

}
