/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.Index;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.grib.collection.GribDataReader;
import ucar.nc2.internal.util.CompareArrayToMa2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * GridCoverage Subsetting
 * Read all the data for a grid variable and compare it to dt.Grid
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGridCompareWithDt {

  @BeforeClass
  public static void before() {
    GribDataReader.validator = new GribCoverageValidator();
  }

  @AfterClass
  public static void after() {
    GribDataReader.validator = null;
  }

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    // NUWG - has CoordinateAlias
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc"});
    // scalar runtime
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/ECME_RIZ_201201101200_00600_GB"});
    // both x,y and lat,lon
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/testCFwriter.nc"});
    // SRC
    result.add(
        new Object[] {TestDir.cdmUnitTestDir + "gribCollections/tp/GFS_Global_onedeg_ana_20150326_0600.grib2.ncx4"});
    // ensemble, time-offset
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/MM_cnrm_129_red.ncml"});
    // scalar vert LOOK change to TimeOffset ??
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/ukmo.nc"});


    // dt fails here because offset axis units (hour) different from runtime (secs)
    // result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4", FeatureType.GRID,
    // false});

    // x,y axis but no projection
    // dt doesnt work correctly
    // result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/Run_20091025_0000.nc", FeatureType.CURVILINEAR,
    // false});

    //// from TestGridCompareCoverage
    FileFilter ff = TestDir.FileFilterSkipSuffix(
        ".ncx4 .gbx9 .cdl .pdf perverse.nc aggFmrc.xml 2003021212_avn-x.nc wrfout_01_000000_0003.nc wrfout_01_000000_0003.ncml");
    try {
      result.add(new Object[] {TestDir.cdmLocalTestDataDir + "ncml/nc/ubyte_1.nc4"});
      result.add(new Object[] {TestDir.cdmLocalTestDataDir + "ncml/nc/cldc.mean.nc"});
      result.add(new Object[] {TestDir.cdmLocalTestDataDir + "ncml/fmrc/GFS_Puerto_Rico_191km_20090729_0000.nc"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/grid/namExtract/test_agg.ncml"});

      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/atd/rgg.20020411.000000.lel.ll.nc"});
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "conventions/atd-radar", ff, result);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "conventions/avhrr", ff, result);
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/awips/awips.nc"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/cedric/test.ncml"});
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "conventions/cf", ff, result, false);
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_01_000000_0003.ncml"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/wrf/wrf_masscore.nc"});
    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  private String endpoint;

  public TestGridCompareWithDt(String endpoint) {
    this.endpoint = endpoint;
  }

  @Test
  public void testGridCoverageDataset() throws IOException {
    System.out.printf("Test Dataset %s%n", endpoint);

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(endpoint, errlog)) {
      assertThat(gds).isNotNull();

      try (GridDataset ds = GridDataset.open(endpoint)) {
        compareCoordinateSystems(gds, ds, true);
        readAll(gds, ds);
      }
    }
  }

  public static void testAll(String filename, boolean readData) throws IOException {
    System.out.printf("Test Dataset %s%n", filename);

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();

      try (GridDataset ds = GridDataset.open(filename)) {
        compareCoordinateSystems(gds, ds, true);
        if (readData) {
          readAll(gds, ds);
        }
      }
    }
  }

  public static void compareCoordinateSystems(ucar.nc2.grid.GridDataset gds, GridDataset ds, boolean show) {
    for (Grid grid : gds.getGrids()) {
      ucar.nc2.dt.grid.GeoGrid geogrid = ds.findGridByName(grid.getName());
      if (geogrid == null) {
        System.out.printf("*** Grid %s not in GridDataset%n", grid.getName());
        continue;
      }
      if (show) {
        System.out.printf("  CS Grid/GeoGrid: %s%n", grid.getName());
      }

      GridCoordSystem oldGcs = geogrid.getCoordinateSystem();
      GridCoordinateSystem newGcs = grid.getCoordinateSystem();

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

  public static void readAll(ucar.nc2.grid.GridDataset gds, GridDataset ds) {
    for (GridDatatype dt : ds.getGrids()) {
      if (dt.getFullName().startsWith("Best/"))
        continue;
      String name = dt.getShortName();
      Optional<Grid> cover = gds.findGrid(name);
      if (cover.isEmpty()) {
        System.out.printf(" SKIP %s %s%n", dt.getFullName(), name);
      } else {
        System.out.printf(" READ %s %s%n", dt.getFullName(), name);
        readAll(cover.get(), dt);
      }
    }
  }

  public static void readAll(Grid grid, GridDatatype dt) {
    GridCoordSystem csys = dt.getCoordinateSystem();
    CoordinateAxis1DTime rtAxis = csys.getRunTimeAxis();
    CoordinateAxis1D ensAxis = csys.getEnsembleAxis();
    CoordinateAxis1D vertAxis = csys.getVerticalAxis();

    readAllRuntimes(grid, dt, rtAxis, ensAxis, vertAxis);
  }

  private static void readAllRuntimes(Grid grid, GridDatatype dt, CoordinateAxis1DTime runtimeAxis,
      CoordinateAxis1D ensAxis, CoordinateAxis1D vertAxis) {
    GridCoordSystem csys = dt.getCoordinateSystem();
    CoordinateAxis1DTime timeAxis1D = csys.getTimeAxis1D();
    CoordinateAxis timeAxis = csys.getTimeAxis();
    CoordinateAxis2D timeAxis2D = (timeAxis instanceof CoordinateAxis2D) ? (CoordinateAxis2D) timeAxis : null;

    if (runtimeAxis == null)
      readAllTimes1D(grid, dt, null, -1, timeAxis1D, ensAxis, vertAxis);

    else if (timeAxis2D == null) { // 1D time or no time
      for (int i = 0; i < runtimeAxis.getSize(); i++)
        readAllTimes1D(grid, dt, runtimeAxis.getCalendarDate(i), i, timeAxis1D, ensAxis, vertAxis);

    } else { // 2D time
      CalendarDateUnit helper =
          CalendarDateUnit.fromAttributes(timeAxis.attributes(), timeAxis.getUnitsString()).orElseThrow();

      if (timeAxis2D.isInterval()) {
        ArrayDouble.D3 bounds = timeAxis2D.getCoordBoundsArray();
        for (int i = 0; i < runtimeAxis.getSize(); i++)
          readAllTimes2D(grid, dt, runtimeAxis.getCalendarDate(i), i, helper, bounds.slice(0, i), ensAxis, vertAxis);

      } else {
        ArrayDouble.D2 coords = timeAxis2D.getCoordValuesArray();
        for (int i = 0; i < runtimeAxis.getSize(); i++)
          readAllTimes2D(grid, dt, runtimeAxis.getCalendarDate(i), i, helper, coords.slice(0, i), ensAxis, vertAxis);
      }
    }
  }

  private static void readAllTimes1D(Grid grid, GridDatatype dt, CalendarDate rt_val, int rt_idx,
      CoordinateAxis1DTime timeAxis, CoordinateAxis1D ensAxis, CoordinateAxis1D vertAxis) {
    if (timeAxis == null)
      readAllEnsembles(grid, dt, rt_val, rt_idx, null, -1, ensAxis, vertAxis);
    else {
      for (int i = 0; i < timeAxis.getSize(); i++) {
        CalendarDate timeDate =
            timeAxis.isInterval() ? timeAxis.getCoordBoundsMidpointDate(i) : timeAxis.getCalendarDate(i);
        readAllEnsembles(grid, dt, rt_val, rt_idx, timeDate, i, ensAxis, vertAxis);
      }
    }
  }

  private static void readAllTimes2D(Grid grid, GridDatatype dt, CalendarDate rt_val, int rt_idx,
      CalendarDateUnit helper, Array timeVals, CoordinateAxis1D ensAxis, CoordinateAxis1D vertAxis) {

    int[] shape = timeVals.getShape();
    if (timeVals.getRank() == 1) {
      timeVals.resetLocalIterator();
      int time_idx = 0;
      while (timeVals.hasNext()) {
        double timeVal = timeVals.nextDouble();
        readAllEnsembles(grid, dt, rt_val, rt_idx, helper.makeFractionalCalendarDate(timeVal), time_idx++, ensAxis,
            vertAxis);
      }

    } else {
      Index index = timeVals.getIndex();
      for (int i = 0; i < shape[0]; i++) {
        double timeVal = (timeVals.getDouble(index.set(i, 0)) + timeVals.getDouble(index.set(i, 1))) / 2;
        readAllEnsembles(grid, dt, rt_val, rt_idx, helper.makeFractionalCalendarDate(timeVal), i, ensAxis, vertAxis);
      }
    }
  }

  private static void readAllEnsembles(Grid grid, GridDatatype dt, CalendarDate rt_val, int rt_idx,
      CalendarDate time_val, int time_idx, CoordinateAxis1D ensAxis, CoordinateAxis1D vertAxis) {
    if (ensAxis == null)
      readAllVertLevels(grid, dt, rt_val, rt_idx, time_val, time_idx, 0, -1, vertAxis);
    else {
      for (int i = 0; i < ensAxis.getSize(); i++)
        readAllVertLevels(grid, dt, rt_val, rt_idx, time_val, time_idx, ensAxis.getCoordValue(i), i, vertAxis);
    }
  }

  private static void readAllVertLevels(Grid grid, GridDatatype dt, CalendarDate rt_val, int rt_idx,
      CalendarDate time_val, int time_idx, double ens_val, int ens_idx, CoordinateAxis1D vertAxis) {
    if (vertAxis == null)
      readOneSlice(grid, dt, rt_val, rt_idx, time_val, time_idx, ens_val, ens_idx, 0, -1, false);
    else {
      for (int i = 0; i < vertAxis.getSize(); i++) {
        double levVal = vertAxis.isInterval() ? vertAxis.getCoordBoundsMidpoint(i) : vertAxis.getCoordValue(i);
        readOneSlice(grid, dt, rt_val, rt_idx, time_val, time_idx, ens_val, ens_idx, levVal, i, false);
      }
    }
  }

  static void readOneSlice(Grid grid, GridDatatype dt, CalendarDate rt_val, int rt_idx, CalendarDate time_val,
      int time_idx, double ens_val, int ens_idx, double vert_val, int vert_idx, boolean show) {
    if (show) {
      System.out.printf("%n===Slice %s runtime=%s (%d) ens=%f (%d) time=%s (%d) vert=%f (%d) %n", grid.getName(),
          rt_val, rt_idx, ens_val, ens_idx, time_val, time_idx, vert_val, vert_idx);
    }

    Array dt_array;
    try {
      dt_array = dt.readDataSlice(rt_idx, ens_idx, time_idx, vert_idx, -1, -1);
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    GridSubset subset = new GridSubset();
    if (rt_idx >= 0)
      subset.setRunTime(rt_val);
    if (ens_idx >= 0)
      subset.setEnsCoord(ens_val);
    if (time_idx >= 0)
      subset.setDate(time_val);
    if (vert_idx >= 0)
      subset.setVertCoord(vert_val);

    GridReferencedArray gc_array;
    try {
      gc_array = grid.readData(subset);
    } catch (IOException | ucar.array.InvalidRangeException e) {
      e.printStackTrace();
      return;
    }

    MaterializedCoordinateSystem mcs = gc_array.getMaterializedCoordinateSystem();
    System.out.printf("  SubsetRanges = %s%n", mcs.getSubsetRanges());

    Formatter errlog = new Formatter();
    boolean ok = false;
    try {
      ok = CompareArrayToMa2.compareData(errlog, "slice", dt_array, gc_array.data(), false, true);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (!ok) {
      String errString = errlog.toString();
      if (errString.length() > 5000) {
        errString = errString.substring(0, 5000);
      }
      System.out.printf("err=%s%n", errString);
    }
    assertThat(ok).isTrue();
  }


}
