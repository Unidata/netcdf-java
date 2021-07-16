/* Copyright Unidata */
package ucar.nc2.ft.coverage;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.calendar.CalendarDate;
import ucar.unidata.geoloc.*;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.category.NeedsExternalResource;
import ucar.unidata.util.test.TestDir;
import java.lang.invoke.MethodHandles;

public class TestCoverageHorizSubset {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testMSG() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "transforms/Eumetsat.VerticalPerspective.grb";
    System.out.printf("open %s%n", filename);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      String gribId = "VAR_3-0-8";
      Coverage coverage = gcs.findCoverageByAttribute(Grib.VARIABLE_ID_ATTNAME, gribId); // "Pixel_scene_type");
      Assert.assertNotNull(gribId, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);

      Assert.assertEquals("coordSys", 3, cs.getShape().length);

      // bbox = ll: 16.79S 20.5W+ ur: 14.1N 20.09E
      LatLonRect bbox = new LatLonRect(-16.79, -20.5, 14.1, 20.9);

      Projection p = hcs.getTransform().getProjection();
      ProjectionRect prect = p.latLonToProjBB(bbox); // must override default implementation
      System.out.printf("latLonToProjBB: %s -> %s %n", bbox, prect);

      ProjectionRect expected =
          new ProjectionRect(ProjectionPoint.create(-2129.5688, -1793.0041), 4297.8453, 3308.3885);
      assertThat(prect.nearlyEquals(expected)).isTrue();

      LatLonRect bb2 = p.projToLatLonBB(prect);
      System.out.printf("projToLatLonBB: %s -> %s %n", prect, bb2);

      SubsetParams params = new SubsetParams().set(SubsetParams.latlonBB, bbox);
      GeoReferencedArray geo = coverage.readData(params);

      int[] expectedShape = new int[] {1, 363, 479};
      Assert.assertArrayEquals(expectedShape, geo.getData().getShape());
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testLatLonSubset() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "conventions/problem/SUPER-NATIONAL_latlon_IR_20070222_1600.nc";
    System.out.printf("open %s%n", filename);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      String gribId = "micron11";
      Coverage coverage = gcs.findCoverage(gribId);
      Assert.assertNotNull(gribId, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);

      Assert.assertEquals("rank", 2, cs.getShape().length);

      LatLonRect bbox = new LatLonRect.Builder(LatLonPoint.create(40.0, -100.0), 10.0, 20.0).build();
      checkLatLonSubset(gcs, coverage, bbox, new int[] {141, 281});

      bbox = new LatLonRect.Builder(LatLonPoint.create(-40.0, -180.0), 120.0, 300.0).build();
      checkLatLonSubset(gcs, coverage, bbox, new int[] {800, 1300});
    }
  }

  // longitude subsetting (CoordAxis1D regular)
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testLongitudeSubset() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "tds/ncep/GFS_Global_onedeg_20100913_0000.grib2";
    System.out.printf("open %s%n", filename);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      String gribId = "VAR_0-3-0_L1";
      Coverage coverage = gcs.findCoverageByAttribute(Grib.VARIABLE_ID_ATTNAME, gribId); // "Pressure_Surface");
      Assert.assertNotNull(gribId, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);
      Assert.assertEquals("rank", 3, cs.getShape().length);

      LatLonRect bbox = new LatLonRect.Builder(LatLonPoint.create(40.0, -100.0), 10.0, 20.0).build();
      checkLatLonSubset(gcs, coverage, bbox, new int[] {1, 11, 21});
    }
  }

  @Test
  @Category(NeedsExternalResource.class)
  public void testCdmRemoteSubset() throws Exception {
    String filename =
        "cdmremote:https://thredds-dev.unidata.ucar.edu/thredds/cdmremote/grib/NCEP/NAM/CONUS_40km/conduit/best";
    System.out.printf("open %s%n", filename);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      String gribId = "Pressure_hybrid";
      Coverage coverage = gcs.findCoverage(gribId);
      Assert.assertNotNull(gribId, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);
      Assert.assertEquals("rank", 4, cs.getShape().length);

      LatLonRect llbb_subset = new LatLonRect(-15, -140, 55, 30);

      System.out.println("subset lat/lon bbox= " + llbb_subset);

      checkLatLonSubset(gcs, coverage, llbb_subset, new int[] {1, 1, 129, 185});
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCrossLongitudeSeam() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "tds/ncep/GFS_Global_0p5deg_20100913_0000.grib2";
    System.out.printf("open %s%n", filename);

    try (FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      Assert.assertNotNull(filename, cc);
      CoverageCollection gcs = cc.findCoverageDataset(FeatureType.GRID);
      Assert.assertNotNull("gcs", gcs);
      String gribId = "VAR_2-0-0_L1";
      Coverage coverage = gcs.findCoverageByAttribute(Grib.VARIABLE_ID_ATTNAME, gribId); // Land_cover_0__sea_1__land_surface
      Assert.assertNotNull(gribId, coverage);

      CoverageCoordSys cs = coverage.getCoordSys();
      Assert.assertNotNull("coordSys", cs);
      System.out.printf(" org coverage shape=%s%n", Arrays.toString(cs.getShape()));

      HorizCoordSys hcs = cs.getHorizCoordSys();
      Assert.assertNotNull("HorizCoordSys", hcs);
      Assert.assertEquals("rank", 3, cs.getShape().length);

      LatLonRect bbox = LatLonRect.builder(40.0, -100.0, 10.0, 120.0).build();
      checkLatLonSubset(gcs, coverage, bbox, new int[] {1, 61, 441});
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testLongitudeSubsetWithHorizontalStride() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "tds/ncep/GFS_Global_onedeg_20100913_0000.grib2";
    String gribId = "VAR_0-3-0_L1";

    try (FeatureDatasetCoverage featureDatasetCoverage = CoverageDatasetFactory.open(filename)) {
      CoverageCollection coverageCollection = featureDatasetCoverage.findCoverageDataset(FeatureType.GRID);
      Coverage coverage = coverageCollection.findCoverageByAttribute(Grib.VARIABLE_ID_ATTNAME, gribId);

      final CalendarDate validTime = CalendarDate.fromUdunitIsoDate(null, "2010-09-21T00:00:00Z").orElseThrow();

      HorizCoordSys origHcs = coverage.getCoordSys().getHorizCoordSys();

      // Next, create the subset param and make the request
      SubsetParams params = new SubsetParams();

      // subset Time axis
      params.setTime(validTime);

      // subset across the seam
      final LatLonRect subsetLatLonRequest = new LatLonRect.Builder(LatLonPoint.create(-15, -10), 30, 20).build();
      params.setLatLonBoundingBox(subsetLatLonRequest);

      // set a horizontal stride
      final int stride = 2;
      params.setHorizStride(stride);

      // make subset
      GeoReferencedArray geo = coverage.readData(params);

      // Check that TimeAxis is 1D, has one coordinate, and it's equal to the time we requested
      CoverageCoordAxis timeAxis = geo.getCoordSysForData().getTimeAxis();
      assertThat(timeAxis).isInstanceOf(CoverageCoordAxis1D.class);
      CoverageCoordAxis1D timeAxis1d = (CoverageCoordAxis1D) timeAxis;
      assertThat(timeAxis1d.getNcoords()).isEqualTo(1);
      assertThat(timeAxis1d.makeDate((double) timeAxis1d.getCoordObject(0))).isEqualTo(validTime);

      // make sure the bounding box requested by subset is contained within the
      // horizontal coordinate system of the GeoReferencedArray produced by the
      // subset
      HorizCoordSys subsetHcs = geo.getCoordSysForData().getHorizCoordSys();
      assertThat(subsetLatLonRequest.containedIn(subsetHcs.calcLatLonBoundingBox())).isTrue();

      // make sure resolution of the lat and lon grids of the subset take into account the stride
      // by comparing the resolution
      CoverageCoordAxis1D origLonAxis = origHcs.getXAxis();
      CoverageCoordAxis1D origLatAxis = origHcs.getYAxis();
      CoverageCoordAxis1D subsetLonAxis = subsetHcs.getXAxis();
      CoverageCoordAxis1D subsetLatAxis = subsetHcs.getYAxis();
      final double tol = 0.001;
      assertThat(origLonAxis.getResolution()).isNotWithin(tol).of(subsetLonAxis.getResolution());
      assertThat(origLonAxis.getResolution()).isWithin(tol).of(subsetLonAxis.getResolution() / stride);
      assertThat(origLatAxis.getResolution()).isNotWithin(tol).of(subsetLatAxis.getResolution());
      assertThat(origLatAxis.getResolution()).isWithin(tol).of(subsetLatAxis.getResolution() / stride);

      // check to make sure we get data from both sides of the seam by testing that
      // half of the array isn't empty.
      // slice along longitude in the middle of the array.
      Array geoData = geo.getData();
      int middle = geoData.getShape()[1] / 2;
      Array data = geo.getData().slice(2, middle).reduce();
      // flip the array
      int numValsToSum = 3;
      Array dataFlip = data.flip(0);
      Section sec = Section.builder().appendRange(0, numValsToSum).build();
      IndexIterator dii = data.getIndexIterator();
      IndexIterator diiFlip = dataFlip.getIndexIterator();

      final double initialSumVal = 0;
      double sumData = initialSumVal;
      double sumDataFlip = initialSumVal;
      for (int i = 0; i < numValsToSum - 1; i++) {
        double val = dii.getDoubleNext();
        double valFlip = diiFlip.getDoubleNext();
        // only sum if not missing
        if (!geo.isMissing(val))
          sumData += val;
        if (!geo.isMissing(valFlip))
          sumDataFlip += valFlip;
      }
      assertThat(sumData).isNotEqualTo(initialSumVal);
      assertThat(sumDataFlip).isNotEqualTo(initialSumVal);
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
    assertThat(gcs2).isNotNull();
    System.out.printf(" data cs shape=%s%n", Arrays.toString(gcs2.getShape()));
    System.out.printf(" data shape=%s%n", Arrays.toString(geo.getData().getShape()));

    assertThat(gcs2.getShape()).isEqualTo(expectedShape);
    assertThat(geo.getData().getShape()).isEqualTo(expectedShape);
  }

}
