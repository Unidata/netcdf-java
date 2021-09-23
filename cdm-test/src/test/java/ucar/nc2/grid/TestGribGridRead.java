/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Arrays;
import ucar.nc2.calendar.CalendarDate;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test Grib Collection reading Grid.
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribGridRead {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final float TOL = 1.0e-5f;

  CalendarDate useDate = CalendarDate.fromUdunitIsoDate(null, "2014-10-27T06:00:00Z").orElseThrow();

  @Test
  public void TestTwoDRead() throws IOException, ucar.array.InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/gfsConus80_file.ncx4";
    String gridName = "Temperature_isobaric";
    System.out.printf("TestTwoDRead %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid coverage = gds.findGrid(gridName).orElseThrow();
      assertThat(coverage).isNotNull();

      GridCoordinateSystem csys = coverage.getCoordinateSystem();
      assertThat(csys).isNotNull();
      assertThat(csys.getNominalShape()).isEqualTo(ImmutableList.of(6, 36, 29, 65, 93));
      GridTimeCoordinateSystem tcs = csys.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();

      GridReferencedArray geoArray =
          coverage.getReader().setVertCoord(300.0).setRunTimeLatest().setDate(useDate).read();
      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();

      System.out.printf(" data shape=%s%n", java.util.Arrays.toString(geoArray.data().getShape()));
      assertThat(geoArray.data().getShape()).isEqualTo(new int[] {1, 1, 1, 65, 93});

      GridHorizCoordinateSystem hcs2 = mcs.getHorizCoordinateSystem();
      assertThat(hcs2).isNotNull();
      System.out.printf(" data hcs shape=%s%n", hcs2.getShape());
      assertThat(hcs2.getShape()).isEqualTo(ImmutableList.of(65, 93));
      ucar.array.Array<Number> data = Arrays.reduceFirst(geoArray.data(), 3);
      float first = data.get(0, 0).floatValue();
      float last = data.get(64, 92).floatValue();
      assertThat(first).isWithin(TOL).of(241.699997f);
      assertThat(last).isWithin(TOL).of(225.099991f);
    }
  }

  @Test
  public void TestSRCRead() throws IOException, ucar.array.InvalidRangeException {
    String filename =
        TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/20141025/GFS_CONUS_80km_20141025_0000.grib1.ncx4";
    String gridName = "Temperature_isobaric";
    System.out.printf("TestSRCRead %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid coverage = gds.findGrid(gridName).orElseThrow();
      assertThat(coverage).isNotNull();

      GridCoordinateSystem csys = coverage.getCoordinateSystem();
      assertThat(csys).isNotNull();
      assertThat(csys.getNominalShape()).isEqualTo(ImmutableList.of(1, 36, 29, 65, 93));
      GridTimeCoordinateSystem tcs = csys.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();

      GridReferencedArray geoArray = coverage.getReader().setVertCoord(200.0).setTimeOffsetCoord(42.0).read();
      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();

      System.out.printf(" data shape=%s%n", java.util.Arrays.toString(geoArray.data().getShape()));
      assertThat(geoArray.data().getShape()).isEqualTo(new int[] {1, 1, 1, 65, 93});

      GridHorizCoordinateSystem hcs2 = mcs.getHorizCoordinateSystem();
      assertThat(hcs2).isNotNull();
      System.out.printf(" data hcs shape=%s%n", hcs2.getShape());
      assertThat(hcs2.getShape()).isEqualTo(ImmutableList.of(65, 93));
      ucar.array.Array<Number> data = Arrays.reduceFirst(geoArray.data(), 3);
      float first = data.get(0, 0).floatValue();
      float last = data.get(64, 92).floatValue();
      assertThat(first).isWithin(TOL).of(219.5f);
      assertThat(last).isWithin(TOL).of(218.6f);
    }
  }

  @Test
  public void TestMRUTCRead() throws IOException, ucar.array.InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/anal/HRRRanalysis.ncx4";
    String gridName = "Temperature_isobaric";
    System.out.printf("TestSRCRead %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid coverage = gds.findGrid(gridName).orElseThrow();
      assertThat(coverage).isNotNull();

      GridCoordinateSystem csys = coverage.getCoordinateSystem();
      assertThat(csys).isNotNull();
      assertThat(csys.getNominalShape()).isEqualTo(ImmutableList.of(4, 5, 1377, 2145));
      GridTimeCoordinateSystem tcs = csys.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();

      GridReferencedArray geoArray = coverage.getReader().setVertCoord(70000).setTimeOffsetCoord(2).read();
      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();

      System.out.printf(" data shape=%s%n", java.util.Arrays.toString(geoArray.data().getShape()));
      assertThat(geoArray.data().getShape()).isEqualTo(new int[] {1, 1, 1377, 2145});
      GridHorizCoordinateSystem hcs2 = mcs.getHorizCoordinateSystem();
      assertThat(hcs2).isNotNull();
      System.out.printf(" data hcs shape=%s%n", hcs2.getShape());
      assertThat(hcs2.getShape()).isEqualTo(ImmutableList.of(1377, 2145));
      ucar.array.Array<Number> data = Arrays.reduceFirst(geoArray.data(), 3);

      float val = data.get(547, 947).floatValue();
      assertThat(val).isWithin(TOL).of(283.1135f);
    }
  }

  @Test
  public void TestMRUTPRead() throws IOException, ucar.array.InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx4";
    String gridName = "Relative_humidity_sigma";
    System.out.printf("TestSRCRead %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid coverage = gds.findGrid(gridName).orElseThrow();
      assertThat(coverage).isNotNull();

      GridCoordinateSystem csys = coverage.getCoordinateSystem();
      assertThat(csys).isNotNull();
      assertThat(csys.getNominalShape()).isEqualTo(ImmutableList.of(2, 1, 181, 360));
      GridTimeCoordinateSystem tcs = csys.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();

      GridReferencedArray geoArray = coverage.getReader().setVertCoord(70000).setTimeOffsetCoord(6).read();
      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();

      System.out.printf(" data shape=%s%n", java.util.Arrays.toString(geoArray.data().getShape()));
      assertThat(geoArray.data().getShape()).isEqualTo(new int[] {1, 1, 181, 360});
      GridHorizCoordinateSystem hcs2 = mcs.getHorizCoordinateSystem();
      assertThat(hcs2).isNotNull();
      System.out.printf(" data hcs shape=%s%n", hcs2.getShape());
      assertThat(hcs2.getShape()).isEqualTo(ImmutableList.of(181, 360));
      ucar.array.Array<Number> data = Arrays.reduceFirst(geoArray.data(), 2);

      float val = data.get(48, 128).floatValue();
      assertThat(val).isWithin(TOL).of(94.0f);
    }
  }

  @Test
  public void TestPofPRead() throws IOException, ucar.array.InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/gfsConus80_file.ncx4";
    String gridName = "Vertical_velocity_pressure_isobaric";
    System.out.printf("TestSRCRead %s%n", filename);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid coverage = gds.findGrid(gridName).orElseThrow();
      assertThat(coverage).isNotNull();

      GridCoordinateSystem csys = coverage.getCoordinateSystem();
      assertThat(csys).isNotNull();
      assertThat(csys.getNominalShape()).isEqualTo(ImmutableList.of(6, 35, 9, 65, 93));
      GridTimeCoordinateSystem tcs = csys.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();

      GridReferencedArray geoArray =
          coverage.getReader().setRunTime(CalendarDate.fromUdunitIsoDate(null, "2014-10-24T12:00:00Z").orElseThrow())
              .setTimeOffsetCoord(42).setVertCoord(500).read();
      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();
      assertThat(mcs).isNotNull();

      System.out.printf(" data shape=%s%n", java.util.Arrays.toString(geoArray.data().getShape()));
      assertThat(geoArray.data().getShape()).isEqualTo(new int[] {1, 1, 1, 65, 93});
      GridHorizCoordinateSystem hcs2 = mcs.getHorizCoordinateSystem();
      assertThat(hcs2).isNotNull();
      System.out.printf(" data hcs shape=%s%n", hcs2.getShape());
      assertThat(hcs2.getShape()).isEqualTo(ImmutableList.of(65, 93));
      ucar.array.Array<Number> data = Arrays.reduceFirst(geoArray.data(), 3);

      float val = data.get(46, 25).floatValue();
      assertThat(val).isWithin(TOL).of(-2.493f);
    }
  }

}
