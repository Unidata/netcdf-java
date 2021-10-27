/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.array.Section;
import ucar.array.Array;
import ucar.unidata.util.test.TestDir;
import java.io.*;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import static com.google.common.truth.Truth.assertThat;

/** Test reading variable data */
public class TestReadSection {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testReadVariableSection() throws InvalidRangeException, IOException {
    try (NetcdfFile ncfile = TestDir.openFileLocal("testWrite.nc")) {
      Variable temp = ncfile.findVariable("temperature");
      assert (null != temp);

      int[] origin = {3, 6};
      int[] shape = {12, 17};

      Variable tempSection = temp.section(new Section(origin, shape));

      // read array section
      Array<Number> Asection = (Array<Number>) tempSection.readArray();
      assert Asection.getRank() == 2;
      assert shape[0] == Asection.getShape()[0];
      assert shape[1] == Asection.getShape()[1];

      // read entire array
      Array<Number> A = (Array<Number>) temp.readArray();
      assert (A.getRank() == 2);

      // compare
      Array<Number> Asection2 = Arrays.section(A, new Section(origin, shape));
      assert (Asection2.getRank() == 2);
      assert (shape[0] == Asection2.getShape()[0]);
      assert (shape[1] == Asection2.getShape()[1]);

      assertThat(Arrays.equalNumbers(Asection, Asection2)).isTrue();
    }
  }

  @Test
  public void testReadVariableSection2() throws InvalidRangeException, IOException {
    try (NetcdfFile ncfile = TestDir.openFileLocal("testWrite.nc")) {
      Variable temp = null;
      assert (null != (temp = ncfile.findVariable("temperature")));

      ArrayList<Range> ranges = new ArrayList<>();
      Range r0 = new Range(3, 14);
      Range r1 = new Range(6, 22);
      ranges.add(r0);
      ranges.add(r1);

      Variable tempSection = temp.section(new Section(ranges));
      assert tempSection.getRank() == 2;
      int[] vshape = tempSection.getShape();
      assert r0.length() == vshape[0];
      assert r1.length() == vshape[1];

      // read array section
      Array<Number> Asection = (Array<Number>) tempSection.readArray();
      assert Asection.getRank() == 2;
      assert r0.length() == Asection.getShape()[0];
      assert r1.length() == Asection.getShape()[1];

      // read entire array
      Array<Number> A = (Array<Number>) temp.readArray();
      assert (A.getRank() == 2);

      // compare
      Array<Number> Asection2 = Arrays.section(A, new Section(ranges));
      assert (Asection2.getRank() == 2);
      assert (r0.length() == Asection2.getShape()[0]);
      assert (r1.length() == Asection2.getShape()[1]);

      assertThat(Arrays.equalNumbers(Asection, Asection2)).isTrue();

    }
  }
}
