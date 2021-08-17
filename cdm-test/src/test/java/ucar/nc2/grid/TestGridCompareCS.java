/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageCoordAxis;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/** Compare reading new and old GridDataset. */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGridCompareCS {

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

      result.add(new Object[] {TestDir.cdmLocalTestDataDir + "ncml/nc/ubyte_1.nc4"});
      result.add(new Object[] {TestDir.cdmLocalTestDataDir + "ncml/nc/cldc.mean.nc"});
      result.add(new Object[] {TestDir.cdmLocalTestDataDir + "ncml/fmrc/GFS_Puerto_Rico_191km_20090729_0000.nc"});

      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "conventions/", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "ft/grid/", ff, result);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  /////////////////////////////////////////////////////////////
  private final String filename;
  private static final boolean show = false;

  public TestGridCompareCS(String filename) {
    this.filename = filename;
  }


  @Test
  public void compareGrid() throws Exception {
    // these are failing in old code.
    if (filename.endsWith("cdm_sea_soundings.nc4"))
      return;
    if (filename.endsWith("IntTimSciSamp.nc"))
      return;
    if (filename.endsWith("fixed.fw0.0Sv.nc"))
      return;

    System.out.printf("compareGrid %s%n", filename);
    compareWithDt(filename);
    compareWithCoverage(filename);
  }

  public static boolean compareWithDt(String filename) throws Exception {
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
        if (show)
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
          if (oldAxis == null) {
            System.out.printf(" ***missing Axis %s %s in old grid%n", newAxis.getName(), newAxis.getAxisType());
            if (newGcs.getFeatureType() == FeatureType.CURVILINEAR) { // ok for CURVILINEAR
              continue;
            }
          }
          assertWithMessage(String.format("    GridAxis: %s %s%n", newAxis.getName(), newAxis.getAxisType()))
              .that(oldAxis).isNotNull();
          if (newAxis.getNominalSize() != oldAxis.getSize()) {
            System.out.printf(" *** Axis %s %s size differs%n", newAxis.getName(), newAxis.getAxisType());
            if (newGcs.getFeatureType() == FeatureType.CURVILINEAR) { // ok for CURVILINEAR
              continue;
            }
            System.out.printf("HEY");
          }
          assertThat(newAxis.getNominalSize()).isEqualTo(oldAxis.getSize());
          int[] oldShape = oldAxis.getShape();
          if (oldShape.length > 0) {
            assertThat(newAxis.getNominalSize()).isEqualTo(oldShape[oldShape.length - 1]);
          } else {
            assertThat(newAxis.getNominalSize()).isEqualTo(oldAxis.getSize());
          }
        }
      }
    }
    return true;
  }

  public static boolean compareWithCoverage(String filename) throws Exception {
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
            if (newAxis.getNominalSize() != oldShape[oldShape.length - 1]) {
              System.out.printf(" *** Axis %s %s size differs %d !=%d%n", newAxis.getName(), newAxis.getAxisType(),
                  newAxis.getNominalSize(), oldShape[oldShape.length - 1]);
              if (newGcs.getFeatureType() == FeatureType.CURVILINEAR) { // ok for CURVILINEAR
                continue;
              }
              System.out.printf("HEY");
            }
            assertThat(newAxis.getNominalSize()).isEqualTo(oldShape[oldShape.length - 1]);
          } else {
            assertThat(newAxis.getNominalSize()).isEqualTo(oldAxis.getNcoords());
          }
        }
      }
    }
    return true;
  }

}

