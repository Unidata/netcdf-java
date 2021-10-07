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

/** Test successive {@link NetcdfFormatWriter} Writing and updating */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestNetcdfWriteAndUpdate {
  @ClassRule
  public static TemporaryFolder tempFolder = new TemporaryFolder();

  private static String writerLocation;

  @BeforeClass
  public static void setupClass() throws IOException, InvalidRangeException {
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

    // test some errors
    assertThrows(RuntimeException.class, () -> Arrays.factory(ArrayType.OBJECT, new int[] {1}, new Object[1]))
        .getMessage().contains("Unimplemented ArrayType");

    try (NetcdfFormatWriter writer = writerb.build()) {

      int[] shape = new int[] {latDim.getLength(), lonDim.getLength()};
      double[] darray = new double[(int) Arrays.computeSize(shape)];
      int count = 0;
      for (int i = 0; i < latDim.getLength(); i++) {
        for (int j = 0; j < lonDim.getLength(); j++) {
          darray[count++] = (i * 1000000 + j * 1000);
        }
      }
      Array<?> A = Arrays.factory(ArrayType.DOUBLE, shape, darray);
      Variable v = writer.findVariable("temperature");
      writer.write(v, A.getIndex(), A);

      // write char variable
      String val = "Testing 1-2-3";
      char[] carray = new char[val.length()];
      for (int j = 0; j < val.length(); j++) {
        carray[j] = val.charAt(j);
      }
      Array<?> ac = Arrays.factory(ArrayType.CHAR, new int[] {val.length()}, carray);
      v = writer.findVariable("svar");
      writer.write(v, ac.getIndex(), ac);

      // write char variable
      byte[] bdata = new byte[latDim.getLength()];
      int start = -latDim.getLength() / 2;
      for (int j = 0; j < latDim.getLength(); j++) {
        bdata[j] = (byte) (start + j);
      }
      Array<?> ba = Arrays.factory(ArrayType.BYTE, new int[] {bdata.length}, bdata);
      v = writer.findVariable("bvar");
      writer.write(v, ba.getIndex(), ba);

      // write String as CHAR
      v = writer.findVariable("svar2");
      writer.writeStringData(v, Index.ofRank(v.getRank()), "Two pairs of ladies stockings!");

      // write String arrays
      v = writer.findVariable("names");
      Index origin = Index.ofRank(v.getRank());
      writer.writeStringData(v, origin, "No pairs of ladies stockings!");
      writer.writeStringData(v, origin.incr(0), "One pairs of ladies stockings!");
      writer.writeStringData(v, origin.incr(0), "Two pairs of ladies stockings!");

      v = writer.findVariable("names2");
      origin = Index.ofRank(v.getRank());
      writer.writeStringData(v, origin, "0 pairs of ladies stockings!");
      writer.writeStringData(v, origin.incr(0), "1 pairs of ladies stockings!");
      writer.writeStringData(v, origin.incr(0), "2 pairs of ladies stockings!");

      // write scalar data
      v = writer.findVariable("scalar");
      writer.write(v, Index.ofRank(v.getRank()),
          Arrays.factory(ArrayType.DOUBLE, new int[] {1}, new double[] {222.333}));
    }
  }

  @Test
  public void test1ReadBack() throws IOException, InvalidRangeException {
    try (NetcdfFile ncfile = NetcdfFiles.open(writerLocation)) {

      // read entire array
      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();

      Array<Double> tA = (Array<Double>) temp.readArray();
      assertThat(tA.getRank()).isEqualTo(2);

      Index ima = tA.getIndex();
      int[] shape = tA.getShape();
      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          assertThat(tA.get(ima.set(i, j))).isEqualTo(i * 1000000 + j * 1000);
        }
      }

      // read part of array
      int[] origin2 = new int[2];
      int[] shape2 = new int[] {1, temp.getShape()[1]};
      tA = (Array<Double>) temp.readArray(new Section(origin2, shape2));
      assertThat(tA.getRank()).isEqualTo(2);
      for (int j = 0; j < shape2[1]; j++) {
        assertThat(tA.get(ima.set(0, j))).isEqualTo(j * 1000);
      }

      // rank reduction
      Array<Double> Areduce = Arrays.reduce(tA);
      Index ima2 = Areduce.getIndex();
      assertThat(Areduce.getRank()).isEqualTo(1);
      for (int j = 0; j < shape2[1]; j++) {
        assertThat(Areduce.get(ima2.set(j))).isEqualTo(j * 1000);
      }

      // read char variable
      Variable c = ncfile.findVariable("svar");
      assertThat(c).isNotNull();
      Array<?> ca = c.readArray();
      assertThat(ca.getArrayType()).isEqualTo(ArrayType.CHAR);
      String sval = Arrays.makeStringFromChar((Array<Byte>) ca);
      assertThat(sval).isEqualTo("Testing 1-2-3");

      // read char variable 2
      Variable c2 = ncfile.findVariable("svar2");
      assertThat(c2).isNotNull();
      Array<?> ca2 = c2.readArray();
      assertThat(ca2.getArrayType()).isEqualTo(ArrayType.CHAR);
      String sval2 = Arrays.makeStringFromChar((Array<Byte>) ca2);
      assertThat(sval2).isEqualTo("Two pairs of ladies stockings!");

      // read String Array
      Variable c3 = ncfile.findVariable("names");
      assertThat(c3).isNotNull();
      Array<?> ca3 = c3.readArray();
      assertThat(ca3.getArrayType()).isEqualTo(ArrayType.CHAR);
      Array<String> sval3 = Arrays.makeStringsFromChar((Array<Byte>) ca3);
      ima = sval3.getIndex();
      assertThat(sval3.get(ima.set0(0))).isEqualTo("No pairs of ladies stockings!");
      assertThat(sval3.get(ima.set0(1))).isEqualTo("One pairs of ladies stockings!");
      assertThat(sval3.get(ima.set0(2))).isEqualTo("Two pairs of ladies stockings!");

      // read String Array - 2
      Variable c4 = ncfile.findVariable("names2");
      assertThat(c4).isNotNull();
      Array<?> ca4 = c4.readArray();
      assertThat(ca4.getArrayType()).isEqualTo(ArrayType.CHAR);
      Array<String> sval4 = Arrays.makeStringsFromChar((Array<Byte>) ca4);
      assertThat(sval4.get(0)).isEqualTo("0 pairs of ladies stockings!");
      assertThat(sval4.get(1)).isEqualTo("1 pairs of ladies stockings!");
      assertThat(sval4.get(2)).isEqualTo("2 pairs of ladies stockings!");
    }
  }

  @Test
  public void test2ExistingWrite() throws IOException, InvalidRangeException {
    try (NetcdfFormatWriter writer = NetcdfFormatWriter.openExisting(writerLocation).build()) {
      Variable v = writer.findVariable("temperature");
      assertThat(v).isNotNull();
      int[] shape = v.getShape();
      double[] parray = new double[(int) Arrays.computeSize(shape)];
      int count = 0;
      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          parray[count++] = i * 1000000 + j * 1000;
        }
      }
      writer.write(v, Index.ofRank(v.getRank()), Arrays.factory(ArrayType.DOUBLE, shape, parray));

      // write char variable
      v = writer.findVariable("svar");
      assertThat(v).isNotNull();
      shape = v.getShape();
      String val = "Testing 1-2-3";
      char[] carray = new char[(int) v.getSize()];
      for (int j = 0; j < val.length(); j++) {
        carray[j] = val.charAt(j);
      }
      writer.write(v, Index.ofRank(v.getRank()), Arrays.factory(ArrayType.CHAR, shape, carray));

      // write byte variable
      v = writer.findVariable("bvar");
      assertThat(v).isNotNull();
      shape = v.getShape();
      int len = shape[0];
      byte[] barray = new byte[len];
      int start = -len / 2;
      for (int j = 0; j < len; j++) {
        barray[j] = (byte) (start + j);
      }
      writer.write(v, Index.ofRank(v.getRank()), Arrays.factory(ArrayType.BYTE, shape, barray));

      // write char variable as String
      v = writer.findVariable("svar2");
      assertThat(v).isNotNull();
      writer.writeStringData(v, Index.ofRank(v.getRank()), "Two pairs of ladies stockings!");

      // write char variable using Array<String>
      v = writer.findVariable("names");
      assertThat(v).isNotNull();
      shape = new int[] {v.getDimension(0).getLength()};
      Array<String> data = Arrays.factory(ArrayType.STRING, shape, new String[] {"No pairs of ladies stockings!",
          "One pairs of ladies stockings!", "Two pairs of ladies stockings!"});
      writer.writeStringData(v, Index.ofRank(v.getRank()), data);

      // write char variable using multiple calls
      v = writer.findVariable("names2");
      assertThat(v).isNotNull();
      Index origin = Index.ofRank(v.getRank());
      writer.writeStringData(v, origin, "0 pairs of ladies stockings!");
      writer.writeStringData(v, origin.incr(0), "1 pairs of ladies stockings!");
      writer.writeStringData(v, origin.incr(0), "2 pairs of ladies stockings!");

      // write scalar data
      v = writer.findVariable("scalar");
      writer.write(v, Index.ofRank(v.getRank()),
          Arrays.factory(ArrayType.DOUBLE, new int[] {1}, new double[] {222.333}));
    }
  }

  // test reading after closing the file
  @Test
  public void test3ReadExisting() throws IOException, InvalidRangeException {
    try (NetcdfFile ncfile = NetcdfFiles.open(writerLocation)) {
      // read entire array
      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();

      Array<Double> tA = (Array<Double>) temp.readArray();
      assertThat(tA.getRank()).isEqualTo(2);

      Index ima = tA.getIndex();
      int[] shape = tA.getShape();
      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          assertThat(tA.get(ima.set(i, j))).isEqualTo(i * 1000000 + j * 1000);
        }
      }

      // read part of array
      int[] origin2 = new int[2];
      int[] shape2 = new int[2];
      shape2[0] = 1;
      shape2[1] = temp.getShape()[1];
      tA = (Array<Double>) temp.readArray(new Section(origin2, shape2));
      assertThat(tA.getRank()).isEqualTo(2);
      for (int j = 0; j < shape2[1]; j++) {
        assertThat(tA.get(ima.set(0, j))).isEqualTo(j * 1000);
      }

      // rank reduction
      Array<Double> Areduce = Arrays.reduce(tA);
      Index ima2 = Areduce.getIndex();
      assertThat(Areduce.getRank()).isEqualTo(1);
      for (int j = 0; j < shape2[1]; j++) {
        assertThat(Areduce.get(ima2.set(j))).isEqualTo(j * 1000);
      }

      // read char variable
      Variable c = ncfile.findVariable("svar");
      assertThat(c).isNotNull();
      Array<Byte> achar = (Array<Byte>) c.readArray();
      assertThat(achar.getArrayType()).isEqualTo(ArrayType.CHAR);
      String sval = Arrays.makeStringFromChar(achar);
      assertThat(sval).isEqualTo("Testing 1-2-3");

      // read char variable 2
      Variable c2 = ncfile.findVariable("svar2");
      assertThat(c2).isNotNull();
      achar = (Array<Byte>) c2.readArray();
      assertThat(achar.getArrayType()).isEqualTo(ArrayType.CHAR);
      String sval2 = Arrays.makeStringFromChar(achar);
      assertThat(sval2).isEqualTo("Two pairs of ladies stockings!");

      // read String Array
      Variable c3 = ncfile.findVariable("names");
      assertThat(c3).isNotNull();
      achar = (Array<Byte>) c3.readArray();
      assertThat(achar.getArrayType()).isEqualTo(ArrayType.CHAR);
      Array<String> sval3 = Arrays.makeStringsFromChar(achar);
      assertThat(sval3.get(0)).isEqualTo("No pairs of ladies stockings!");
      assertThat(sval3.get(1)).isEqualTo("One pairs of ladies stockings!");
      assertThat(sval3.get(2)).isEqualTo("Two pairs of ladies stockings!");

      // read String Array - 2
      Variable c4 = ncfile.findVariable("names2");
      assertThat(c4).isNotNull();
      achar = (Array<Byte>) c4.readArray();
      assertThat(achar.getArrayType()).isEqualTo(ArrayType.CHAR);
      Array<String> sval4 = Arrays.makeStringsFromChar(achar);
      assertThat(sval4.get(0)).isEqualTo("0 pairs of ladies stockings!");
      assertThat(sval4.get(1)).isEqualTo("1 pairs of ladies stockings!");
      assertThat(sval4.get(2)).isEqualTo("2 pairs of ladies stockings!");
    }
  }
}
