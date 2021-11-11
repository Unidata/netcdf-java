/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.List;
import org.junit.Test;

/** Test {@link ArrayVlen} */
public class TestArrayVlen {

  @Test
  public void testBasics() {
    int[] shape = new int[] {1, 2, -1};
    short[] arr1 = new short[] {1, 2, 3, 4, 5};
    short[] arr2 = new short[] {6, 7};
    short[][] ragged = new short[][] {arr1, arr2};
    ArrayVlen<Short> array = ArrayVlen.factory(ArrayType.SHORT, shape, ragged);
    assertThat(array.isVlen()).isTrue();

    Array<Short> elem1 = array.get(0, 0);
    assertThat(Iterables.toString(elem1)).isEqualTo("[1, 2, 3, 4, 5]");
    Array<Short> elem2 = array.get(0, 1);
    assertThat(Iterables.toString(elem2)).isEqualTo("[6, 7]");

    List<Integer> list = ImmutableList.of(5, 2);
    int count = 0;
    for (Object val : array) {
      Array<Short> elem = (Array<Short>) val;
      assertThat(elem.length()).isEqualTo(list.get(count));
      count++;
    }

    assertThrows(IllegalArgumentException.class, () -> array.get(0, 2, 2));

    assertThat(array.getShape()).isEqualTo(new int[] {1, 2});
    assertThat(array.getRank()).isEqualTo(2);
    assertThat(array.totalLength()).isEqualTo(7);
    assertThat(array.getArrayType()).isEqualTo(ArrayType.SHORT);

    short[][] result = new short[2][];
    array.arraycopy(0, result, 0, result.length);
    assertThat(result[0]).isEqualTo(arr1);
    assertThat(result[1]).isEqualTo(arr2);

    // non canonical order
    Array<Array<Short>> array2 = Arrays.flip(array, 1);
    short[][] result2 = new short[2][];
    array2.arraycopy(0, result2, 0, result2.length);
    assertThat(result2[1]).isEqualTo(arr1);
    assertThat(result2[0]).isEqualTo(arr2);
  }

  @Test
  public void testMutable() {
    int[] shape = new int[] {1, 2, -1};
    short[] arr1 = new short[] {1, 2, 3, 4, 5};
    short[] arr2 = new short[] {6, 7};
    ArrayVlen<Short> array = ArrayVlen.factory(ArrayType.SHORT, shape);
    array.set(0, arr1);
    array.set(1, arr2);

    Array<Short> elem1 = array.get(0, 0);
    assertThat(Iterables.toString(elem1)).isEqualTo("[1, 2, 3, 4, 5]");
    Array<Short> elem2 = array.get(0, 1);
    assertThat(Iterables.toString(elem2)).isEqualTo("[6, 7]");

    List<Integer> list = ImmutableList.of(5, 2);
    int count = 0;
    for (Object val : array) {
      Array<Short> elem = (Array<Short>) val;
      assertThat(elem.length()).isEqualTo(list.get(count));
      count++;
    }

    ArrayVlen<Short> flipped = (ArrayVlen<Short>) Arrays.flip(array, 1);

    Array<Short> felem1 = flipped.get(0, 0);
    assertThat(Iterables.toString(felem1)).isEqualTo("[6, 7]");
    Array<Short> felem2 = flipped.get(0, 1);
    assertThat(Iterables.toString(felem2)).isEqualTo("[1, 2, 3, 4, 5]");

    List<Integer> fragged = ImmutableList.of(2, 5);
    count = 0;
    for (Object val : flipped) {
      Array<Short> elem = (Array<Short>) val;
      assertThat(elem.length()).isEqualTo(fragged.get(count));
      count++;
    }
  }

  @Test
  public void testTypes() {
    ArrayVlen<Short> array = ArrayVlen.factory(ArrayType.BYTE, new int[] {11});
    Storage<Array<Short>> storage = array.storage();
    array.set(0, new byte[] {1});
    assertThat(storage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(storage.length()).isEqualTo(1);
    assertThat(Iterables.size(array)).isEqualTo(11);
    assertThat(array.get(array.getIndex())).isNotNull();

    ArrayVlen<Byte> carray = ArrayVlen.factory(ArrayType.CHAR, new int[] {12});
    Storage<Array<Byte>> cstorage = carray.storage();
    carray.set(0, new byte[] {1});
    assertThat(cstorage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(cstorage.length()).isEqualTo(1);
    assertThat(Iterables.size(carray)).isEqualTo(12);
    assertThat(carray.get(carray.getIndex())).isNotNull();

    ArrayVlen<Integer> iarray = ArrayVlen.factory(ArrayType.INT, new int[] {13});
    Storage<Array<Integer>> istorage = iarray.storage();
    iarray.set(0, new int[] {1});
    assertThat(istorage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(istorage.length()).isEqualTo(1);
    assertThat(Iterables.size(iarray)).isEqualTo(13);
    assertThat(iarray.get(iarray.getIndex())).isNotNull();

    ArrayVlen<Integer> larray = ArrayVlen.factory(ArrayType.ULONG, new int[] {14});
    Storage<Array<Integer>> lstorage = larray.storage();
    larray.set(0, new long[] {1});
    assertThat(lstorage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(lstorage.length()).isEqualTo(1);
    assertThat(Iterables.size(larray)).isEqualTo(14);
    assertThat(larray.get(larray.getIndex())).isNotNull();

    ArrayVlen<Double> darray = ArrayVlen.factory(ArrayType.DOUBLE, new int[] {15});
    Storage<Array<Double>> dstorage = darray.storage();
    darray.set(0, new double[] {1});
    assertThat(dstorage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(dstorage.length()).isEqualTo(1);
    assertThat(Iterables.size(dstorage)).isEqualTo(15);
    assertThat(darray.get(darray.getIndex())).isNotNull();

    ArrayVlen<Float> farray = ArrayVlen.factory(ArrayType.FLOAT, new int[] {16});
    Storage<Array<Float>> fstorage = farray.storage();
    farray.set(0, new float[] {1, 2, 3});
    assertThat(fstorage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(fstorage.length()).isEqualTo(3);
    assertThat(Iterables.size(fstorage)).isEqualTo(16);
    assertThat(farray.get(farray.getIndex())).isNotNull();

    ArrayVlen<String> sarray = ArrayVlen.factory(ArrayType.STRING, new int[] {17});
    Storage<Array<String>> sstorage = sarray.storage();
    sarray.set(0, new String[] {"one", "two"});
    assertThat(sstorage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(sstorage.length()).isEqualTo(2);
    assertThat(Iterables.size(sstorage)).isEqualTo(17);
    assertThat(sarray.get(sarray.getIndex())).isNotNull();
  }


}
