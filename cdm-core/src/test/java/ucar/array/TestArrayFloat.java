/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

/** Test {@link ArrayFloat} */
public class TestArrayFloat {

  @Test
  public void testBasics() {
    int[] shape = new int[] {1, 2, 3};
    float[] parray = new float[] {1, 2, 3, 4, 5, 6};
    Storage<Float> store = new ArrayFloat.StorageF(parray);
    ArrayFloat array = new ArrayFloat(shape, store);

    assertThat(array.get(0, 0, 0)).isEqualTo(1);
    assertThat(array.get(0, 0, 1)).isEqualTo(2);
    assertThat(array.get(0, 0, 2)).isEqualTo(3);
    assertThat(array.get(0, 1, 0)).isEqualTo(4);
    assertThat(array.get(0, 1, 1)).isEqualTo(5);
    assertThat(array.get(0, 1, 2)).isEqualTo(6);

    int count = 0;
    for (float val : array) {
      assertThat(val).isEqualTo(count + 1);
      count++;
    }

    assertThrows(IllegalArgumentException.class, () -> array.get(0, 2, 2));
    assertThrows(IllegalArgumentException.class, () -> array.get(0, 1));

    float[] result = new float[3];
    array.arraycopy(1, result, 0, 3);
    assertThat(result).isEqualTo(new float[] {2, 3, 4});

    assertThat(Arrays.copyPrimitiveArray(array)).isEqualTo(parray);
  }

  @Test
  public void testNonCanonicalOrder() {
    int[] shape = new int[] {1, 2, 3};
    Storage<Float> store = new ArrayFloat.StorageF(new float[] {1, 2, 3, 4, 5, 6});
    Array<Float> array = new ArrayFloat(shape, store);
    array = Arrays.flip(array, 1);
    float[] expected = new float[] {4, 5, 6, 1, 2, 3};
    int count = 0;
    for (float val : array) {
      assertThat(val).isEqualTo(expected[count]);
      count++;
    }

    float[] result = new float[3];
    array.arraycopy(1, result, 0, 3);
    assertThat(result).isEqualTo(new float[] {5, 6, 1});
  }

  @Test
  public void testCombine() {
    int[] shape1 = new int[] {1, 2, 3};
    Array<Float> array1 = Arrays.factory(ArrayType.FLOAT, shape1, new float[] {1, 2, 3, 4, 5, 6});
    Array<Float> array2 = Arrays.factory(ArrayType.FLOAT, shape1, new float[] {7, 8, 9, 10, 11, 12});

    int[] shape = new int[] {2, 2, 3};
    Array<Float> array = Arrays.combine(ArrayType.FLOAT, shape, ImmutableList.of(array1, array2));

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
    for (float val : array) {
      assertThat(val).isEqualTo(count + 1);
      count++;
    }
  }

  @Test
  public void testFactoryFill() {
    int[] shape = new int[] {1, 2, 3};
    Array<Float> array = Arrays.factoryFill(ArrayType.FLOAT, shape, -999);

    assertThat(array.getSize()).isEqualTo(Arrays.computeSize(shape));
    assertThat(array.getShape()).isEqualTo(shape);

    for (float val : array) {
      assertThat(val).isEqualTo(-999);
    }
  }

  @Test
  public void testMakeArray() {
    Array<Float> array = Arrays.makeArray(ArrayType.FLOAT, 1000, 0.0, 1.5, 10, 10, 10);
    int count = 0;
    for (float val : array) {
      assertThat(val).isEqualTo(1.5f * count);
      count++;
    }
  }
}
