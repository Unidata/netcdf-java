/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.Index;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.*;
import java.lang.invoke.MethodHandles;

/** Test nc2 read JUnit framework. */
@Category(NeedsCdmUnitTest.class)
public class TestH5ReadArray {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @org.junit.Test
  public void testReadArrayType() throws IOException, InvalidRangeException {
    try (NetcdfFile ncfile = TestH5.openH5("support/SDS_array_type.h5")) {

      Variable dset = null;
      assert (null != (dset = ncfile.findVariable("IntArray")));
      assert (dset.getArrayType() == ArrayType.INT);

      assert (dset.getRank() == 3);
      assert (dset.getShape()[0] == 10);
      assert (dset.getShape()[1] == 5);
      assert (dset.getShape()[2] == 4);

      // read entire array
      Array<Integer> A;
      try {
        A = (Array<Integer>) dset.readArray();
      } catch (IOException e) {
        System.err.println("ERROR reading file");
        assert (false);
        return;
      }
      assert (A.getRank() == 3);

      Index ima = A.getIndex();
      int[] shape = A.getShape();

      for (int i = 0; i < shape[0]; i++)
        for (int j = 0; j < shape[1]; j++)
          for (int k = 0; k < shape[2]; k++)
            if (A.get(ima.set(i, j, k)) != i) {
              assert false;
            }


      // read part of array
      dset.setCaching(false); // turn off caching to test read subset
      int[] origin2 = new int[3];
      int[] shape2 = new int[] {10, 1, 1};

      A = (Array<Integer>) dset.readArray(new Section(origin2, shape2));
      assert (A.getRank() == 3);
      assert (A.getShape()[0] == 10);
      assert (A.getShape()[1] == 1);
      assert (A.getShape()[2] == 1);

      ima = A.getIndex();
      for (int j = 0; j < shape2[0]; j++) {
        assert (A.get(ima.set0(j)) == j);
      }

      // rank reduction
      Array<Integer> Areduce = Arrays.reduce(A);
      Index ima2 = Areduce.getIndex();
      assert (Areduce.getRank() == 1);
      ima = A.getIndex();

      for (int j = 0; j < shape2[0]; j++) {
        assert (A.get(ima.set0(j)) == j);
      }

    }
  }

}
