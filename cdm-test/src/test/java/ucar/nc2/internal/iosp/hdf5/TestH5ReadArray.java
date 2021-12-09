/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.Index;
import ucar.array.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import static com.google.common.truth.Truth.assertThat;

/** Test nc2 read JUnit framework. */
@Category(NeedsCdmUnitTest.class)
public class TestH5ReadArray {

  @org.junit.Test
  public void testReadArrayType() throws Exception {
    try (NetcdfFile ncfile = TestH5.openH5("support/SDS_array_type.h5")) {
      Variable dset = ncfile.findVariable("IntArray");
      assertThat(dset).isNotNull();

      assertThat(dset.getArrayType()).isEqualTo(ArrayType.INT);
      assertThat(dset.getRank()).isEqualTo(3);
      assertThat(dset.getShape()[0]).isEqualTo(10);
      assertThat(dset.getShape()[1]).isEqualTo(5);
      assertThat(dset.getShape()[2]).isEqualTo(4);

      // read entire array
      Array<Integer> A = (Array<Integer>) dset.readArray();
      assertThat(A.getRank()).isEqualTo(3);

      Index ima = A.getIndex();
      int[] shape = A.getShape();

      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          for (int k = 0; k < shape[2]; k++) {
            assertThat(A.get(ima.set(i, j, k))).isEqualTo(i);
          }
        }
      }

      // read part of array
      dset.setCaching(false); // turn off caching to test read subset
      int[] origin2 = new int[3];
      int[] shape2 = new int[] {10, 1, 1};

      A = (Array<Integer>) dset.readArray(new Section(origin2, shape2));
      assertThat(A.getRank()).isEqualTo(3);
      assertThat(A.getShape()[0]).isEqualTo(10);
      assertThat(A.getShape()[1]).isEqualTo(1);
      assertThat(A.getShape()[2]).isEqualTo(1);

      ima = A.getIndex();
      for (int j = 0; j < shape2[0]; j++) {
        assertThat(A.get(ima.set0(j))).isEqualTo(j);
      }

      // rank reduction
      Array<Integer> Areduce = Arrays.reduce(A);
      assertThat(Areduce.getRank()).isEqualTo(1);
      ima = A.getIndex();

      for (int j = 0; j < shape2[0]; j++) {
        assertThat(A.get(ima.set0(j))).isEqualTo(j);
      }

    }
  }

}
