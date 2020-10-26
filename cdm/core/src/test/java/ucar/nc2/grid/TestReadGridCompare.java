/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.ft2.coverage.*;
import ucar.nc2.internal.util.CompareArrayToMa2;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/** Compare reading netcdf with Array */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestReadGridCompare {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    FileFilter ff = TestDir.FileFilterSkipSuffix(".ncx4 .gbx9 .cdl .pdf perverse.nc aggFmrc.xml");
    List<Object[]> result = new ArrayList<>(500);
    try {
      // both files are messed up.
      // result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_01_000000_0003.nc"});
      // result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_01_000000_0003.ncml"});

      // in this case the time coordinate is not monotonic.
      // result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/nuwg/2003021212_avn-x.nc"});

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
    compareGridCoverage(filename);
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
        assertThat(newGcs.getTimeAxis() == null).isEqualTo(gcs.getTimeAxis() == null);

        if (newGcs.getTimeAxis() != null) {
          GridAxis1DTime newTimeAxis = newGcs.getTimeAxis();
          CoordinateAxis1DTime timeAxis = (CoordinateAxis1DTime) gcs.getTimeAxis();
          assertThat(newTimeAxis.getNcoords()).isEqualTo(timeAxis.getSize());

          for (int timeIdx = 0; timeIdx < newTimeAxis.getNcoords(); timeIdx++) {
            CalendarDate date = newTimeAxis.getCalendarDate(timeIdx);
            assertThat(date.toString()).isEqualTo(timeAxis.getCalendarDate(timeIdx).toString());

            if (newGcs.getVerticalAxis() != null) {
              ok &= doVert(grid, geogrid, date, timeIdx); // time and vertical

            } else { // time, no vertical
              GridSubset subset = new GridSubset();
              subset.setTime(date);
              GridReferencedArray geoArray = grid.readData(subset);

              ucar.ma2.Array org = geogrid.readVolumeData(timeIdx);
              Formatter f = new Formatter();
              boolean ok1 = CompareArrayToMa2.compareData(f, grid.getName(), org, geoArray.data(), false, true);
              if (!ok1) {
                System.out.printf("%s%n", f);
              }
              ok &= ok1;
            }
          }
        } else if (newGcs.getVerticalAxis() != null) { // no time, only vertical
          ok &= doVert(grid, geogrid, null, -1);
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

  private static boolean doVert(Grid grid, ucar.nc2.dt.grid.GeoGrid geogrid, CalendarDate date, int timeIdx)
      throws Exception {
    boolean ok = true;
    GridAxis1D newVertAxis = grid.getCoordinateSystem().getVerticalAxis();
    CoordinateAxis1D vertAxis = geogrid.getCoordinateSystem().getVerticalAxis();
    assertThat(newVertAxis.getNcoords()).isEqualTo(vertAxis.getSize());
    for (int vertIdx = 0; vertIdx < newVertAxis.getNcoords(); vertIdx++) {
      GridSubset subset = new GridSubset();
      subset.setTime(date);
      subset.setVertCoord(newVertAxis.getCoordMidpoint(vertIdx));
      GridReferencedArray geoArray = grid.readData(subset);

      ucar.ma2.Array org = geogrid.readDataSlice(timeIdx, vertIdx, -1, -1);
      Formatter f = new Formatter();
      boolean ok1 = CompareArrayToMa2.compareData(f, grid.getName(), org, geoArray.data(), false, true);
      if (!ok1) {
        System.out.printf("timeIdx %d vertIdx %d %s%n", timeIdx, vertIdx, f);
      }
      ok &= ok1;
    }
    return ok;
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
            CalendarDate date = newTimeAxis.getCalendarDate(timeIdx);
            assertThat(date).isEqualTo(timeAxis.makeDate(timeAxis.getCoordMidpoint(timeIdx)));

            if (newGcs.getVerticalAxis() != null) {
              ok &= doVert(grid, coverage, date, timeIdx); // both time and vert

            } else { // just time
              GridSubset subset = new GridSubset();
              subset.setTime(date);
              GridReferencedArray gridArray = grid.readData(subset);

              SubsetParams subsetp = new SubsetParams();
              subsetp.setTime(date);
              GeoReferencedArray geoArray = coverage.readData(subsetp);

              Formatter f = new Formatter();
              boolean ok1 =
                  CompareArrayToMa2.compareData(f, grid.getName(), geoArray.getData(), gridArray.data(), false, true);
              if (!ok1) {
                System.out.printf("%s%n", f);
              }
              ok &= ok1;
            }
          }
        } else { // no time, just vert
          ok &= doVert(grid, coverage, null, -1);
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

  private static boolean doVert(Grid grid, Coverage coverage, CalendarDate date, int timeIdx) throws Exception {
    boolean ok = true;
    GridAxis1D newVertAxis = grid.getCoordinateSystem().getVerticalAxis();
    CoverageCoordAxis zAxis = coverage.getCoordSys().getZAxis();
    assertThat(zAxis).isInstanceOf(CoverageCoordAxis1D.class);
    CoverageCoordAxis1D vertAxis = (CoverageCoordAxis1D) zAxis;

    assertThat(newVertAxis.getNcoords()).isEqualTo(vertAxis.getNcoords());
    for (int vertIdx = 0; vertIdx < newVertAxis.getNcoords(); vertIdx++) {
      GridSubset subset = new GridSubset();
      subset.setTime(date);
      subset.setVertCoord(newVertAxis.getCoordMidpoint(vertIdx));
      GridReferencedArray gridArray = grid.readData(subset);

      SubsetParams subsetp = new SubsetParams();
      subsetp.setTime(date);
      subsetp.setVertCoord(vertAxis.getCoordMidpoint(vertIdx));
      GeoReferencedArray geoArray = coverage.readData(subsetp);

      Formatter f = new Formatter();
      boolean ok1 = CompareArrayToMa2.compareData(f, grid.getName(), geoArray.getData(), gridArray.data(), false, true);
      if (!ok1) {
        System.out.printf("timeIdx %d vertIdx %d %s%n", timeIdx, vertIdx, f);
      }
      ok &= ok1;

    }
    return ok;
  }

}

