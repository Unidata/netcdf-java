/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.geoloc.vertical;

import com.google.common.base.Strings;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridHorizCoordinateSystem;
import ucar.nc2.grid.GridTimeCoordinateSystem;
import ucar.nc2.internal.grid.GridNetcdfDataset;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/** Test basic projection methods */
@Category(NeedsCdmUnitTest.class)
public class TestVertical {

  @Test
  public void testOceanSG2() throws java.io.IOException, InvalidRangeException {
    open(TestDir.cdmUnitTestDir + "transforms/ocean_his_g2.nc", "u", OceanSG2.class, SimpleUnit.meterUnit);
  }

  @Test
  public void testExistingFieldVerticalTransform() throws java.io.IOException, InvalidRangeException {
    open(TestDir.cdmUnitTestDir + "transforms/VExisting3D_NUWG.nc", "rhu_hybr", ExistingFieldVerticalTransform.class,
        SimpleUnit.factory("gp m"));
  }

  @Test
  public void testHIRLAMhybrid() throws java.io.IOException, InvalidRangeException {
    open(TestDir.cdmUnitTestDir + "transforms/HIRLAMhybrid.ncml", "Relative_humidity_hybrid",
        AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit);
  }

  @Test
  public void testOceanS() throws java.io.IOException, InvalidRangeException {
    open(TestDir.cdmUnitTestDir + "transforms/roms_ocean_s_coordinate.nc", "temp", OceanS.class, SimpleUnit.meterUnit);
  }

  @Test
  public void testOceanSigma() throws java.io.IOException, InvalidRangeException {
    open(TestDir.cdmUnitTestDir + "conventions/cf/gomoos_cf.nc", "temp", OceanSigma.class, SimpleUnit.meterUnit);
  }

  @Test
  public void testAtmSigma() throws java.io.IOException, InvalidRangeException {
    open(TestDir.cdmUnitTestDir + "transforms/temperature.nc", "Temperature", AtmosSigma.class,
        SimpleUnit.pressureUnit);
  }

  @Test
  public void testAtmHybrid() throws java.io.IOException, InvalidRangeException {
    open(TestDir.cdmUnitTestDir + "conventions/cf/ccsm2.nc", "T", AtmosHybridSigmaPressure.class,
        SimpleUnit.pressureUnit);
  }

  @Test
  public void testHybridSigmaPressure() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "transforms/HybridSigmaPressure.nc";
    open(filename, "T", AtmosHybridSigmaPressure.class, SimpleUnit.pressureUnit);
  }

  @Test
  public void testWrfEta() throws java.io.IOException, InvalidRangeException {
    open(TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_v2_Lambert.nc", "T", WrfEta.class, SimpleUnit.pressureUnit);
  }

  @Test
  public void testWrfEta2() throws java.io.IOException, InvalidRangeException {
    open(TestDir.cdmUnitTestDir + "/conventions/wrf/wrfout_d01_2006-03-08_21-00-00", "T", WrfEta.class,
        SimpleUnit.pressureUnit);
  }

  static void open(String filename, String gridName, Class<?> vtClass, SimpleUnit vunit)
      throws IOException, InvalidRangeException {
    System.out.printf("compare %s %s%n", filename, gridName);

    Formatter errlog = new Formatter();
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertWithMessage(errlog.toString()).that(grido.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = grido.get();
      Grid grid = gridDataset.findGrid(gridName).orElseThrow();

      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      VerticalTransform vt = gcs.getVerticalTransform();
      assertThat(vt).isNotNull();
      System.out.printf(" VerticalTransform new %s%n", vt.getClass().getName());
      assertThat(vt.getClass()).isEqualTo(vtClass);

      // should be compatible with vunit
      String vertCoordUnit = vt.getUnitString();
      if (vunit != null) {
        assertWithMessage(String.format("%s expect %s", vertCoordUnit, vunit)).that(vunit.isCompatible(vertCoordUnit))
            .isTrue();
      } else {
        assertWithMessage(String.format("%s expect %s", vertCoordUnit, vunit))
            .that(Strings.isNullOrEmpty(vertCoordUnit)).isTrue();
      }

      GridTimeCoordinateSystem tcs = grid.getTimeCoordinateSystem();
      int ntimes = tcs == null ? 1 : tcs.getTimeOffsetAxis(0).getNominalSize();
      for (int timeIndex = 0; timeIndex < ntimes; timeIndex++) {
        Array<Number> zt = vt.getCoordinateArray3D(timeIndex);
      }

      GridHorizCoordinateSystem hcs = grid.getHorizCoordinateSystem();
      int yindex = hcs.getShape().get(0) / 2;
      int xindex = hcs.getShape().get(1) / 2;

      for (int timeIndex = 0; timeIndex < ntimes; timeIndex++) {
        Array<Number> zt = vt.getCoordinateArray1D(timeIndex, xindex, yindex);
      }
    }
  }

}
