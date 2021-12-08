/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

@Category(NeedsCdmUnitTest.class)
public class TestH5filter {

  @org.junit.Test
  public void testFilterNoneApplied() throws IOException {
    // actually bogus - apparently all filters arre turned off
    // but its a test of filtered data with no filter actually applied
    try (NetcdfFile ncfile = TestH5.openH5("support/zip.h5")) {
      Variable v = ncfile.findVariable("Data/Compressed_Data");
      assertThat(v).isNotNull();
      Array<Number> data = (Array<Number>) v.readArray();
      int[] shape = data.getShape();
      assertThat(shape[0]).isEqualTo(1000);
      assertThat(shape[1]).isEqualTo(20);

      Index ima = data.getIndex();
      for (int i = 0; i < 1000; i++)
        for (int j = 0; j < 20; j++) {
          int val = data.get(ima.set(i, j)).intValue();
          assertThat(val).isEqualTo(i + j);
        }
    }
  }

  @org.junit.Test
  public void test2() throws IOException {
    // H5header.setDebugFlags( DebugFlags.create("H5header/header"));

    // probably bogus also, cant find any non-zero filtered variables
    try (NetcdfFile ncfile = TestH5.openH5("wrf/wrf_input_seq.h5")) {
      Variable v = ncfile.findVariable("DATASET=INPUT/GSW");
      assertThat(v).isNotNull();
      Array data = v.readArray();
      int[] shape = data.getShape();
      assertThat(shape[0]).isEqualTo(1);
      assertThat(shape[1]).isEqualTo(20);
      assertThat(shape[2]).isEqualTo(10);
    }
  }

  @org.junit.Test
  public void testDeflate() throws IOException {
    // H5header.setDebugFlags( DebugFlags.create("H5header/header"));
    try (NetcdfFile ncfile = TestH5.openH5("msg/MSG1_8bit_HRV.H5")) {

      // picture looks ok in ToolsUI
      Variable v = ncfile.findVariable("image1/image_data");
      assertThat(v).isNotNull();
      Array data = v.readArray();
      int[] shape = data.getShape();
      assertThat(shape[0]).isEqualTo(1000);
      assertThat(shape[1]).isEqualTo(1500);
    }
  }

  @org.junit.Test
  public void testMissing() throws IOException {
    // H5header.setDebugFlags( DebugFlags.create("H5header/header"));
    try (NetcdfFile ncfile = TestH5.openH5("HIRDLS/HIRDLS2-AFGL_b027_na.he5")) {

      // picture looks ok in ToolsUI
      Variable v = ncfile.findVariable("HDFEOS/SWATHS/HIRDLS/Data_Fields/Altitude");
      assertThat(v).isNotNull();
      Array data = v.readArray();
      int[] shape = data.getShape();
      assertThat(shape[0]).isEqualTo(6);
      assertThat(shape[1]).isEqualTo(145);
    }
  }


}
