/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

/** Test {@link Index} */
public class TestIndex {

  @Test
  public void testBasics() {
    Index index = Index.ofRank(3);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[3]);
    assertThat(index.set(6, 5, 4).getCurrentIndex()).isEqualTo(new int[] {6, 5, 4});
    assertThat(index.set0(66).getCurrentIndex()).isEqualTo(new int[] {66, 5, 4});
    assertThat(index.set1(22).getCurrentIndex()).isEqualTo(new int[] {66, 22, 4});
    assertThat(index.set2(11).getCurrentIndex()).isEqualTo(new int[] {66, 22, 11});
    assertThat(index.incr(2).getCurrentIndex()).isEqualTo(new int[] {66, 22, 12});
    assertThat(index.incr(1).getCurrentIndex()).isEqualTo(new int[] {66, 23, 12});
    assertThat(index.incr(0).getCurrentIndex()).isEqualTo(new int[] {67, 23, 12});

    Index index2 = Index.of(1, 2, 3, 4);
    assertThat(index2.getCurrentIndex()).isEqualTo(new int[] {1, 2, 3, 4});

    Index index3 = new Index(index2);
    assertThat(index3.getCurrentIndex()).isEqualTo(index2.getCurrentIndex());
  }


  @Test
  public void testSetters() {
    int[] shape = new int[] {1, 2, 3, 4, 5, 6, 7};
    Array array = new ArrayDouble(shape, 0);
    Index index = array.getIndex();

    assertThat(index.getCurrentIndex().length).isEqualTo(7);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[7]);

    index.set0(1);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[] {1, 0, 0, 0, 0, 0, 0});

    index.set1(2);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[] {1, 2, 0, 0, 0, 0, 0});

    index.set2(3);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[] {1, 2, 3, 0, 0, 0, 0});

    index.set3(4);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[] {1, 2, 3, 4, 0, 0, 0});

    index.set4(5);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[] {1, 2, 3, 4, 5, 0, 0});

    index.set5(6);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[] {1, 2, 3, 4, 5, 6, 0});

    index.set6(7);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[] {1, 2, 3, 4, 5, 6, 7});

    index.setDim(4, -5);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[] {1, 2, 3, 4, -5, 6, 7});

    index.set(2, 3, 4, 5, 6, 7, 8);
    assertThat(index.getCurrentIndex()).isEqualTo(new int[] {2, 3, 4, 5, 6, 7, 8});

    assertThrows(ArrayIndexOutOfBoundsException.class, () -> index.set(0, 1, 2, 3));
  }

}
