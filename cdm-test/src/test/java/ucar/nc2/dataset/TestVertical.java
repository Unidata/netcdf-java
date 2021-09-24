/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.geoloc.VerticalTransform;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

import static com.google.common.truth.Truth.assertThat;

/** Test basic projection methods */
@Category(NeedsCdmUnitTest.class)
public class TestVertical {

  @Test
  public void testOceanS() throws java.io.IOException, InvalidRangeException {
    try (NetcdfDataset gds =
        NetcdfDatasets.openDataset(TestDir.cdmUnitTestDir + "transforms/roms_ocean_s_coordinate.nc")) {
      System.out.printf("openDataset %s%n", gds.getLocation());

      VariableDS vds = (VariableDS) gds.findVariable("temp");
      assertThat(vds).isNotNull();
      assertThat(vds.getShape()).isEqualTo(new int[] {2, 20, 60, 160});

      CoordinateSystem cs = vds.getCoordinateSystems().get(0);
      assertThat(cs).isNotNull();

      VerticalCT vct = cs.getVerticalCT();
      assertThat(vct).isNotNull();
      assertThat(vct.getVerticalTransformType()).isEqualTo(VerticalCT.Type.OceanS);

      VerticalTransform vt = vct.makeVerticalTransform(gds, null);
      assertThat(vt).isNotNull();

      ArrayDouble.D3 ca = vt.getCoordinateArray(0);
      assertThat(ca).isNotNull();
      assertThat(ca.getShape()).isEqualTo(new int[] {20, 2, 60});
    }
  }

  @Test
  public void testOceanSigma() throws java.io.IOException, InvalidRangeException {
    try (NetcdfDataset gds = NetcdfDatasets.openDataset(TestDir.cdmUnitTestDir + "conventions/cf/gomoos_cf.nc")) {
      System.out.printf("openDataset %s%n", gds.getLocation());

      VariableDS vds = (VariableDS) gds.findVariable("temp");
      assertThat(vds).isNotNull();
      assertThat(vds.getShape()).isEqualTo(new int[] {2, 22, 120, 180});

      CoordinateSystem cs = vds.getCoordinateSystems().get(0);
      assertThat(cs).isNotNull();

      VerticalCT vct = cs.getVerticalCT();
      assertThat(vct).isNotNull();
      assertThat(vct.getVerticalTransformType()).isEqualTo(VerticalCT.Type.OceanSigma);

      VerticalTransform vt = vct.makeVerticalTransform(gds, null);
      assertThat(vt).isNotNull();

      CoordinateAxis1D taxis = (CoordinateAxis1D) cs.findAxis(AxisType.Time);
      assertThat(taxis).isNotNull();
      for (int t = 0; t < taxis.getSize(); t++) {
        ArrayDouble.D3 ca = vt.getCoordinateArray(t);
        assertThat(ca).isNotNull();
        assertThat(ca.getShape()).isEqualTo(new int[] {22, 2, 120});
      }
    }
  }

  @Test
  public void testAtmSigma() throws java.io.IOException, InvalidRangeException {
    try (NetcdfDataset gds = NetcdfDatasets.openDataset(TestDir.cdmUnitTestDir + "transforms/temperature.nc")) {
      System.out.printf("openDataset %s%n", gds.getLocation());

      VariableDS vds = (VariableDS) gds.findVariable("Temperature");
      assertThat(vds).isNotNull();
      assertThat(vds.getShape()).isEqualTo(new int[] {2, 128, 164});

      CoordinateSystem cs = vds.getCoordinateSystems().get(0);
      assertThat(cs).isNotNull();

      VerticalCT vct = cs.getVerticalCT();
      assertThat(vct).isNotNull();
      assertThat(vct.getVerticalTransformType()).isEqualTo(VerticalCT.Type.Sigma);

      VerticalTransform vt = vct.makeVerticalTransform(gds, null);
      assertThat(vt).isNotNull();

      ArrayDouble.D3 ca = vt.getCoordinateArray(0);
      assertThat(ca).isNotNull();
      assertThat(ca.getShape()).isEqualTo(new int[] {2, 128, 164});
    }
  }

  @Test
  public void testAtmHybrid() throws java.io.IOException, InvalidRangeException {
    try (NetcdfDataset gds = NetcdfDatasets.openDataset(TestDir.cdmUnitTestDir + "conventions/cf/ccsm2.nc")) {
      System.out.printf("openDataset %s%n", gds.getLocation());

      VariableDS vds = (VariableDS) gds.findVariable("T");
      assertThat(vds).isNotNull();
      assertThat(vds.getShape()).isEqualTo(new int[] {1, 26, 64, 128});

      CoordinateSystem cs = vds.getCoordinateSystems().get(0);
      assertThat(cs).isNotNull();

      VerticalCT vct = cs.getVerticalCT();
      assertThat(vct).isNotNull();
      assertThat(vct.getVerticalTransformType()).isEqualTo(VerticalCT.Type.HybridSigmaPressure);

      VerticalTransform vt = vct.makeVerticalTransform(gds, null);
      assertThat(vt).isNotNull();

      ArrayDouble.D3 ca = vt.getCoordinateArray(0);
      assertThat(ca).isNotNull();
      assertThat(ca.getShape()).isEqualTo(new int[] {26, 64, 128});
    }
  }

  @Test
  public void testWrfEta() throws java.io.IOException, InvalidRangeException {
    try (NetcdfDataset gds =
        NetcdfDatasets.openDataset(TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_v2_Lambert.nc")) {
      System.out.printf("openDataset %s%n", gds.getLocation());

      VariableDS vds = (VariableDS) gds.findVariable("T");
      assertThat(vds).isNotNull();
      assertThat(vds.getShape()).isEqualTo(new int[] {13, 27, 60, 73});

      CoordinateSystem cs = vds.getCoordinateSystems().get(0);
      assertThat(cs).isNotNull();

      VerticalCT vct = cs.getVerticalCT();
      assertThat(vct).isNotNull();
      assertThat(vct.getVerticalTransformType()).isEqualTo(VerticalCT.Type.WRFEta);

      VerticalTransform vt = vct.makeVerticalTransform(gds, null);
      assertThat(vt).isNotNull();

      ArrayDouble.D3 ca = vt.getCoordinateArray(0);
      assertThat(ca).isNotNull();
      assertThat(ca.getShape()).isEqualTo(new int[] {13, 27, 60});
    }
  }

  @Test
  public void testStride() throws java.io.IOException, InvalidRangeException {
    try (NetcdfDataset gds =
        NetcdfDatasets.openDataset(TestDir.cdmUnitTestDir + "/conventions/wrf/wrfout_d01_2006-03-08_21-00-00")) {
      System.out.printf("openDataset %s%n", gds.getLocation());

      VariableDS vds = (VariableDS) gds.findVariable("T");
      assertThat(vds).isNotNull();
      assertThat(vds.getShape()).isEqualTo(new int[] {1, 44, 399, 399});

      // LOOK WAS
      // grid = grid.makeSubset(null, null, null, 1, 2, 4);

      CoordinateSystem cs = vds.getCoordinateSystems().get(0);
      assertThat(cs).isNotNull();

      VerticalCT vct = cs.getVerticalCT();
      assertThat(vct).isNotNull();
      assertThat(vct.getVerticalTransformType()).isEqualTo(VerticalCT.Type.WRFEta);

      VerticalTransform vt = vct.makeVerticalTransform(gds, null);
      assertThat(vt).isNotNull();

      ArrayDouble.D3 ca = vt.getCoordinateArray(0);
      assertThat(ca).isNotNull();
      assertThat(ca.getShape()).isEqualTo(new int[] {44, 399, 399});
    }
  }

}
