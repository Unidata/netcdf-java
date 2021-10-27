/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import junit.framework.*;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.unidata.util.test.TestDir;
import java.io.*;

import static com.google.common.truth.Truth.assertThat;

/** Test reading variable data */

public class TestReadSlice extends TestCase {

  public void testReadSlice1() throws InvalidRangeException, IOException {
    try (NetcdfFile ncfile = TestDir.openFileLocal("testWrite.nc")) {
      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      int[] shape = temp.getShape();

      Variable tempSlice = temp.slice(0, 12);

      // read array section
      Array<?> Asection = tempSlice.readArray();
      assertThat(Asection.getRank()).isEqualTo(1);
      assertThat(Asection.getShape()[0]).isEqualTo(shape[1]);

      // read entire array
      Array<?> A = temp.readArray();
      assertThat(A.getRank()).isEqualTo(2);

      // compare
      Array<?> Asection2 = Arrays.slice(A, 0, 12);
      assertThat(Asection2.getRank()).isEqualTo(1);

      Arrays.equalNumbers((Array<Number>) Asection, (Array<Number>) Asection2);
    }
    System.out.println("*** testReadSlice1 done");
  }

  public void testReadSlice2() throws InvalidRangeException, IOException {
    try (NetcdfFile ncfile = TestDir.openFileLocal("testWrite.nc")) {
      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      int[] shape = temp.getShape();

      Variable tempSlice = temp.slice(1, 55);

      // read array section
      Array<?> Asection = tempSlice.readArray();
      assertThat(Asection.getRank()).isEqualTo(1);
      assertThat(Asection.getShape()[0]).isEqualTo(shape[0]);

      // read entire array
      Array<?> A = temp.readArray();
      assertThat(A.getRank()).isEqualTo(2);

      // compare
      Array<?> Asection2 = Arrays.slice(A, 1, 55);
      assertThat(Asection2.getRank()).isEqualTo(1);

      Arrays.equalNumbers((Array<Number>) Asection, (Array<Number>) Asection2);
    }
    System.out.println("*** testReadSlice2 done");
  }

  public void testReadSliceCompose() throws InvalidRangeException, IOException {
    try (NetcdfFile ncfile = TestDir.openFileLocal("testWrite.nc")) {
      System.out.printf("Open %s%n", ncfile.getLocation());

      Variable temp = ncfile.findVariable("temperature");
      assertThat(temp).isNotNull();
      int[] shape = temp.getShape();
      assert shape[0] == 64;
      assert shape[1] == 128;

      Variable tempSlice = temp.slice(1, 55); // fix dimension 1, eg temp(*,55)
      Variable slice2 = tempSlice.slice(0, 12); // fix dimension 0, eg temp(12,55)
      assert slice2.getRank() == 0; // contract is that rank is reduced by one for each slice

      // read array section
      Array<?> Asection = slice2.readArray();

      // read entire array
      Array<?> A = temp.readArray();
      assert (A.getRank() == 2);

      // compare
      Array<?> data = Arrays.slice(A, 1, 55);
      data = Arrays.slice(data, 0, 12);
      assert (data.getRank() == 0);

      Arrays.equalNumbers((Array<Number>) Asection, (Array<Number>) data);
    }
    System.out.println("*** testReadSliceCompose done");
  }

}
