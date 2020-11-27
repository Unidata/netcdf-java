package ucar.nc2.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Arrays;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

public class TestGridSubset {

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testTimeOffsetRegular() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/ndfd_spc/NDFD-SPC.ncx4";

    Formatter infoLog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, infoLog)) {
      System.out.println("readGridDataset: " + gridDataset.getLocation());

      Grid grid = gridDataset.findGrid("Convective_Hazard_Outlook_surface_24_Hour_Average")
          .orElseThrow(() -> new RuntimeException("Cant find grid"));

      GridCoordinateSystem csys = grid.getCoordinateSystem();
      GridAxis1DTime runtimeAxis = csys.getRunTimeAxis();
      GridAxisOffsetTimeRegular timeOffset = (GridAxisOffsetTimeRegular) csys.getTimeOffsetAxis();
      assertThat(timeOffset).isNotNull();
      assertThat(timeOffset.getSpacing()).isEqualTo(GridAxis.Spacing.discontiguousInterval);

      assertThat(runtimeAxis.getNcoords()).isGreaterThan(1);
      CalendarDate wantRuntime = runtimeAxis.getCalendarDate(1);
      GridAxis1DTime timeAxis = timeOffset.getTimeAxisForRun(wantRuntime);
      assertThat(timeAxis.getNcoords()).isGreaterThan(1);
      CoordInterval coord = timeAxis.getCoordInterval(1);
      GridSubset subset = new GridSubset().setRunTime(wantRuntime).setTimeOffsetIntv(coord);

      GridReferencedArray geoArray = grid.readData(subset);
      testGeoArray(geoArray, 3);
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testTimeOffset() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";

    Formatter infoLog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, infoLog)) {
      System.out.println("readGridDataset: " + gridDataset.getLocation());

      Grid grid =
          gridDataset.findGrid("Sunshine_Duration_surface").orElseThrow(() -> new RuntimeException("Cant find grid"));

      GridCoordinateSystem csys = grid.getCoordinateSystem();
      GridAxis1DTime runtimeAxis = csys.getRunTimeAxis();
      assertThat(runtimeAxis).isNotNull();
      GridAxis1D timeOffset = (GridAxis1D) csys.getTimeOffsetAxis();
      assertThat(timeOffset).isNotNull();
      assertThat(timeOffset.getSpacing()).isEqualTo(GridAxis.Spacing.irregularPoint);
      assertThat(timeOffset.getNcoords()).isEqualTo(93);

      assertThat(runtimeAxis.getNcoords()).isEqualTo(4);
      CalendarDate wantRuntime = runtimeAxis.getCalendarDate(1);
      double wantOffset = runtimeAxis.getCoordMidpoint(1);
      GridSubset subset = new GridSubset().setRunTime(wantRuntime).setTimeOffset(wantOffset);

      GridReferencedArray geoArray = grid.readData(subset);
      testGeoArray(geoArray, 2);
    }
  }

  private void testGeoArray(GridReferencedArray geoArray, int expected) {
    assertThat(Arrays.reduce(geoArray.data()).getRank()).isEqualTo(expected);
    int[] shape = geoArray.data().getShape();
    int count = 0;
    for (GridAxis axis : geoArray.csSubset().getGridAxes()) {
      if (axis instanceof GridAxis1D) { // awkward
        assertThat(((GridAxis1D) axis).getNcoords()).isEqualTo(shape[count]);
      }
      count++;
    }
  }
}
