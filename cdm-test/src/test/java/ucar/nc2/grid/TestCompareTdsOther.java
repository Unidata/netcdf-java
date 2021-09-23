/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashSet;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Compare reading TDS Grib Collections with old and new GridDataset. */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestCompareTdsOther {
  private static final String topDir = TestDir.cdmUnitTestDir + "tds_index/";

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    FileFilter ff = TestDir.FileFilterSkipSuffix(".gbx9");
    List<Object[]> result = new ArrayList<>(500);
    try {
      /// CMC
      /*
       * There are two separate grids here, with disjunct variables. These should possibly be separated into two
       * datasets, perhaps in the LDM feed?
       * PolarStereographic 190X245 -56,-99
       * PolarStereographic 399X493- 60,-90
       * Unidata TDS separated these into “Derived Fields” and “Model Fields”, see
       * https://thredds.ucar.edu/thredds/catalog/grib/CMC/RDPS/NA_15km/catalog.html
       */
      // result.add(new Object[] {topDir + "CMC/RDPS/NA_15km/CMC_RDPS_ps15km_20201027_0000.grib2.ncx4", 58, 17, 24});

      // fnmoc
      result.add(new Object[] {topDir + "FNMOC/WW3/Global_1p0deg/FNMOC_WW3_Global_1p0deg.ncx4", 15, 1, 4});
      result.add(new Object[] {topDir + "FNMOC/WW3/Europe/FNMOC_WW3_Europe.ncx4", 15, 2, 5});
      result.add(
          new Object[] {topDir + "FNMOC/COAMPS/Southern_California/FNMOC_COAMPS_Southern_California.ncx4", 19, 5, 9});
      result
          .add(new Object[] {topDir + "FNMOC/COAMPS/Northeast_Pacific/FNMOC_COAMPS_Northeast_Pacific.ncx4", 20, 5, 9});
      result.add(
          new Object[] {topDir + "FNMOC/COAMPS/Equatorial_America/FNMOC_COAMPS_Equatorial_America.ncx4", 19, 5, 9});
      result.add(new Object[] {topDir + "FNMOC/COAMPS/Europe/FNMOC_COAMPS_Europe.ncx4", 18, 5, 8});
      result.add(new Object[] {topDir + "FNMOC/COAMPS/Western_Atlantic/FNMOC_COAMPS_Western_Atlantic.ncx4", 18, 7, 11});
      result.add(new Object[] {topDir + "FNMOC/NAVGEM/Global_0p5deg/FNMOC_NAVGEM_Global_0p5deg.ncx4", 103, 37, 49});

      // The HRRR/CONUS_3km/wrfprs is a mess with huge missing. validtime1 looks wrong
      // result.add(new Object[] {topDir + "NOAA_GSD/HRRR/CONUS_3km/wrfprs/GSD_HRRR_CONUS_3km_wrfprs.ncx4", 15, 1, 5});
      result.add(new Object[] {
          topDir + "NOAA_GSD/HRRR/CONUS_3km/surface/HRRR_CONUS_3km_surface_202011230000.grib2.ncx4", 129, 36, 39});
      result
          .add(new Object[] {topDir + "NOAA_GSD/HRRR/CONUS_3km/surface/GSD_HRRR_CONUS_3km_surface.ncx4", 129, 36, 38});

      // TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "ft/grid/", ff, result);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return result;
  }

  /////////////////////////////////////////////////////////////
  @Parameterized.Parameter(0)
  public String filename;
  @Parameterized.Parameter(1)
  public int ngrids;
  @Parameterized.Parameter(2)
  public int ncoordSys;
  @Parameterized.Parameter(3)
  public int nAxes;

  @Test
  public void checkGridDataset() throws Exception {
    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog);
        ucar.nc2.dt.grid.GridDataset oldDataset = ucar.nc2.dt.grid.GridDataset.open(filename)) {
      if (gridDataset == null) {
        System.out.printf("Cant open as GridDataset: %s%n", filename);
        return;
      }
      System.out.printf("checkGridDataset: %s%n", gridDataset.getLocation());
      assertThat(gridDataset.getGridCoordinateSystems()).hasSize(ncoordSys);
      assertThat(gridDataset.getGridAxes()).hasSize(nAxes);
      assertThat(gridDataset.getGrids()).hasSize(ngrids);

      HashSet<GridCoordinateSystem> csysSet = new HashSet<>();
      HashSet<GridAxis<?>> axisSet = new HashSet<>();
      for (Grid grid : gridDataset.getGrids()) {
        csysSet.add(grid.getCoordinateSystem());
        axisSet.addAll(grid.getCoordinateSystem().getGridAxes());
      }
      assertThat(csysSet).hasSize(ncoordSys);
      assertThat(axisSet).hasSize(nAxes);

      TestGridCompareWithDt.compareCoordinateSystems(gridDataset, oldDataset, true);
    }
  }
}

