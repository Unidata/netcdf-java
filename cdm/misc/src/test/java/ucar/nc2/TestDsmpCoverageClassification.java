/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import java.io.IOException;
import java.util.Formatter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.ft2.coverage.adapter.DtCoverageCS;
import ucar.nc2.ft2.coverage.adapter.DtCoverageCSBuilder;
import ucar.nc2.ft2.coverage.adapter.DtCoverageDataset;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Test Dsmp Coverage Classification */
@Category(NeedsCdmUnitTest.class)
public class TestDsmpCoverageClassification {

  String endpoint = TestDir.cdmUnitTestDir + "formats/dmsp/F14200307192230.s.OIS";
  FeatureType expectType = FeatureType.SWATH;
  int domain = 2;
  int range = 3;
  int ncoverages = 2;

  @Test
  public void testAdapter() throws IOException {
    System.out.printf("open %s%n", endpoint);

    try (DtCoverageDataset gds = DtCoverageDataset.open(endpoint)) {
      Assert.assertNotNull(endpoint, gds);
      Assert.assertEquals("NGrids", ncoverages, gds.getGrids().size());
      Assert.assertEquals(expectType, gds.getCoverageType());
    }

    // check DtCoverageCS
    try (NetcdfDataset ds = NetcdfDataset.openDataset(endpoint)) {
      Formatter errlog = new Formatter();
      DtCoverageCSBuilder builder = DtCoverageCSBuilder.classify(ds, errlog); // uses cs with largest # axes
      Assert.assertNotNull(errlog.toString(), builder);
      DtCoverageCS cs = builder.makeCoordSys();
      Assert.assertEquals(expectType, cs.getCoverageType());
      Assert.assertEquals("Domain", domain, CoordinateSystem.makeDomain(cs.getCoordAxes()).size());
      Assert.assertEquals("Range", range, cs.getCoordAxes().size());
    }
  }

  @Test
  public void testFactory() throws IOException {
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

