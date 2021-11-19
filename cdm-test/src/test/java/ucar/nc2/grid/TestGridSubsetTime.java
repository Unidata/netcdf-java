/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.primitives.Ints;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.grib.collection.GribArrayReader;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test CoverageSubsetTime, esp 2DTime
 */
@Category(NeedsCdmUnitTest.class)
public class TestGridSubsetTime {

  @BeforeClass
  public static void before() {
    GribArrayReader.validator = new GribCoverageValidator();
  }

  @AfterClass
  public static void after() {
    GribArrayReader.validator = null;
  }

  @Test // there is no interval with offset value = 51
  public void testNoIntervalFound() throws IOException, InvalidRangeException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    String covName = "Momentum_flux_u-component_surface_Mixed_intervals_Average";

    System.out.format("testNoIntervalFound Dataset %s coverage %s%n", endpoint, covName);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(endpoint, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(covName).orElseThrow();
      GridCoordinateSystem csys = grid.getCoordinateSystem();
      assertThat(csys).isNotNull();

      GridSubset params = GridSubset.create();
      CalendarDate runtime = CalendarDate.fromUdunitIsoDate(null, "2015-03-01T12:00:00Z").orElseThrow();
      params.setRunTimeCoord(runtime);
      Long offsetVal = 51L; // should fail
      params.setTimeOffsetCoord(offsetVal);
      System.out.printf("  subset %s%n", params);

      GridReferencedArray geo = grid.readData(params);
      testGeoArray(geo, runtime, null, offsetVal);

      // should be empty, but instead its a bunch of NaNs
      Array<Number> datan = geo.data();
      assertThat(Float.isNaN(datan.iterator().next().floatValue())).isTrue();
    }
  }

  private static void testGeoArray(GridReferencedArray geo, CalendarDate runtime, CalendarDate time, Long offsetVal) {
    MaterializedCoordinateSystem geoCs = geo.getMaterializedCoordinateSystem();

    GridTimeCoordinateSystem tcs = geoCs.getTimeCoordSystem();
    assertThat(tcs).isNotNull();
    GridAxisPoint runtimeAxis = tcs.getRunTimeAxis();
    assertThat((Object) runtimeAxis).isNotNull();

    assertThat(runtimeAxis.getNominalSize()).isEqualTo(1);
    if (runtime != null) {
      assertThat(runtime).isEqualTo(tcs.getRuntimeDate(0));
    }

    GridAxis<?> timeAxis = tcs.getTimeOffsetAxis(0);
    assertThat((Object) timeAxis).isNotNull();
    assertThat(timeAxis.getNominalSize()).isEqualTo(1);

    if (offsetVal != null) {
      time = tcs.getRuntimeDateUnit().makeCalendarDate(offsetVal);
    }

    List<Integer> shapeCs = geoCs.getMaterializedShape();
    int[] dataShape = geo.data().getShape();
    assertThat(shapeCs).isEqualTo(Ints.asList(dataShape));
  }

}
