/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import com.google.common.collect.Iterables;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dt.GridDatatype;
import ucar.unidata.util.StringUtil2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.FileFilter;
import java.util.*;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/** Compare reading TDS Grib Collections with old and new GridDataset. */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestTdsOtherGribCollections {
  private static final String topDir = TestDir.cdmUnitTestDir + "tds_index/";

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    FileFilter ff = TestDir.FileFilterSkipSuffix(".gbx9");
    List<Object[]> result = new ArrayList<>(500);
    try {
      /// CMC
      result.add(new Object[] {topDir + "CMC/RDPS/NA_15km/CMC_RDPS_ps15km_20201010_0000.grib2.ncx4", 58, 16, 22});

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
          topDir + "NOAA_GSD/HRRR/CONUS_3km/surface/HRRR_CONUS_3km_surface_202011060000.grib2.ncx4", 129, 36, 39});
      result
          .add(new Object[] {topDir + "NOAA_GSD/HRRR/CONUS_3km/surface/GSD_HRRR_CONUS_3km_surface.ncx4", 128, 35, 37});

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
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(filename, errlog)) {
      if (gridDataset == null) {
        System.out.printf("Cant open as GridDataset: %s%n", filename);
        return;
      }
      System.out.printf("%ncheckGridDataset: %s%n", gridDataset.getLocation());
      assertThat(gridDataset.getGridCoordinateSystems()).hasSize(ncoordSys);
      assertThat(gridDataset.getGridAxes()).hasSize(nAxes);
      assertThat(gridDataset.getGrids()).hasSize(ngrids);

      HashSet<GridCoordinateSystem> csysSet = new HashSet<>();
      HashSet<GridAxis> axisSet = new HashSet<>();
      for (Grid grid : gridDataset.getGrids()) {
        csysSet.add(grid.getCoordinateSystem());
        for (GridAxis axis : grid.getCoordinateSystem().getGridAxes()) {
          axisSet.add(axis);
        }
      }
      assertThat(csysSet).hasSize(ncoordSys);
      assertThat(axisSet).hasSize(nAxes);
    }
  }

  @Test
  public void compareGridDataset() throws Exception {
    boolean ok = true;
    Formatter errlog = new Formatter();
    try (GridDataset newDataset = GridDatasetFactory.openGridDataset(filename, errlog);
        ucar.nc2.dt.grid.GridDataset dataset = ucar.nc2.dt.grid.GridDataset.open(filename)) {
      if (newDataset == null) {
        System.out.printf("Cant open as GridDataset: %s%n", filename);
        fail();
      }
      System.out.printf(" NewGrid: %d %d %d %n", newDataset.getGrids().size(), newDataset.getGridCoordinateSystems().size(),
          newDataset.getGridAxes().size());
      System.out.printf(" OldGrid: %d %d %n", dataset.getGrids().size(), dataset.getGridsets().size());
      // assertThat(dataset.getGrids()).hasSize(2 * ngrids);
      // assertThat(dataset.getGridsets()).hasSize(2 * ncoordSys);

      for (Grid grid : newDataset.getGrids()) {
        GridDatatype geogrid = findInOld(grid.getName(), dataset);
        if (geogrid == null) {
          System.out.printf(" GeoGrid %s not in OldGrid%n", grid.getName());
          ok = false;
        } else {
          if (!compareCoordinateNames(grid.getCoordinateSystem().getName(), geogrid.getCoordinateSystem().getName())) {
            System.out.printf("    Grid %s: %s%n", grid.getName(), grid.getCoordinateSystem().getName());
            System.out.printf(" GeoGrid %s: %s%n%n", geogrid.getName(), geogrid.getCoordinateSystem().getName());
          }
        }
      }
      for (GridDatatype geogrid : dataset.getGrids()) {
        if (geogrid.getName().startsWith("Best")) {
          continue;
        }
        String name = removeGroup(geogrid.getName());
        if (!newDataset.findGrid(name).isPresent()) {
          CoordinateAxis timeAxis = geogrid.getCoordinateSystem().getTimeAxis();
          System.out.printf(" GeoGrid %s not in NewGrid time=%s%n", name, timeAxis.getNameAndDimensions());
        }
      }
    }
    assertThat(ok).isTrue();
  }

  private GridDatatype findInOld(String want, ucar.nc2.dt.grid.GridDataset dataset) {
    for (GridDatatype geogrid : dataset.getGrids()) {
      if (removeGroup(geogrid.getName()).equals(want)) {
        return geogrid;
      }
    }
    return null;
  }

  private String removeGroup(String name) {
    int pos = name.indexOf("/");
    return (pos < 0) ? name : name.substring(pos + 1);
  }

  private boolean compareCoordinateNames(String newName, String oldName) {
    boolean result = true;
    Iterable<String> oldNames = StringUtil2.split(oldName);
    Iterable<String> newNames = StringUtil2.split(newName);
    if (Iterables.size(oldNames) != Iterables.size(newNames)) {
      System.out.printf(" Old size = %d != %d%n", Iterables.size(oldNames), Iterables.size(newNames));
      result = false;
    }
    Iterator<String> oldIter = oldNames.iterator();
    Iterator<String> newIter = newNames.iterator();
    while (oldIter.hasNext() && newIter.hasNext()) {
      String old = StringUtil2.remove(oldIter.next(), "TwoD/");
      String nnn = StringUtil2.remove(newIter.next(), "Offset");
      if (!old.equals(nnn)) {
        result = false;
      }
    }
    return result;
  }
}

