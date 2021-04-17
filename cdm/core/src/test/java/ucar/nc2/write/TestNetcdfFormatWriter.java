/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import com.google.common.collect.ImmutableList;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import ucar.ma2.*;
import java.io.IOException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.internal.util.CompareNetcdf2;

/** Test NetcdfFormatWriter */
public class TestNetcdfFormatWriter {
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  /*
   * byte Band1(y, x);
   * > Band1:_Unsigned = "true";
   * > Band1:_FillValue = -1b; // byte
   * >
   * > byte Band2(y, x);
   * > Band2:_Unsigned = "true";
   * > Band2:valid_range = 0s, 254s; // short
   */
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
  public void testWriteRecordOneAtaTime() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(filename);
    // define dimensions, including unlimited
    Dimension latDim = writerb.addDimension("lat", 3);
    Dimension lonDim = writerb.addDimension("lon", 4);
    writerb.addDimension(Dimension.builder().setName("time").setIsUnlimited(true).build());

    // define Variables
    writerb.addVariable("lat", DataType.FLOAT, "lat").addAttribute(new Attribute("units", "degrees_north"));
    writerb.addVariable("lon", DataType.FLOAT, "lon").addAttribute(new Attribute("units", "degrees_east"));
    writerb.addVariable("rh", DataType.INT, "time lat lon")
        .addAttribute(new Attribute("long_name", "relative humidity")).addAttribute(new Attribute("units", "percent"));
    writerb.addVariable("T", DataType.DOUBLE, "time lat lon")
        .addAttribute(new Attribute("long_name", "surface temperature")).addAttribute(new Attribute("units", "degC"));
    writerb.addVariable("time", DataType.INT, "time").addAttribute(new Attribute("units", "hours since 1990-01-01"));

    try (NetcdfFormatWriter writer = writerb.build()) {
      // write out the non-record variables
      writer.write("lat", Array.makeFromJavaArray(new float[] {41, 40, 39}, false));
      writer.write("lon", Array.makeFromJavaArray(new float[] {-109, -107, -105, -103}, false));

      //// heres where we write the record variables
      // different ways to create the data arrays.
      // Note the outer dimension has shape 1, since we will write one record at a time
      ArrayInt rhData = new ArrayInt.D3(1, latDim.getLength(), lonDim.getLength(), false);
      ArrayDouble.D3 tempData = new ArrayDouble.D3(1, latDim.getLength(), lonDim.getLength());
      Array timeData = Array.factory(DataType.INT, new int[] {1});
      Index ima = rhData.getIndex();

      int[] origin = new int[] {0, 0, 0};
      int[] time_origin = new int[] {0};

      // loop over each record
      for (int timeIdx = 0; timeIdx < 10; timeIdx++) {
        // make up some data for this record, using different ways to fill the data arrays.
        timeData.setInt(timeData.getIndex(), timeIdx * 12);

        for (int latIdx = 0; latIdx < latDim.getLength(); latIdx++) {
          for (int lonIdx = 0; lonIdx < lonDim.getLength(); lonIdx++) {
            rhData.setInt(ima.set(0, latIdx, lonIdx), timeIdx * latIdx * lonIdx);
            tempData.set(0, latIdx, lonIdx, timeIdx * latIdx * lonIdx / 3.14159);
          }
        }
        // write the data out for one record
        // set the origin here
        time_origin[0] = timeIdx;
        origin[0] = timeIdx;
        writer.write("rh", origin, rhData);
        writer.write("T", origin, tempData);
        writer.write("time", time_origin, timeData);
      } // loop over record
    }
  }

  // fix for bug introduced 2/9/10, reported by Christian Ward-Garrison cwardgar@usgs.gov
  @Test
  public void testRecordSizeBug() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();
    int size = 10;
    Array timeDataAll = Array.factory(DataType.INT, new int[] {size});

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(filename).setFill(false);
    writerb.addUnlimitedDimension("time");
    writerb.addVariable("time", DataType.INT, "time").addAttribute(new Attribute("units", "hours since 1990-01-01"));

    try (NetcdfFormatWriter writer = writerb.build()) {
      IndexIterator iter = timeDataAll.getIndexIterator();
      Array timeData = Array.factory(DataType.INT, new int[] {1});
      int[] time_origin = new int[] {0};

      for (int time = 0; time < size; time++) {
        int val = time * 12;
        iter.setIntNext(val);
        timeData.setInt(timeData.getIndex(), val);
        time_origin[0] = time;
        writer.write("time", time_origin, timeData);
      }
    }

    try (NetcdfFile ncFile = NetcdfFiles.open(filename)) {
      Array result = ncFile.readSection("time");
      Assert.assertEquals("0 12 24 36 48 60 72 84 96 108", result.toString().trim());
    }
  }
}
