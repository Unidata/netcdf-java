/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.ma2.Section;

/** Test {@link Array} */
public class TestArray {

  private Array<Double> array;

  @Before
  public void setup() {
    int[] shape = new int[] {1, 2, 3};
    double[] data = new double[] {1, 2, 3, 4, 5, 6};
    array = Arrays.factory(DataType.DOUBLE, shape, data);
  }

  @Test
  public void testArray() {
    assertThat(array.getDataType()).isEqualTo(DataType.DOUBLE);
    assertThat(array.getRank()).isEqualTo(3);
    assertThat(array.getShape()).isEqualTo(new int[] {1, 2, 3});
    assertThat(array.length()).isEqualTo(6);
    assertThat(array.getIndex().getCurrentIndex()).isEqualTo(new int[3]);
    assertThat(array.toString()).isEqualTo("1.0, 2.0, 3.0, 4.0, 5.0, 6.0");

    Section expected = Section.builder().appendRange(1).appendRange(2).appendRange(3).build();
    assertThat(array.getSection()).isEqualTo(expected);

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
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }

    try {
      assertThat(array.get(0, 1)).isEqualTo(4);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
  }

}
