/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import junit.framework.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.Index;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.unidata.util.test.TestDir;
import java.io.*;
import java.lang.invoke.MethodHandles;

import static com.google.common.truth.Truth.assertThat;

/** Test reading variable data */

public class TestReadVariableData extends TestCase {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public TestReadVariableData(String name) {
    super(name);
  }

  public void testNC3Read() throws IOException, InvalidRangeException {
    try (NetcdfFile ncfile = TestDir.openFileLocal("testWrite.nc")) {

      assert (null != ncfile.findDimension("lat"));
      assert (null != ncfile.findDimension("lon"));

      Variable temp = null;
      assert (null != (temp = ncfile.findVariable("temperature")));

      // read entire array
      Array<Double> A = (Array<Double>) temp.readArray();
      assert (A.getRank() == 2);

      int i, j;
      Index ima = A.getIndex();
      int[] shape = A.getShape();
      assert shape[0] == 64;
      assert shape[1] == 128;

      for (i = 0; i < shape[0]; i++) {
        for (j = 0; j < shape[1]; j++) {
          double dval = A.get(ima.set(i, j));
          assert (dval == (double) (i * 1000000 + j * 1000)) : dval;
        }
      }

      // read part of array
      int[] origin2 = new int[2];
      int[] shape2 = new int[2];
      shape2[0] = 1;
      shape2[1] = temp.getShape()[1];
      A = (Array<Double>) temp.readArray(new Section(origin2, shape2));
      assert (A.getRank() == 2);

      for (j = 0; j < shape2[1]; j++) {
        assert (A.get(ima.set(0, j)) == (double) (j * 1000));
      }

      // rank reduction
      Array<Double> Areduce = Arrays.reduce(A);
      Index ima2 = Areduce.getIndex();
      assert (Areduce.getRank() == 1);

      for (j = 0; j < shape2[1]; j++) {
        assert (Areduce.get(ima2.set(j)) == (double) (j * 1000));
      }

      // read char variable
      Variable c = null;
      assert (null != (c = ncfile.findVariable("svar")));
      Array<Byte> ac = (Array<Byte>) c.readArray();
      String val = Arrays.makeStringFromChar(ac);
      assert val.equals("Testing 1-2-3") : val;

      // read char variable 2
      Variable c2 = null;
      assert (null != (c2 = ncfile.findVariable("svar2")));
      Array<Byte> ac2 = (Array<Byte>) c2.readArray();
      assertThat(Arrays.makeStringFromChar(ac2)).isEqualTo("Two pairs of ladies stockings!");

      // read String Array
      Variable c3 = null;
      assert (null != (c3 = ncfile.findVariable("names")));
      Array<Byte> ac3 = (Array<Byte>) c3.readArray();
      Array<String> s3 = Arrays.makeStringsFromChar(ac3);

      assertThat(s3.get(0)).isEqualTo("No pairs of ladies stockings!");
      assertThat(s3.get(1)).isEqualTo("One pair of ladies stockings!");
      assertThat(s3.get(2)).isEqualTo("Two pairs of ladies stockings!");

      // read String Array - 2
      Variable c4 = null;
      assert (null != (c4 = ncfile.findVariable("names2")));
      Array<Byte> ac4 = (Array<Byte>) c4.readArray();
      Array<String> s4 = Arrays.makeStringsFromChar(ac4);

      assertThat(s4.get(0)).isEqualTo("0 pairs of ladies stockings!");
      assertThat(s4.get(1)).isEqualTo("1 pair of ladies stockings!");
      assertThat(s4.get(2)).isEqualTo("2 pairs of ladies stockings!");

    }
    System.out.println("**************TestRead done");
  }
}
