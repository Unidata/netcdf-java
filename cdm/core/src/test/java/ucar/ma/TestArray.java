/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import ucar.ma2.DataType;

/** Test {@link Array} */
public class TestArray {

  private Array<Double> array;

  @Before
  public void setup() {
    int[] shape = new int[] {1, 2, 3};
    double[] data = new double[] {1, 2, 3, 4, 5, 6};
    array = Array.factory(DataType.DOUBLE, shape, data);
  }

  @Test
  public void testArray() {
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

  @Test
  public void testFlip() {
    Array<Double> flip0 = array.flip(0);
    assertThat(flip0.get(0, 0, 0)).isEqualTo(1);
    assertThat(flip0.get(0, 0, 1)).isEqualTo(2);
    assertThat(flip0.get(0, 0, 2)).isEqualTo(3);
    assertThat(flip0.get(0, 1, 0)).isEqualTo(4);
    assertThat(flip0.get(0, 1, 1)).isEqualTo(5);
    assertThat(flip0.get(0, 1, 2)).isEqualTo(6);

    Array<Double> flip1 = array.flip(1);
    assertThat(flip1.get(0, 1, 0)).isEqualTo(1);
    assertThat(flip1.get(0, 1, 1)).isEqualTo(2);
    assertThat(flip1.get(0, 1, 2)).isEqualTo(3);
    assertThat(flip1.get(0, 0, 0)).isEqualTo(4);
    assertThat(flip1.get(0, 0, 1)).isEqualTo(5);
    assertThat(flip1.get(0, 0, 2)).isEqualTo(6);

    Array<Double> flip2 = array.flip(2);
    assertThat(flip2.get(0, 0, 2)).isEqualTo(1);
    assertThat(flip2.get(0, 0, 1)).isEqualTo(2);
    assertThat(flip2.get(0, 0, 0)).isEqualTo(3);
    assertThat(flip2.get(0, 1, 2)).isEqualTo(4);
    assertThat(flip2.get(0, 1, 1)).isEqualTo(5);
    assertThat(flip2.get(0, 1, 0)).isEqualTo(6);
  }

  @Test
  public void testPermute() {
    int[] permute = new int[] {0, 2, 1};
    Array<Double> pArray = array.permute(permute);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(0, 1, 0)).isEqualTo(2);
    assertThat(pArray.get(0, 2, 0)).isEqualTo(3);
    assertThat(pArray.get(0, 0, 1)).isEqualTo(4);
    assertThat(pArray.get(0, 1, 1)).isEqualTo(5);
    assertThat(pArray.get(0, 2, 1)).isEqualTo(6);

    permute = new int[] {2, 1, 0};
    pArray = array.permute(permute);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(1, 0, 0)).isEqualTo(2);
    assertThat(pArray.get(2, 0, 0)).isEqualTo(3);
    assertThat(pArray.get(0, 1, 0)).isEqualTo(4);
    assertThat(pArray.get(1, 1, 0)).isEqualTo(5);
    assertThat(pArray.get(2, 1, 0)).isEqualTo(6);

    permute = new int[] {2, 0, 1};
    pArray = array.permute(permute);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(1, 0, 0)).isEqualTo(2);
    assertThat(pArray.get(2, 0, 0)).isEqualTo(3);
    assertThat(pArray.get(0, 0, 1)).isEqualTo(4);
    assertThat(pArray.get(1, 0, 1)).isEqualTo(5);
    assertThat(pArray.get(2, 0, 1)).isEqualTo(6);

    try {
      permute = new int[] {0, 2, 3};
      array.permute(permute);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }

    try {
      permute = new int[] {0, 2, 2};
      array.permute(permute);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void testReshape() {
    int[] reshape = new int[] {3, 2, 1};
    Array<Double> pArray = array.reshape(reshape);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(0, 1, 0)).isEqualTo(2);
    assertThat(pArray.get(1, 0, 0)).isEqualTo(3);
    assertThat(pArray.get(1, 1, 0)).isEqualTo(4);
    assertThat(pArray.get(2, 0, 0)).isEqualTo(5);
    assertThat(pArray.get(2, 1, 0)).isEqualTo(6);

    reshape = new int[] {6, 1, 1};
    pArray = array.reshape(reshape);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(1, 0, 0)).isEqualTo(2);
    assertThat(pArray.get(2, 0, 0)).isEqualTo(3);
    assertThat(pArray.get(3, 0, 0)).isEqualTo(4);
    assertThat(pArray.get(4, 0, 0)).isEqualTo(5);
    assertThat(pArray.get(5, 0, 0)).isEqualTo(6);

    reshape = new int[] {2, 3, 1};
    pArray = array.reshape(reshape);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(0, 1, 0)).isEqualTo(2);
    assertThat(pArray.get(0, 2, 0)).isEqualTo(3);
    assertThat(pArray.get(1, 0, 0)).isEqualTo(4);
    assertThat(pArray.get(1, 1, 0)).isEqualTo(5);
    assertThat(pArray.get(1, 2, 0)).isEqualTo(6);

    try {
      reshape = new int[] {2, 2, 2};
      array.reshape(reshape);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
  }

}
