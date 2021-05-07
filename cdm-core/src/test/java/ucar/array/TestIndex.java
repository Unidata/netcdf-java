/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

/** Test {@link Index} */
public class TestIndex {

  private Array<Double> array;

  @Before
  public void setup() {
    int[] shape = new int[] {1, 2, 3, 4, 5, 6, 7};
    array = new ArrayDouble(shape);
  }

  @Test
  public void testSetters() {
    Index index = array.getIndex();
    assertThat(index.getCurrentIndex().length).isEqualTo(7);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[7]);

    index.set0(1);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[] {1, 0, 0, 0, 0, 0, 0});

    index.set1(2);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[] {1, 2, 0, 0, 0, 0, 0});

    index.set2(3);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[] {1, 2, 3, 0, 0, 0, 0});

    index.set3(4);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[] {1, 2, 3, 4, 0, 0, 0});

    index.set4(5);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[] {1, 2, 3, 4, 5, 0, 0});

    index.set5(6);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[] {1, 2, 3, 4, 5, 6, 0});

    index.set6(7);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[] {1, 2, 3, 4, 5, 6, 7});

    index.setDim(4, -5);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[] {1, 2, 3, 4, -5, 6, 7});

    index.set(2, 3, 4, 5, 6, 7, 8);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[] {2, 3, 4, 5, 6, 7, 8});

    try {
      index.set(0, 1, 2, 3);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ArrayIndexOutOfBoundsException.class);
    }
  }

}
