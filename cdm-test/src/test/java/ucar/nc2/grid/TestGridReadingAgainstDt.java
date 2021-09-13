/* Copyright */
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/**
 * GridCoverage Subsetting
 * Read all the data for a grid variable and compare it to dt.Grid
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGridReadingAgainstDt {

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
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", FeatureType.GRID, false});
    // scalar runtime
    result.add(
        new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/ECME_RIZ_201201101200_00600_GB", FeatureType.GRID, false});
    // both x,y and lat,lon
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/testCFwriter.nc", FeatureType.GRID, false});
    // SRC
    result
        .add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/tp/GFS_Global_onedeg_ana_20150326_0600.grib2.ncx4",
            FeatureType.GRID, false});
    // ensemble, time-offset
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/MM_cnrm_129_red.ncml", FeatureType.FMRC, false});
    // scalar vert LOOK change to TimeOffset ??
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/ukmo.nc", FeatureType.FMRC, false});


    // dt fails here because offset axis units (hour) different from runtime (secs)
    // result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4", FeatureType.GRID,
    // false});

    // x,y axis but no projection
    // dt doesnt work correctly
    // result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/Run_20091025_0000.nc", FeatureType.CURVILINEAR,
    // false});
    return result;
  }

  private String endpoint;
  private FeatureType expectType;
  private boolean sample;

  public TestGridReadingAgainstDt(String endpoint, FeatureType expectType, boolean sample) {
    this.endpoint = endpoint;
    this.expectType = expectType;
    this.sample = sample;
  }

  @Test
  public void testGridCoverageDataset() throws IOException {
    System.out.printf("Test Dataset %s%n", endpoint);

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(endpoint, errlog)) {
      assertThat(gds).isNotNull();

      // check DtCoverageCS
      try (GridDataset ds = GridDataset.open(endpoint)) {
        for (GridDatatype dt : ds.getGrids()) {
          if (dt.getFullName().startsWith("Best/"))
            continue;

          GridCoordSystem csys = dt.getCoordinateSystem();
          CoordinateAxis1DTime rtAxis = csys.getRunTimeAxis();
          CoordinateAxis1D ensAxis = csys.getEnsembleAxis();
          CoordinateAxis1D vertAxis = csys.getVerticalAxis();

          String name = dt.getShortName();
          System.out.printf("  %s %s%n", dt.getFullName(), name);
          Grid cover = gds.findGrid(name).orElseThrow();
          assertThat(cover).isNotNull();

          readAllRuntimes(cover, dt, rtAxis, ensAxis, vertAxis);
        }
      }
    }
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
    if (true)
      System.out.printf("%n===Slice %s runtime=%s (%d) ens=%f (%d) time=%s (%d) vert=%f (%d) %n", grid.getName(),
          rt_val, rt_idx, ens_val, ens_idx, time_val, time_idx, vert_val, vert_idx);

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
    System.out.printf("SubsetRanges = %s%n", mcs.getSubsetRanges());

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
      fail();
    }
    assertThat(ok).isTrue();
  }


}
