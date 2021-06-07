package ucar.nc2.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.nc2.calendar.CalendarDate;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

public class TestReadGridSubset {

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testTimeOffsetRegular() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/SPC/NDFD-SPC.ncx4";

    Formatter infoLog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, infoLog)) {
      System.out.println("readGridDataset: " + gridDataset.getLocation());
      // float Convective_Hazard_Outlook_surface_24_hours_Average(
      Grid grid = gridDataset.findGrid("Convective_Hazard_Outlook_surface_24_Hour_Average")
          .orElseThrow(() -> new RuntimeException("Cant find grid"));

      GridCoordinateSystem csys = grid.getCoordinateSystem();
      GridAxis1DTime runtimeAxis = csys.getRunTimeAxis();
      GridAxisOffsetTimeRegular timeOffset = (GridAxisOffsetTimeRegular) csys.getTimeOffsetAxis();
      assertThat(timeOffset).isNotNull();
      assertThat(timeOffset.getSpacing()).isEqualTo(GridAxis.Spacing.discontiguousInterval);

      assertThat(runtimeAxis.getNcoords()).isGreaterThan(1);
      CalendarDate wantRuntime = runtimeAxis.getCalendarDate(10);
      GridAxis1D toAxis = timeOffset.getTimeOffsetAxisForRun(wantRuntime);
      assertThat(toAxis.getNcoords()).isGreaterThan(1);
      CoordInterval coord = toAxis.getCoordInterval(1);
      GridSubset subset = new GridSubset().setRunTime(wantRuntime).setTimeOffsetCoord(coord);
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
      GridSubset subset = new GridSubset().setRunTime(wantRuntime).setTimeOffsetCoord(wantOffset);

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
