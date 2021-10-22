/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.Index;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

/** Test successive {@link NetcdfFormatWriter} Writing and updating */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestNetcdfWriteArrayAndUpdate {
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
    writerb.addVariable("temperature", ArrayType.DOUBLE, dims).addAttribute(new Attribute("units", "K"))
        .addAttribute(Attribute.fromArray("scale", Arrays.factory(ArrayType.INT, new int[] {3}, new int[] {1, 2, 3})));

    // add a string-valued variable: char svar(80)
    Dimension svar_len = writerb.addDimension("svar_len", 80);
    writerb.addVariable("svar", ArrayType.CHAR, "svar_len");
    writerb.addVariable("svar2", ArrayType.CHAR, "svar_len");

    // add a 2D string-valued variable: char names(names, 80)
    Dimension names = writerb.addDimension("names", 3);
    writerb.addVariable("names", ArrayType.CHAR, "names svar_len");
    writerb.addVariable("names2", ArrayType.CHAR, "names svar_len");

    // add a scalar variable
    writerb.addVariable("scalar", ArrayType.DOUBLE, new ArrayList<>());

    // signed byte
    writerb.addVariable("bvar", ArrayType.BYTE, "lat");

    // add global attributes
    writerb.addAttribute(new Attribute("yo", "face"));
    writerb.addAttribute(new Attribute("versionD", 1.2));
    writerb.addAttribute(new Attribute("versionF", (float) 1.2));
    writerb.addAttribute(new Attribute("versionI", 1));
    writerb.addAttribute(new Attribute("versionS", (short) 2));
    writerb.addAttribute(new Attribute("versionB", (byte) 3));

    try (NetcdfFormatWriter writer = writerb.build()) {
      // write double variable
      double[] A = new double[latDim.getLength() * lonDim.getLength()];
      int count = 0;
      for (int j = 0; j < latDim.getLength(); j++) {
        for (int i = 0; i < lonDim.getLength(); i++) {
          A[count++] = (j * 1000000 + i * 1000);
        }
      }

      Variable v = writer.findVariable("temperature");
      try {
        writer.config().forVariable(v).withPrimitiveArray(A).withShape(latDim.getLength(), lonDim.getLength()).write();
      } catch (IOException e) {
        System.err.println("ERROR writing file");
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
      }

      // write char variable
      char[] ac = new char[svar_len.getLength()];
      String val = "Testing 1-2-3";
      count = 0;
      for (int j = 0; j < val.length(); j++) {
        ac[count++] = val.charAt(j);
      }

      v = writer.findVariable("svar");
      assertThat(v).isNotNull();
      try {
        writer.config().forVariable(v).withPrimitiveArray(ac).withShape(svar_len.getLength()).write();
      } catch (IOException e) {
        System.err.println("ERROR writing Achar");
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
      }

      // write byte variable
      Variable bvar = writer.findVariable("bvar");
      assertThat(bvar).isNotNull();
      byte[] barray = new byte[latDim.getLength()];
      long start = -bvar.getSize() / 2;
      count = 0;
      for (int j = 0; j < latDim.getLength(); j++) {
        barray[count++] = (byte) (start + j);
      }

      try {
        writer.config().forVariable(bvar).withPrimitiveArray(barray).write();
      } catch (IOException e) {
        System.err.println("ERROR writing bvar");
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
      }

      // write char variable as String
      try {
        v = writer.findVariable("svar2");
        assertThat(v).isNotNull();
        writer.config().forVariable(v).withString("Two pairs of ladies stockings!").write();
      } catch (IOException e) {
        System.err.println("ERROR writing svar2");
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
      }

      // write String array
      try {
        v = writer.findVariable("names");
        assertThat(v).isNotNull();
        Index origin = Index.ofRank(v.getRank());
        writer.config().forVariable(v).withOrigin(origin).withString("No pairs of ladies stockings!").write();
        writer.config().forVariable(v).withOrigin(origin.incr(0)).withString("One pairs of ladies stockings!").write();
        writer.config().forVariable(v).withOrigin(origin.incr(0)).withString("Two pairs of ladies stockings!").write();
      } catch (IOException e) {
        System.err.println("ERROR writing Achar3");
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
      }

      // write scalar data
      try {
        double[] data = new double[] {222.333};
        v = writer.findVariable("scalar");
        writer.config().forVariable(v).withPrimitiveArray(data).write();
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
  public void test1ReadBack() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(writerLocation)) {

      // read entire array
      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();

      Array<Double> tA = (Array<Double>) temp.readArray();
      assertThat(tA.getRank()).isEqualTo(2);

      int count = 0;
      int lenx = temp.getShape(temp.getRank() - 1);
      for (double val : tA) {
        int i = count % lenx;
        int j = count / lenx;
        assertThat(val).isEqualTo(j * 1000000 + i * 1000);
        count++;
      }

      // read part of array
      int[] origin2 = new int[2];
      int[] shape2 = new int[2];
      shape2[0] = 1;
      shape2[1] = temp.getShape()[1];
      try {
        tA = (Array<Double>) temp.readArray(new Section(origin2, shape2));
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

      Index idx = tA.getIndex();
      for (int j = 0; j < shape2[1]; j++) {
        assertThat(tA.get(idx.set(0, j))).isEqualTo(j * 1000);
      }

      // rank reduction
      Array<?> Areduce = Arrays.reduce(tA);
      Index ima2 = Areduce.getIndex();
      assertThat(Areduce.getRank()).isEqualTo(1);
      for (int j = 0; j < shape2[1]; j++) {
        assertThat(Areduce.get(ima2.set(j))).isEqualTo(j * 1000);
      }

      // read char variable
      Variable c = ncfile.findVariable("svar");
      assertThat(c).isNotNull();
      Array<?> a = c.readArray();
      assertThat(a.getArrayType()).isEqualTo(ArrayType.CHAR);
      Array<Byte> achar = (Array<Byte>) a;
      String sval = Arrays.makeStringFromChar(achar);
      assertThat(sval).isEqualTo("Testing 1-2-3");

      // read char variable 2
      Variable c2 = ncfile.findVariable("svar2");
      assertThat(c2).isNotNull();
      a = c2.readArray();

      assertThat(a.getArrayType()).isEqualTo(ArrayType.CHAR);
      Array<Byte> achar2 = (Array<Byte>) a;
      Array<String> achar2Data = Arrays.makeStringsFromChar(achar2);
      assertThat(achar2Data.get(0)).isEqualTo("Two pairs of ladies stockings!");

      // read byte variable
      Variable bvar = ncfile.findVariable("bvar");
      assertThat(bvar).isNotNull();
      Array<Byte> bdata = (Array<Byte>) bvar.readArray();
      long start = -bvar.getSize() / 2;
      count = 0;
      for (byte val : bdata) {
        assertThat(val).isEqualTo(start + count);
        count++;
      }

      // read String Array
      Variable c3 = ncfile.findVariable("names");
      assertThat(c3).isNotNull();
      a = c3.readArray();

      assertThat(a.getArrayType()).isEqualTo(ArrayType.CHAR);
      Array<Byte> achar3 = (Array<Byte>) a;
      Array<String> achar3Data = Arrays.makeStringsFromChar(achar3);

      assertThat(achar3Data.get(0)).isEqualTo("No pairs of ladies stockings!");
      assertThat(achar3Data.get(1)).isEqualTo("One pairs of ladies stockings!");
      assertThat(achar3Data.get(2)).isEqualTo("Two pairs of ladies stockings!");

      // read String Array - 2
      Variable c4 = ncfile.findVariable("names2");
      assertThat(c4).isNotNull();
      a = c4.readArray();

      assertThat(a.getArrayType()).isEqualTo(ArrayType.CHAR);
      Array<Byte> achar4 = (Array<Byte>) a;
      Array<String> achar4Data = Arrays.makeStringsFromChar(achar4);

      assertThat(achar4Data.get(0).equals("0 pair of ladies stockings!"));
      assertThat(achar4Data.get(1).equals("1 pair of ladies stockings!"));
      assertThat(achar4Data.get(2).equals("2 pair of ladies stockings!"));

      // read scalar data
      Variable ss = ncfile.findVariable("scalar");
      assertThat(ss).isNotNull();
      Array<Double> ssdata = (Array<Double>) ss.readArray();
      assertThat(ssdata.get(0)).isEqualTo(222.333);
    }
  }

  @Test
  public void test2WriteExisting() throws IOException {
    try (NetcdfFormatWriter writer = NetcdfFormatWriter.openExisting(writerLocation).build()) {
      Variable v = writer.findVariable("temperature");
      assertThat(v).isNotNull();
      int[] shape = v.getShape();
      int latDim = shape[0];
      int lonDim = shape[1];

      // write double variable
      double[] A = new double[latDim * lonDim];
      int count = 0;
      for (int j = 0; j < latDim; j++) {
        for (int i = 0; i < lonDim; i++) {
          A[count++] = (j * 1000000 + i * 1001);
        }
      }

      try {
        writer.config().forVariable(v).withPrimitiveArray(A).write();
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
      char[] ac = new char[shape[0]];
      String val = "Testing 1-2-many";
      count = 0;
      for (int j = 0; j < val.length(); j++) {
        ac[count++] = val.charAt(j);
      }

      try {
        writer.config().forVariable(v).withPrimitiveArray(ac).write();
      } catch (IOException e) {
        System.err.println("ERROR writing Achar");
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
      }

      // write byte variable
      v = writer.findVariable("bvar");
      assertThat(v).isNotNull();
      shape = v.getShape();
      int len = shape[0];
      byte[] barray = new byte[len];
      int start = -len / 2 - 1;
      count = 0;
      for (int j = 0; j < len; j++) {
        barray[count++] = (byte) (start + j);
      }

      try {
        writer.config().forVariable(v).withPrimitiveArray(barray).write();
      } catch (IOException e) {
        System.err.println("ERROR writing bvar");
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
      }

      // write char variable as String
      try {
        v = writer.findVariable("svar2");
        assertThat(v).isNotNull();
        writer.writeStringData(v, Index.ofRank(v.getRank()), "Twelve pairs of ladies stockings!");
      } catch (IOException e) {
        System.err.println("ERROR writing svar2");
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
      }

      // write String array
      try {
        v = writer.findVariable("names");
        assertThat(v).isNotNull();
        Index index = Index.ofRank(v.getRank());
        writer.writeStringData(v, index, "0 pairs of ladies stockings!");
        writer.writeStringData(v, index.incr(0), "1 pairs of ladies stockings!");
        writer.writeStringData(v, index.incr(0), "2 pairs of ladies stockings!");
      } catch (IOException e) {
        System.err.println("ERROR writing Achar3");
        fail();
      } catch (InvalidRangeException e) {
        e.printStackTrace();
        fail();
      }

      // write scalar data
      try {
        double[] data = new double[] {23.32};
        v = writer.findVariable("scalar");
        writer.config().forVariable(v).withPrimitiveArray(data).write();
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
  public void test3ReadExisting() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(writerLocation)) {
      // read entire array
      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();

      Array<Double> tA = (Array<Double>) temp.readArray();
      assertThat(tA.getRank()).isEqualTo(2);

      int count = 0;
      int lenx = temp.getShape(temp.getRank() - 1);
      for (double val : tA) {
        int i = count % lenx;
        int j = count / lenx;
        assertThat(val).isEqualTo(j * 1000000 + i * 1001);
        count++;
      }

      // read part of array
      int[] origin2 = new int[2];
      int[] shape2 = new int[2];
      shape2[0] = 1;
      shape2[1] = temp.getShape()[1];
      try {
        tA = (Array<Double>) temp.readArray(new Section(origin2, shape2));
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

      Index idx = tA.getIndex();
      for (int j = 0; j < shape2[1]; j++) {
        assertThat(tA.get(idx.set(0, j))).isEqualTo(j * 1001);
      }

      // rank reduction
      Array Areduce = Arrays.reduce(tA);
      Index ima2 = Areduce.getIndex();
      assertThat(Areduce.getRank()).isEqualTo(1);

      for (int j = 0; j < shape2[1]; j++) {
        assertThat(Areduce.get(ima2.set(j))).isEqualTo(j * 1001);
      }

      // read char variable
      Variable c = ncfile.findVariable("svar");
      assertThat(c).isNotNull();
      Array<?> a = c.readArray();
      assertThat(a.getArrayType()).isEqualTo(ArrayType.CHAR);
      Array<Byte> achar = (Array<Byte>) a;
      String sval = Arrays.makeStringFromChar(achar);
      assertThat(sval).isEqualTo("Testing 1-2-many");

      // read char variable 2
      Variable c2 = ncfile.findVariable("svar2");
      assertThat(c2).isNotNull();
      a = c2.readArray();

      assertThat(a.getArrayType()).isEqualTo(ArrayType.CHAR);
      Array<Byte> achar2 = (Array<Byte>) a;
      Array<String> achar2Data = Arrays.makeStringsFromChar(achar2);
      assertThat(achar2Data.get(0)).isEqualTo("Twelve pairs of ladies stockings!");

      // read byte variable
      Variable bvar = ncfile.findVariable("bvar");
      assertThat(bvar).isNotNull();
      Array<Byte> bdata = (Array<Byte>) bvar.readArray();
      long start = -bvar.getSize() / 2 - 1;
      count = 0;
      for (byte val : bdata) {
        assertThat(val).isEqualTo(start + count);
        count++;
      }

      // read String Array
      Variable c3 = ncfile.findVariable("names");
      assertThat(c3).isNotNull();
      a = c3.readArray();

      assertThat(a.getArrayType()).isEqualTo(ArrayType.CHAR);
      Array<Byte> achar3 = (Array<Byte>) a;
      Array<String> achar3Data = Arrays.makeStringsFromChar(achar3);

      assertThat(achar3Data.get(0)).isEqualTo("0 pairs of ladies stockings!");
      assertThat(achar3Data.get(1)).isEqualTo("1 pairs of ladies stockings!");
      assertThat(achar3Data.get(2)).isEqualTo("2 pairs of ladies stockings!");

      // read String Array - 2
      Variable c4 = ncfile.findVariable("names2");
      assertThat(c4).isNotNull();
      a = c4.readArray();

      assertThat(a.getArrayType()).isEqualTo(ArrayType.CHAR);
      Array<Byte> achar4 = (Array<Byte>) a;
      Array<String> achar4Data = Arrays.makeStringsFromChar(achar4);

      assertThat(achar4Data.get(0).equals("0 pair of ladies stockings!"));
      assertThat(achar4Data.get(1).equals("1 pair of ladies stockings!"));
      assertThat(achar4Data.get(2).equals("2 pair of ladies stockings!"));

      // read scalar data
      Variable ss = ncfile.findVariable("scalar");
      assertThat(ss).isNotNull();
      Array<Double> ssdata = (Array<Double>) ss.readArray();
      assertThat(ssdata.get(0)).isEqualTo(23.32);
    }
  }
}
