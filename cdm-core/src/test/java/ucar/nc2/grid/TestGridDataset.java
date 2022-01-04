/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import org.junit.Test;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
import ucar.unidata.util.test.TestDir;

import java.util.Formatter;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

/** Test {@link GridDataset} */
public class TestGridDataset {

  @Test
  public void testBasics() throws Exception {
    String filename = TestDir.cdmLocalTestDataDir + "ncml/fmrc/GFS_Puerto_Rico_191km_20090729_0000.nc";
    Formatter errlog = new Formatter();

    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      System.out.println("readGridDataset: " + gds.getLocation());
      assertThat(gds.toString()).startsWith(String.format("name = GFS_Puerto_Rico_191km_20090729_0000.nc%n"
          + "location = ../cdm-core/src/test/data/ncml/fmrc/GFS_Puerto_Rico_191km_20090729_0000.nc%n"
          + "featureType = GRID"));

      Grid grid = gds.findGridByAttribute("Grib_Variable_Id", "VAR_7-0-2-11_L100").orElseThrow();
      assertThat(grid.getName()).isEqualTo("Temperature_isobaric");
      Attribute att = grid.attributes().findAttribute("Grib_Variable_Id");
      assertThat(att).isEqualTo(new Attribute("Grib_Variable_Id", "VAR_7-0-2-11_L100"));

      assertThat(grid.toString()).startsWith("float Temperature_isobaric(time=20, isobaric1=6, y=39, x=45);");

      Optional<Grid> bad = gds.findGridByAttribute("failure", "VAR_7-0-2-11_L100");
      assertThat(bad).isEmpty();

      // test Grid
      assertThat(grid.getHorizCoordinateSystem()).isNotNull();
      assertThat(grid.getTimeCoordinateSystem()).isNotNull();

      // test GridCoordinateSystem
      assertThat(grid.getCoordinateSystem()).isNotNull();
      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat((Object) gcs.findCoordAxisByType(AxisType.Time)).isNotNull();
      assertThat((Object) gcs.findCoordAxisByType(AxisType.Ensemble)).isNull();

      assertThat(gcs.toString()).startsWith(String.format("Coordinate System (time isobaric1 y x)%n"
          + " time (GridAxisPoint) %n"
          + " isobaric1 (GridAxisPoint) %n"
          + " y (GridAxisPoint) %n"
          + " x (GridAxisPoint) "));

      assertThat(gcs.showFnSummary()).isEqualTo("GRID(T,Z,Y,X)");
    }
  }
}
