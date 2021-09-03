/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.coverage;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.ft2.coverage.GeoReferencedArray;
import ucar.nc2.ft2.coverage.HorizCoordSys;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.test.category.NeedsExternalResource;

/**
 * Test making a FeatureDatasetCoverage from opendap dataset.
 */
public class TestDodsCoverage {

  @Test
  @Category(NeedsExternalResource.class)
  public void testDodsSubset() throws Exception {
    String filename = "dods://thredds.ucar.edu/thredds/dodsC/grib/NCEP/GFS/CONUS_80km/best";
    System.out.printf("open %s%n", filename);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      String gribId = "Pressure_surface";
      Coverage coverage = gcs.findCoverage(gribId);
      Assert.assertNotNull(gribId, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);
      // Assert.assertArrayEquals(new int[]{65, 361, 720}, cs.getShape());

      LatLonRect llbb = gcs.getLatlonBoundingBox();
      LatLonRect llbb_subset = LatLonRect.builder(llbb.getLowerLeftPoint(), 20.0, llbb.getWidth() / 2).build();

      checkLatLonSubset(gcs, coverage, llbb_subset, new int[] {1, 35, 45});
    }
  }

  private void checkLatLonSubset(CoverageCollection gcs, Coverage coverage, LatLonRect bbox, int[] expectedShape)
      throws Exception {
    System.out.printf(" coverage llbb = %s width=%f%n", gcs.getLatlonBoundingBox().toString2(),
        gcs.getLatlonBoundingBox().getWidth());
    System.out.printf(" constrain bbox= %s width=%f%n", bbox.toString2(), bbox.getWidth());

    SubsetParams params = new SubsetParams().setLatLonBoundingBox(bbox).setTimePresent();
    GeoReferencedArray geo = coverage.readData(params);
    CoverageCoordSys gcs2 = geo.getCoordSysForData();
    Assert.assertNotNull("CoordSysForData", gcs2);
    System.out.printf(" data cs shape=%s%n", Arrays.toString(gcs2.getShape()));
    System.out.printf(" data shape=%s%n", Arrays.toString(geo.getData().getShape()));

    Assert.assertArrayEquals("CoordSys=Data shape", gcs2.getShape(), geo.getData().getShape());
    Assert.assertArrayEquals("expected data shape", expectedShape, geo.getData().getShape());
  }

}
