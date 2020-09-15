/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.write;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.internal.util.CompareNetcdf2;

/** NetcdfFormatWriter tests */
public class TestNetcdfFormatUpdater {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(filename).setFill(false);
    writerb.addUnlimitedDimension("time");
    writerb.addAttribute(new Attribute("name", "value"));

    // public Variable addVariable(Group g, String shortName, DataType dataType, String dims) {
    writerb.addVariable("time", DataType.INT, "time").addAttribute(new Attribute(CDM.UNSIGNED, "true"))
        .addAttribute(new Attribute(CDM.SCALE_FACTOR, 10.0))
        .addAttribute(Attribute.builder(CDM.VALID_RANGE).setValues(ImmutableList.of(10, 240), false).build());

    // write
    try (NetcdfFormatWriter writer = writerb.build()) {
      Array data = Array.makeFromJavaArray(new int[] {0, 1, 2, 3});
      writer.write("time", data);
    }

    // open existing, add data along unlimited dimension
    try (NetcdfFormatUpdater writer = NetcdfFormatUpdater.openExisting(filename).setFill(false).build()) {
      Variable time = writer.findVariable("time");
      assert time.getSize() == 4 : time.getSize();

      Array data = Array.makeFromJavaArray(new int[] {4, 5, 6});
      int[] origin = new int[] {(int) time.getSize()};
      writer.write("time", origin, data);
    }

    // read it back
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Variable vv = ncfile.getRootGroup().findVariableLocal("time");
      assert vv.getSize() == 7 : vv.getSize();

      Array expected = Array.makeArray(DataType.INT, 7, 0, 1);
      Array data = vv.read();
      assert CompareNetcdf2.compareData("time", expected, data);
    }

    // open existing, add more data along unlimited dimension
    try (NetcdfFormatUpdater writer = NetcdfFormatUpdater.openExisting(filename).setFill(false).build()) {
      Variable time = writer.findVariable("time");
      assert time.getSize() == 7 : time.getSize();

      Array data = Array.makeFromJavaArray(new int[] {7, 8});
      int[] origin = new int[] {(int) time.getSize()};
      writer.write("time", origin, data);
    }

    // read it back
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Variable vv = ncfile.getRootGroup().findVariableLocal("time");
      assert vv.getSize() == 9 : vv.getSize();

      Array expected = Array.makeArray(DataType.INT, 9, 0, 1);
      Array data = vv.read();
      assert CompareNetcdf2.compareData("time", expected, data);
    }
  }
}
