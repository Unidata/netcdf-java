/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.Attribute;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

@Category(NeedsCdmUnitTest.class)
public class TestGribGridBuilding {

  @Test
  public void testScalarRuntimeCoordinate() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1.ncx4";
    String gridName = "Pressure_surface";
    System.out.printf("testScalarRuntimeCoordinate %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid coverage = gds.findGrid(gridName).orElseThrow();
      assertThat(coverage).isNotNull();

      GridCoordinateSystem csys = coverage.getCoordinateSystem();
      assertThat(csys).isNotNull();
      GridTimeCoordinateSystem tcs = csys.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();

      GridAxis<?> runtime = csys.findCoordAxisByType(AxisType.RunTime);
      assertThat((Object) runtime).isNotNull();
      assertThat(runtime.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
      assertThat(runtime.getDependenceType()).isEqualTo(GridAxisDependenceType.independent); // could be scalar
      CalendarDate startDate = tcs.getRuntimeDate(0);
      assertThat(startDate).isEqualTo(CalendarDate.fromUdunitIsoDate(null, "2012-02-27T00:00:00Z").orElseThrow());
    }
  }

  @Test
  public void test2DTimeCoordinates() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km.ncx4";
    String gridName = "Pressure_surface";
    System.out.printf("test2DTimeCoordinates %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid coverage = gds.findGrid(gridName).orElseThrow();
      assertThat(coverage).isNotNull();

      GridCoordinateSystem csys = coverage.getCoordinateSystem();
      assertThat(csys).isNotNull();
      GridTimeCoordinateSystem tcs = csys.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();

      GridAxis<?> runtime = csys.findCoordAxisByType(AxisType.RunTime);
      assertThat((Object) runtime).isNotNull();
      assertThat(runtime.getSpacing()).isEqualTo(GridAxisSpacing.irregularPoint);
      assertThat(runtime.getDependenceType()).isEqualTo(GridAxisDependenceType.independent);
      CalendarDate startDate = tcs.getRuntimeDate(0);
      assertThat(startDate).isEqualTo(CalendarDate.fromUdunitIsoDate(null, "2012-02-27T00:00:00Z").orElseThrow());
      assertThat(runtime.getResolution()).isEqualTo(24000);

      GridAxis<?> time = csys.findCoordAxisByType(AxisType.TimeOffset);
      assertThat((Object) time).isNotNull();
      assertThat(time.getSpacing()).isEqualTo(GridAxisSpacing.irregularPoint);
      assertThat(time.getDependenceType()).isEqualTo(GridAxisDependenceType.independent); // could be scalar
      CalendarDate startDate1 = tcs.getTimesForRuntime(0).get(0);
      assertThat(startDate1).isEqualTo(CalendarDate.fromUdunitIsoDate(null, "2012-02-27T00:00:00Z").orElseThrow());
      // assertThat(time.getResolution()).isEqualTo(6.0); // mode vs mean
      assertThat(time.getResolution()).isWithin(1.0e-8).of(6.857142857142857);

      assertThat(tcs.type).isEqualTo(GridTimeCoordinateSystem.Type.Offset);
    }
  }

  @Test
  public void testTimeOffsetSubsetWhenTimePresent() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ncss/GFS/CONUS_80km/GFS_CONUS_80km_20120227_0000.grib1";
    String gridName = "Temperature_isobaric";
    System.out.printf("testTimeOffsetSubsetWhenTimePresent %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid coverage = gds.findGrid(gridName).orElseThrow();
      assertThat(coverage).isNotNull();

      GridCoordinateSystem csys = coverage.getCoordinateSystem();
      assertThat(csys).isNotNull();
      GridTimeCoordinateSystem tcs = csys.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();

      GridAxis<?> runtime = csys.findCoordAxisByType(AxisType.RunTime);
      assertThat((Object) runtime).isNotNull();
      assertThat(runtime.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
      assertThat(runtime.getDependenceType()).isEqualTo(GridAxisDependenceType.independent); // could be scalar
      CalendarDate startDate = tcs.getRuntimeDate(0);
      assertThat(startDate).isEqualTo(CalendarDate.fromUdunitIsoDate(null, "2012-02-27T00:00:00Z").orElseThrow());

      GridAxis<?> time = csys.findCoordAxisByType(AxisType.TimeOffset);
      assertThat((Object) time).isNotNull();
      assertThat(time.getSpacing()).isEqualTo(GridAxisSpacing.irregularPoint);
      assertThat(time.getDependenceType()).isEqualTo(GridAxisDependenceType.independent);
      CalendarDate startDate2 = tcs.getRuntimeDate(0);
      assertThat(startDate2).isEqualTo(CalendarDate.fromUdunitIsoDate(null, "2012-02-27T00:00:00Z").orElseThrow());
      assertThat(time.getResolution()).isWithin(1.0e-8).of(6.857142857142857);
    }
  }

  @Test
  public void testGaussianLats() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/grib1/cfs.wmo";
    String gridName = "Albedo_surface_1_Month_Average";
    System.out.printf("testGaussianLats %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid coverage = gds.findGrid(gridName).orElseThrow();
      assertThat(coverage).isNotNull();

      GridCoordinateSystem csys = coverage.getCoordinateSystem();
      assertThat(csys).isNotNull();
      GridTimeCoordinateSystem tcs = csys.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();

      GridAxis<?> latAxis = csys.findCoordAxisByType(AxisType.Lat);
      assertThat((Object) latAxis).isNotNull();
      assertThat(latAxis.getSpacing()).isEqualTo(GridAxisSpacing.irregularPoint);
      assertThat(latAxis.getDependenceType()).isEqualTo(GridAxisDependenceType.independent);

      Attribute att = latAxis.attributes().findAttribute(CDM.GAUSSIAN);
      assertThat(att).isNotNull();
      assertThat(att.getStringValue()).isEqualTo("true");
    }
  }

}
