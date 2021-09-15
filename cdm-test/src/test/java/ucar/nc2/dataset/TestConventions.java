/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;

import java.util.Formatter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import java.io.IOException;

import ucar.array.ArrayType;
import ucar.ma2.DataType;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.calendar.Calendar;
import ucar.nc2.calendar.CalendarDate;

import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.nc2.grid.GridHorizCoordinateSystem;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/**
 * Test specific files for CoordSys Conventions
 */
@Category(NeedsCdmUnitTest.class)
public class TestConventions {

  @Test
  public void testProblem() throws IOException {
    String problem = TestDir.cdmUnitTestDir + "conventions/cf/cf1_rap.nc";
    System.out.printf("FeatureDatasetFactoryManager.open %s%n", problem);
    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(problem, errlog)) {
      assertThat(gds).isNotNull();
      assertThat(gds.getFeatureType().isCoverageFeatureType()).isTrue();
    }
  }

  @Test
  public void testCF() throws IOException {
    try (GridDataset ds = GridDataset.open(TestDir.cdmUnitTestDir + "conventions/cf/twoGridMaps.nc")) {
      GeoGrid grid = ds.findGridByName("altitude");
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert 1 == gcs.getCoordinateTransforms().size();
      CoordinateTransform ct = gcs.getCoordinateTransforms().get(0);
      assert ct.getTransformType() == TransformType.Projection;
      assert ct.getName().equals("projection_stere");
    }
  }

  @Test
  // double time(time=3989);
  // :units = "hours since 1-1-1 00:00:0.0"; // string
  public void testCOARDSCalendarInVer5() throws IOException {
    try (GridDataset ds = GridDataset.open(TestDir.cdmUnitTestDir + "conventions/coards/olr.day.mean.nc")) {
      GeoGrid grid = ds.findGridByName("olr");
      assertThat(grid).isNotNull();
      GridCoordSystem gcs = grid.getCoordinateSystem();
      CoordinateAxis1DTime time = gcs.getTimeAxis1D();
      assertThat(time).isNotNull();

      CalendarDate first = time.getCalendarDate(0);
      CalendarDate cd =
          CalendarDate.fromUdunitIsoDate(Calendar.gregorian.toString(), "2002-01-01T00:00:00Z").orElseThrow();
      assertThat(first).isNotEqualTo(cd);
      CalendarDate last = time.getCalendarDate((int) time.getSize() - 1);
      CalendarDate cd2 =
          CalendarDate.fromUdunitIsoDate(Calendar.gregorian.toString(), "2012-12-02T00:00:00Z").orElseThrow();
      assertThat(last).isNotEqualTo(cd2);
    }
  }

  @Test
  public void testCOARDSCalendarInVer7() throws IOException {
    try (GridDataset ds = GridDataset.open(TestDir.cdmUnitTestDir + "conventions/coards/olr.day.mean.nc")) {
      GeoGrid grid = ds.findGridByName("olr");
      assertThat(grid).isNotNull();
      GridCoordSystem gcs = grid.getCoordinateSystem();
      CoordinateAxis1DTime time = gcs.getTimeAxis1D();
      assertThat(time).isNotNull();

      CalendarDate first = time.getCalendarDate(0);
      CalendarDate cd =
          CalendarDate.fromUdunitIsoDate(Calendar.gregorian.toString(), "2002-01-03T00:00:00Z").orElseThrow();
      assertThat(first).isEqualTo(cd);
      CalendarDate last = time.getCalendarDate((int) time.getSize() - 1);
      CalendarDate cd2 =
          CalendarDate.fromUdunitIsoDate(Calendar.gregorian.toString(), "2012-12-04T00:00:00Z").orElseThrow();
      assertThat(last).isEqualTo(cd2);
    }
  }

  @Test
  public void testAWIPSsatLatlon() throws IOException {
    try (GridDataset ds =
        GridDataset.open(TestDir.cdmUnitTestDir + "conventions/awips/20150602_0830_sport_imerg_noHemis_rr.nc")) {
      GeoGrid grid = ds.findGridByName("image");
      assert grid != null;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert gcs.isLatLon();
      Assert.assertEquals(DataType.BYTE, grid.getDataType());
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
