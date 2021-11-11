/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

/** Test {@link IndexFn} */
public class TestIndexFn {

  @Test
  public void testBasics() {
    int[] shape = new int[] {1, 2, 3};
    IndexFn index = IndexFn.builder(shape).build();
    assertThat(index.getShape()).isEqualTo(shape);
    assertThat(index.length()).isEqualTo(6);

    assertThat(index.get(0, 0, 0)).isEqualTo(0);
    assertThat(index.get(0, 0, 2)).isEqualTo(2);
    assertThat(index.get(0, 1, 2)).isEqualTo(5);

    assertThrows(IllegalArgumentException.class, () -> index.get(0, 2, 2));
    assertThrows(IllegalArgumentException.class, () -> index.get(0, 1));

    assertThat(index.contains(0, 1, 2)).isTrue();
    assertThat(index.contains(1, 1, 2)).isFalse();

    IndexFn index2 = index.toBuilder().build();
    assertThat(index2).isEqualTo(index);
    assertThat(index2.hashCode()).isEqualTo(index.hashCode());

    assertThat(index2.toString())
        .contains("IndexFn{shape=[1, 2, 3], stride=[6, 3, 1], rank=3, length=6, offset=0, canonicalOrder=true}");
  }

  @Test
  public void testFlip() {
    int[] shape = new int[] {1, 2, 3};
    IndexFn index = IndexFn.builder(shape).build();
    assertThat(index.toString2()).isEqualTo("0, 1, 2, 3, 4, 5");

    IndexFn indexf1 = index.flip(1);
    assertThat(index.toString2()).isEqualTo("0, 1, 2, 3, 4, 5");
    assertThat(indexf1.toString2()).isEqualTo("3, 4, 5, 0, 1, 2");

    IndexFn indexf2 = index.flip(2);
    assertThat(indexf2.toString2()).isEqualTo("2, 1, 0, 5, 4, 3");
  }

  @Test
  public void testSection() throws InvalidRangeException {
    int[] shape = new int[] {100};
    IndexFn indexFn = IndexFn.builder(shape).build();

    IndexFn subset = indexFn.section(Section.builder().appendRange(new Range(11, 15)).build());
    assertThat(subset.toString2()).isEqualTo("11, 12, 13, 14, 15");
    assertThat(subset.getRank()).isEqualTo(1);
    assertThat(subset.getShape()).isEqualTo(new int[] {5});
    assertThat(subset.length()).isEqualTo(5);
  }

}
