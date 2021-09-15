/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

/** Test {@link Storage} */
public class TestStorage {

  @Test
  public void testStorage() {
    Storage<Double> store = new ArrayDouble.StorageD(new double[] {1, 2, 3});
    assertThat(store.get(0)).isEqualTo(1);
    assertThat(store.get(1)).isEqualTo(2);
    assertThat(store.get(2)).isEqualTo(3);

    assertThrows(ArrayIndexOutOfBoundsException.class, () -> store.get(3));

    for (Double val : store) {
      System.out.printf("%s%n", val);
    }
  }

}
