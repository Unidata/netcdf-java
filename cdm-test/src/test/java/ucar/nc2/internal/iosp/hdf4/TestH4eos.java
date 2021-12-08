/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.iosp.hdf4;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Test reading HDF4 EOS files. */
@Category(NeedsCdmUnitTest.class)
public class TestH4eos {
  static public String testDir = TestDir.cdmUnitTestDir + "formats/hdf4/eos/";

  // test the coordSysBuilder - check if grid exists
  @Test
  public void testModisGeo() throws Exception {
    // GEO (lat//lon)
    testGridExists(testDir + "modis/MOD17A3.C5.1.GEO.2000.hdf", "MOD_Grid_MOD17A3/Data_Fields/Npp_0\\.05deg",
        "Npp_0.05deg");
  }

  @Test
  public void testModisSinusoidal() throws Exception {
    // SINUSOIDAL
    testGridExists(testDir + "modis/MOD13Q1.A2012321.h00v08.005.2012339011757.hdf",
        "MODIS_Grid_16DAY_250m_500m_VI/Data_Fields/250m_16_days_NIR_reflectance", "250m_16_days_NIR_reflectance");
  }

  private void testGridExists(String filename, String vname, String gname) throws IOException {
    System.out.printf("filename= %s%n", filename);
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Variable v = ncfile.findVariable(vname);
      assertThat(v).isNotNull();
    }

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid v = gds.findGrid(gname).orElseThrow();
      assertThat(v).isNotNull();
    }
  }

  @Test
  public void testSpecificVariableSection() throws Exception {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmUnitTestDir + "formats/hdf4/96108_08.hdf")) {

      Variable v = ncfile.findVariable("CalibratedData");
      assertThat(v).isNotNull();
      assertThat(v.getRank()).isEqualTo(3);
      int[] shape = v.getShape();
      assertThat(shape[0]).isEqualTo(810);
      assertThat(shape[1]).isEqualTo(50);
      assertThat(shape[2]).isEqualTo(716);

      Array<?> data = v.readArray(new Section("0:809:10,0:49:5,0:715:2"));
      assertThat(data.getRank()).isEqualTo(3);
      int[] dshape = data.getShape();
      assertThat(dshape[0]).isEqualTo(810 / 10);
      assertThat(dshape[1]).isEqualTo(50 / 5);
      assertThat(dshape[2]).isEqualTo(716 / 2);

      // read entire array
      Array<?> A = v.readArray();

      // compare
      Array<?> Asection = Arrays.section(A, new Section("0:809:10,0:49:5,0:715:2"));
      assertThat(Asection.getRank()).isEqualTo(3);
      for (int i = 0; i < 3; i++)
        assertThat(Asection.getShape()[i]).isEqualTo(dshape[i]);

      CompareArrayToArray.compareData(v.getShortName(), data, Asection);
    }
  }


}
