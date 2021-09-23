/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test Grid Classifications.
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGridClassificationP {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    // NUWG - has CoordinateAlias
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/03061219_ruc.nc", "T", FeatureType.GRID, 4, 31,
        "GRID(T,Z,Y,X)"});
    // scalar runtime, with ensemble
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/ECME_RIZ_201201101200_00600_GB",
        "Total_precipitation_surface", FeatureType.GRID, 4, 5, "GRID(R,TO,E,Y,X)"});
    /* ensemble, time-offset */
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/MM_cnrm_129_red.ncml", "geopotential",
        FeatureType.GRID, 6, 1, "GRID(R,TO,E,Z,Y,X)"});
    // scalar vert
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/ukmo.nc", "temperature_2m", FeatureType.GRID, 4, 1,
        "GRID(R,TO,Y,X): Z"});
    // ok
    result
        .add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/tp/GFS_Global_onedeg_ana_20150326_0600.grib2.ncx4",
            "Temperature_isobaric", FeatureType.GRID, 4, 65, "GRID(R,TO,Z,Y,X)"});
    // both x,y and lat,lon
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/testCFwriter.nc", "Temperature", FeatureType.GRID, 3,
        2, "GRID(Z,Y,X)"});

    // GRIB Curvilinear
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/fmrc/rtofs/ofs.20091122/ofs_atl.t00z.F024.grb.grib2",
        "3-D_Temperature_depth_below_sea", FeatureType.CURVILINEAR, 4, 7, "CURVILINEAR(R,TO,Z,Y,X)"});

    // x,y axis but no projection
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/coverage/Run_20091025_0000.nc", "u", FeatureType.CURVILINEAR,
        4, 24, "CURVILINEAR(T,Z,Y,X)"});

    /* netcdf Curvilinear, not 1D */
    result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/cf/mississippi.nc", "temp", FeatureType.CURVILINEAR,
        4, 24, "CURVILINEAR(T,Z,Y,X)"});

    // SWATH
    result.add(new Object[] {
        TestDir.cdmUnitTestDir + "formats/hdf4/AIRS.2003.01.24.116.L2.RetStd_H.v5.0.14.0.G07295101113.hdf", "",
        FeatureType.SWATH, 2, 93, "fn"});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "formats/hdf4/ssec/MYD06_L2.A2006188.1655.005.2006194124315.hdf",
        "", FeatureType.SWATH, 3, 28, "fn"});

    return result;
  }

  final String endpoint;
  final String vname;
  final FeatureType expectType;
  final int domain, ncoverages;
  final String fn;

  public TestGridClassificationP(String endpoint, String vname, FeatureType expectType, int domain, int ncoverages,
      String fn) {
    this.endpoint = endpoint;
    this.vname = vname;
    this.expectType = expectType;
    this.domain = domain;
    this.ncoverages = ncoverages;
    this.fn = fn;
  }

  @Test
  public void testFactory() throws IOException {
    System.out.printf("Open %s %s%n", endpoint, vname);
    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(endpoint, errlog)) {
      if (expectType == FeatureType.SWATH) {
        assertThat(gds).isNull();
        return;
      } else {
        assertThat(gds).isNotNull();
      }

      Grid grid = gds.findGrid(vname).orElseThrow();
      GridCoordinateSystem csys = grid.getCoordinateSystem();
      assertThat(csys.getFeatureType()).isEqualTo(expectType);
      assertThat(csys.showFnSummary()).isEqualTo(fn);

      assertThat(gds.getGrids()).hasSize(ncoverages);
    }
  }

}
