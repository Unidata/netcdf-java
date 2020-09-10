/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;
import ucar.ma2.DataType;

/** Test {@link Storage} */
public class TestStorage {

  @Test
  public void testStorage() {
    Storage<Double> store = Storage.factory(DataType.DOUBLE, new double[] {1, 2, 3});
    assertThat(store.get(0)).isEqualTo(1);
    assertThat(store.get(1)).isEqualTo(2);
    assertThat(store.get(2)).isEqualTo(3);

    try {
      assertThat(store.get(3)).isEqualTo(4);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(ArrayIndexOutOfBoundsException.class);
    }

    for (Double val : store) {
      System.out.printf("%s%n", val);
    }
  }

}
