/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dataset.VariableDS;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

/*
 * double time(time=1);
 * :units = "calendar months since 2004-12-30T00:00Z"; // string
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribCalendarProblem {
  private static final String vname =
      "Volumetric_soil_moisture_content_layer_between_two_depths_below_surface_layer_1_Month_Average";

  @Test
  public void testGribWithMonthUnits() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "formats/grib1/cfs.wmo";

    try (NetcdfDataset ds = NetcdfDatasets.openDataset(endpoint)) {
      assertThat(ds).isNotNull();
      Variable v = ds.findVariable("time");
      assertThat(v).isNotNull();
      assertThat(v).isInstanceOf(VariableDS.class);
    }
  }

  @Test
  public void testGridWithMonthUnits() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "formats/grib1/cfs.wmo";

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset ds = ucar.nc2.grid.GridDatasetFactory.openGridDataset(endpoint, errlog)) {
      assertThat(ds).isNotNull();
      Optional<ucar.nc2.grid.Grid> gridO = ds.findGrid(vname);
      assertThat(gridO).isPresent();
    }
  }
}
