/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import org.junit.experimental.categories.Category;
import ucar.array.ArrayType;
import ucar.array.Array;
import ucar.array.Index;
import ucar.nc2.*;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.*;
import java.util.Iterator;

import static com.google.common.truth.Truth.assertThat;

/** Test compressed data from H5 read JUnit framework. */
@Category(NeedsCdmUnitTest.class)
public class TestH5compressed {

  @org.junit.Test
  public void testReadCompressedInt() throws IOException {
    // actually doesnt seem to be compressed ??

    try (NetcdfFile ncfile = TestH5.openH5("support/zip.h5")) {
      Variable dset = ncfile.findVariable("Data/Compressed_Data");
      assertThat(dset).isNotNull();
      assertThat(dset.getArrayType()).isEqualTo(ArrayType.INT);

      assertThat(dset.getRank()).isEqualTo(2);
      assertThat(dset.getShape()[0]).isEqualTo(1000);
      assertThat(dset.getShape()[1]).isEqualTo(20);

      // read entire array
      Array<Integer> A = (Array<Integer>) dset.readArray();
      assertThat(A.getRank()).isEqualTo(2);
      assertThat(A.getShape()[0]).isEqualTo(1000);
      assertThat(A.getShape()[1]).isEqualTo(20);

      int[] shape = A.getShape();
      Index ima = A.getIndex();
      for (int i = 0; i < shape[0]; i++)
        for (int j = 0; j < shape[1]; j++) {
          assertThat(A.get(ima.set(i, j))).isEqualTo(i + j);
        }

    }
  }

  @org.junit.Test
  public void testReadCompressedByte() throws IOException {
    // actually doesnt seem to be compressed ??

    try (NetcdfFile ncfile = TestH5.openH5("msg/MSG1_8bit_HRV.H5")) {
      Variable dset = ncfile.findVariable("image1/image_preview");
      assertThat(dset).isNotNull();
      assertThat(dset.getArrayType()).isEqualTo(ArrayType.UBYTE);

      assertThat(dset.getRank()).isEqualTo(2);
      assertThat(dset.getShape()[0]).isEqualTo(64);
      assertThat(dset.getShape()[1]).isEqualTo(96);

      // read entire array
      Array<Byte> A = (Array<Byte>) dset.readArray();
      assertThat(A.getRank()).isEqualTo(2);
      assertThat(A.getShape()[0]).isEqualTo(64);
      assertThat(A.getShape()[1]).isEqualTo(96);

      byte[] firstRow = new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31,
          31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 32, 32,
          33, 34, 35, 36, 37, 39, 39, 40, 42, 42, 44, 44, 57, 59, 52, 52, 53, 55, 59, 62, 63, 66, 71, 79, 81, 83, 85,
          87, 89, 90, 87, 84, 84, 87, 94, 82, 80, 76, 77, 68, 59, 57, 61, 68, 81, 42};

      byte[] lastRow = new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 17, 31, 31, 31, 31,
          31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31, 31,
          31, 31, 31, 31, 31, 31, 31, 31, 31, 30, 31, 28, 20, 11, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

      int[] shape = A.getShape();
      Index ima = A.getIndex();
      int lrow = shape[0] - 1;
      for (int j = 0; j < shape[1]; j++) {
        assertThat(A.get(ima.set(0, j))).isEqualTo(firstRow[j]);
        assertThat(A.get(ima.set(lrow, j))).isEqualTo(lastRow[j]);
      }
    }
  }

  @org.junit.Test
  public void testEndian() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestN4reading.testDir + "endianTest.nc4")) {
      Variable v = ncfile.findVariable("TMP");
      assertThat(v).isNotNull();
      assertThat(v.getArrayType()).isEqualTo(ArrayType.FLOAT);

      Array data = v.readArray();
      assertThat(data.getArrayType()).isEqualTo(ArrayType.FLOAT);
      Iterator<Float> iter = data.iterator();

      // large values indicate incorrect inflate or byte swapping
      while (iter.hasNext()) {
        float val = iter.next();
        assertThat(Math.abs(val)).isLessThan(100.0f);
      }
    }
  }


}
