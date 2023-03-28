/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import static com.google.common.truth.Truth.assertThat;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;

/** Test NetcdfFormatWriter */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestWrite {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @ClassRule
  public static TemporaryFolder tempFolder = new TemporaryFolder();

  private static String writerLocation;

  @BeforeClass
  public static void setupClass() throws IOException {
    writerLocation = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(writerLocation);

    // add dimensions
    Dimension latDim = writerb.addDimension("lat", 64);
    Dimension lonDim = writerb.addDimension("lon", 128);

    // add Variable double temperature(lat,lon)
    List<Dimension> dims = new ArrayList<>();
    dims.add(latDim);
    dims.add(lonDim);
    writerb.addVariable("temperature", DataType.DOUBLE, dims).addAttribute(new Attribute("units", "K")) // add a 1D
                                                                                                        // attribute of
                                                                                                        // length 3
        .addAttribute(new Attribute("scale", Array.factory(DataType.INT, new int[] {3}, new int[] {1, 2, 3})));

    // add a string-valued variable: char svar(80)
    Dimension svar_len = writerb.addDimension("svar_len", 80);
    writerb.addVariable("svar", DataType.CHAR, "svar_len");
    writerb.addVariable("svar2", DataType.CHAR, "svar_len");

    // add a 2D string-valued variable: char names(names, 80)
    Dimension names = writerb.addDimension("names", 3);
    writerb.addVariable("names", DataType.CHAR, "names svar_len");
    writerb.addVariable("names2", DataType.CHAR, "names svar_len");

    // add a scalar variable
    writerb.addVariable("scalar", DataType.DOUBLE, new ArrayList<>());

    // signed byte
    writerb.addVariable("bvar", DataType.BYTE, "lat");

    // add global attributes
    writerb.addAttribute(new Attribute("yo", "face"));
    writerb.addAttribute(new Attribute("versionD", 1.2));
    writerb.addAttribute(new Attribute("versionF", (float) 1.2));
    writerb.addAttribute(new Attribute("versionI", 1));
    writerb.addAttribute(new Attribute("versionS", (short) 2));
    writerb.addAttribute(new Attribute("versionB", (byte) 3));
    writerb.addAttribute(new Attribute(CDM.NCPROPERTIES, "test internal attribute is removed when writing"));

    // test some errors
    try {
      Array bad = Array.makeObjectArray(DataType.OBJECT, ArrayList.class, new int[] {1}, null);
      writerb.addAttribute(new Attribute("versionB", bad));
      assert (false);
    } catch (IllegalArgumentException e) {
      assert (true);
    }

    try (NetcdfFormatWriter writer = writerb.build()) {
      // write some data
      ArrayDouble A = new ArrayDouble.D2(latDim.getLength(), lonDim.getLength());
      int i, j;
      Index ima = A.getIndex();
      // write
      for (i = 0; i < latDim.getLength(); i++) {
        for (j = 0; j < lonDim.getLength(); j++) {
          A.setDouble(ima.set(i, j), (i * 1000000 + j * 1000));
        }
      }

      int[] origin = new int[2];
      Variable v = writer.findVariable("temperature");
      try {
        writer.write(v, origin, A);
      } catch (IOException e) {
        System.err.println("ERROR writing file");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      // write char variable
      int[] origin1 = new int[1];
      ArrayChar ac = new ArrayChar.D1(svar_len.getLength());
      ima = ac.getIndex();
      String val = "Testing 1-2-3";
      for (j = 0; j < val.length(); j++) {
        ac.setChar(ima.set(j), val.charAt(j));
      }

      v = writer.findVariable("svar");
      try {
        writer.write(v, origin1, ac);
      } catch (IOException e) {
        System.err.println("ERROR writing Achar");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      // write char variable
      ArrayByte.D1 barray = new ArrayByte.D1(latDim.getLength(), false);
      int start = -latDim.getLength() / 2;
      for (j = 0; j < latDim.getLength(); j++) {
        barray.setByte(j, (byte) (start + j));
      }

      v = writer.findVariable("bvar");
      try {
        writer.write(v, barray);
      } catch (IOException e) {
        System.err.println("ERROR writing bvar");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      // write char variable as String
      try {
        ArrayChar ac2 = new ArrayChar.D1(svar_len.getLength());
        ac2.setString("Two pairs of ladies stockings!");
        v = writer.findVariable("svar2");
        writer.write(v, origin1, ac2);
      } catch (IOException e) {
        System.err.println("ERROR writing Achar2");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      // write String array
      try {
        ArrayChar ac2 = new ArrayChar.D2(names.getLength(), svar_len.getLength());
        ima = ac2.getIndex();
        ac2.setString(ima.set(0), "No pairs of ladies stockings!");
        ac2.setString(ima.set(1), "One pair of ladies stockings!");
        ac2.setString(ima.set(2), "Two pairs of ladies stockings!");
        v = writer.findVariable("names");
        writer.write(v, origin, ac2);
      } catch (IOException e) {
        System.err.println("ERROR writing Achar3");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      // write String array
      try {
        ArrayChar ac2 = new ArrayChar.D2(names.getLength(), svar_len.getLength());
        ac2.setString(0, "0 pairs of ladies stockings!");
        ac2.setString(1, "1 pair of ladies stockings!");
        ac2.setString(2, "2 pairs of ladies stockings!");
        v = writer.findVariable("names2");
        writer.write(v, origin, ac2);
      } catch (IOException e) {
        System.err.println("ERROR writing Achar4");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      // write scalar data
      // write String array
      try {
        ArrayDouble.D0 datas = new ArrayDouble.D0();
        datas.set(222.333);
        v = writer.findVariable("scalar");
        writer.write(v, datas);
      } catch (IOException e) {
        System.err.println("ERROR writing scalar");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }
    }
  }

  @Test
  public void shouldRemoveInternalAttribute() throws IOException {
    try (NetcdfFile netcdfFile = NetcdfFiles.open(writerLocation)) {
      assertThat(netcdfFile.findGlobalAttributeIgnoreCase(CDM.NCPROPERTIES)).isNull();
    }
  }

  @Test
  public void testReadBack() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(writerLocation)) {

      // read entire array
      Variable temp = ncfile.findVariable("temperature");
      assert (null != temp);

      Array tA = temp.read();
      assert (tA.getRank() == 2);

      Index ima = tA.getIndex();
      int[] shape = tA.getShape();

      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          assert (tA.getDouble(ima.set(i, j)) == (double) (i * 1000000 + j * 1000));
        }
      }

      // read part of array
      int[] origin2 = new int[2];
      int[] shape2 = new int[2];
      shape2[0] = 1;
      shape2[1] = temp.getShape()[1];
      try {
        tA = temp.read(origin2, shape2);
      } catch (InvalidRangeException e) {
        System.err.println("ERROR reading file " + e);
        assert (false);
        return;
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assert (false);
        return;
      }
      assert (tA.getRank() == 2);

      for (int j = 0; j < shape2[1]; j++) {
        assert (tA.getDouble(ima.set(0, j)) == (double) (j * 1000));
      }

      // rank reduction
      Array Areduce = tA.reduce();
      Index ima2 = Areduce.getIndex();
      assert (Areduce.getRank() == 1);

      for (int j = 0; j < shape2[1]; j++) {
        assert (Areduce.getDouble(ima2.set(j)) == (double) (j * 1000));
      }

      // read char variable
      Variable c = ncfile.findVariable("svar");
      Assert.assertNotNull(c);
      try {
        tA = c.read();
      } catch (IOException e) {
        assert (false);
      }
      assert (tA instanceof ArrayChar);
      ArrayChar achar = (ArrayChar) tA;
      String sval = achar.getString(achar.getIndex());
      assert sval.equals("Testing 1-2-3") : sval;
      // System.out.println( "val = "+ val);

      // read char variable 2
      Variable c2 = ncfile.findVariable("svar2");
      Assert.assertNotNull(c2);
      try {
        tA = c2.read();
      } catch (IOException e) {
        assert (false);
      }
      assert (tA instanceof ArrayChar);
      ArrayChar ac2 = (ArrayChar) tA;
      Assert.assertEquals("Two pairs of ladies stockings!", ac2.getString());

      // read String Array
      Variable c3 = ncfile.findVariable("names");
      Assert.assertNotNull(c3);
      try {
        tA = c3.read();
      } catch (IOException e) {
        assert (false);
      }
      assert (tA instanceof ArrayChar);
      ArrayChar ac3 = (ArrayChar) tA;
      ima = ac3.getIndex();

      assert (ac3.getString(ima.set(0)).equals("No pairs of ladies stockings!"));
      assert (ac3.getString(ima.set(1)).equals("One pair of ladies stockings!"));
      assert (ac3.getString(ima.set(2)).equals("Two pairs of ladies stockings!"));

      // read String Array - 2
      Variable c4 = ncfile.findVariable("names2");
      Assert.assertNotNull(c4);
      try {
        tA = c4.read();
      } catch (IOException e) {
        assert (false);
      }
      assert (tA instanceof ArrayChar);
      ArrayChar ac4 = (ArrayChar) tA;

      assert (ac4.getString(0).equals("0 pairs of ladies stockings!"));
      assert (ac4.getString(1).equals("1 pair of ladies stockings!"));
      assert (ac4.getString(2).equals("2 pairs of ladies stockings!"));
    }
  }

  @Test
  public void testNC3WriteExisting() throws IOException, InvalidRangeException {
    try (NetcdfFormatWriter writer = NetcdfFormatWriter.openExisting(writerLocation).build()) {
      Variable v = writer.findVariable("temperature");
      Assert.assertNotNull(v);
      int[] shape = v.getShape();
      ArrayDouble A = new ArrayDouble.D2(shape[0], shape[1]);
      int i, j;
      Index ima = A.getIndex();
      for (i = 0; i < shape[0]; i++) {
        for (j = 0; j < shape[1]; j++) {
          A.setDouble(ima.set(i, j), i * 1000000 + j * 1000);
        }
      }

      int[] origin = new int[2];
      try {
        writer.write(v, origin, A);
      } catch (IOException e) {
        System.err.println("ERROR writing file");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      // write char variable
      v = writer.findVariable("svar");
      Assert.assertNotNull(v);
      shape = v.getShape();
      int[] origin1 = new int[1];
      ArrayChar ac = new ArrayChar.D1(shape[0]);
      ima = ac.getIndex();
      String val = "Testing 1-2-3";
      for (j = 0; j < val.length(); j++) {
        ac.setChar(ima.set(j), val.charAt(j));
      }

      try {
        writer.write(v, origin1, ac);
      } catch (IOException e) {
        System.err.println("ERROR writing Achar");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      // write char variable
      v = writer.findVariable("bvar");
      Assert.assertNotNull(v);
      shape = v.getShape();
      int len = shape[0];
      ArrayByte.D1 barray = new ArrayByte.D1(len, false);
      int start = -len / 2;
      for (j = 0; j < len; j++) {
        barray.setByte(j, (byte) (start + j));
      }

      try {
        writer.write("bvar", barray);
      } catch (IOException e) {
        System.err.println("ERROR writing bvar");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      // write char variable as String
      v = writer.findVariable("svar2");
      Assert.assertNotNull(v);
      shape = v.getShape();
      len = shape[0];
      try {
        ArrayChar ac2 = new ArrayChar.D1(len);
        ac2.setString("Two pairs of ladies stockings!");
        writer.write(v, ac2);
      } catch (IOException e) {
        System.err.println("ERROR writing Achar2");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }

      // write char variable using writeStringDataToChar
      v = writer.findVariable("names");
      Assert.assertNotNull(v);
      shape = new int[] {v.getDimension(0).getLength()};
      Array data = Array.factory(DataType.STRING, shape);
      ima = data.getIndex();
      data.setObject(ima.set(0), "No pairs of ladies stockings!");
      data.setObject(ima.set(1), "One pair of ladies stockings!");
      data.setObject(ima.set(2), "Two pairs of ladies stockings!");
      writer.writeStringDataToChar(v, origin, data);

      // write another String array
      v = writer.findVariable("names2");
      Assert.assertNotNull(v);
      shape = v.getShape();
      ArrayChar ac2 = new ArrayChar.D2(shape[0], shape[1]);
      ac2.setString(0, "0 pairs of ladies stockings!");
      ac2.setString(1, "1 pair of ladies stockings!");
      ac2.setString(2, "2 pairs of ladies stockings!");
      writer.write(v, origin, ac2);

      // write scalar data
      try {
        ArrayDouble.D0 datas = new ArrayDouble.D0();
        datas.set(222.333);
        v = writer.findVariable("scalar");
        Assert.assertNotNull(v);
        writer.write(v, datas);
      } catch (IOException e) {
        System.err.println("ERROR writing scalar");
        assert (false);
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        assert (false);
      }
    }
  }

  // test reading after closing the file
  @Test
  public void testNC3ReadExisting() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(writerLocation)) {
      // read entire array
      Variable temp = ncfile.findVariable("temperature");
      assert (null != temp);

      Array tA = temp.read();
      assert (tA.getRank() == 2);

      Index ima = tA.getIndex();
      int[] shape = tA.getShape();

      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          assert (tA.getDouble(ima.set(i, j)) == (double) (i * 1000000 + j * 1000));
        }
      }

      // read part of array
      int[] origin2 = new int[2];
      int[] shape2 = new int[2];
      shape2[0] = 1;
      shape2[1] = temp.getShape()[1];
      try {
        tA = temp.read(origin2, shape2);
      } catch (InvalidRangeException e) {
        System.err.println("ERROR reading file " + e);
        assert (false);
        return;
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assert (false);
        return;
      }
      assert (tA.getRank() == 2);

      for (int j = 0; j < shape2[1]; j++) {
        assert (tA.getDouble(ima.set(0, j)) == (double) (j * 1000));
      }

      // rank reduction
      Array Areduce = tA.reduce();
      Index ima2 = Areduce.getIndex();
      assert (Areduce.getRank() == 1);

      for (int j = 0; j < shape2[1]; j++) {
        assert (Areduce.getDouble(ima2.set(j)) == (double) (j * 1000));
      }

      // read char variable
      Variable c = ncfile.findVariable("svar");
      Assert.assertNotNull(c);
      try {
        tA = c.read();
      } catch (IOException e) {
        assert (false);
      }
      assert (tA instanceof ArrayChar);
      ArrayChar achar = (ArrayChar) tA;
      String sval = achar.getString(achar.getIndex());
      assert sval.equals("Testing 1-2-3") : sval;
      // System.out.println( "val = "+ val);

      // read char variable 2
      Variable c2 = ncfile.findVariable("svar2");
      Assert.assertNotNull(c2);
      try {
        tA = c2.read();
      } catch (IOException e) {
        assert (false);
      }
      assert (tA instanceof ArrayChar);
      ArrayChar ac2 = (ArrayChar) tA;
      assert (ac2.getString().equals("Two pairs of ladies stockings!"));

      // read String Array
      Variable c3 = ncfile.findVariable("names");
      Assert.assertNotNull(c3);
      try {
        tA = c3.read();
      } catch (IOException e) {
        assert (false);
      }
      assert (tA instanceof ArrayChar);
      ArrayChar ac3 = (ArrayChar) tA;
      ima = ac3.getIndex();

      assert (ac3.getString(ima.set(0)).equals("No pairs of ladies stockings!"));
      assert (ac3.getString(ima.set(1)).equals("One pair of ladies stockings!"));
      assert (ac3.getString(ima.set(2)).equals("Two pairs of ladies stockings!"));

      // read String Array - 2
      Variable c4 = ncfile.findVariable("names2");
      Assert.assertNotNull(c4);
      try {
        tA = c4.read();
      } catch (IOException e) {
        assert (false);
      }
      assert (tA instanceof ArrayChar);
      ArrayChar ac4 = (ArrayChar) tA;

      assert (ac4.getString(0).equals("0 pairs of ladies stockings!"));
      assert (ac4.getString(1).equals("1 pair of ladies stockings!"));
      assert (ac4.getString(2).equals("2 pairs of ladies stockings!"));
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
    writerb.addDimension(Dimension.builder().setName("time").setIsUnlimited(true).build());
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
