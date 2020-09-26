/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.Iterables;
import org.junit.Test;
import ucar.ma2.DataType;

/** Test {@link ArrayVlen} */
public class TestArrayVlen {

  @Test
  public void testBasics() {
    int[] shape = new int[] {1, 2, -1};
    short[] arr1 = new short[] {1, 2, 3, 4, 5};
    short[] arr2 = new short[] {6, 7};
    short[][] ragged = new short[][] {arr1, arr2};
    ArrayVlen<Short> array = ArrayVlen.factory(DataType.SHORT, shape, ragged);

    assertThat(array.get(0, 0)).isEqualTo(arr1);
    assertThat(array.get(0, 1)).isEqualTo(arr2);

    int count = 0;
    for (Object val : array) {
      assertThat(val).isEqualTo(ragged[count]);
      count++;
    }

    // Note that this does fail
    try {
      assertThat(array.get(0, 2, 2)).isEqualTo(8);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }

    assertThat(array.getShape()).isEqualTo(new int[] {1, 2});
    assertThat(array.getRank()).isEqualTo(2);
    assertThat(array.length()).isEqualTo(2);
    assertThat(array.getDataType()).isEqualTo(DataType.VLEN);
    assertThat(array.getPrimitiveArrayType()).isEqualTo(DataType.SHORT);
  }

  @Test
  public void testMutable() {
    int[] shape = new int[] {1, 2, -1};
    short[] arr1 = new short[] {1, 2, 3, 4, 5};
    short[] arr2 = new short[] {6, 7};
    short[][] ragged = new short[][] {arr1, arr2};
    ArrayVlen<Short> array = ArrayVlen.factory(DataType.SHORT, shape);
    array.set(0, arr1);
    array.set(1, arr2);

    assertThat(array.get(0, 0)).isEqualTo(arr1);
    assertThat(array.get(0, 1)).isEqualTo(arr2);

    int count = 0;
    for (Object val : array) {
      assertThat(val).isEqualTo(ragged[count]);
      count++;
    }

    ArrayVlen<Short> flipped = (ArrayVlen<Short>) Arrays.flip(array, 1);

    assertThat(flipped.get(0, 0)).isEqualTo(arr2);
    assertThat(flipped.get(0, 1)).isEqualTo(arr1);

    short[][] fragged = new short[][] {arr2, arr1};
    count = 0;
    for (Object val : flipped) {
      assertThat(val).isEqualTo(fragged[count]);
      count++;
    }
  }

  @Test
  public void testTypes() {
    ArrayVlen<Short> array = ArrayVlen.factory(DataType.BYTE, new int[] {11});
    Storage<Object> storage = array.storage();
    array.set(0, new byte[] {1});
    assertThat(storage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(storage.getLength()).isEqualTo(11);
    assertThat(Iterables.size(array)).isEqualTo(11);

    ArrayVlen<Character> carray = ArrayVlen.factory(DataType.CHAR, new int[] {12});
    Storage<Object> cstorage = carray.storage();
    carray.set(0, new char[] {1});
    assertThat(cstorage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(cstorage.getLength()).isEqualTo(12);
    assertThat(Iterables.size(carray)).isEqualTo(12);

    ArrayVlen<Integer> iarray = ArrayVlen.factory(DataType.INT, new int[] {13});
    Storage<Object> istorage = iarray.storage();
    iarray.set(0, new int[] {1});
    assertThat(istorage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(istorage.getLength()).isEqualTo(13);
    assertThat(Iterables.size(iarray)).isEqualTo(13);

    ArrayVlen<Integer> larray = ArrayVlen.factory(DataType.ULONG, new int[] {14});
    Storage<Object> lstorage = larray.storage();
    larray.set(0, new long[] {1});
    assertThat(lstorage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(lstorage.getLength()).isEqualTo(14);
    assertThat(Iterables.size(larray)).isEqualTo(14);

    ArrayVlen<Double> darray = ArrayVlen.factory(DataType.DOUBLE, new int[] {15});
    Storage<Object> dstorage = darray.storage();
    darray.set(0, new double[] {1});
    assertThat(dstorage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(dstorage.getLength()).isEqualTo(15);
    assertThat(Iterables.size(dstorage)).isEqualTo(15);

    ArrayVlen<Float> farray = ArrayVlen.factory(DataType.FLOAT, new int[] {16});
    Storage<Object> fstorage = farray.storage();
    farray.set(0, new float[] {1});
    assertThat(fstorage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(fstorage.getLength()).isEqualTo(16);
    assertThat(Iterables.size(fstorage)).isEqualTo(16);

    ArrayVlen<String> sarray = ArrayVlen.factory(DataType.STRING, new int[] {17});
    Storage<Object> sstorage = sarray.storage();
    sarray.set(0, new String[] {"one"});
    assertThat(sstorage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(sstorage.getLength()).isEqualTo(17);
    assertThat(Iterables.size(sstorage)).isEqualTo(17);

  }


}
