/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Before;
import org.junit.Test;

import java.util.stream.StreamSupport;

/** Test {@link Array} */
public class TestArray {

  private Array<Double> array;

  @Before
  public void setup() {
    int[] shape = new int[] {1, 2, 3};
    double[] data = new double[] {1, 2, 3, 4, 5, 6};
    array = Arrays.factory(ArrayType.DOUBLE, shape, data);
  }

  @Test
  public void testBasics() {
    assertThat(array.getArrayType()).isEqualTo(ArrayType.DOUBLE);
    assertThat(array.getRank()).isEqualTo(3);
    assertThat(array.getShape()).isEqualTo(new int[] {1, 2, 3});
    assertThat(array.length()).isEqualTo(6);
    assertThat(array.isVlen()).isFalse();
    assertThat(array.getIndex().getCurrentIndex()).isEqualTo(new int[3]);
    assertThat(array.show()).isEqualTo("1.0, 2.0, 3.0, 4.0, 5.0, 6.0");

    Section expected = Section.builder().appendRange(1).appendRange(2).appendRange(3).build();
    assertThat(array.getSection()).isEqualTo(expected);

    assertThat(array.getScalar()).isEqualTo(1);
    assertThat(array.get(0, 0, 0)).isEqualTo(1);
    assertThat(array.get(0, 0, 1)).isEqualTo(2);
    assertThat(array.get(0, 0, 2)).isEqualTo(3);
    assertThat(array.get(0, 1, 0)).isEqualTo(4);
    assertThat(array.get(0, 1, 1)).isEqualTo(5);
    assertThat(array.get(0, 1, 2)).isEqualTo(6);

    assertThat(array.contains(0, 1, 2)).isTrue();
    assertThat(array.contains(1, 1, 2)).isFalse();

    int count = 0;
    for (double val : array) {
      assertThat(val).isEqualTo(count + 1);
      count++;
    }

    assertThrows(IllegalArgumentException.class, () -> array.get(0, 2, 2));
    assertThrows(IllegalArgumentException.class, () -> array.get(0, 1));

    // contents different
    Array<?> array2 = Arrays.factory(ArrayType.DOUBLE, array.getShape(), new double[(int) array.getSize()]);
    assertThat(array2).isEqualTo(array);
    assertThat(array2.hashCode()).isEqualTo(array.hashCode());
    assertThat(array.toString()).isEqualTo(
        "Array{arrayType=double, indexFn=IndexFn{shape=[1, 2, 3], stride=[6, 3, 1], rank=3, length=6, offset=0, canonicalOrder=true}, rank=3}");
  }

  @Test
  public void testIterator() {
    double sum = 0;
    for (double val : array) {
      sum += val;
    }
    assertThat(sum).isEqualTo(21.0);

    double sum2 = StreamSupport.stream(array.spliterator(), false).mapToDouble(Double::doubleValue).sum();
    assertThat(sum2).isEqualTo(21.0);
  }

  @Test
  public void testException() {
    assertThrows(IllegalArgumentException.class, () -> array.get(99, 1, 1));
    assertThrows(IllegalArgumentException.class, () -> {
      Index idx = array.getIndex();
      array.get(idx.set(99, 1, 1));
    });
    assertThrows(IllegalArgumentException.class, () -> {
      Index idx = array.getIndex();
      array.get(idx.set5(99));
    });
  }

}
