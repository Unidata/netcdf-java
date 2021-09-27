/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.geoloc.vertical;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.VerticalCT;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridHorizCoordinateSystem;
import ucar.nc2.grid.GridTimeCoordinateSystem;
import ucar.nc2.internal.grid.GridNetcdfDataset;
import ucar.nc2.internal.util.CompareArrayToMa2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/**
 * Compare new and old VerticalTransforms.
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
          ucar.nc2.geoloc.vertical.OceanSigma.class});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/cf/bora_feb_001.nc", "AKs",
          ucar.nc2.geoloc.vertical.OceanS.class});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/cf/bora_feb_001.nc", "salt",
          ucar.nc2.geoloc.vertical.OceanS.class});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/cf/temperature.nc", "Temperature",
          ucar.nc2.geoloc.vertical.AtmosSigma.class});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/cf/ccsm2.nc", "MQ",
          ucar.nc2.geoloc.vertical.AtmosHybridSigmaPressure.class});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/csm/ha0001.nc", "CME",
          ucar.nc2.geoloc.vertical.AtmosHybridSigmaPressure.class});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/csm/ha0001.nc", "CGS",
          ucar.nc2.geoloc.vertical.AtmosHybridSigmaPressure.class});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/fmrc/espresso/espresso_his_20130505_0000_0001.nc", "u",
          ucar.nc2.geoloc.vertical.OceanSG1.class});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/fmrc/espresso/espresso_his_20130505_0000_0001.nc", "w",
          ucar.nc2.geoloc.vertical.OceanSG1.class});

      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_v2_Lambert.nc", "U",
          ucar.nc2.geoloc.vertical.WrfEta.class});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_v2_Lambert.nc", "V",
          ucar.nc2.geoloc.vertical.WrfEta.class});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_v2_Lambert.nc", "W",
          ucar.nc2.geoloc.vertical.WrfEta.class});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_v2_Lambert.nc", "T",
          ucar.nc2.geoloc.vertical.WrfEta.class});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_d01_2006-03-08_21-00-00", "U",
          ucar.nc2.geoloc.vertical.WrfEta.class});
    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  /////////////////////////////////////////////////////////////
  private final String filename;
  private final String gridName;
  private final Class<?> vtClass;

  public TestCompareVerticalTransforms(String filename, String gridName, Class<?> vtClass) {
    this.filename = filename;
    this.gridName = gridName;
    this.vtClass = vtClass;
  }

  @Test
  public void compare() throws IOException, InvalidRangeException, ucar.ma2.InvalidRangeException {
    System.out.printf("compare %s %s%n", filename, gridName);

    Formatter errlog = new Formatter();
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      VariableDS vds = (VariableDS) ds.findVariable(gridName);
      VerticalCT vct = null;
      CoordinateAxis timeAxis = null;
      for (CoordinateSystem csys : vds.getCoordinateSystems()) {
        vct = csys.getVerticalCT();
        if (vct != null) {
          timeAxis = csys.findAxis(AxisType.Time);
          break;
        }
      }
      assertThat(vct).isNotNull();
      Dimension timeDim = null;
      if (timeAxis != null) {
        timeDim = timeAxis.getDimension(0);
      }
      ucar.unidata.geoloc.VerticalTransform oldVt = vct.makeVerticalTransform(ds, timeDim);

      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertWithMessage(errlog.toString()).that(grido.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = grido.get();
      Grid grid = gridDataset.findGrid(gridName).orElseThrow();

      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      VerticalTransform vt = gcs.getVerticalTransform();
      assertThat(vt).isNotNull();
      System.out.printf(" compare old %s new %s%n", oldVt.getClass().getName(), vt.getClass().getName());
      assertThat(vt.getClass()).isEqualTo(vtClass);

      GridTimeCoordinateSystem tcs = grid.getTimeCoordinateSystem();
      int ntimes = tcs == null ? 1 : tcs.getTimeOffsetAxis(0).getNominalSize();
      for (int timeIndex = 0; timeIndex < ntimes; timeIndex++) {
        ucar.ma2.ArrayDouble.D3 oldZt = oldVt.getCoordinateArray(timeIndex);
        Array<Number> zt = vt.getCoordinateArray3D(timeIndex);
        Formatter errlog2 = new Formatter();
        boolean ok =
            CompareArrayToMa2.compareData(errlog2, String.format("timeIdx = %d", timeIndex), oldZt, zt, false, false);
        if (!ok) {
          System.out.printf(" 3D FAILED errlog=%s%n", errlog2);
        }
        assertThat(ok).isTrue();
      }

      GridHorizCoordinateSystem hcs = grid.getHorizCoordinateSystem();
      int yindex = hcs.getShape().get(0) / 2;
      int xindex = hcs.getShape().get(1) / 2;

      for (int timeIndex = 0; timeIndex < ntimes; timeIndex++) {
        ucar.ma2.ArrayDouble.D1 oldZt = oldVt.getCoordinateArray1D(timeIndex, xindex, yindex);
        Array<Number> zt = vt.getCoordinateArray1D(timeIndex, xindex, yindex);
        Formatter errlog2 = new Formatter();
        boolean ok =
            CompareArrayToMa2.compareData(errlog2, String.format("timeIdx = %d", timeIndex), oldZt, zt, false, false);
        if (!ok) {
          System.out.printf(" 1D FAILED errlog=%s%n", errlog2);
        }
        assertThat(ok).isTrue();
      }
    }
  }

}
