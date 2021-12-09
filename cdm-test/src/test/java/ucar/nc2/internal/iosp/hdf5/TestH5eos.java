/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Arrays;
import ucar.nc2.*;
import ucar.array.ArrayType;
import ucar.array.Array;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.IOException;

/** Test HDF5 Eos files */
@Category(NeedsCdmUnitTest.class)
public class TestH5eos {

  @Test
  public void testStructMetadata() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("HIRDLS/HIRDLS2-Aura73p_b029_2000d275.he5")) {

      Group root = ncfile.getRootGroup();
      Group g = root.findGroupLocal("HDFEOS_INFORMATION");
      Variable dset = g.findVariableLocal("StructMetadata.0");
      assertThat(dset).isNotNull();
      assertThat(dset.getArrayType()).isEqualTo(ArrayType.CHAR);

      // read entire array
      Array A = dset.readArray();
      assertThat(A.getRank()).isEqualTo(1);

      String sval = Arrays.makeStringFromChar(A);
      System.out.println(dset.getFullName());
      System.out.println(" Length = " + sval.length());
      System.out.println(" Value = " + sval);
    }
  }

  @Test
  public void test1() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("HIRDLS/HIR2ARSP_c3_na.he5")) {
      Variable v = ncfile.findVariable("HDFEOS/SWATHS/H2SO4_H2O_Tisdale/Data_Fields/Wavenumber");
      assertThat(v).isNotNull();
      Dimension dim = v.getDimension(0);
      assertThat(dim).isNotNull();
      assertThat(dim.getShortName()).isNotNull();
      assertThat(dim.getShortName()).isEqualTo("nChans");
    }
  }

  @Test
  public void testNestedVariable() throws IOException {
    try (NetcdfFile ncfile = TestH5.openH5("HIRDLS/HIRDLS1_v4.0.2a-aIrix-c2_2003d106.he5")) {
      Variable v = ncfile.findVariable("HDFEOS/SWATHS/HIRDLS_L1_Swath/Data_Fields/Elevation_Angle");
      assertThat(v).isNotNull();
      assertThat(v.getRank()).isEqualTo(4);
      assertThat(v.getDimension(0).getShortName()).isEqualTo("MaF");
      assertThat(v.getDimension(1).getShortName()).isEqualTo("MiF");
      assertThat(v.getDimension(2).getShortName()).isEqualTo("CR");
      assertThat(v.getDimension(3).getShortName()).isEqualTo("CC");
    }
  }

  @Test
  public void testNetcdf4() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles
        .open(TestDir.cdmUnitTestDir + "formats/netcdf4/VNP10A1_A2018001_h31v11_001_2019126193423_HEGOUT.nc")) {
    }
  }

}
