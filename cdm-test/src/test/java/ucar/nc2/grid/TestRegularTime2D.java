/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.constants.AxisType;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test Grib 2D time that is regular, not orthogonal.
 */
@Category(NeedsCdmUnitTest.class)
public class TestRegularTime2D {

  @Test
  public void testRegularTime2D() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "datasets/NDFD-CONUS-5km/NDFD-CONUS-5km.ncx4";
    String gridName = "Maximum_temperature_height_above_ground_12_Hour_Maximum";
    System.out.printf("file %s coverage %s%n", filename, gridName);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();

      assertThat(gcs.getNominalShape()).isEqualTo(ImmutableList.of(4, 4, 1, 689, 1073));

      GridAxisPoint reftime = (GridAxisPoint) gcs.findCoordAxisByType(AxisType.RunTime);
      assertThat((Object) reftime).isNotNull();
      assertThat(reftime.getNominalSize()).isEqualTo(4);
      double[] want = new double[] {0., 12., 24., 36.}; // used to be in hours
      int hour = 0;
      for (Number value : reftime) {
        assertThat(value.doubleValue()).isEqualTo(3600.0 * want[hour++]);
      }

      GridAxisInterval validtime = (GridAxisInterval) gcs.findCoordAxisByType(AxisType.TimeOffset);
      assertThat((Object) validtime).isNotNull();
      assertThat(validtime.getNominalSize()).isEqualTo(4);
      double[] start = new double[] {84, 108, 132, 156}; // used to be in hours
      double[] end = new double[] {96, 120, 144, 168}; // used to be in hours
      hour = 0;
      for (CoordInterval intv : validtime) {
        assertThat(intv.start()).isEqualTo(start[hour]);
        assertThat(intv.end()).isEqualTo(end[hour]);
        hour++;
      }
    }
  }
}
