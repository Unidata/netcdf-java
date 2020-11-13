package ucar.nc2.grid;

import org.junit.Test;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;

import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

public class TestTdsGribProblems {

  /*
   * All NCEP/NBM/Ocean/National_Blend_Ocean_20201010_0000.grib2.ncx4
   * Fails on Variable 'Wind_direction_from_which_blowing_height_above_ground' already exists
   * Due to: Two identical variable differ by PDS:
   * 
   * (4.0) Product definition template 4.0 - analysis or forecast at a horizontal level or in a horizontal layer at a
   * point in time
   * (4.6) Product definition template 4.6 - percentile forecasts at a horizontal level or in a horizontal layer at a
   * point in time
   * 
   * which is not reflected in the name.
   * Check to see if its happening in TDS 4 and 5.
   */
  @Test
  public void testNbmOcean() throws Exception {
    String filename = "/media/twobee/tds/NCEP/NBM/Ocean/NCEP_OCEAN_MODEL_BLEND.ncx4";

    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      if (gridDataset == null) {
        System.out.printf("Cant open as GridDataset: %s%n", filename);
        return;
      }
      System.out.printf("checkGridDataset: %s%n", gridDataset.getLocation());
    }
  }

  // Heres an excessive number of coordinate systems (234). Are they unique??
  // 6 groups (TwoD, Best) x (LC 368 x 518, 384 x 634, 380 x 609). Grid could have one group with multiple coordsys
  // having
  // different grids sizes? Problem is that they have the same variables in each group. Possible these are different
  // model runs
  // that are not getting properly separated?? Or a moving grid ?? I think moving grid. Individual runtimes have single
  // grid.
  // 1 runtime, 2 timeOffsets and 33 vertical coords.
  @Test
  public void testFirewxnest() throws Exception {
    String filename = "/media/twobee/tds/NCEP/NAM/Firewxnest/NAM-Firewxnest.ncx4";

    try (NetcdfDataset dataset = NetcdfDatasets.openDataset(filename)) {
      System.out.printf("testNoProjection: %s ncoordsys = %d%n", dataset.getLocation(),
          dataset.getCoordinateSystems().size());
      for (CoordinateSystem csys : dataset.getCoordinateSystems()) {
        System.out.printf(" projection %s for coordsys = %s %n", csys.getProjection(), csys.getName());
      }
    }
  }

  @Test
  public void testMrmsRadar() throws Exception {
    String filename =
        "/media/snake/0B681ADF0B681ADF/tds/index_files/NCEP/MRMS/Radar/MRMS_Radar_20201011_2200.grib2.ncx4";

    try (NetcdfDataset dataset = NetcdfDatasets.openDataset(filename)) {
      System.out.printf("testNoProjection: %s ncoordsys = %d%n", dataset.getLocation(),
          dataset.getCoordinateSystems().size());
      for (CoordinateSystem csys : dataset.getCoordinateSystems()) {
        System.out.printf(" projection %s for coordsys = %s %n", csys.getProjection(), csys.getName());
      }
    }
  }

  @Test
  public void testTimeOffsetUnevenHours() throws Exception {
    String filename = "/media/twobee/tds/NCEP/NBM/PuertoRico/NCEP_PUERTORICO_MODEL_BLEND.ncx4";
    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      if (gridDataset == null) {
        System.out.printf("Cant open as GridDataset: %s%n", filename);
        return;
      }
      System.out.printf("testTimeOffsetHours: %s%n", gridDataset.getLocation());
      Grid grid = gridDataset.findGrid("Minimum_temperature_height_above_ground_12_Hour_Minimum").orElse(null);
      assertThat(grid).isNotNull();
      GridCoordinateSystem gsys = grid.getCoordinateSystem();
      GridAxis timeOffset = gsys.getTimeOffsetAxis();
      assertThat(timeOffset).isNotNull();
      assertThat(timeOffset).isInstanceOf(GridAxisOffsetTimeRegular.class);
      GridAxisOffsetTimeRegular timeOffsetReg = (GridAxisOffsetTimeRegular) timeOffset;
      assertThat(timeOffsetReg.isInterval()).isTrue();

      // The coords are vlen (max 11) and filled with NaNs where invalid
      int[] expected = new int[] {10, 3, 10, 1, 11, 3, 10, 1}; // the length of the valid values
      Array<Double> bounds = timeOffsetReg.getCoordBoundsAsArray();
      assertThat(bounds.getShape()).isEqualTo(new int[] {8, 11, 2});
      for (int hour = 0; hour < 8; hour++) {
        Array<Double> hourArray = Arrays.slice(bounds, 0, hour);
        assertThat(hourArray.getShape()).isEqualTo(new int[] {11, 2});
        int count = 0;
        for (double val : hourArray) {
          if (!Double.isNaN(val)) {
            count++;
          }
        }
        assertWithMessage("hour " + hour).that(count).isEqualTo(2 * expected[hour]);
      }

    }
  }
}
