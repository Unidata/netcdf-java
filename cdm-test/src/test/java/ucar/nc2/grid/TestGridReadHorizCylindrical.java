/* Copyright Unidata */
package ucar.nc2.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.Index;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.grib.collection.Grib;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link GridHorizCoordinateSystem} reading with horizontal subsets on cylindrical longitude coords */
@Category(NeedsCdmUnitTest.class)
public class TestGridReadHorizCylindrical {
  private static final double tol = 0.001;

  // longitude subsetting (CoordAxis1D regular) }
  @Test
  public void testLongitudeSubset() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "tds/ncep/GFS_Global_onedeg_20100913_0000.grib2";
    System.out.printf("open %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();

      String gribId = "VAR_0-3-0_L1";
      Grid coverage = gds.findGridByAttribute(Grib.VARIABLE_ID_ATTNAME, gribId).orElseThrow(); // "Pressure_Surface");
      assertThat(coverage).isNotNull();

      GridCoordinateSystem cs = coverage.getCoordinateSystem();
      assertThat(cs).isNotNull();
      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();
      System.out.printf("lon = %s%n", hcs.getXHorizAxis());

      LatLonRect llbox = LatLonRect.builder(LatLonPoint.create(40.0, -100.0), 10.0, 20.0).build();
      ProjectionRect pbox = hcs.getProjection().latLonToProjBB(llbox);
      LatLonProjection proj = (LatLonProjection) hcs.getProjection();
      System.out.printf("proj CenterLon = %s%n", proj.getCenterLon());
      System.out.printf("llbox = %s%n", llbox);
      System.out.printf("pbox = %s%n", pbox);

      TestGridReadHorizSubset.checkLatLonSubset(hcs, coverage, llbox, new int[] {1, 1, 11, 21});
    }
  }

  @Test
  public void testCrossLongitudeSeam() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "tds/ncep/GFS_Global_0p5deg_20100913_0000.grib2";
    System.out.printf("open %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();

      String gribId = "VAR_2-0-0_L1";
      Grid grid = gds.findGridByAttribute(Grib.VARIABLE_ID_ATTNAME, gribId).orElseThrow(); // "Land_cover_0__sea_1__land_surface");
      assertThat(grid).isNotNull();
      System.out.printf("grid %s%n", grid.getName());

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();
      assertThat(cs.getNominalShape()).isEqualTo(ImmutableList.of(1, 65, 361, 720));
      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();

      LatLonRect bbox = LatLonRect.builder(40.0, -100.0, 10.0, 120.0).build();
      GridReferencedArray geoArray =
          TestGridReadHorizSubset.checkLatLonSubset(hcs, grid, bbox, new int[] {1, 1, 61, 441});

      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      GridHorizCoordinateSystem subsetHcs = mcs.getHorizCoordinateSystem();
      GridAxisPoint subsetLonAxis = Preconditions.checkNotNull(subsetHcs.getXHorizAxis());
      assertThat(subsetLonAxis.getAxisType()).isEqualTo(AxisType.Lon);
      assertThat(subsetLonAxis.isSubset).isTrue();
      assertThat(subsetLonAxis.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
      assertThat(subsetLonAxis.startValue).isEqualTo(260.0);
      assertThat(subsetLonAxis.getNominalSize()).isEqualTo(441);
      assertThat(subsetLonAxis.getResolution()).isEqualTo(.5);
    }
  }

  // LOOK need examples for irregular and nominal

  @Test
  public void testLongitudeSubsetWithHorizontalStride() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "tds/ncep/GFS_Global_onedeg_20100913_0000.grib2";
    System.out.printf("open %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();

      String gribId = "VAR_0-3-0_L1";
      Grid coverage = gds.findGridByAttribute(Grib.VARIABLE_ID_ATTNAME, gribId).orElseThrow();
      assertThat(coverage).isNotNull();

      GridCoordinateSystem cs = coverage.getCoordinateSystem();
      assertThat(cs).isNotNull();
      GridHorizCoordinateSystem origHcs = cs.getHorizCoordinateSystem();
      assertThat(origHcs).isNotNull();

      // Next, create the subset param and make the request
      final CalendarDate validTime = CalendarDate.fromUdunitIsoDate(null, "2010-09-21T00:00:00Z").orElseThrow();
      // subset across the seam
      final LatLonRect subsetLatLonRequest = LatLonRect.builder(LatLonPoint.create(-15, -10), 30, 20).build();

      // make subset without stride
      GridReferencedArray geoArrayNo =
          coverage.getReader().setTime(validTime).setLatLonBoundingBox(subsetLatLonRequest).read();
      assertThat(geoArrayNo).isNotNull();
      System.out.printf("geoArray = %s%n", geoArrayNo);

      GridHorizCoordinateSystem subsetHcsNo = geoArrayNo.getMaterializedCoordinateSystem().getHorizCoordinateSystem();
      GridAxisPoint subsetLonAxisNo = Preconditions.checkNotNull(subsetHcsNo.getXHorizAxis());
      assertThat(subsetLonAxisNo.getAxisType()).isEqualTo(AxisType.Lon);
      assertThat(subsetLonAxisNo.isSubset).isTrue();
      assertThat(subsetLonAxisNo.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
      assertThat(subsetLonAxisNo.startValue).isEqualTo(350.0);
      assertThat(subsetLonAxisNo.getNominalSize()).isEqualTo(21);
      assertThat(subsetLonAxisNo.getResolution()).isEqualTo(1);

      // make subset with stride
      final int stride = 2;
      GridReferencedArray geoArray = coverage.getReader().setTime(validTime).setLatLonBoundingBox(subsetLatLonRequest)
          .setHorizStride(stride).read();
      assertThat(geoArray).isNotNull();
      System.out.printf("geoArray = %s%n", geoArray);

      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      GridHorizCoordinateSystem subsetHcs = mcs.getHorizCoordinateSystem();
      GridAxisPoint subsetLonAxis = Preconditions.checkNotNull(subsetHcs.getXHorizAxis());
      assertThat(subsetLonAxis.getAxisType()).isEqualTo(AxisType.Lon);
      assertThat(subsetLonAxis.isSubset).isTrue();
      assertThat(subsetLonAxis.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
      assertThat(subsetLonAxis.startValue).isEqualTo(350.0);
      assertThat(subsetLonAxis.getNominalSize()).isEqualTo(11);
      assertThat(subsetLonAxis.getResolution()).isEqualTo(2.0);

      // Note that the horizontal coordinate system of the GeoReferencedArray produced by the subset
      // may be slightly bigger than the requested bounding box
      System.out.printf("request = %s%n", subsetLatLonRequest);
      System.out.printf("result = %s%n", subsetHcs.getLatLonBoundingBox());

      // make sure resolution of the lat and lon grids of the subset take into account the stride
      // by comparing the resolution
      GridAxisPoint origLonAxis = Preconditions.checkNotNull(origHcs.getXHorizAxis());
      GridAxisPoint origLatAxis = Preconditions.checkNotNull(origHcs.getYHorizAxis());
      GridAxisPoint subsetLatAxis = Preconditions.checkNotNull(subsetHcs.getYHorizAxis());
      assertThat(origLonAxis.getResolution()).isNotWithin(tol).of(subsetLonAxis.getResolution());
      assertThat(origLonAxis.getResolution()).isWithin(tol).of(subsetLonAxis.getResolution() / stride);
      assertThat(origLatAxis.getResolution()).isNotWithin(tol).of(subsetLatAxis.getResolution());
      assertThat(origLatAxis.getResolution()).isWithin(tol).of(subsetLatAxis.getResolution() / stride);

      // check to make sure we get data from both sides of the seam by testing that half of the array isn't empty.
      // slice along longitude in the middle of the array.
      int rank = geoArray.data().getRank();
      Array<Number> geoData = Arrays.reduceFirst(geoArray.data(), rank - 2);
      int middle = geoData.getShape()[1] / 2;
      Array<Number> dataSlice = Arrays.slice(geoData, 1, middle);
      // flip the array
      Array<Number> dataFlip = Arrays.flip(dataSlice, 0);
      Index sliceIndex = dataSlice.getIndex();
      Index flipIndex = dataFlip.getIndex();

      final double initialSumVal = 0;
      int numValsToSum = 3;
      double sumData = initialSumVal;
      double sumDataFlip = initialSumVal;
      for (int i = 0; i < numValsToSum - 1; i++) {
        double val = dataSlice.get(sliceIndex.set(i)).doubleValue();
        double valFlip = dataFlip.get(flipIndex.set(i)).doubleValue();
        // only sum if not missing
        if (!coverage.isMissing(val))
          sumData += val;
        if (!coverage.isMissing(valFlip))
          sumDataFlip += valFlip;
      }
      assertThat(sumData).isNotEqualTo(initialSumVal);
      assertThat(sumDataFlip).isNotEqualTo(initialSumVal);
    }
  }

}
