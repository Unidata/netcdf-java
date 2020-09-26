/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.List;
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

    Array<Short> elem1 = (Array<Short>) array.get(0, 0);
    assertThat(Iterables.toString(elem1)).isEqualTo("[1, 2, 3, 4, 5]");
    Array<Short> elem2 = (Array<Short>) array.get(0, 1);
    assertThat(Iterables.toString(elem2)).isEqualTo("[6, 7]");

    List<Integer> list = ImmutableList.of(5, 2);
    int count = 0;
    for (Object val : array) {
      Array<Short> elem = (Array<Short>) val;
      assertThat(elem.length()).isEqualTo(list.get(count));
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

    Array<Short> elem1 = (Array<Short>) array.get(0, 0);
    assertThat(Iterables.toString(elem1)).isEqualTo("[1, 2, 3, 4, 5]");
    Array<Short> elem2 = (Array<Short>) array.get(0, 1);
    assertThat(Iterables.toString(elem2)).isEqualTo("[6, 7]");

    List<Integer> list = ImmutableList.of(5, 2);
    int count = 0;
    for (Object val : array) {
      Array<Short> elem = (Array<Short>) val;
      assertThat(elem.length()).isEqualTo(list.get(count));
      count++;
    }

    ArrayVlen<Short> flipped = (ArrayVlen<Short>) Arrays.flip(array, 1);

    Array<Short> felem1 = (Array<Short>) flipped.get(0, 0);
    assertThat(Iterables.toString(felem1)).isEqualTo("[6, 7]");
    Array<Short> felem2 = (Array<Short>) flipped.get(0, 1);
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
    ArrayVlen<Short> array = ArrayVlen.factory(DataType.BYTE, new int[] {11});
    Storage<Object> storage = array.storage();
    array.set(0, new byte[] {1});
    assertThat(storage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(storage.getLength()).isEqualTo(11);
    assertThat(Iterables.size(array)).isEqualTo(11);
    assertThat(array.get(array.getIndex())).isNotNull();

    ArrayVlen<Character> carray = ArrayVlen.factory(DataType.CHAR, new int[] {12});
    Storage<Object> cstorage = carray.storage();
    carray.set(0, new char[] {1});
    assertThat(cstorage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(cstorage.getLength()).isEqualTo(12);
    assertThat(Iterables.size(carray)).isEqualTo(12);
    assertThat(carray.get(carray.getIndex())).isNotNull();

    ArrayVlen<Integer> iarray = ArrayVlen.factory(DataType.INT, new int[] {13});
    Storage<Object> istorage = iarray.storage();
    iarray.set(0, new int[] {1});
    assertThat(istorage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(istorage.getLength()).isEqualTo(13);
    assertThat(Iterables.size(iarray)).isEqualTo(13);
    assertThat(iarray.get(iarray.getIndex())).isNotNull();

    ArrayVlen<Integer> larray = ArrayVlen.factory(DataType.ULONG, new int[] {14});
    Storage<Object> lstorage = larray.storage();
    larray.set(0, new long[] {1});
    assertThat(lstorage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(lstorage.getLength()).isEqualTo(14);
    assertThat(Iterables.size(larray)).isEqualTo(14);
    assertThat(larray.get(larray.getIndex())).isNotNull();

    ArrayVlen<Double> darray = ArrayVlen.factory(DataType.DOUBLE, new int[] {15});
    Storage<Object> dstorage = darray.storage();
    darray.set(0, new double[] {1});
    assertThat(dstorage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(dstorage.getLength()).isEqualTo(15);
    assertThat(Iterables.size(dstorage)).isEqualTo(15);
    assertThat(darray.get(darray.getIndex())).isNotNull();

    ArrayVlen<Float> farray = ArrayVlen.factory(DataType.FLOAT, new int[] {16});
    Storage<Object> fstorage = farray.storage();
    farray.set(0, new float[] {1});
    assertThat(fstorage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(fstorage.getLength()).isEqualTo(16);
    assertThat(Iterables.size(fstorage)).isEqualTo(16);
    assertThat(farray.get(farray.getIndex())).isNotNull();

    ArrayVlen<String> sarray = ArrayVlen.factory(DataType.STRING, new int[] {17});
    Storage<Object> sstorage = sarray.storage();
    sarray.set(0, new String[] {"one"});
    assertThat(sstorage.getClass()).isAssignableTo(StorageMutable.class);
    assertThat(sstorage.getLength()).isEqualTo(17);
    assertThat(Iterables.size(sstorage)).isEqualTo(17);
    assertThat(sarray.get(sarray.getIndex())).isNotNull();
  }


}
