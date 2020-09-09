/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;
import ucar.ma2.DataType;

/** Test {@link ArrayDouble} */
public class TestArrayDouble {

  @Test
  public void testArrayDouble() {
    int[] shape = new int[] {1, 2, 3};
    Storage<Double> store = Storage.factory(DataType.DOUBLE, new double[] {1, 2, 3, 4, 5, 6});
    ArrayDouble array = new ArrayDouble(shape, store);

    assertThat(array.get(0, 0, 0)).isEqualTo(1);
    assertThat(array.get(0, 0, 1)).isEqualTo(2);
    assertThat(array.get(0, 0, 2)).isEqualTo(3);
    assertThat(array.get(0, 1, 0)).isEqualTo(4);
    assertThat(array.get(0, 1, 1)).isEqualTo(5);
    assertThat(array.get(0, 1, 2)).isEqualTo(6);

    int count = 0;
    for (double val : array) {
      assertThat(val).isEqualTo(count + 1);
      count++;
    }

    // Note that this does fail
    try {
      assertThat(array.get(0, 2, 2)).isEqualTo(8);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ArrayIndexOutOfBoundsException.class);
    }

    try {
      assertThat(array.get(0, 1)).isEqualTo(4);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }

  }

}
