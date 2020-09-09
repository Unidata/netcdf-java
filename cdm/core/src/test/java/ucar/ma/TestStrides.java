/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

/** Test {@link Strides} */
public class TestStrides {

  @Test
  public void testIndex() {
    int[] shape = new int[] {1, 2, 3};
    Strides index = Strides.builder(shape).build();
    assertThat(index.getShape()).isEqualTo(shape);
    assertThat(index.getSize()).isEqualTo(6);

    assertThat(index.get(0, 0, 0)).isEqualTo(0);
    assertThat(index.get(0, 0, 2)).isEqualTo(2);
    assertThat(index.get(0, 1, 2)).isEqualTo(5);

    // Note that this doesnt fail, though is out of bounds.
    assertThat(index.get(0, 2, 2)).isEqualTo(8);

    try {
      assertThat(index.get(0, 1)).isEqualTo(4);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }

  }

}
