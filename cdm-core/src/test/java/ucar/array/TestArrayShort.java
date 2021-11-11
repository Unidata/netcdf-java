/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

/** Test {@link ArrayShort} */
public class TestArrayShort {

  @Test
  public void testBasics() {
    int[] shape = new int[] {1, 2, 3};
    short[] parray = new short[] {1, 2, 3, 4, 5, 6};
    Storage<Short> store = new ArrayShort.StorageS(parray);
    ArrayShort array = new ArrayShort(ArrayType.SHORT, shape, store);

    assertThat(array.get(0, 0, 0)).isEqualTo(1);
    assertThat(array.get(0, 0, 1)).isEqualTo(2);
    assertThat(array.get(0, 0, 2)).isEqualTo(3);
    assertThat(array.get(0, 1, 0)).isEqualTo(4);
    assertThat(array.get(0, 1, 1)).isEqualTo(5);
    assertThat(array.get(0, 1, 2)).isEqualTo(6);

    int count = 0;
    for (short val : array) {
      assertThat(val).isEqualTo(count + 1);
      count++;
    }

    assertThrows(IllegalArgumentException.class, () -> array.get(0, 2, 2));
    assertThrows(IllegalArgumentException.class, () -> array.get(0, 1));

    short[] result = new short[3];
    array.arraycopy(1, result, 0, 3);
    assertThat(result).isEqualTo(new short[] {2, 3, 4});

    assertThat(array.storage()).isEqualTo(store);
    assertThat(Arrays.copyPrimitiveArray(array)).isEqualTo(parray);
  }

  @Test
  public void testNonCanonicalOrder() {
    int[] shape = new int[] {1, 2, 3};
    Storage<Short> store = new ArrayShort.StorageS(new short[] {1, 2, 3, 4, 5, 6});
    Array<Short> array = new ArrayShort(ArrayType.SHORT, shape, store);
    array = Arrays.flip(array, 1);
    short[] expected = new short[] {4, 5, 6, 1, 2, 3};
    int count = 0;
    for (short val : array) {
      assertThat(val).isEqualTo(expected[count]);
      count++;
    }

    short[] result = new short[3];
    array.arraycopy(1, result, 0, 3);
    assertThat(result).isEqualTo(new short[] {5, 6, 1});
  }

  @Test
  public void testCombine() {
    int[] shape1 = new int[] {1, 2, 3};
    Array<Short> array1 = Arrays.factory(ArrayType.SHORT, shape1, new short[] {1, 2, 3, 4, 5, 6});
    Array<Short> array2 = Arrays.factory(ArrayType.SHORT, shape1, new short[] {7, 8, 9, 10, 11, 12});

    int[] shape = new int[] {2, 2, 3};
    Array<Short> array = Arrays.combine(ArrayType.SHORT, shape, ImmutableList.of(array1, array2));

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
    for (short val : array) {
      assertThat(val).isEqualTo(count + 1);
      count++;
    }
  }

  @Test
  public void testMisc() {
    int[] shape1 = new int[] {1, 2, 3};
    Array<Short> array = Arrays.factoryFill(ArrayType.SHORT, shape1, 256);
    Index index = array.getIndex();
    assertThat(array.get(index.set(0, 1, 2))).isEqualTo(256);
  }

  @Test
  public void testFactoryFill() {
    int[] shape = new int[] {1, 2, 3};
    Array<Short> array = Arrays.factoryFill(ArrayType.SHORT, shape, -9);

    assertThat(array.getSize()).isEqualTo(Arrays.computeSize(shape));
    assertThat(array.getShape()).isEqualTo(shape);

    for (short val : array) {
      assertThat(val).isEqualTo(-9);
    }
  }

  @Test
  public void testMakeArray() {
    Array<Short> array = Arrays.makeArray(ArrayType.SHORT, 1000, 0.0, 1, 10, 10, 10);
    short count = 0;
    for (short val : array) {
      assertThat(val).isEqualTo(count);
      count++;
    }
  }

}
