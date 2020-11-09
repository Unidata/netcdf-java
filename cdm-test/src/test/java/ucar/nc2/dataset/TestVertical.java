/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.*;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.geoloc.VerticalTransform;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

/** Test basic projection methods */
@Category(NeedsCdmUnitTest.class)
public class TestVertical {

  @Test
  public void testOceanS() throws java.io.IOException, InvalidRangeException {
    try (GridDataset gds =
        ucar.nc2.dt.grid.GridDataset.open(TestDir.cdmUnitTestDir + "transforms/roms_ocean_s_coordinate.nc")) {

      GridDatatype grid = gds.findGridDatatype("temp");
      assert grid != null;

      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert gcs != null;

      VerticalCT vct = gcs.getVerticalCT();
      assert vct != null;
      assert vct.getVerticalTransformType() == VerticalCT.Type.OceanS;

      VerticalTransform vt = gcs.getVerticalTransform();
      assert vt != null;

      ArrayDouble.D3 ca = vt.getCoordinateArray(0);
      assert ca != null;
      assert ca.getRank() == 3 : ca.getRank();

      int[] shape = ca.getShape();
      for (int i = 0; i < 3; i++)
        System.out.println(" shape " + i + " = " + shape[i]);

    }
  }

  @Test
  public void testOceanSigma() throws java.io.IOException, InvalidRangeException {
    try (GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(TestDir.cdmUnitTestDir + "conventions/cf/gomoos_cf.nc")) {

      GridDatatype grid = gds.findGridDatatype("temp");
      assert grid != null;

      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert gcs != null;

      VerticalCT vct = gcs.getVerticalCT();
      assert vct != null;
      assert vct.getVerticalTransformType() == VerticalCT.Type.OceanSigma;

      VerticalTransform vt = gcs.getVerticalTransform();
      assert vt != null;

      CoordinateAxis1DTime taxis = gcs.getTimeAxis1D();
      for (int t = 0; t < taxis.getSize(); t++) {
        System.out.printf("vert coord for time = %s%n", taxis.getCalendarDate(t));
        ArrayDouble.D3 ca = vt.getCoordinateArray(t);
        assert ca != null;
        assert ca.getRank() == 3 : ca.getRank();

        int[] shape = ca.getShape();
        for (int i = 0; i < 3; i++)
          System.out.println(" shape " + i + " = " + shape[i]);
      }
    }
  }

  @Test
  public void testAtmSigma() throws java.io.IOException, InvalidRangeException {
    try (GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(TestDir.cdmUnitTestDir + "transforms/temperature.nc")) {

      GridDatatype grid = gds.findGridDatatype("Temperature");
      assert grid != null;

      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert gcs != null;

      VerticalCT vct = gcs.getVerticalCT();
      assert vct != null;
      assert vct.getVerticalTransformType() == VerticalCT.Type.Sigma;

      VerticalTransform vt = gcs.getVerticalTransform();
      assert vt != null;

      ArrayDouble.D3 ca = vt.getCoordinateArray(0);
      assert ca != null;
      assert ca.getRank() == 3 : ca.getRank();

      int[] shape = ca.getShape();
      for (int i = 0; i < 3; i++)
        System.out.println(" shape " + i + " = " + shape[i]);
    }
  }

  @Test
  public void testAtmHybrid() throws java.io.IOException, InvalidRangeException {
    try (GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(TestDir.cdmUnitTestDir + "conventions/cf/ccsm2.nc")) {

      GridDatatype grid = gds.findGridDatatype("T");
      assert grid != null;

      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert gcs != null;

      VerticalCT vct = gcs.getVerticalCT();
      assert vct != null;
      assert vct.getVerticalTransformType() == VerticalCT.Type.HybridSigmaPressure : vct.getVerticalTransformType();

      VerticalTransform vt = gcs.getVerticalTransform();
      assert vt != null;

      ArrayDouble.D3 ca = vt.getCoordinateArray(0);
      assert ca != null;
      assert ca.getRank() == 3 : ca.getRank();

      int[] shape = ca.getShape();
      for (int i = 0; i < 3; i++)
        System.out.println(" shape " + i + " = " + shape[i]);
    }
  }

  @Test
  public void testWrfEta() throws java.io.IOException, InvalidRangeException {
    try (GridDataset gds =
        ucar.nc2.dt.grid.GridDataset.open(TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_v2_Lambert.nc")) {

      GridDatatype grid = gds.findGridDatatype("T");
      assert grid != null;

      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert gcs != null;

      VerticalCT vct = gcs.getVerticalCT();
      assert vct != null;
      assert vct.getVerticalTransformType() == VerticalCT.Type.WRFEta : vct.getVerticalTransformType();

      VerticalTransform vt = gcs.getVerticalTransform();
      assert vt != null;

      ArrayDouble.D3 ca = vt.getCoordinateArray(0);
      assert ca != null;
      assert ca.getRank() == 3 : ca.getRank();

      int[] shape = ca.getShape();
      for (int i = 0; i < 3; i++)
        System.out.println(" shape " + i + " = " + shape[i]);
    }
  }

  @Test
  public void testStride() throws java.io.IOException, InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "/conventions/wrf/wrfout_d01_2006-03-08_21-00-00";
    try (GridDataset gds = ucar.nc2.dt.grid.GridDataset.open(filename)) {
      GridDatatype grid = gds.findGridDatatype("T");
      assert grid != null;

      grid = grid.makeSubset(null, null, null, 1, 2, 4);

      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert gcs != null;

      VerticalTransform vt = gcs.getVerticalTransform();
      assert vt != null;

      ArrayDouble.D3 ca = vt.getCoordinateArray(0);
      assert ca != null;
      assert ca.getRank() == 3 : ca.getRank();

      int[] shape = ca.getShape();
      for (int i = 0; i < 3; i++)
        System.out.println(" shape " + i + " = " + shape[i]);

      assert shape[0] == 44;
      assert shape[1] == 399 / 2 + 1;
      assert shape[2] == 399 / 4 + 1;
    }
  }
}
