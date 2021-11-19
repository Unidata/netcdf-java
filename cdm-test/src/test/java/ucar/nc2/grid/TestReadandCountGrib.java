/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.util.ArrayList;
import java.util.List;

/** Check opening grib datasets - local files */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestReadandCountGrib {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[] {"grib1/", "cfs.wmo", 51, 4, 7, 3});
    // result.add(new Object[] {"grib2/", "eta218.wmo", 57, 16, 20, 11}); // not handling groups
    result.add(new Object[] {"grib1/", "extended.wmo", 4, 3, 6, 2});
    // result.add(new Object[]{"grib1/","ensemble.wmo", 24, 16, 20, 10}); not supporting ensembles in GRIB1 yet
    // result.add(new Object[]{"grib1/data/","ecmf.wmo", 56, 44, 116, 58});
    result.add(new Object[] {"grib1/", "don_ETA.wmo", 28, 11, 14, 8});
    result.add(new Object[] {"grib1/", "pgbanl.fnl", 76, 15, 18, 14});
    result.add(new Object[] {"grib1/", "radar_national_rcm.grib", 1, 1, 4, 0});
    result.add(new Object[] {"grib1/", "radar_national.grib", 1, 1, 4, 0});
    // result.add(new Object[]{"grib1/data/","thin.wmo", 240, 87, 117, 63});
    // result.add(new Object[]{"grib1/data/","ukm.wmo", 96, 49, 68, 32});
    result.add(new Object[] {"grib1/", "AVN.wmo", 22, 10, 13, 7});
    result.add(new Object[] {"grib1/", "AVN-I.wmo", 20, 8, 11, 7}); //
    result.add(new Object[] {"grib1/", "MRF.wmo", 15, 8, 11, 6}); //
    result.add(new Object[] {"grib1/", "OCEAN.wmo", 1, 1, 4, 0}); // not handling groups
    result.add(new Object[] {"grib1/", "RUC.wmo", 27, 7, 11, 5});
    result.add(new Object[] {"grib1/", "RUC2.wmo", 44, 10, 14, 5});
    result.add(new Object[] {"grib1/", "WAVE.wmo", 7, 3, 8, 1}); // not handling groups

    result.add(new Object[] {"grib2/", "eta2.wmo", 35, 9, 12, 7});
    result.add(new Object[] {"grib2/", "ndfd.wmo", 1, 1, 4, 0}); //
    // result.add(new Object[]{"grib2/","eta218.wmo", 57, 13, 29, 20}); // multiple horiz coords == groups
    result.add(new Object[] {"grib2/", "PMSL_000", 1, 1, 4, 0});
    result.add(new Object[] {"grib2/", "CLDGRIB2.2005040905", 5, 1, 4, 0});
    // result.add(new Object[]{"grib2/","LMPEF_CLM_050518_1200.grb", 1, 1, 3, 0});
    result.add(new Object[] {"grib2/", "AVOR_000.grb", 1, 1, 5, 1}); //
    result.add(new Object[] {"grib2/", "AVN.5deg.wmo", 117, 17, 19, 14}); // */

    // ens not monotonic
    // result.add(new Object[] {"grib2/", "gribdecoder-20101101.enspost.t00z.prcp.grib", 2, 2, 5, 0});
    return result;
  }

  String dir, name;
  int ngrids, ncoordSys, ncoordAxes, nVertCooordAxes;

  public TestReadandCountGrib(String dir, String name, int ngrids, int ncoordSys, int ncoordAxes, int nVertCooordAxes) {
    this.dir = dir;
    this.name = name;
    this.ngrids = ngrids;
    this.ncoordSys = ncoordSys;
    this.ncoordAxes = ncoordAxes;
    this.nVertCooordAxes = nVertCooordAxes;
  }

  @Test
  public void doOne() throws Exception {
    dir = TestDir.cdmUnitTestDir + "formats/" + dir;
    TestReadandCount.doOne(dir + name, ngrids, ncoordSys, ncoordAxes, nVertCooordAxes);
  }

}
