/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.geoloc.vertical;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.array.InvalidRangeException;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Open VerticalTransforms.
 * These are all the test files I can find with a vertical transform.
 * We can port/implement more transforms if we get a test file.
 */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestCompareVerticalTransforms {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(500);
    try {
      result.add(new Object[] {TestDir.cdmUnitTestDir + "transforms/erie_test.ncml", "temp",
          ucar.nc2.geoloc.vertical.OceanSigma.class, SimpleUnit.meterUnit});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "transforms/gomoos.ncml", "temp",
          ucar.nc2.geoloc.vertical.OceanSigma.class, SimpleUnit.meterUnit});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "transforms/OceanSigma.nc", "temp",
          ucar.nc2.geoloc.vertical.OceanSigma.class, SimpleUnit.meterUnit});

      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/cf/bora_feb_001.nc", "AKs",
          ucar.nc2.geoloc.vertical.OceanS.class, SimpleUnit.meterUnit});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/cf/bora_feb_001.nc", "salt",
          ucar.nc2.geoloc.vertical.OceanS.class, SimpleUnit.meterUnit});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "transforms/OceanS.nc", "salt",
          ucar.nc2.geoloc.vertical.OceanS.class, SimpleUnit.meterUnit});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "transforms/OceanS2.nc", "salt",
          ucar.nc2.geoloc.vertical.OceanS.class, SimpleUnit.meterUnit});

      result.add(new Object[] {TestDir.cdmUnitTestDir + "transforms/ocean_his_g1.nc", "u",
          ucar.nc2.geoloc.vertical.OceanSG1.class, SimpleUnit.meterUnit});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "transforms/ocean_his_g2.nc", "u",
          ucar.nc2.geoloc.vertical.OceanSG2.class, SimpleUnit.meterUnit});

      result.add(new Object[] {TestDir.cdmUnitTestDir + "transforms/VExisting3D_NUWG.nc", "rhu_hybr",
          ucar.nc2.geoloc.vertical.ExistingFieldVerticalTransform.class, SimpleUnit.factory("gp m")});

      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/cf/temperature.nc", "Temperature",
          ucar.nc2.geoloc.vertical.AtmosSigma.class, SimpleUnit.pressureUnit});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "transforms/Sigma_LC.nc", "Temperature",
          ucar.nc2.geoloc.vertical.AtmosSigma.class, SimpleUnit.pressureUnit});

      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/cf/ccsm2.nc", "MQ",
          ucar.nc2.geoloc.vertical.AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/csm/ha0001.nc", "CME",
          ucar.nc2.geoloc.vertical.AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/csm/ha0001.nc", "CGS",
          ucar.nc2.geoloc.vertical.AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "transforms/HybridSigmaPressure.nc", "T",
          ucar.nc2.geoloc.vertical.AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "transforms/climo.cam2.h0.0000-09.nc", "T",
          ucar.nc2.geoloc.vertical.AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit});
      // @Ignore("fails because not correctly slicing vertical dimension out")
      // result.add(new Object[] {TestDir.cdmUnitTestDir + "transforms/HIRLAMhybrid.ncml", "Relative_humidity_hybrid",
      // ucar.nc2.geoloc.vertical.AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit});

      result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/fmrc/espresso/espresso_his_20130505_0000_0001.nc", "u",
          ucar.nc2.geoloc.vertical.OceanSG1.class, SimpleUnit.meterUnit});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/fmrc/espresso/espresso_his_20130505_0000_0001.nc", "w",
          ucar.nc2.geoloc.vertical.OceanSG1.class, SimpleUnit.meterUnit});

      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_v2_Lambert.nc", "U",
          ucar.nc2.geoloc.vertical.WrfEta.class, SimpleUnit.pressureUnit});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_v2_Lambert.nc", "V",
          ucar.nc2.geoloc.vertical.WrfEta.class, SimpleUnit.pressureUnit});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_v2_Lambert.nc", "W",
          ucar.nc2.geoloc.vertical.WrfEta.class, SimpleUnit.meterUnit}); // z_stag has meters coord
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_v2_Lambert.nc", "T",
          ucar.nc2.geoloc.vertical.WrfEta.class, SimpleUnit.pressureUnit});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_d01_2006-03-08_21-00-00", "U",
          ucar.nc2.geoloc.vertical.WrfEta.class, SimpleUnit.pressureUnit});
    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  /////////////////////////////////////////////////////////////
  private final String filename;
  private final String gridName;
  private final Class<?> vtClass;
  private final SimpleUnit vunit;

  public TestCompareVerticalTransforms(String filename, String gridName, Class<?> vtClass, SimpleUnit vunit) {
    this.filename = filename;
    this.gridName = gridName;
    this.vtClass = vtClass;
    this.vunit = vunit;
  }

  @Test
  public void compare() throws IOException, InvalidRangeException, ucar.ma2.InvalidRangeException {
    TestVertical.open(filename, gridName, vtClass, vunit);
  }

}
