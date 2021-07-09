/*
 *  Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 *  See LICENSE for license information.
 */

package ucar.nc2.grib.grid;

import com.google.common.collect.Streams;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageCoordAxis;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.grid2.Grid;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridCoordinateSystem;
import ucar.nc2.grid2.GridDataset;
import ucar.nc2.grid2.GridDatasetFactory;
import ucar.nc2.grid2.GridTimeCoordinateSystem;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/** Compare reading Grib through new and old GridDataset. */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGribGridCompare {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    FileFilter ff = TestDir.FileFilterSkipSuffix(
        ".ncx4 .gbx9 .cdl .pdf perverse.nc aggFmrc.xml 2003021212_avn-x.nc wrfout_01_000000_0003.nc wrfout_01_000000_0003.ncml");
    List<Object[]> result = new ArrayList<>(500);
    try {
      // in this case the time coordinate is not monotonic.
      // result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/nuwg/2003021212_avn-x.nc"});

      // in this case the vert coordinate is not identified.
      // Note that TestDir skips this in favor of the ncml file in the same directory (true?)
      // result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/avhrr/amsr-avhrr-v2.20040729.nc"});
      // result.add(new Object[] {TestDir.cdmUnitTestDir + "wrf/wrfout_01_000000_0003.ncml"});

      result.add(new Object[] {
          TestDir.cdmUnitTestDir + "tds_index/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20201027_0000.grib1.ncx4"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "tds_index/NCEP/NAM/Polar_90km/NAM-Polar_90km.ncx4"});
      // TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "ft/grid/", ff, result);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return result;
  }

  /////////////////////////////////////////////////////////////

  public TestGribGridCompare(String filename) {
    this.filename = filename;
  }

  private final String filename;

  @Test
  public void compareGrid() throws Exception {
    compareDtCoordinateSystems(filename);
    compareCoverageCoordinateSystems(filename);
    compareGridCoordinateSystems(filename);
  }

  public static boolean compareDtCoordinateSystems(String filename) throws Exception {
    System.out.printf("%n");
    Formatter errlog = new Formatter();
    try (GridDataset newDataset = GridDatasetFactory.openGridDataset(filename, errlog);
        ucar.nc2.dt.grid.GridDataset oldDataset = ucar.nc2.dt.grid.GridDataset.open(filename)) {
      if (newDataset == null) {
        System.out.printf("Cant open as ucar.nc2.grid2.GridDataset: %s%n", filename);
        return false;
      }
      if (oldDataset == null) {
        System.out.printf("Cant open as ucar.nc2.dt.grid.GridDataset: %s%n", filename);
        return false;
      }
      System.out.printf("compareDtCoordinateSystems: %s%n", newDataset.getLocation());

      for (Grid grid : newDataset.getGrids()) {
        ucar.nc2.dt.grid.GeoGrid geogrid = oldDataset.findGridByName(grid.getName());
        if (geogrid == null) {
          System.out.printf("*** Grid %s not in GridDataset%n", grid.getName());
          continue;
        }
        System.out.printf("  Grid/GeoGrid: %s%n", grid.getName());

        GridCoordSystem oldGcs = geogrid.getCoordinateSystem();
        GridCoordinateSystem newGcs = grid.getCoordinateSystem();
        GridTimeCoordinateSystem newTcs = grid.getTimeCoordinateSystem();

        for (GridAxis<?> newAxis : newGcs.getGridAxes()) {
          CoordinateAxis oldAxis = oldGcs.getCoordinateAxes().stream()
              .filter(a -> a.getAxisType().equals(newAxis.getAxisType())).findFirst().orElse(null);
          if (oldAxis == null) {
            oldAxis = oldGcs.getCoordinateAxes().stream().filter(a -> a.getShortName().equals(newAxis.getName()))
                .findFirst().orElse(null);
          }
          assertWithMessage(String.format("    GridAxis: %s %s%n", newAxis.getName(), newAxis.getAxisType()))
              .that(oldAxis).isNotNull();
          assertThat(newAxis.getNominalSize()).isEqualTo(oldAxis.getSize());
        }
      }
    }
    return true;
  }

  public static boolean compareCoverageCoordinateSystems(String filename) throws Exception {
    System.out.printf("%n");
    Formatter errlog = new Formatter();
    try (GridDataset newDataset = GridDatasetFactory.openGridDataset(filename, errlog);
        FeatureDatasetCoverage covDataset = CoverageDatasetFactory.open(filename)) {
      if (newDataset == null) {
        System.out.printf("Cant open as ucar.nc2.grid2.GridDataset: %s%n", filename);
        return false;
      }
      if (covDataset == null) {
        System.out.printf("Cant open as FeatureDatasetCoverage: %s%n", filename);
        return false;
      }
      CoverageCollection covCollection = covDataset.getCoverageCollections().get(0);
      System.out.printf("compareCoverageCoordinateSystems: %s%n", newDataset.getLocation());

      for (Grid grid : newDataset.getGrids()) {
        Coverage coverage = covCollection.findCoverage(grid.getName());
        if (coverage == null) {
          System.out.printf("*** Coverage %s not in covCollection%n", grid.getName());
          continue;
        }
        System.out.printf("  Grid/Coverage: %s%n", grid.getName());

        CoverageCoordSys oldGcs = coverage.getCoordSys();
        GridCoordinateSystem newGcs = grid.getCoordinateSystem();
        GridTimeCoordinateSystem newTcs = grid.getTimeCoordinateSystem();

        for (GridAxis<?> newAxis : newGcs.getGridAxes()) {
          CoverageCoordAxis oldAxis = oldGcs.getAxes().stream()
              .filter(a -> a.getAxisType().equals(newAxis.getAxisType())).findFirst().orElse(null);
          if (oldAxis == null) {
            oldAxis =
                oldGcs.getAxes().stream().filter(a -> a.getName().equals(newAxis.getName())).findFirst().orElse(null);
          }
          assertWithMessage(String.format("    GridAxis: %s %s%n", newAxis.getName(), newAxis.getAxisType()))
              .that(oldAxis).isNotNull();
          int[] oldShape = oldAxis.getShape();
          if (oldShape.length > 0) {
            assertThat(newAxis.getNominalSize()).isEqualTo(oldShape[oldShape.length - 1]);
          } else {
            assertThat(newAxis.getNominalSize()).isEqualTo(oldAxis.getNcoords());
          }
        }
      }
    }
    return true;
  }

  public static boolean compareGridCoordinateSystems(String filename) throws Exception {
    System.out.printf("%n");
    Formatter errlog = new Formatter();
    try (GridDataset newDataset = GridDatasetFactory.openGridDataset(filename, errlog);
        ucar.nc2.grid.GridDataset oldDataset = ucar.nc2.grid.GridDatasetFactory.openGridDataset(filename, errlog)) {
      if (newDataset == null) {
        System.out.printf("Cant open as ucar.nc2.grid2.GridDataset: %s%n", errlog);
        return false;
      }
      if (oldDataset == null) {
        System.out.printf("Cant open as ucar.nc2.grid.GridDataset: %s%n", errlog);
        return false;
      }
      System.out.printf("compareGridCoordinateSystems: %s%n", newDataset.getLocation());

      for (Grid grid : newDataset.getGrids()) {
        ucar.nc2.grid.Grid oldGrid = oldDataset.findGrid(grid.getName()).orElse(null);
        if (oldGrid == null) {
          System.out.printf("*** Grid %s not in ucar.nc2.grid.GridDataset%n", grid.getName());
          continue;
        }
        System.out.printf("  Grid/Grid: %s%n", grid.getName());

        ucar.nc2.grid.GridCoordinateSystem oldGcs = oldGrid.getCoordinateSystem();
        GridCoordinateSystem newGcs = grid.getCoordinateSystem();
        GridTimeCoordinateSystem newTcs = grid.getTimeCoordinateSystem();

        for (GridAxis<?> newAxis : newGcs.getGridAxes()) {
          ucar.nc2.grid.GridAxis oldAxis = Streams.stream(oldGcs.getGridAxes())
              .filter(a -> a.getAxisType().equals(newAxis.getAxisType())).findFirst().orElse(null);
          if (oldAxis == null) {
            oldAxis = Streams.stream(oldGcs.getGridAxes()).filter(a -> a.getName().equals(newAxis.getName()))
                .findFirst().orElse(null);
          }
          assertWithMessage(String.format("GridAxis: %s %s%n", newAxis.getName(), newAxis.getAxisType())).that(oldAxis)
              .isNotNull();
          int[] oldShape = oldAxis.getNominalShape();
          assertThat(newAxis.getNominalSize()).isEqualTo(oldShape[oldShape.length - 1]);
        }
      }
    }
    return true;
  }
}

