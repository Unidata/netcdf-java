/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;
import ucar.ma2.Array;
import ucar.ma2.ArrayByte;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/** Test successive NetcdfFormat Writint and updating */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestNetcdfWriteAndUpdate {
  @ClassRule
  public static TemporaryFolder tempFolder = new TemporaryFolder();

  private static String writerLocation;

  @BeforeClass
  public static void setupClass() throws IOException {
    writerLocation = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(writerLocation);

    // add dimensions
    Dimension latDim = writerb.addDimension("lat", 64);
    Dimension lonDim = writerb.addDimension("lon", 128);

    // add Variable double temperature(lat,lon)
    List<Dimension> dims = new ArrayList<>();
    dims.add(latDim);
    dims.add(lonDim);
    // add a 1D attribute of length 3
    writerb.addVariable("temperature", DataType.DOUBLE, dims).addAttribute(new Attribute("units", "K"))
        .addAttribute(Attribute.fromArray("scale", Array.factory(DataType.INT, new int[] {3}, new int[] {1, 2, 3})));

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

    // test some errors
    try {
      Array bad = Array.makeObjectArray(DataType.OBJECT, ArrayList.class, new int[] {1}, null);
      writerb.addAttribute(Attribute.fromArray("versionB", bad));
      fail();
    } catch (Exception e) {
      assertThat(e.getMessage()).contains("Unimplemented ArrayType");
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
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
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
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
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
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
      }

      // write char variable as String
      try {
        ArrayChar ac2 = new ArrayChar.D1(svar_len.getLength());
        ac2.setString("Two pairs of ladies stockings!");
        v = writer.findVariable("svar2");
        writer.write(v, origin1, ac2);
      } catch (IOException e) {
        System.err.println("ERROR writing Achar2");
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
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
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
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
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
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
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
      }
    }
  }

  @Test
  public void testReadBack() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(writerLocation)) {

      // read entire array
      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();

      Array tA = temp.read();
      assertThat(tA.getRank()).isEqualTo(2);

      Index ima = tA.getIndex();
      int[] shape = tA.getShape();

      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          assertThat(tA.getDouble(ima.set(i, j))).isEqualTo(i * 1000000 + j * 1000);
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
        fail();
        return;
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        fail();
        return;
      }
      assertThat(tA.getRank()).isEqualTo(2);

      for (int j = 0; j < shape2[1]; j++) {
        assertThat(tA.getDouble(ima.set(0, j))).isEqualTo(j * 1000);
      }

      // rank reduction
      Array Areduce = tA.reduce();
      Index ima2 = Areduce.getIndex();
      assertThat(Areduce.getRank()).isEqualTo(1);

      for (int j = 0; j < shape2[1]; j++) {
        assertThat(Areduce.getDouble(ima2.set(j))).isEqualTo(j * 1000);
      }

      // read char variable
      Variable c = ncfile.findVariable("svar");
      assertThat(c).isNotNull();
      try {
        tA = c.read();
      } catch (IOException e) {
        fail();
      }
      assertThat(tA).isInstanceOf(ArrayChar.class);
      ArrayChar achar = (ArrayChar) tA;
      String sval = achar.getString(achar.getIndex());
      assertThat(sval).isEqualTo("Testing 1-2-3");
      // System.out.println( "val = "+ val);

      // read char variable 2
      Variable c2 = ncfile.findVariable("svar2");
      assertThat(c2).isNotNull();
      try {
        tA = c2.read();
      } catch (IOException e) {
        fail();
      }
      assertThat(tA).isInstanceOf(ArrayChar.class);
      ArrayChar ac2 = (ArrayChar) tA;
      assertThat(ac2.getString()).isEqualTo("Two pairs of ladies stockings!");

      // read String Array
      Variable c3 = ncfile.findVariable("names");
      assertThat(c3).isNotNull();
      try {
        tA = c3.read();
      } catch (IOException e) {
        fail();
      }
      assertThat(tA).isInstanceOf(ArrayChar.class);
      ArrayChar ac3 = (ArrayChar) tA;
      ima = ac3.getIndex();

      assertThat(ac3.getString(ima.set(0)).equals("No pairs of ladies stockings!"));
      assertThat(ac3.getString(ima.set(1)).equals("One pair of ladies stockings!"));
      assertThat(ac3.getString(ima.set(2)).equals("Two pairs of ladies stockings!"));

      // read String Array - 2
      Variable c4 = ncfile.findVariable("names2");
      assertThat(c4).isNotNull();
      try {
        tA = c4.read();
      } catch (IOException e) {
        fail();
      }
      assertThat(tA).isInstanceOf(ArrayChar.class);
      ArrayChar ac4 = (ArrayChar) tA;

      assertThat(ac4.getString(0).equals("0 pairs of ladies stockings!"));
      assertThat(ac4.getString(1).equals("1 pair of ladies stockings!"));
      assertThat(ac4.getString(2).equals("2 pairs of ladies stockings!"));
    }
  }

  @Test
  public void testNC3WriteExisting() throws IOException, InvalidRangeException {
    try (NetcdfFormatWriter writer = NetcdfFormatWriter.openExisting(writerLocation).build()) {
      Variable v = writer.findVariable("temperature");
      assertThat(v).isNotNull();
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
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
      }

      // write char variable
      v = writer.findVariable("svar");
      assertThat(v).isNotNull();
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
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
      }

      // write char variable
      v = writer.findVariable("bvar");
      assertThat(v);
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
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
      }

      // write char variable as String
      v = writer.findVariable("svar2");
      assertThat(v);
      shape = v.getShape();
      len = shape[0];
      try {
        ArrayChar ac2 = new ArrayChar.D1(len);
        ac2.setString("Two pairs of ladies stockings!");
        writer.write(v, ac2);
      } catch (IOException e) {
        System.err.println("ERROR writing Achar2");
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
      }

      // write char variable using writeStringDataToChar
      v = writer.findVariable("names");
      assertThat(v);
      shape = new int[] {v.getDimension(0).getLength()};
      Array data = Array.factory(DataType.STRING, shape);
      ima = data.getIndex();
      data.setObject(ima.set(0), "No pairs of ladies stockings!");
      data.setObject(ima.set(1), "One pair of ladies stockings!");
      data.setObject(ima.set(2), "Two pairs of ladies stockings!");
      writer.writeStringDataToChar(v, origin, data);

      // write another String array
      v = writer.findVariable("names2");
      assertThat(v);
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
        assertThat(v);
        writer.write(v, datas);
      } catch (IOException e) {
        System.err.println("ERROR writing scalar");
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
      }
    }
  }

  // test reading after closing the file
  @Test
  public void testNC3ReadExisting() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(writerLocation)) {
      // read entire array
      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();

      Array tA = temp.read();
      assertThat(tA.getRank()).isEqualTo(2);

      Index ima = tA.getIndex();
      int[] shape = tA.getShape();

      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          assertThat(tA.getDouble(ima.set(i, j))).isEqualTo(i * 1000000 + j * 1000);
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
        fail();
        return;
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        fail();
        return;
      }
      assertThat(tA.getRank()).isEqualTo(2);

      for (int j = 0; j < shape2[1]; j++) {
        assertThat(tA.getDouble(ima.set(0, j))).isEqualTo(j * 1000);
      }

      // rank reduction
      Array Areduce = tA.reduce();
      Index ima2 = Areduce.getIndex();
      assertThat(Areduce.getRank()).isEqualTo(1);

      for (int j = 0; j < shape2[1]; j++) {
        assertThat(Areduce.getDouble(ima2.set(j))).isEqualTo(j * 1000);
      }

      // read char variable
      Variable c = ncfile.findVariable("svar");
      assertThat(c);
      try {
        tA = c.read();
      } catch (IOException e) {
        fail();
      }
      assertThat(tA).isInstanceOf(ArrayChar.class);
      ArrayChar achar = (ArrayChar) tA;
      String sval = achar.getString(achar.getIndex());
      assertThat(sval).isEqualTo("Testing 1-2-3");
      // System.out.println( "val = "+ val);

      // read char variable 2
      Variable c2 = ncfile.findVariable("svar2");
      assertThat(c2);
      try {
        tA = c2.read();
      } catch (IOException e) {
        fail();
      }
      assertThat(tA).isInstanceOf(ArrayChar.class);
      ArrayChar ac2 = (ArrayChar) tA;
      assertThat(ac2.getString()).isEqualTo("Two pairs of ladies stockings!");

      // read String Array
      Variable c3 = ncfile.findVariable("names");
      assertThat(c3);
      try {
        tA = c3.read();
      } catch (IOException e) {
        fail();
      }
      assertThat(tA).isInstanceOf(ArrayChar.class);
      ArrayChar ac3 = (ArrayChar) tA;
      ima = ac3.getIndex();

      assertThat(ac3.getString(ima.set(0))).isEqualTo("No pairs of ladies stockings!");
      assertThat(ac3.getString(ima.set(1))).isEqualTo("One pair of ladies stockings!");
      assertThat(ac3.getString(ima.set(2))).isEqualTo("Two pairs of ladies stockings!");

      // read String Array - 2
      Variable c4 = ncfile.findVariable("names2");
      assertThat(c4);
      try {
        tA = c4.read();
      } catch (IOException e) {
        fail();
      }
      assertThat(tA).isInstanceOf(ArrayChar.class);
      ArrayChar ac4 = (ArrayChar) tA;

      assertThat(ac4.getString(0)).isEqualTo("0 pairs of ladies stockings!");
      assertThat(ac4.getString(1)).isEqualTo("1 pair of ladies stockings!");
      assertThat(ac4.getString(2)).isEqualTo("2 pairs of ladies stockings!");
    }
  }
}
