/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.internal.util.CompareArrayToMa2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/** Compare reading new and old GridDataset. */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
@Ignore("Slow")
public class TestReadGridCompare {

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

  public TestReadGridCompare(String filename) {
    this.filename = filename;
  }

  private final String filename;

  @Test
  public void compareGrid() throws Exception {
    // these are failing in old code.
    if (filename.endsWith("cdm_sea_soundings.nc4"))
      return;
    if (filename.endsWith("IntTimSciSamp.nc"))
      return;

    compareGridDataset(filename);

    // Coverage has lots of bugs - fix later
    // compareGridCoverage(filename);
  }

  public static void compareGridDataset(String filename) throws Exception {
    Formatter errlog = new Formatter();
    try (GridDataset newDataset = GridDatasetFactory.openGridDataset(filename, errlog);
        ucar.nc2.dt.grid.GridDataset dataset = ucar.nc2.dt.grid.GridDataset.open(filename)) {
      if (newDataset == null) {
        System.out.printf("Cant open as GridDataset: %s%n", filename);
        return;
      }
      System.out.printf("compareGridDataset: %s%n", newDataset.getLocation());

      boolean ok = true;
      for (Grid grid : newDataset.getGrids()) {
        ucar.nc2.dt.grid.GeoGrid geogrid = dataset.findGridByName(grid.getName());
        if (geogrid == null) {
          System.out.printf("Grid %s not in Geogrid: ", grid.getName());
          return;
        }
        System.out.println("  Grid/GeoGrid: " + grid.getName());
        ucar.nc2.dt.GridCoordSystem gcs = geogrid.getCoordinateSystem();

        GridCoordinateSystem newGcs = grid.getCoordinateSystem();
        if (newGcs.getTimeAxis() != null && gcs.getTimeAxis() != null) {
          GridAxis1DTime newTimeAxis = newGcs.getTimeAxis();
          CoordinateAxis1DTime timeAxis = (CoordinateAxis1DTime) gcs.getTimeAxis();
          assertThat(newTimeAxis.getNcoords()).isEqualTo(timeAxis.getSize());

          int timeIdx = 0;
          for (Object timeCoord : newTimeAxis) {
            if (newGcs.getVerticalAxis() != null) {
              ok &= doVert(grid, geogrid, timeCoord, timeIdx); // time and vertical

            } else { // time, no vertical
              GridSubset subset = new GridSubset();
              subset.setTimeCoord(timeCoord);
              ok &= doOne(grid, geogrid, subset, timeIdx, -1); // no time, no vertical
            }
            timeIdx++;
          }
        } else {
          if (newGcs.getVerticalAxis() != null) {
            ok &= doVert(grid, geogrid, 0.0, -1); // no time, only vertical
          } else {
            doOne(grid, geogrid, new GridSubset(), -1, -1); // no time, no vertical
          }
        }
      }
      assertThat(ok).isTrue();
    } catch (RuntimeException rte) {
      if (rte.getMessage() != null && rte.getMessage().contains("not monotonic")) {
        return;
      }
      rte.printStackTrace();
      fail();
    }
  }

  private static boolean doVert(Grid grid, ucar.nc2.dt.grid.GeoGrid geogrid, Object timeCoord, int timeIdx)
      throws Exception {
    boolean ok = true;
    GridAxis1D newVertAxis = grid.getCoordinateSystem().getVerticalAxis();
    CoordinateAxis1D vertAxis = geogrid.getCoordinateSystem().getVerticalAxis();
    assertThat(newVertAxis.getNcoords()).isEqualTo(vertAxis.getSize());
    int vertIdx = 0;
    for (Object vertCoord : newVertAxis) {
      GridSubset subset = new GridSubset();
      subset.setTimeCoord(timeCoord);
      subset.setVertCoord(vertCoord);
      ok &= doOne(grid, geogrid, subset, timeIdx, vertIdx);
      vertIdx++;
    }
    return ok;
  }

  private static boolean doOne(Grid grid, ucar.nc2.dt.grid.GeoGrid geogrid, GridSubset subset, int timeIdx, int vertIdx)
      throws IOException, ucar.array.InvalidRangeException {
    GridReferencedArray geoArray = grid.readData(subset);
    ucar.ma2.Array org = geogrid.readDataSlice(timeIdx, vertIdx, -1, -1);
    Formatter f = new Formatter();
    boolean ok1 = CompareArrayToMa2.compareData(f, grid.getName(), org, geoArray.data(), true, true);
    if (!ok1) {
      System.out.printf("gridSubset= %s; time vert= (%d %d) %n%s%n", subset, timeIdx, vertIdx, f);
    }
    return ok1;
  }

  public static void compareGridCoverage(String filename) throws Exception {
    Formatter errlog = new Formatter();
    try (GridDataset newDataset = GridDatasetFactory.openGridDataset(filename, errlog);
        FeatureDatasetCoverage covDataset = CoverageDatasetFactory.open(filename)) {
      if (newDataset == null) {
        System.out.printf("Cant open as GridDataset: %s%n", filename);
        return;
      }
      System.out.printf("compareGridCoverage: %s%n", newDataset.getLocation());

      CoverageCollection covCollection = covDataset.getCoverageCollections().get(0);

      boolean ok = true;
      for (Grid grid : newDataset.getGrids()) {
        Coverage coverage = covCollection.findCoverage(grid.getName());
        if (coverage == null) {
          System.out.printf("  Grid %s not in coverage: " + grid.getName());
          return;
        }
        System.out.println("  Grid/Coverage: " + grid.getName());

        CoverageCoordSys gcs = coverage.getCoordSys();

        GridCoordinateSystem newGcs = grid.getCoordinateSystem();
        assertThat(newGcs.getTimeAxis() == null).isEqualTo(gcs.getTimeAxis() == null);

        if (newGcs.getTimeAxis() != null) {
          GridAxis1DTime newTimeAxis = newGcs.getTimeAxis();

          assertThat(gcs.getTimeAxis()).isNotNull();
          CoverageCoordAxis axis = gcs.getTimeAxis();
          assertThat(axis).isInstanceOf(CoverageCoordAxis1D.class);
          CoverageCoordAxis1D timeAxis = (CoverageCoordAxis1D) axis;

          assertThat(newTimeAxis.getNcoords()).isEqualTo(timeAxis.getNcoords());
          for (int timeIdx = 0; timeIdx < newTimeAxis.getNcoords(); timeIdx++) {
            double timeCoord = newTimeAxis.getCoordMidpoint(timeIdx);

            if (newGcs.getVerticalAxis() != null) {
              ok &= doVert(grid, coverage, timeCoord); // both time and vert

            } else { // just time
              GridSubset subset = new GridSubset();
              subset.setTimeCoord(timeCoord);

              SubsetParams subsetp = new SubsetParams();
              subsetp.setTimeCoord(timeCoord);

              ok &= doOne(grid, coverage, subset, subsetp);
            }
          }
        } else { // no time, just vert
          if (newGcs.getVerticalAxis() != null) {
            ok &= doVert(grid, coverage, 0.0); // no time, only vertical
          } else {
            doOne(grid, coverage, new GridSubset(), new SubsetParams()); // no time, no vertical
          }
        }
      }
      assertThat(ok).isTrue();
    } catch (RuntimeException rte) {
      if (rte.getMessage() != null && rte.getMessage().contains("not monotonic")) {
        return;
      }
      rte.printStackTrace();
      fail();
    }
  }

  private static boolean doVert(Grid grid, Coverage coverage, double timeCoord) throws Exception {
    boolean ok = true;
    GridAxis1D newVertAxis = grid.getCoordinateSystem().getVerticalAxis();
    CoverageCoordAxis zAxis = coverage.getCoordSys().getZAxis();
    assertThat(zAxis).isInstanceOf(CoverageCoordAxis1D.class);
    CoverageCoordAxis1D vertAxis = (CoverageCoordAxis1D) zAxis;

    assertThat(newVertAxis.getNcoords()).isEqualTo(vertAxis.getNcoords());
    for (int vertIdx = 0; vertIdx < newVertAxis.getNcoords(); vertIdx++) {
      GridSubset subset = new GridSubset();
      subset.setTimeCoord(timeCoord);
      subset.setVertCoord(newVertAxis.getCoordMidpoint(vertIdx));

      SubsetParams subsetp = new SubsetParams();
      subsetp.setTimeCoord(timeCoord);
      subsetp.setVertCoord(vertAxis.getCoordMidpoint(vertIdx));

      ok &= doOne(grid, coverage, subset, subsetp);
    }
    return ok;
  }

  private static boolean doOne(Grid grid, Coverage coverage, GridSubset subset, SubsetParams subsetp)
      throws IOException, ucar.array.InvalidRangeException, ucar.ma2.InvalidRangeException {
    GridReferencedArray gridArray = grid.readData(subset);
    GeoReferencedArray covArray = coverage.readData(subsetp);
    Formatter f = new Formatter();
    boolean ok1 = CompareArrayToMa2.compareData(f, grid.getName(), covArray.getData(), gridArray.data(), true, true);
    if (!ok1) {
      System.out.printf("gridSubset= %s; covSubset= %s%n%s%n", subset, subsetp, f);
    }
    return ok1;
  }

}

