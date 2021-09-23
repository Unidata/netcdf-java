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
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.CoordinateAxis2D;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.grib.collection.GribDataReader;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Read specific Grid fields and compare results with dt.GridDataset.
 * Generally the ones that are failing in TestGridReadingAgainstDt
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGridCompareSlice {

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

    result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4", FeatureType.GRID,
        "5-Wave_Geopotential_Height_isobaric", "2015-03-01T00:00:00Z", null, "2015-03-01T00:00:00Z ", 5000.0, null});

    result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4", FeatureType.GRID,
        "5-Wave_Geopotential_Height_isobaric", "2015-03-01T00:00:00Z", null, "2015-03-01T03:00:00Z ", null, null});

    /*
     * dt fails here because offset axis units (hour) different from runtime (secs)
     * result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4", FeatureType.GRID,
     * "Total_ozone_entire_atmosphere_single_layer", "2015-03-01T00:00:00Z", null, "2015-03-01T06:00:00Z ", null,
     * null});
     */

    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", FeatureType.GRID, "RH_lpdg", null,
        null, "2003-06-12T19:00:00Z", 150.0, null}); // NUWG - has CoordinateAlias

    // SRC TP
    result.add(new Object[] {
        TestDir.cdmUnitTestDir + "gribCollections/tp/GFS_Global_onedeg_ana_20150326_0600.grib2.ncx4", FeatureType.GRID,
        "Relative_humidity_sigma_layer", "2015-03-26T06:00:00Z", null, "2015-03-26T06:00:00Z", 0.580000, null});

    result.add(new Object[] {
        TestDir.cdmUnitTestDir + "gribCollections/tp/GFS_Global_onedeg_ana_20150326_0600.grib2.ncx4", FeatureType.GRID,
        "Relative_humidity_sigma_layer", "2015-03-26T06:00:00Z", null, "2015-03-26T06:00:00Z", 0.665000, null});

    result.add(new Object[] {
        TestDir.cdmUnitTestDir + "gribCollections/tp/GFS_Global_onedeg_ana_20150326_0600.grib2.ncx4", FeatureType.GRID,
        "Absolute_vorticity_isobaric", "2015-03-26T06:00:00Z", null, "2015-03-26T06:00:00Z", 100000.0, null}); // */

    // problem here is fractional units: makeOffsetFromRefDate() rounds off
    // days since 2009-06-14 04:00:00; 132.00346, 132.0104, 132.01736...
    // dt doesnt work correctly
    // result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/Run_20091025_0000.nc", FeatureType.CURVILINEAR,
    // "elev", null, null, "2009-10-24T04:14:59.121Z", null, null}); // x,y axis but no projection */

    return result;
  }

  String endpoint;
  FeatureType type;
  String dtName, gridName;
  CalendarDate rt_val;
  Double ens_val;
  CalendarDate time_val;
  Double vert_val;
  Integer time_idx;

  public TestGridCompareSlice(String endpoint, FeatureType type, String covName, String rt_val, Double ens_val,
      String time_val, Double vert_val, Integer time_idx) {
    this.endpoint = endpoint;
    this.type = type;
    this.gridName = covName;
    this.dtName = (type == FeatureType.FMRC) ? "TwoD/" + covName : "Best/" + covName;
    this.rt_val = rt_val == null ? null : CalendarDate.fromUdunitIsoDate(null, rt_val).orElseThrow();
    this.ens_val = ens_val;
    this.time_val = time_val == null ? null : CalendarDate.fromUdunitIsoDate(null, time_val).orElseThrow();
    this.vert_val = vert_val;
    this.time_idx = time_idx;
  }

  @Test
  public void testReadGridSlice() throws IOException { // read single slice
    System.out.printf("Test Dataset %s coverage %s%n", endpoint, gridName);

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(endpoint, errlog)) {
      assertThat(gds).isNotNull();
      Grid cover = gds.findGrid(gridName).orElseThrow();
      assertThat(cover).isNotNull();

      // check DtCoverageCS
      try (GridDataset ds = GridDataset.open(endpoint)) {
        GridDatatype dt = ds.findGridByName(dtName);
        if (dt == null) {
          dt = ds.findGridByName(gridName);
        }
        assertThat(dt).isNotNull();

        GridCoordSystem csys = dt.getCoordinateSystem();
        CoordinateAxis1DTime rtAxis = csys.getRunTimeAxis();
        CoordinateAxis1D ensAxis = csys.getEnsembleAxis();
        CoordinateAxis1DTime timeAxis = csys.getTimeAxis1D();
        CoordinateAxis1D vertAxis = csys.getVerticalAxis();

        int calcTimeIdx = -1;
        int rt_idx = (rtAxis == null || rt_val == null) ? -1 : rtAxis.findTimeIndexFromCalendarDate(rt_val);
        if (time_idx == null) {
          if (time_val != null) {
            if (timeAxis != null) {
              calcTimeIdx = timeAxis.findTimeIndexFromCalendarDate(time_val);
            } else if (rt_idx >= 0) {
              CoordinateAxis2D timeAxis2D = (CoordinateAxis2D) csys.getTimeAxis();
              calcTimeIdx = timeAxis2D.findTimeIndexFromCalendarDate(rt_idx, time_val);
              // timeAxis = csys.getTimeAxisForRun(rt_idx); // LOOK doesnt work for interval coords
              // if (timeAxis != null)
              // calcTimeIdx = timeAxis.findTimeIndexFromCalendarDate(time_val); // LOOK theres a bug here, set time_idx
              // as workaround
            }
          }
        } else {
          calcTimeIdx = time_idx;
        }

        int ens_idx = (ensAxis == null || ens_val == null) ? -1 : ensAxis.findCoordElement(ens_val);
        int vert_idx = (vertAxis == null || vert_val == null) ? -1 : vertAxis.findCoordElement(vert_val);

        TestGridCompareWithDt.readOneSlice(cover, dt, rt_val, rt_idx, time_val, calcTimeIdx,
            ens_val == null ? 0 : ens_val, ens_idx, vert_val == null ? 0 : vert_val, vert_idx, true);
      }
    }
  }

}
