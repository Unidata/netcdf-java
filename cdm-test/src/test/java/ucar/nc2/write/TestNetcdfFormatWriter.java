/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.write;

import com.google.common.collect.ImmutableList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import ucar.nc2.util.CompareNetcdf2;

/** NetcdfFormatWriter tests */
public class TestNetcdfFormatWriter {
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
  public void testUnsignedAttribute() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(filename);
    writerb.addUnlimitedDimension("time");

    writerb.addVariable("time", DataType.BYTE, "time").addAttribute(new Attribute(CDM.UNSIGNED, "true"))
        .addAttribute(new Attribute(CDM.SCALE_FACTOR, 10.0))
        .addAttribute(Attribute.builder(CDM.VALID_RANGE).setValues(ImmutableList.of(10, 240), false).build());

    /*
     * byte Band1(y, x);
     * > Band1:_Unsigned = "true";
     * > Band1:_FillValue = -1b; // byte
     */
    writerb.addVariable("Band1", DataType.BYTE, "time").addAttribute(new Attribute(CDM.UNSIGNED, "true"))
        .addAttribute(new Attribute(CDM.FILL_VALUE, (byte) -1)).addAttribute(new Attribute(CDM.SCALE_FACTOR, 1.0));

    /*
     * byte Band2(y, x);
     * > Band2:_Unsigned = "true";
     * > Band2:valid_range = 0s, 254s; // short
     */
    writerb.addVariable("Band2", DataType.BYTE, "time").addAttribute(new Attribute(CDM.UNSIGNED, "true"))
        .addAttribute(new Attribute(CDM.SCALE_FACTOR, 1.0)).addAttribute(
            Attribute.builder(CDM.VALID_RANGE).setValues(ImmutableList.of((short) 0, (short) 254), false).build());

    try (NetcdfFormatWriter writer = writerb.build()) {
      Array timeData = Array.factory(DataType.BYTE, new int[] {1});
      int[] time_origin = new int[] {0};

      for (int time = 0; time < 256; time++) {
        timeData.setInt(timeData.getIndex(), time);
        time_origin[0] = time;
        writer.write("time", time_origin, timeData);
        writer.write("Band1", time_origin, timeData);
        writer.write("Band2", time_origin, timeData);
      }
    }

    Array expected = Array.makeArray(DataType.BYTE, 256, 0, 1);
    try (NetcdfFile ncFile = NetcdfFiles.open(filename)) {
      Array result2 = ncFile.readSection("time");
      CompareNetcdf2.compareData("time", expected, result2);
    }
  }

  // test writing big format
  @Test
  public void testBig() throws IOException, InvalidRangeException {
    long start = System.nanoTime();
    long stop;
    double took;
    String varName = "example";

    int timeSize = 8;
    int latSize = 8022;
    int lonSize = 10627;
    long approxSize = (long) timeSize * latSize * lonSize * 4 + 4000;

    System.out.println("File size  (B)  = " + approxSize);
    System.out.println("File size~ (MB) = " + Math.round(approxSize / Math.pow(2, 20)));

    String fileName = tempFolder.newFile().getAbsolutePath();
    NetcdfFormatWriter.Builder writerb =
        NetcdfFormatWriter.createNewNetcdf3(fileName).setFill(false).setPreallocateSize(approxSize);
    writerb.addUnlimitedDimension("time");
    // fileWriter.setLargeFile(true); TODO is this ok, not set, test succeeds?

    String timeUnits = "hours since 2008-06-06 12:00:0.0";
    String coordUnits = "degrees";

    writerb.addVariable("time", DataType.FLOAT, "time").addAttribute(new Attribute("units", timeUnits));

    setDimension(writerb, "lat", coordUnits, latSize);
    setDimension(writerb, "lon", coordUnits, lonSize);

    writerb.addVariable(varName, DataType.FLOAT, "time lat lon").addAttribute(new Attribute("_FillValue", -9999))
        .addAttribute(new Attribute(CDM.MISSING_VALUE, -9999));

    try (NetcdfFormatWriter writer = writerb.build()) {
      stop = System.nanoTime();
      took = (stop - start) * .001 * .001 * .001;
      System.out.println("That took " + took + " secs");
      start = stop;

      System.out.println("Writing netcdf <=");

      int[] shape = new int[] {1, 1, lonSize};
      float[] floatStorage = new float[lonSize];
      Array floatArray = Array.factory(DataType.FLOAT, shape, floatStorage);
      for (int t = 0; t < timeSize; t++) {
        for (int i = 0; i < latSize; i++) {
          int[] origin = new int[] {t, i, 0};
          writer.write(varName, origin, floatArray);
        }
      }

      stop = System.nanoTime();
      took = (stop - start) * .001 * .001 * .001;
      System.out.println("That took " + took + " secs");
      start = stop;
    }
  }

  private static Dimension setDimension(NetcdfFormatWriter.Builder ncFile, String name, String units, int length) {
    Dimension dimension = ncFile.addDimension(name, length);
    ncFile.addVariable(name, DataType.FLOAT, name).addAttribute(new Attribute("units", units));
    return dimension;
  }

  @Test
  public void testWriteUnlimited() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(filename);
    writerb.addUnlimitedDimension("time");
    writerb.addAttribute(new Attribute("name", "value"));

    // public Variable addVariable(Group g, String shortName, DataType dataType, String dims) {
    writerb.addVariable("time", DataType.DOUBLE, "time");

    // write
    try (NetcdfFormatWriter writer = writerb.build()) {
      Array data = Array.makeFromJavaArray(new double[] {0, 1, 2, 3});
      writer.write("time", data);

      Variable time = writer.findVariable("time"); // ?? immutable ??
      assert time.getSize() == 4 : time.getSize();
    }

    // read it back
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Variable vv = ncfile.getRootGroup().findVariableLocal("time");
      assert vv.getSize() == 4 : vv.getSize();
      Array expected = Array.makeArray(DataType.DOUBLE, 4, 0, 1);
      Array data = vv.read();
      assert CompareNetcdf2.compareData("time", expected, data);
    }
  }

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
    try (NetcdfFormatWriter writer = NetcdfFormatWriter.openExisting(filename).setFill(false).build()) {
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
    try (NetcdfFormatWriter writer = NetcdfFormatWriter.openExisting(filename).setFill(false).build()) {
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
