package ucar.nc2.grid2;

import com.google.common.collect.ImmutableList;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.util.Formatter;
import java.util.HashSet;

import static com.google.common.truth.Truth.assertThat;

/** Test problems reading {@link GridDataset} */
@Category(NeedsCdmUnitTest.class)
public class TestTdsGribProblems {
  private static final String indexDir = TestDir.cdmUnitTestDir + "tds_index/";

  @Test
  public void checkGEFSensemble() throws Exception {
    String filename =
        TestDir.cdmUnitTestDir + "ncss/GEFS/Global_1p0deg_Ensemble/member/GEFS-Global_1p0deg_Ensemble-members.ncx4";
    checkGridDataset(filename, 35, 11, 17, 17);
  }

  // LOOK: this has two groups
  @Test
  public void checkTwoGroups() throws Exception {
    String filename = indexDir + "CMC/RDPS/NA_15km/CMC_RDPS_ps15km_20201027_0000.grib2.ncx4";
    checkGridDataset(filename, 54, 14, 17, 17);
  }

  private void checkGridDataset(String filename, int ngrids, int ncoordSys, int nAxes, int uniqueAxes)
      throws Exception {
    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      if (gridDataset == null) {
        System.out.printf("Cant open as GridDataset: %s%n", filename);
        return;
      }
      System.out.printf("%ncheckGridDataset: %s%n", gridDataset.getLocation());
      assertThat(gridDataset.getGridCoordinateSystems()).hasSize(ncoordSys);
      assertThat(gridDataset.getGridAxes()).hasSize(nAxes);
      assertThat(gridDataset.getGrids()).hasSize(ngrids);

      // checks that all gridAxes are used in a grid and are unique
      HashSet<GridCoordinateSystem> csysSet = new HashSet<>();
      HashSet<GridAxis<?>> axisSet = new HashSet<>();
      for (Grid grid : gridDataset.getGrids()) {
        csysSet.add(grid.getCoordinateSystem());
        axisSet.addAll(grid.getCoordinateSystem().getGridAxes());
      }
      assertThat(csysSet).hasSize(ncoordSys);
      assertThat(axisSet).hasSize(uniqueAxes);
    }
  }

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
   * This is fixed by PR#564, but ncx4 must be regenerated.
   */
  @Test
  public void testNbmOcean() throws Exception {
    String filename = indexDir + "NCEP/NBM/Ocean/NCEP_OCEAN_MODEL_BLEND.ncx4";

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
  // having different grids sizes? Problem is that they have the same variables in each group. Possible these are
  // different model runs that are not getting properly separated?? Or a moving grid ?? I think moving grid. Individual
  // runtimes have single grid.
  // 1 runtime, 2 timeOffsets and 33 vertical coords.
  @Test
  public void testFirewxnest() throws Exception {
    String filename = indexDir + "NCEP/NAM/Firewxnest/NAM-Firewxnest.ncx4";

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
    String filename = indexDir + "NCEP/MRMS/Radar/MRMS_Radar_20201111_2200.grib2.ncx4";

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
    String filename = indexDir + "NCEP/NBM/PuertoRico/NCEP_PUERTORICO_MODEL_BLEND.ncx4";
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
      GridTimeCoordinateSystem tsys = gsys.getTimeCoordinateSystem();
      assertThat(tsys).isNotNull();
      assertThat(tsys.getType()).isEqualTo(GridTimeCoordinateSystem.Type.OffsetRegular);
      assertThat(tsys.getNominalShape()).isEqualTo(ImmutableList.of(117, 11));

      // This is a regular time2D, with 8 forecast in a day.
      int[] expected = new int[] {10, 3, 10, 1, 11, 3, 10, 1}; // the length of the valid values

      GridAxisPoint runtime = tsys.getRunTimeAxis();
      assertThat((Object) runtime).isNotNull();
      for (int i = 0; i < runtime.getNominalSize(); i++) {
        GridAxis<?> timeOffset = tsys.getTimeOffsetAxis(i);
        assertThat((Object) timeOffset).isNotNull();
        assertThat(timeOffset.isInterval()).isTrue();
        assertThat((Object) timeOffset).isInstanceOf(GridAxisInterval.class);
        assertThat(timeOffset.getNominalSize()).isEqualTo(expected[i % 8]);
      }
    }
  }

  @Test
  public void testHrrrConusSurface() throws Exception {
    String filename = indexDir + "NOAA_GSD/HRRR/CONUS_3km/surface/HRRR_CONUS_3km_surface_202011260000.grib2.ncx4";

    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      if (gridDataset == null) {
        System.out.printf("Cant open as GridDataset: %s%n", filename);
        return;
      }
      System.out.printf("checkGridDataset: %s%n", gridDataset.getLocation());
    }
  }

  @Test
  @Ignore("doesnt work")
  public void testHrrrConusWrfprs() throws Exception {
    String filename = indexDir + "NOAA_GSD/HRRR/CONUS_3km/wrfprs/GSD_HRRR_CONUS_3km_wrfprs.ncx4";
    new TestGridCompareData1(filename).compareWithGrid1(false);
  }

  @Test
  public void testRuntimeCoordinateValues() throws Exception {
    String filename = indexDir + "NCEP/GFS/Alaska_20km/GFS-Alaska_20km.ncx4";
    new TestGridCompareData1(filename).compareWithGrid1(false);
  }

  @Test
  public void testCoordIntervalOutOfBounds() throws Exception {
    String filename = indexDir + "NCEP/GFS/Global_0p5deg/GFS-Global_0p5deg.ncx4";
    new TestGridCompareData1(filename).compareWithGrid1(false);
  }

  @Test
  public void testObservationDataset() throws Exception {
    String filename = indexDir + "NCEP/HRRR/CONUS_2p5km_Analysis/NCEP_HRRR_CONUS_2p5km_Analysis.ncx4";
    new TestGridCompareData1(filename).compareWithGrid1(false);
  }

}
