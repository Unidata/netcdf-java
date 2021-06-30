/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid2;

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
public class TestGridCompare {

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

  public TestGridCompare(String filename) {
    this.filename = filename;
  }


  @Test
  public void compareGrid() throws Exception {
    // these are failing in old code.
    if (filename.endsWith("cdm_sea_soundings.nc4"))
      return;
    if (filename.endsWith("IntTimSciSamp.nc"))
      return;
    if (filename.contains("conventions/cf/ipcc"))
      return;

    compareWithDt(filename);
    compareWithCoverage(filename);
    compareWithGrid1(filename);
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

        for (GridAxis newAxis : newGcs.getGridAxes()) {
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

        for (GridAxis newAxis : newGcs.getGridAxes()) {
          CoverageCoordAxis oldAxis = oldGcs.getAxes().stream()
              .filter(a -> a.getAxisType().equals(newAxis.getAxisType())).findFirst().orElse(null);
          if (oldAxis == null) {
            oldAxis =
                oldGcs.getAxes().stream().filter(a -> a.getName().equals(newAxis.getName())).findFirst().orElse(null);
          }
          assertWithMessage(String.format("    GridAxis: %s %s%n", newAxis.getName(), newAxis.getAxisType()))
              .that(oldAxis).isNotNull();
          assertThat(newAxis.getNominalSize()).isEqualTo(oldAxis.getNcoords());
        }
      }
    }
    return true;
  }

  public static boolean compareWithGrid1(String filename) throws Exception {
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
          assertWithMessage(String.format("    GridAxis: %s %s%n", newAxis.getName(), newAxis.getAxisType()))
              .that(oldAxis).isNotNull();
          assertThat(newAxis.getNominalSize()).isEqualTo(oldAxis.getNominalShape()[0]);
        }
      }
    }
    return true;
  }

  /*
   * public static void compareGridDataset(String filename) throws Exception {
   * Formatter errlog = new Formatter();
   * try (GridDataset newDataset = GridDatasetFactory.openGridDataset(filename, errlog);
   * ucar.nc2.dt.grid.GridDataset dataset = ucar.nc2.dt.grid.GridDataset.open(filename)) {
   * if (newDataset == null) {
   * System.out.printf("Cant open as GridDataset: %s%n", filename);
   * return;
   * }
   * System.out.printf("compareGridDataset: %s%n", newDataset.getLocation());
   * 
   * boolean ok = true;
   * for (Grid grid : newDataset.getGrids()) {
   * ucar.nc2.dt.grid.GeoGrid geogrid = dataset.findGridByName(grid.getName());
   * if (geogrid == null) {
   * System.out.printf("Grid %s not in Geogrid: ", grid.getName());
   * return;
   * }
   * System.out.println("  Grid/GeoGrid: " + grid.getName());
   * ucar.nc2.dt.GridCoordSystem oldGcs = geogrid.getCoordinateSystem();
   * 
   * GridCoordinateSystem newGcs = grid.getCoordinateSystem();
   * GridTimeCoordinateSystem newTcs = grid.getTimeCoordinateSystem();
   * if (newTcs.getTimeOffsetAxis(0) != null && oldGcs.getTimeAxis() != null) {
   * GridAxis newTimeAxis = newTcs.getTimeOffsetAxis(0);
   * CoordinateAxis1DTime timeAxis = (CoordinateAxis1DTime) oldGcs.getTimeAxis();
   * assertThat(newTimeAxis.getNominalSize()).isEqualTo(timeAxis.getSize());
   * 
   * int timeIdx = 0;
   * for (Object timeCoord : newTimeAxis.) {
   * if (newGcs.getVerticalAxis() != null) {
   * ok &= doVert(grid, geogrid, timeCoord, timeIdx); // time and vertical
   * 
   * } else { // time, no vertical
   * GridReader subset = grid.getReader().setTimeCoord(timeCoord);
   * ok &= doOne(grid, geogrid, subset, timeIdx, -1); // no time, no vertical
   * }
   * timeIdx++;
   * }
   * } else {
   * if (newGcs.getVerticalAxis() != null) {
   * ok &= doVert(grid, geogrid, 0.0, -1); // no time, only vertical
   * } else {
   * doOne(grid, geogrid, grid.getReader(), -1, -1); // no time, no vertical
   * }
   * }
   * }
   * assertThat(okGridDataset;
   * } catch (RuntimeException rte) {
   * if (rte.getMessage() != null && rte.getMessage().contains("not monotonic")) {
   * return;
   * }
   * rte.printStackTrace();
   * fail();
   * }
   * }
   * 
   * private static boolean doVert(Grid grid, ucar.nc2.dt.grid.GeoGrid geogrid, Object timeCoord, int timeIdx)
   * throws Exception {
   * boolean ok = true;
   * GridAxis newVertAxis = grid.getCoordinateSystem().getVerticalAxis();
   * CoordinateAxis1D vertAxis = geogrid.getCoordinateSystem().getVerticalAxis();
   * assertThat(newVertAxis.getNominalSize()).isEqualTo(vertAxis.getSize());
   * int vertIdx = 0;
   * for (Object vertCoord : newVertAxis.getCoordinates()) {
   * GridReader subset = grid.getReader().setTimeCoord(timeCoord).setVertCoord(vertCoord);
   * ok &= doOne(grid, geogrid, subset, timeIdx, vertIdx);
   * vertIdx++;
   * }
   * return ok;
   * }
   * 
   * private static boolean doOne(Grid grid, ucar.nc2.dt.grid.GeoGrid geogrid, GridReader subset, int timeIdx, int
   * vertIdx)
   * throws IOException, ucar.array.InvalidRangeException {
   * GridReferencedArray geoArray = subset.read();
   * ucar.ma2.Array org = geogrid.readDataSlice(timeIdx, vertIdx, -1, -1);
   * Formatter f = new Formatter();
   * boolean ok1 = CompareArrayToMa2.compareData(f, grid.getName(), org, geoArray.data(), true, true);
   * if (!ok1) {
   * System.out.printf("gridSubset= %s; time vert= (%d %d) %n%s%n", subset, timeIdx, vertIdx, f);
   * }
   * return ok1;
   * }
   * 
   */

  /*
   * public static void compareGridCoverage(String filename) throws Exception {
   * Formatter errlog = new Formatter();
   * try (GridDataset newDataset = GridDatasetFactory.openGridDataset(filename, errlog);
   * FeatureDatasetCoverage covDataset = CoverageDatasetFactory.open(filename)) {
   * if (newDataset == null) {
   * System.out.printf("Cant open as GridDataset: %s%n", filename);
   * return;
   * }
   * System.out.printf("compareGridCoverage: %s%n", newDataset.getLocation());
   * 
   * CoverageCollection covCollection = covDataset.getCoverageCollections().get(0);
   * 
   * boolean ok = true;
   * for (Grid grid : newDataset.getGrids()) {
   * Coverage coverage = covCollection.findCoverage(grid.getName());
   * if (coverage == null) {
   * System.out.printf("  Grid %s not in coverage: " + grid.getName());
   * return;
   * }
   * System.out.println("  Grid/Coverage: " + grid.getName());
   * 
   * CoverageCoordSys gcs = coverage.getCoordSys();
   * 
   * GridCoordinateSystem newGcs = grid.getCoordinateSystem();
   * assertThat(newGcs.getTimeAxis() == null).isEqualTo(gcs.getTimeAxis() == null);
   * 
   * if (newGcs.getTimeAxis() != null) {
   * GridAxis taxis = newGcs.getTimeAxis();
   * assertThat(taxis).isInstanceOf(GridAxis1DTime.class);
   * GridAxis1DTime newTimeAxis = (GridAxis1DTime) taxis;
   * 
   * assertThat(gcs.getTimeAxis()).isNotNull();
   * CoverageCoordAxis axis = gcs.getTimeAxis();
   * assertThat(axis).isInstanceOf(CoverageCoordAxis1D.class);
   * CoverageCoordAxis1D timeAxis = (CoverageCoordAxis1D) axis;
   * 
   * assertThat(newTimeAxis.getNcoords()).isEqualTo(timeAxis.getNcoords());
   * for (int timeIdx = 0; timeIdx < newTimeAxis.getNcoords(); timeIdx++) {
   * double timeCoord = newTimeAxis.getCoordMidpoint(timeIdx);
   * 
   * if (newGcs.getVerticalAxis() != null) {
   * ok &= doVert(grid, coverage, timeCoord); // both time and vert
   * 
   * } else { // just time
   * GridSubset subset = new GridSubset();
   * subset.setTimeCoord(timeCoord);
   * 
   * SubsetParams subsetp = new SubsetParams();
   * subsetp.setTimeCoord(timeCoord);
   * 
   * ok &= doOne(grid, coverage, subset, subsetp);
   * }
   * }
   * } else { // no time, just vert
   * if (newGcs.getVerticalAxis() != null) {
   * ok &= doVert(grid, coverage, 0.0); // no time, only vertical
   * } else {
   * doOne(grid, coverage, new GridSubset(), new SubsetParams()); // no time, no vertical
   * }
   * }
   * }
   * assertThat(ok).isTrue();
   * } catch (RuntimeException rte) {
   * if (rte.getMessage() != null && rte.getMessage().contains("not monotonic")) {
   * return;
   * }
   * rte.printStackTrace();
   * fail();
   * }
   * }
   * 
   * private static boolean doVert(Grid grid, Coverage coverage, double timeCoord) throws Exception {
   * boolean ok = true;
   * GridAxis1D newVertAxis = grid.getCoordinateSystem().getVerticalAxis();
   * CoverageCoordAxis zAxis = coverage.getCoordSys().getZAxis();
   * assertThat(zAxis).isInstanceOf(CoverageCoordAxis1D.class);
   * CoverageCoordAxis1D vertAxis = (CoverageCoordAxis1D) zAxis;
   * 
   * assertThat(newVertAxis.getNcoords()).isEqualTo(vertAxis.getNcoords());
   * for (int vertIdx = 0; vertIdx < newVertAxis.getNcoords(); vertIdx++) {
   * GridSubset subset = new GridSubset();
   * subset.setTimeCoord(timeCoord);
   * subset.setVertCoord(newVertAxis.getCoordMidpoint(vertIdx));
   * 
   * SubsetParams subsetp = new SubsetParams();
   * subsetp.setTimeCoord(timeCoord);
   * subsetp.setVertCoord(vertAxis.getCoordMidpoint(vertIdx));
   * 
   * ok &= doOne(grid, coverage, subset, subsetp);
   * }
   * return ok;
   * }
   * 
   * private static boolean doOne(Grid grid, Coverage coverage, GridSubset subset, SubsetParams subsetp)
   * throws IOException, ucar.array.InvalidRangeException, ucar.ma2.InvalidRangeException {
   * GridReferencedArray gridArray = grid.readData(subset);
   * GeoReferencedArray covArray = coverage.readData(subsetp);
   * Formatter f = new Formatter();
   * boolean ok1 = CompareArrayToMa2.compareData(f, grid.getName(), covArray.getData(), gridArray.data(), true, true);
   * if (!ok1) {
   * System.out.printf("gridSubset= %s; covSubset= %s%n%s%n", subset, subsetp, f);
   * }
   * return ok1;
   * }
   * 
   */

}

