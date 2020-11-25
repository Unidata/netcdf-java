package ucar.nc2.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
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
      assertThat(geoArray.data().getRank()).isEqualTo(3);
    }
  }
}
