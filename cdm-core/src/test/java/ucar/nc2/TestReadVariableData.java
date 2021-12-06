/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import junit.framework.*;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.Index;
import ucar.array.Section;
import ucar.unidata.util.test.TestDir;

import static com.google.common.truth.Truth.assertThat;

/** Test reading variable data */
public class TestReadVariableData extends TestCase {

  public TestReadVariableData(String name) {
    super(name);
  }

  public void testNC3Read() throws Exception {
    try (NetcdfFile ncfile = TestDir.openFileLocal("testWrite.nc")) {
      assertThat(null != ncfile.findDimension("lat"));
      assertThat(null != ncfile.findDimension("lon"));

      Variable temp = null;
      assertThat(null != (temp = ncfile.findVariable("temperature")));

      // read entire array
      Array<Double> A = (Array<Double>) temp.readArray();
      assertThat(A.getRank()).isEqualTo(2);

      int i, j;
      Index ima = A.getIndex();
      int[] shape = A.getShape();
      assertThat(shape[0]).isEqualTo(64);
      assertThat(shape[1]).isEqualTo(128);

      for (i = 0; i < shape[0]; i++) {
        for (j = 0; j < shape[1]; j++) {
          double dval = A.get(ima.set(i, j));
          assertThat(dval).isEqualTo((double) (i * 1000000 + j * 1000));
        }
      }

      // read part of array
      int[] origin2 = new int[2];
      int[] shape2 = new int[2];
      shape2[0] = 1;
      shape2[1] = temp.getShape()[1];
      A = (Array<Double>) temp.readArray(new Section(origin2, shape2));
      assertThat(A.getRank()).isEqualTo(2);

      for (j = 0; j < shape2[1]; j++) {
        assertThat(A.get(ima.set(0, j))).isEqualTo((double) (j * 1000));
      }

      // rank reduction
      Array<Double> Areduce = Arrays.reduce(A);
      Index ima2 = Areduce.getIndex();
      assertThat(Areduce.getRank()).isEqualTo(1);

      for (j = 0; j < shape2[1]; j++) {
        assertThat(Areduce.get(ima2.set(j))).isEqualTo((double) (j * 1000));
      }

      // read char variable
      Variable c = ncfile.findVariable("svar");
      assertThat(c).isNotNull();
      Array<Byte> ac = (Array<Byte>) c.readArray();
      String val = Arrays.makeStringFromChar(ac);
      assertThat(val).isEqualTo("Testing 1-2-3");

      // read char variable 2
      Variable c2 = ncfile.findVariable("svar2");
      assertThat(c2).isNotNull();
      Array<Byte> ac2 = (Array<Byte>) c2.readArray();
      assertThat(Arrays.makeStringFromChar(ac2)).isEqualTo("Two pairs of ladies stockings!");

      // read String Array
      Variable c3 = ncfile.findVariable("names");
      assertThat(c3).isNotNull();
      Array<Byte> ac3 = (Array<Byte>) c3.readArray();
      Array<String> s3 = Arrays.makeStringsFromChar(ac3);

      assertThat(s3.get(0)).isEqualTo("No pairs of ladies stockings!");
      assertThat(s3.get(1)).isEqualTo("One pair of ladies stockings!");
      assertThat(s3.get(2)).isEqualTo("Two pairs of ladies stockings!");

      // read String Array - 2
      Variable c4 = ncfile.findVariable("names2");
      assertThat(c4).isNotNull();
      Array<Byte> ac4 = (Array<Byte>) c4.readArray();
      Array<String> s4 = Arrays.makeStringsFromChar(ac4);

      assertThat(s4.get(0)).isEqualTo("0 pairs of ladies stockings!");
      assertThat(s4.get(1)).isEqualTo("1 pair of ladies stockings!");
      assertThat(s4.get(2)).isEqualTo("2 pairs of ladies stockings!");
    }
    System.out.println("**************TestRead done");
  }
}
