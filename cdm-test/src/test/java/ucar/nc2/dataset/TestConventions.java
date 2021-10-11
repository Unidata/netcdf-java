/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;

import java.util.Formatter;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import java.io.IOException;
import java.util.List;

import ucar.array.ArrayType;
import ucar.nc2.calendar.Calendar;
import ucar.nc2.calendar.CalendarDate;

import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.nc2.grid.GridHorizCoordinateSystem;
import ucar.nc2.grid.GridTimeCoordinateSystem;
import ucar.unidata.geoloc.projection.Stereographic;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/**
 * Test specific files for CoordSys Conventions
 */
@Category(NeedsCdmUnitTest.class)
public class TestConventions {

  @Test
  public void testVTfail() throws IOException {
    testOpen(TestDir.cdmUnitTestDir + "conventions/csm/atmos.tuv.monthly.nc");
  }

  @Test
  public void testProblem() throws IOException {
    testOpen(TestDir.cdmUnitTestDir + "conventions/cf/cf1_rap.nc");
  }

  private void testOpen(String filename) throws IOException {
    System.out.printf("testProblem %s%n", filename);
    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      assertThat(gds.getFeatureType().isCoverageFeatureType()).isTrue();
    }
  }

  @Test
  public void testCF() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/twoGridMaps.nc";
    System.out.printf("testCF %s%n", filename);
    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid("altitude").orElseThrow();
      GridHorizCoordinateSystem hcs = grid.getHorizCoordinateSystem();
      assertThat(hcs.getProjection()).isInstanceOf(Stereographic.class);
    }
  }

  // double time(time=3989);
  // :units = "hours since 1-1-1 00:00:0.0"; // string

  @Test
  public void testCOARDSCalendarInVer7() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "conventions/coards/olr.day.mean.nc";
    System.out.printf("testCOARDSCalendarInVer7 %s%n", filename);
    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid("olr").orElseThrow();
      GridTimeCoordinateSystem tcs = grid.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();
      List<CalendarDate> times = tcs.getTimesForRuntime(0);
      CalendarDate cd =
          CalendarDate.fromUdunitIsoDate(Calendar.gregorian.toString(), "2002-01-03T00:00:00Z").orElseThrow();
      assertThat(times.get(0)).isEqualTo(cd);
      CalendarDate last = times.get(times.size() - 1);
      CalendarDate cd2 =
          CalendarDate.fromUdunitIsoDate(Calendar.gregorian.toString(), "2012-12-04T00:00:00Z").orElseThrow();
      assertThat(last).isEqualTo(cd2);
    }
  }

  @Test
  public void testAWIPSsatLatlon() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "conventions/awips/20150602_0830_sport_imerg_noHemis_rr.nc";
    System.out.printf("testAWIPSsatLatlon %s%n", filename);
    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid("image").orElseThrow();
      assertThat(grid.getArrayType()).isEqualTo(ArrayType.BYTE);
      GridHorizCoordinateSystem hcs = grid.getHorizCoordinateSystem();
      assertThat(hcs.isLatLon()).isTrue();
    }
  }

  @Test
  public void testIfps() throws IOException {
    String problem = TestDir.cdmUnitTestDir + "conventions/ifps/HUNGrids.netcdf";
    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(problem, errlog)) {
      assertThat(gds).isNotNull();
      Grid coverage = gds.findGrid("T_SFC").orElseThrow();
      GridHorizCoordinateSystem hcs = coverage.getHorizCoordinateSystem();
      assertThat(hcs.getProjection()).isNotNull();
      assertThat(coverage.getArrayType()).isEqualTo(ArrayType.FLOAT);
    }
  }

}
