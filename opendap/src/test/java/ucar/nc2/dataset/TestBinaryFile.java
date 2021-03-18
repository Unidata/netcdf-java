/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.junit.Test;
import ucar.array.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class TestBinaryFile {

  private static final String filename = "file:src/test/data/cdm_read/GFS_Global_0p5deg_20201006_0600.grib2.dods";

  private static final String varname1 = "Temperature_surface";
  private static final String varname2 = "time3";

  @Test
  public void testFileOpen() throws IOException {
    try (NetcdfFile ncf = NetcdfDatasets.openFile(filename, null)) {
      assertThat(ncf).isNotNull();
    }
  }

  @Test
  public void testDatasetOpen() throws IOException {
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(filename)) {
      assertThat(ncd).isNotNull();
    }
  }

  @Test
  public void testGlobalMetadata() throws IOException {
    try (NetcdfFile ncf = NetcdfDatasets.openFile(filename, null)) {
      assertThat(ncf.findGlobalAttributeIgnoreCase("conventions")).isNotNull();
    }
  }

  @Test
  public void testVar1() throws IOException {
    try (NetcdfFile ncf = NetcdfDatasets.openFile(filename, null)) {
      assert ncf.findVariable(varname1) != null;
    }
  }

  @Test
  public void testVar2() throws IOException {
    try (NetcdfFile ncf = NetcdfDatasets.openFile(filename, null)) {
      assert ncf.findVariable(varname2) != null;
    }
  }

  @Test
  public void testCompareVarsMa2() throws IOException {
    // Bug addressed by Unidata/netcdf-java#499 in that the we were not handling
    // data from multiple variables in a single .dods response. When bug was present,
    // var1 and var2 had the same array (shape and values).
    try (NetcdfFile ncf = NetcdfDatasets.openFile(filename, null)) {
      Variable var1 = ncf.findVariable(varname1);
      Variable var2 = ncf.findVariable(varname2);
      assert var1 != null;
      assert var2 != null;
      assertThat(var1.getShape()).isNotEqualTo(var2.getShape());
      ucar.ma2.Array data1 = var1.read();
      ucar.ma2.Array data2 = var2.read();
      assertThat(data1).isNotEqualTo(data2);
    }
  }

  @Test
  public void testCompareVars() throws IOException {
    // Bug addressed by Unidata/netcdf-java#499 in that the we were not handling
    // data from multiple variables in a single .dods response. When bug was present,
    // var1 and var2 had the same array (shape and values).
    try (NetcdfFile ncf = NetcdfDatasets.openFile(filename, null)) {
      Variable var1 = ncf.findVariable(varname1);
      Variable var2 = ncf.findVariable(varname2);
      assert var1 != null;
      assert var2 != null;
      assertThat(var1.getShape()).isNotEqualTo(var2.getShape());
      Array<?> data1 = var1.readArray();
      Array<?> data2 = var2.readArray();
      assertThat(data1).isNotEqualTo(data2);
    }
  }

  @Test
  public void testVarMetadata() throws IOException {
    try (NetcdfFile ncf = NetcdfDatasets.openFile(filename, null)) {
      Variable temperature = ncf.findVariable(varname1);
      assertThat(temperature.findAttribute("units")).isNotNull();
    }
  }

  @Test
  public void testVarReadFullMa2() throws IOException {
    try (NetcdfFile ncf = NetcdfDatasets.openFile(filename, null)) {
      Variable temperature = ncf.findVariable(varname1);
      ucar.ma2.Array temperatureData = temperature.read();
      assertThat(temperatureData.getShape()).isEqualTo(new int[] {3, 8, 15});
    }
  }

  @Test
  public void testVarReadFull() throws IOException {
    try (NetcdfFile ncf = NetcdfDatasets.openFile(filename, null)) {
      Variable temperature = ncf.findVariable(varname1);
      Array temperatureData = temperature.readArray();
      assertThat(temperatureData.getShape()).isEqualTo(new int[] {3, 8, 15});
    }
  }

  @Test
  public void testVarReadSectionMa2() throws IOException, InvalidRangeException {
    try (NetcdfFile ncf = NetcdfDatasets.openFile(filename, null)) {
      Variable temperature = ncf.findVariable(varname1);
      ucar.ma2.Array temperatureData = temperature.read("0,0,0:4");
      assertThat(temperatureData.getShape()).isEqualTo(new int[] {1, 1, 5});
    }
  }

  @Test
  public void testVarReadSection() throws IOException, ucar.array.InvalidRangeException {
    try (NetcdfFile ncf = NetcdfDatasets.openFile(filename, null)) {
      Variable temperature = ncf.findVariable(varname1);
      ucar.array.Section section = ucar.array.Section.builder().appendRange(0).appendRange(0).appendRange(0, 4).build();
      Array<?> temperatureData = temperature.readArray(section);
      assertThat(temperatureData.getShape()).isEqualTo(section.getShape());
    }
  }
}
