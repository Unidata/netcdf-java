/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import com.google.common.collect.ImmutableList;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.internal.util.CompareArrayToArray;

import static com.google.common.truth.Truth.assertThat;

/** NetcdfFormatWriter tests */
public class TestNetcdfFormatUpdater {

  /*
   * byte Band1(y, x);
   * > Band1:_Unsigned = "true";
   * > Band1:_FillValue = -1b; // byte
   * >
   * > byte Band2(y, x);
   * > Band2:_Unsigned = "true";
   * > Band2:valid_range = 0s, 254s; // short
   */

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testOpenExisting() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(filename).setFill(false);
    writerb.addUnlimitedDimension("time");
    writerb.addAttribute(new Attribute("name", "value"));

    // public Variable addVariable(Group g, String shortName, ArrayType dataType, String dims) {
    writerb.addVariable("time", ArrayType.INT, "time").addAttribute(new Attribute(CDM.UNSIGNED, "true"))
        .addAttribute(new Attribute(CDM.SCALE_FACTOR, 10.0))
        .addAttribute(Attribute.builder(CDM.VALID_RANGE).setValues(ImmutableList.of(10, 240), false).build());

    // write
    try (NetcdfFormatWriter writer = writerb.build()) {
      Array data = Arrays.factory(ArrayType.INT, new int[] {4}, new int[] {0, 1, 2, 3});
      writer.write(writer.findVariable("time"), data.getIndex(), data);
    }

    // open existing, add data along unlimited dimension
    try (NetcdfFormatWriter writer = NetcdfFormatWriter.openExisting(filename).setFill(false).build()) {
      Variable time = writer.findVariable("time");
      assertThat(time.getSize()).isEqualTo(4);

      Array data = Arrays.factory(ArrayType.INT, new int[] {3}, new int[] {4, 5, 6});
      writer.write(writer.findVariable("time"), data.getIndex().set((int) time.getSize()), data);
    }

    // read it back
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Variable vv = ncfile.getRootGroup().findVariableLocal("time");
      assertThat(vv.getSize()).isEqualTo(7);

      Array expected = Arrays.makeArray(ArrayType.INT, 7, 0, 1);
      Array data = vv.readArray();
      assertThat(CompareArrayToArray.compareData("time", expected, data)).isTrue();
    }

    // open existing, add more data along unlimited dimension
    try (NetcdfFormatWriter writer = NetcdfFormatWriter.openExisting(filename).setFill(false).build()) {
      Variable time = writer.findVariable("time");
      assertThat(time.getSize()).isEqualTo(7);

      Array data = Arrays.factory(ArrayType.INT, new int[] {2}, new int[] {7, 8});
      writer.write(writer.findVariable("time"), data.getIndex().set((int) time.getSize()), data);
    }

    // read it back
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Variable vv = ncfile.getRootGroup().findVariableLocal("time");
      assert vv.getSize() == 9 : vv.getSize();

      Array expected = Arrays.makeArray(ArrayType.INT, 9, 0, 1);
      Array data = vv.readArray();
      assertThat(CompareArrayToArray.compareData("time", expected, data)).isTrue();
    }
  }
}
