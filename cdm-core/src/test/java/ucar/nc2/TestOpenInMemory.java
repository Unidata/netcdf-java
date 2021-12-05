/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.Index;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.util.IO;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class TestOpenInMemory {

  private NetcdfFile openInMemory(String filename) throws IOException {
    String pathname = TestDir.cdmLocalTestDataDir + filename;
    System.out.println("**** OpenInMemory " + pathname);

    byte[] ba = IO.readFileToByteArray(pathname);
    NetcdfFile ncfile = NetcdfFiles.openInMemory("OpenInMemory", ba);
    System.out.println(ncfile);
    return ncfile;
  }

  @Test
  public void testRead() throws IOException {

    try (NetcdfFile ncfile = openInMemory("testWrite.nc")) {
      assertThat(ncfile.findDimension("lat")).isNotNull();
      assertThat(ncfile.findDimension("lon")).isNotNull();

      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();

      // read entire array
      Array<Number> A;
      try {
        A = (Array<Number>) temp.readArray();
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assertThat(false);
        return;
      }
      assertThat(A.getRank()).isEqualTo(2);

      int i, j;
      Index ima = A.getIndex();
      int[] shape = A.getShape();
      assertThat(shape[0]).isEqualTo(64);
      assertThat(shape[1]).isEqualTo(128);

      for (i = 0; i < shape[0]; i++) {
        for (j = 0; j < shape[1]; j++) {
          double dval = A.get(ima.set(i, j)).doubleValue();
          assertThat((double) (i * 1000000 + j * 1000)).isEqualTo(dval);
        }
      }

      // read part of array
      int[] origin2 = new int[2];
      int[] shape2 = new int[2];
      shape2[0] = 1;
      shape2[1] = temp.getShape()[1];
      try {
        A = (Array<Number>) temp.readArray(new Section(origin2, shape2));
      } catch (InvalidRangeException e) {
        System.err.println("ERROR reading file " + e);
        assertThat(false);
        return;
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assertThat(false);
        return;
      }
      assertThat(A.getRank()).isEqualTo(2);

      for (j = 0; j < shape2[1]; j++) {
        assertThat(A.get(ima.set(0, j)).doubleValue()).isEqualTo((double) (j * 1000));
      }

      // rank reduction
      Array<Number> Areduce = Arrays.reduce(A);
      Index ima2 = Areduce.getIndex();
      assertThat(Areduce.getRank()).isEqualTo(1);

      for (j = 0; j < shape2[1]; j++) {
        assertThat(Areduce.get(ima2.set(j)).doubleValue()).isEqualTo((double) (j * 1000));
      }

      // read char variable
      Array<Byte> Abyte = null;
      Variable c = ncfile.findVariable("svar");
      assertThat(c).isNotNull();
      try {
        Abyte = (Array<Byte>) c.readArray();
      } catch (IOException e) {
        assertThat(false);
      }
      assertThat(Abyte.getArrayType()).isEqualTo(ArrayType.CHAR);
      String val = Arrays.makeStringFromChar(Abyte);
      assertThat(val).isEqualTo("Testing 1-2-3");

      // read char variable 2
      Variable c2 = ncfile.findVariable("svar2");
      assertThat(c2).isNotNull();
      try {
        Abyte = (Array<Byte>) c2.readArray();
      } catch (IOException e) {
        assertThat(false);
      }
      assertThat(Abyte.getArrayType()).isEqualTo(ArrayType.CHAR);
      String val2 = Arrays.makeStringFromChar(Abyte);
      assertThat(val2).isEqualTo("Two pairs of ladies stockings!");

      // read String Array
      Variable c3 = ncfile.findVariable("names");
      assertThat(c3).isNotNull();
      try {
        Abyte = (Array<Byte>) c3.readArray();
      } catch (IOException e) {
        assertThat(false);
      }
      assertThat(Abyte.getArrayType()).isEqualTo(ArrayType.CHAR);
      Array<String> vals = Arrays.makeStringsFromChar(Abyte);
      ima = vals.getIndex();
      assertThat(vals.get(ima.set(0))).isEqualTo("No pairs of ladies stockings!");
      assertThat(vals.get(ima.set(1))).isEqualTo("One pair of ladies stockings!");
      assertThat(vals.get(ima.set(2))).isEqualTo("Two pairs of ladies stockings!");

      // read String Array - 2
      Variable c4 = ncfile.findVariable("names2");
      assertThat(c4).isNotNull();
      try {
        Abyte = (Array<Byte>) c4.readArray();
      } catch (IOException e) {
        assertThat(false);
      }
      assertThat(Abyte.getArrayType()).isEqualTo(ArrayType.CHAR);
      Array<String> vals3 = Arrays.makeStringsFromChar(Abyte);
      assertThat(vals3.get(0)).isEqualTo("0 pairs of ladies stockings!");
      assertThat(vals3.get(1)).isEqualTo("1 pair of ladies stockings!");
      assertThat(vals3.get(2)).isEqualTo("2 pairs of ladies stockings!");
    }
    System.out.println("**************TestRead done");
  }

}
