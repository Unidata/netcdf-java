/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.UtilsMa2Test;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Test reading variable data */
@Category(NeedsCdmUnitTest.class)
public class TestReadStrides {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testReadStridesGini() throws IOException, InvalidRangeException {
    testReadStrides(TestDir.cdmUnitTestDir + "formats/gini/HI-NATIONAL_14km_IR_20050918_2000.gini");
  }

  private void testReadStrides(String filename) throws IOException, InvalidRangeException {
    try (NetcdfFile ncfile = TestDir.open(filename)) {
      for (Variable v : ncfile.getVariables()) {
        if (v.getRank() == 0)
          continue;
        if (!v.hasCachedData())
          v.setCaching(false);
        testVariableReadStrides(v);
      }
    }
  }

  private void testVariableReadStrides(Variable v) throws IOException, InvalidRangeException {
    Array allData = v.read();

    int[] shape = v.getShape();
    if (shape.length < 5)
      return;
    for (int first = 0; first < 3; first++) {
      for (int stride = 2; stride < 5; stride++) {

        ArrayList<Range> ranges = new ArrayList<>();
        for (int value : shape) {
          int last = value - 1;
          Range r = new Range(first, last, stride);
          ranges.add(r);
        }

        System.out.println(v.getFullName() + " test range= " + new Section(ranges));
        Array sectionRead = v.read(ranges);
        Array sectionMake = allData.sectionNoReduce(ranges);
        UtilsMa2Test.testEquals(sectionRead, sectionMake);
      }
    }
  }

}
