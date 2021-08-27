/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.ft2.coverage.Coverage;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageCoordAxis;
import ucar.nc2.ft2.coverage.CoverageCoordAxis1D;
import ucar.nc2.ft2.coverage.CoverageCoordSys;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.ft2.coverage.GeoReferencedArray;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.internal.util.CompareArrayToMa2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Compare reading GridDataset and CoverageDataset. */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGridCompareCoverage {

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
      result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/grid/cg/cg.ncml"});
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

  /////////////////////////////////////////////////////////////
  private static final boolean show = false;

  private final String filename;
  private boolean readData = true;

  public TestGridCompareCoverage(String filename) {
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

    System.out.printf("%ncompare GridDataset %s%n", filename);
    compareWithCoverage(readData);
  }

  public boolean compareWithCoverage(boolean readData) throws Exception {
    this.readData = readData;

    Formatter errlog = new Formatter();
    try (GridDataset newDataset = GridDatasetFactory.openGridDataset(filename, errlog);
        FeatureDatasetCoverage cc = CoverageDatasetFactory.open(filename)) {
      assertThat(cc).isNotNull();
      CoverageCollection oldDataset = cc.getCoverageCollections().get(0);
      assertThat(oldDataset).isNotNull();

      if (newDataset == null) {
        System.out.printf(" Cant open as ucar.nc2.grid2.GridDataset: %s%n", errlog);
        return false;
      }
      if (oldDataset == null) {
        System.out.printf(" Cant open as ucar.nc2.grid.GridDataset: %s%n", errlog);
        return false;
      }

      for (Grid grid : newDataset.getGrids()) {
        Coverage oldGrid = oldDataset.findCoverage(grid.getName());
        if (oldGrid == null) {
          System.out.printf("*** Coverage %s not in CoverageCollection%n", grid.getName());
          continue;
        }
        System.out.printf("  Grid: %s%n", grid.getName());
        boolean ok = true;

        CoverageCoordSys oldGcs = oldGrid.getCoordSys();
        CoverageCoordAxis oldRuntime = oldGcs.getAxis(AxisType.RunTime);

        GridTimeCoordinateSystem tcs = grid.getTimeCoordinateSystem();
        if (oldRuntime != null) {
          assertThat(tcs).isNotNull();
        }

        if (tcs != null) {
          GridAxisPoint runtime = tcs.getRunTimeAxis();
          if (runtime != null && oldRuntime != null) {
            ok &= doRuntime(grid, runtime, oldGrid, oldRuntime);
          } else {
            GridAxis<?> timeOffsetAxis = tcs.getTimeOffsetAxis(0);
            CoverageCoordAxis oldTimeOffsetAxis = oldGcs.getAxis(AxisType.TimeOffset);
            if (timeOffsetAxis != null && oldTimeOffsetAxis != null) {
              ok &= doOffsetTime(grid, grid.getReader(), 0, oldGrid, new SubsetParams());
            }
            CoverageCoordAxis oldTimeAxis = oldGcs.getTimeAxis();
            if (timeOffsetAxis != null && oldTimeAxis != null) {
              ok &= doOffsetTime(grid, grid.getReader(), 0, oldGrid, new SubsetParams());
            }
          }
        } else {
          ok &= doVert(grid, grid.getReader(), oldGrid, new SubsetParams());
        }

        if (!ok) {
          System.out.printf("*** Grid %s read and compare failed%n", grid.getName());
        }
      }
    }
    return true;
  }

  private boolean doRuntime(Grid grid, GridAxisPoint runtime, Coverage oldGrid, CoverageCoordAxis oldRuntime)
      throws Exception {

    boolean ok = true;
    GridTimeCoordinateSystem tcs = grid.getTimeCoordinateSystem();
    CoverageCoordAxis1D oldRuntime1D = (CoverageCoordAxis1D) oldRuntime;

    for (int runtimeIdx = 0; runtimeIdx < oldRuntime1D.getNcoords(); runtimeIdx++) {
      double timeCoord = runtime.getCoordMidpoint(runtimeIdx);
      double timeCoordOld = oldRuntime1D.getCoordMidpoint(runtimeIdx);
      assertThat(timeCoord).isEqualTo(timeCoordOld);

      CalendarDate runtimeDate = tcs.getRuntimeDate(runtimeIdx);

      GridReader reader = grid.getReader().setRunTime(runtimeDate);
      SubsetParams subsetOld = new SubsetParams().setRunTime(runtimeDate);
      ok &= doOffsetTime(grid, reader, runtimeIdx, oldGrid, subsetOld);
    }
    return ok;
  }

  private boolean doOffsetTime(Grid grid, GridReader reader, int runtimeIdx, Coverage oldGrid, SubsetParams subsetOld)
      throws Exception {
    boolean ok = true;

    GridTimeCoordinateSystem tcs = grid.getTimeCoordinateSystem();
    GridAxis<?> timeAxis = tcs.getTimeOffsetAxis(runtimeIdx);

    GridCoordinateSystem gcs = grid.getCoordinateSystem();
    GridAxis<?> vertAxis = gcs.getVerticalAxis();

    for (Object timeCoord : timeAxis) {
      reader.setTimeOffsetCoord(timeCoord);
      if (timeCoord instanceof Double) {
        subsetOld.setTimeCoord((Double) timeCoord);
      } else {
        CoordInterval intv = (CoordInterval) timeCoord;
        subsetOld.setTimeOffsetIntv(new double[] {intv.start(), intv.end()});
      }
      if (vertAxis != null) {
        ok &= doVert(grid, reader, oldGrid, subsetOld);
      }
    }
    return ok;
  }

  private boolean doVert(Grid grid, GridReader reader, Coverage oldGrid, SubsetParams subsetOld) throws Exception {
    boolean ok = true;

    GridCoordinateSystem gcs = grid.getCoordinateSystem();
    GridAxis<?> vertAxis = gcs.getVerticalAxis();
    if (vertAxis != null) {
      for (Object vertCoord : vertAxis) {
        reader.setVertCoord(vertCoord);
        if (vertCoord instanceof Double) {
          subsetOld.setVertCoord((Double) vertCoord);
        } else {
          CoordInterval intv = (CoordInterval) vertCoord;
          subsetOld.setVertCoordIntv(new double[] {intv.start(), intv.end()});
        }
        ok &= doOne(grid, reader, oldGrid, subsetOld);
      }
    } else {
      ok &= doOne(grid, reader, oldGrid, subsetOld);
    }
    return ok;
  }

  private boolean doOne(Grid grid, GridReader reader, Coverage oldGrid, SubsetParams subsetOld)
      throws IOException, ucar.array.InvalidRangeException, InvalidRangeException {

    if (!readData) {
      return true;
    }

    GridReferencedArray gridArray = reader.read();
    GeoReferencedArray oldArray = oldGrid.readData(subsetOld);
    Formatter f = new Formatter();
    boolean ok1 = CompareArrayToMa2.compareData(f, grid.getName(), oldArray.getData(), gridArray.data(), true, true);
    if (!ok1) {
      System.out.printf("   *** FAIL reader= %s; subsetOld= %s%n%s%n", reader, subsetOld, f);
    } else if (show) {
      System.out.printf("   GOOD: reader= %s; subsetOld= %s%n", reader, subsetOld);
    }
    return ok1;
  }

}

