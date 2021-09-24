/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Count geogrid objects - sanity check when anything changes. */

@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestReadandCount {

  private static final boolean showCount = true;
  private static final String griddir = TestDir.cdmUnitTestDir + "conventions/";
  private static final String grib1dir = TestDir.cdmUnitTestDir + "formats/grib1/";
  private static final String grib2dir = TestDir.cdmUnitTestDir + "formats/grib2/";

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[] {griddir + "avhrr/", "amsr-avhrr-v2.20040729.nc", 4, 1, 3, 0});

    /*
     * doOne(griddir+"atd-radar/","rgg.20020411.000000.lel.ll.nc", 5, 1, 4, 1);
     * doOne(griddir+"atd-radar/","SPOL_3Volumes.nc", 3, 1, 4, 1);
     * 
     * doOne(griddir+"gief/","coamps.wind_uv.nc", 2, 1, 4, 1);
     * 
     * //// coards derived
     * doOne(griddir+"coards/", "cldc.mean.nc", 1, 1, 3, 0);
     * doOne(griddir+"coards/","inittest24.QRIDV07200.nc", -1, -1, -1, -1); // no "positive" att on level
     */
    result.add(new Object[] {griddir + "coards/", "inittest24.QRIDV07200.ncml", 1, 1, 3, 1});
    // result.add(new Object[]{griddir+"avhrr/","amsr-avhrr-v2.20040729.nc", 4, 1, 4, 1});

    // result.add(new Object[]{griddir+"csm/","o3monthly.nc", 4, 1, 7, 2});
    result.add(new Object[] {griddir + "csm/", "ha0001.nc", 35, 3, 5, 2}); //

    result.add(new Object[] {griddir + "cf/", "cf1.nc", 1, 1, 3, 0});
    result.add(new Object[] {griddir + "cf/", "ccsm2.nc", 107, 3, 5, 2}); //
    result.add(new Object[] {griddir + "cf/", "tomw.nc", 19, 3, 2, 1});
    result.add(new Object[] {griddir + "cf/", "cf1_rap.nc", 11, 1, 0, 0}); // not getting x, y
    // result.add(new Object[]{"C:/data/conventions/cf/signell/","signell_july2_03.nc", -1, -1, -1, -1}); // 2D lat,
    // lon; no x,y
    // ** result.add(new Object[]{griddir+"cf/","feb2003_short.nc", 14, 4, 4, 1});
    result.add(new Object[] {griddir + "cf/", "feb2003_short2.nc", 22, 9, 2, 1});
    result.add(new Object[] {griddir + "cf/", "temperature.nc", 2, 3, 3, 1});

    result.add(new Object[] {griddir + "gdv/", "testGDV.nc", 30, 1, 3, 0});
    result.add(new Object[] {griddir + "gdv/", "OceanDJF.nc", 15, 1, 4, 1});

    // uses GDV as default
    result.add(new Object[] {griddir + "mars/", "temp_air_01082000.ncml", 1, 1, 4, 1}); // uses GDV
    // result.add(new Object[]{"C:/data/conventions/mm5/","n040.nc", -1, -1, -1, -1}); // no Conventions code

    result.add(new Object[] {griddir + "m3io/", "agg.cctmJ3fx.b312.nh3c1.dep_wa.annual.2001base.nc", 13, 1, 4, 1}); // m3io
    result.add(new Object[] {griddir + "m3io/", "19L.nc", 23, 1, 4, 1}); // M3IOVGGrid

    //// the uglies
    result.add(new Object[] {griddir + "nuwg/", "avn-x.nc", 31, 4, 6, 3});
    // result.add(new Object[] {griddir + "nuwg/", "2003021212_avn-x.nc", 30, 5, 7, 4}); // time not monotonic
    result.add(new Object[] {griddir + "nuwg/", "avn-q.nc", 22, 7, 9, 6});
    result.add(new Object[] {griddir + "nuwg/", "eta.nc", 28, 9, 10, 7});
    result.add(new Object[] {griddir + "nuwg/", "ocean.nc", 5, 1, 3, 0});
    result.add(new Object[] {griddir + "nuwg/", "ruc.nc", 31, 5, 6, 3});
    result.add(new Object[] {griddir + "nuwg/", "CMC-HGT.nc", 1, 1, 3, 0}); // */

    result.add(new Object[] {griddir + "wrf/", "wrfout_v2_Lambert.nc", 57, 8, 8, 3});
    result.add(new Object[] {griddir + "wrf/", "wrf2-2005-02-01_12.nc", 60, 8, 8, 3});
    result.add(new Object[] {griddir + "wrf/", "wrfout_d01_2006-03-08_21-00-00", 70, 8, 8, 3});
    result.add(new Object[] {griddir + "wrf/", "wrfrst_d01_2002-07-02_12_00_00.nc", 162, 8, 8, 3});

    result.add(new Object[] {griddir + "awips/", "19981109_1200.nc", 43, 13, 14, 11});
    result.add(new Object[] {griddir + "awips/", "20150602_0830_sport_imerg_noHemis_rr.nc", 1, 1, 2, 0});

    result.add(new Object[] {griddir + "ifps/", "HUNGrids.netcdf", 26, 26, 27, 0}); // *

    // our grib reader */
    result.add(new Object[] {grib1dir, "AVN.wmo", 22, -1, -1, -1});
    result.add(new Object[] {grib1dir, "RUC_W.wmo", 44, -1, -1, -1});
    result.add(new Object[] {grib1dir, "NOGAPS-Temp-Regional.grib", 1, -1, -1, -1}); // */

    result.add(new Object[] {grib1dir, "eta.Y.Q.wmo", 25, -1, -1, -1});
    result.add(new Object[] {grib2dir, "ndfd.wmo", 1, -1, -1, -1});

    // radar mosaic
    result.add(new Object[] {grib1dir, "radar_national.grib", 1, 1, 4, 0});
    result.add(new Object[] {grib1dir, "radar_regional.grib", 1, 1, 4, 0});

    // staggered grid
    result.add(new Object[] {TestDir.cdmUnitTestDir, "ft/grid/stag/bora_feb.nc", 19, 11, 3, 2});

    return result;
  }

  String dir, name;
  int ngrids, ncoordSys, ncoordAxes, nVertCooordAxes;

  public TestReadandCount(String dir, String name, int ngrids, int ncoordSys, int ncoordAxes, int nVertCooordAxes) {
    this.dir = dir;
    this.name = name;
    this.ngrids = ngrids;
    this.ncoordSys = ncoordSys;
    this.ncoordAxes = ncoordAxes;
    this.nVertCooordAxes = nVertCooordAxes;
  }

  @org.junit.Test
  public void openAsGridAndCount() throws Exception {
    doOne(dir + name, ngrids, ncoordSys, ncoordAxes, nVertCooordAxes);
  }

  static public void doOne(String filename, int ngrids, int ncoordSys, int ncoordAxes, int nVertCooordAxes)
      throws Exception {
    System.out.printf("test read GridDataset= %s%n", filename);

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();

      int countGrids = gds.getGrids().size();
      int countCoordAxes = gds.getGridAxes().size();
      int countCoordSys = gds.getGridCoordinateSystems().size();

      // count vertical axes
      int countVertCooordAxes = 0;
      for (GridAxis<?> axis : gds.getGridAxes()) {
        if (axis.getAxisType().isVert())
          countVertCooordAxes++;
      }

      if (showCount) {
        System.out.printf(" grids   = %d (%d)%n", countGrids, ngrids);
        System.out.printf(" coordSys= %d (%d)%n", countCoordSys, ncoordSys);
        System.out.printf(" coordAxs= %d (%d)%n", countCoordAxes, ncoordAxes);
        System.out.printf(" vertAxes= %d (%d)%n", countVertCooordAxes, nVertCooordAxes);
      }

      if (ngrids >= 0) {
        assertThat(countGrids).isEqualTo(ngrids);
      }
      if (ncoordSys >= 0) {
        assertThat(countCoordSys).isEqualTo(ncoordSys);
      }
      if (ncoordAxes >= 0) {
        assertThat(countCoordAxes).isEqualTo(ncoordAxes);
      }
      if (nVertCooordAxes >= 0) {
        assertThat(countVertCooordAxes).isEqualTo(nVertCooordAxes);
      }

    }
  }

}
