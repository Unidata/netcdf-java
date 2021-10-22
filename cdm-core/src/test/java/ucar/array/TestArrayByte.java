/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

/** Test {@link ArrayByte} */
public class TestArrayByte {

  @Test
  public void testBasics() {
    int[] shape = new int[] {1, 2, 3};
    Storage<Byte> store = new ArrayByte.StorageS(new byte[] {1, 2, 3, 4, 5, 6});
    ArrayByte array = new ArrayByte(ArrayType.BYTE, shape, store);

    assertThat(array.get(0, 0, 0)).isEqualTo(1);
    assertThat(array.get(0, 0, 1)).isEqualTo(2);
    assertThat(array.get(0, 0, 2)).isEqualTo(3);
    assertThat(array.get(0, 1, 0)).isEqualTo(4);
    assertThat(array.get(0, 1, 1)).isEqualTo(5);
    assertThat(array.get(0, 1, 2)).isEqualTo(6);

    int count = 0;
    for (byte val : array) {
      assertThat(val).isEqualTo(count + 1);
      count++;
    }

    assertThrows(IllegalArgumentException.class, () -> array.get(0, 2, 2));
    assertThrows(IllegalArgumentException.class, () -> array.get(0, 1));

    byte[] result = new byte[3];
    array.arraycopy(1, result, 0, 3);
    assertThat(result).isEqualTo(new byte[] {2, 3, 4});

    assertThat(array.storage()).isEqualTo(store);
  }

  @Test
  public void testNonCanonicalOrder() {
    int[] shape = new int[] {1, 2, 3};
    Storage<Byte> store = new ArrayByte.StorageS(new byte[] {1, 2, 3, 4, 5, 6});
    Array<Byte> array = new ArrayByte(ArrayType.BYTE, shape, store);
    array = Arrays.flip(array, 1);
    byte[] expected = new byte[] {4, 5, 6, 1, 2, 3};
    int count = 0;
    for (byte val : array) {
      assertThat(val).isEqualTo(expected[count]);
      count++;
    }

    byte[] result = new byte[3];
    array.arraycopy(1, result, 0, 3);
    assertThat(result).isEqualTo(new byte[] {5, 6, 1});
  }

  @Test
  public void testFactoryCopy() {
    int[] shape1 = new int[] {1, 2, 3};
    Array<Byte> array1 = Arrays.factory(ArrayType.BYTE, shape1, new byte[] {1, 2, 3, 4, 5, 6});
    Array<Byte> array2 = Arrays.factory(ArrayType.BYTE, shape1, new byte[] {7, 8, 9, 10, 11, 12});

    int[] shape = new int[] {2, 2, 3};
    Array<Byte> array = (Array<Byte>) Arrays.combine(ArrayType.BYTE, shape, ImmutableList.of(array1, array2));

    assertThat(array.get(0, 0, 0)).isEqualTo(1);
    assertThat(array.get(0, 0, 1)).isEqualTo(2);
    assertThat(array.get(0, 0, 2)).isEqualTo(3);
    assertThat(array.get(0, 1, 0)).isEqualTo(4);
    assertThat(array.get(0, 1, 1)).isEqualTo(5);
    assertThat(array.get(0, 1, 2)).isEqualTo(6);
    assertThat(array.get(1, 0, 0)).isEqualTo(7);
    assertThat(array.get(1, 0, 1)).isEqualTo(8);
    assertThat(array.get(1, 0, 2)).isEqualTo(9);
    assertThat(array.get(1, 1, 0)).isEqualTo(10);
    assertThat(array.get(1, 1, 1)).isEqualTo(11);
    assertThat(array.get(1, 1, 2)).isEqualTo(12);

    int count = 0;
    for (byte val : array) {
      assertThat(val).isEqualTo(count + 1);
      count++;
    }
  }

  @Test
  public void testMisc() {
    int[] shape1 = new int[] {1, 2, 3};
    Array<Byte> array = Arrays.factoryFill(ArrayType.BYTE, shape1, 255);
    Index index = array.getIndex();
    assertThat(array.get(index.set(0, 1, 2))).isEqualTo(-1);
  }


}
