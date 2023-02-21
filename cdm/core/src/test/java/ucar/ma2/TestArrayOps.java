/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

/** Test ma2 get/put methods in the JUnit framework. */

public class TestArrayOps {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final double TOLERANCE = 1.0E-10;

  int m = 4, n = 3, p = 2;
  int[] shapeArray = {m, n, p};
  ArrayDouble array = new ArrayDouble(shapeArray);
  int i, j, k;
  Index index = array.getIndex();

  @Before
  public void setUp() {

    // write
    int count = 0;
    for (i = 0; i < m; i++) {
      index.set0(i);
      for (j = 0; j < n; j++) {
        index.set1(j);
        for (k = 0; k < p; k++) {
          array.setDouble(index.set2(k), (double) (count++));
        }
      }
    }
  }

  @Test
  public void testReshape() {
    System.out.println("test reshape");

    checkArrayValues(array.reshape(new int[] {4, 6}));

    try {
      array.reshape(new int[] {12});
      assert (false);
    } catch (IllegalArgumentException e) {
      assert (true);
    }

    checkArrayValues(array.reshape(new int[] {24}));

    checkArrayValues(array.reshape(new int[] {2, 2, 3, 2}));
  }

  private static void checkArrayValues(Array array) {
    IndexIterator indexIterator = array.getIndexIterator();
    int count = 0;
    while (indexIterator.hasNext()) {
      assertThat(indexIterator.getDoubleNext()).isWithin(TOLERANCE).of(count++);
    }
  }
}
