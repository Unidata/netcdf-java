/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;

/** Test {@link Arrays} */
public class TestArrays {

  private Array<Double> array;

  @Before
  public void setup() {
    int[] shape = new int[] {1, 2, 3};
    double[] data = new double[] {1, 2, 3, 4, 5, 6};
    array = Arrays.factory(ArrayType.DOUBLE, shape, data);
  }

  @Test
  public void testFlip() {
    Array<Double> flip0 = Arrays.flip(array, 0);
    assertThat(flip0.get(0, 0, 0)).isEqualTo(1);
    assertThat(flip0.get(0, 0, 1)).isEqualTo(2);
    assertThat(flip0.get(0, 0, 2)).isEqualTo(3);
    assertThat(flip0.get(0, 1, 0)).isEqualTo(4);
    assertThat(flip0.get(0, 1, 1)).isEqualTo(5);
    assertThat(flip0.get(0, 1, 2)).isEqualTo(6);

    double[] expected = new double[] {1, 2, 3, 4, 5, 6};
    int count = 0;
    for (double val : flip0) {
      assertThat(val).isEqualTo(expected[count]);
      count++;
    }

    Array<Double> flip1 = Arrays.flip(array, 1);
    assertThat(flip1.get(0, 1, 0)).isEqualTo(1);
    assertThat(flip1.get(0, 1, 1)).isEqualTo(2);
    assertThat(flip1.get(0, 1, 2)).isEqualTo(3);
    assertThat(flip1.get(0, 0, 0)).isEqualTo(4);
    assertThat(flip1.get(0, 0, 1)).isEqualTo(5);
    assertThat(flip1.get(0, 0, 2)).isEqualTo(6);

    expected = new double[] {4, 5, 6, 1, 2, 3};
    count = 0;
    for (double val : flip1) {
      assertThat(val).isEqualTo(expected[count]);
      count++;
    }

    Array<Double> flip2 = Arrays.flip(array, 2);
    assertThat(flip2.get(0, 0, 2)).isEqualTo(1);
    assertThat(flip2.get(0, 0, 1)).isEqualTo(2);
    assertThat(flip2.get(0, 0, 0)).isEqualTo(3);
    assertThat(flip2.get(0, 1, 2)).isEqualTo(4);
    assertThat(flip2.get(0, 1, 1)).isEqualTo(5);
    assertThat(flip2.get(0, 1, 0)).isEqualTo(6);

    expected = new double[] {3, 2, 1, 6, 5, 4};
    count = 0;
    for (double val : flip2) {
      assertThat(val).isEqualTo(expected[count]);
      count++;
    }
  }

  @Test
  public void testPermute() {
    int[] permute = new int[] {0, 2, 1};
    Array<Double> pArray = Arrays.permute(array, permute);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(0, 1, 0)).isEqualTo(2);
    assertThat(pArray.get(0, 2, 0)).isEqualTo(3);
    assertThat(pArray.get(0, 0, 1)).isEqualTo(4);
    assertThat(pArray.get(0, 1, 1)).isEqualTo(5);
    assertThat(pArray.get(0, 2, 1)).isEqualTo(6);

    permute = new int[] {2, 1, 0};
    pArray = Arrays.permute(array, permute);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(1, 0, 0)).isEqualTo(2);
    assertThat(pArray.get(2, 0, 0)).isEqualTo(3);
    assertThat(pArray.get(0, 1, 0)).isEqualTo(4);
    assertThat(pArray.get(1, 1, 0)).isEqualTo(5);
    assertThat(pArray.get(2, 1, 0)).isEqualTo(6);

    permute = new int[] {2, 0, 1};
    pArray = Arrays.permute(array, permute);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(1, 0, 0)).isEqualTo(2);
    assertThat(pArray.get(2, 0, 0)).isEqualTo(3);
    assertThat(pArray.get(0, 0, 1)).isEqualTo(4);
    assertThat(pArray.get(1, 0, 1)).isEqualTo(5);
    assertThat(pArray.get(2, 0, 1)).isEqualTo(6);

    try {
      permute = new int[] {0, 2, 3};
      array = Arrays.permute(array, permute);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }

    try {
      permute = new int[] {0, 2, 2};
      array = Arrays.permute(array, permute);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void testReshape() {
    int[] reshape = new int[] {3, 2, 1};
    Array<Double> pArray = Arrays.reshape(array, reshape);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(0, 1, 0)).isEqualTo(2);
    assertThat(pArray.get(1, 0, 0)).isEqualTo(3);
    assertThat(pArray.get(1, 1, 0)).isEqualTo(4);
    assertThat(pArray.get(2, 0, 0)).isEqualTo(5);
    assertThat(pArray.get(2, 1, 0)).isEqualTo(6);

    reshape = new int[] {6, 1, 1};
    pArray = Arrays.reshape(array, reshape);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(1, 0, 0)).isEqualTo(2);
    assertThat(pArray.get(2, 0, 0)).isEqualTo(3);
    assertThat(pArray.get(3, 0, 0)).isEqualTo(4);
    assertThat(pArray.get(4, 0, 0)).isEqualTo(5);
    assertThat(pArray.get(5, 0, 0)).isEqualTo(6);

    reshape = new int[] {2, 3, 1};
    pArray = Arrays.reshape(array, reshape);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(0, 1, 0)).isEqualTo(2);
    assertThat(pArray.get(0, 2, 0)).isEqualTo(3);
    assertThat(pArray.get(1, 0, 0)).isEqualTo(4);
    assertThat(pArray.get(1, 1, 0)).isEqualTo(5);
    assertThat(pArray.get(1, 2, 0)).isEqualTo(6);

    try {
      reshape = new int[] {2, 2, 2};
      Arrays.reshape(array, reshape);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void testReduceDim() {
    Array<Double> rArray = Arrays.reduce(array, 0);
    assertThat(rArray.getShape()).isEqualTo(new int[] {2, 3});

    try {
      Arrays.reduce(array, 1);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }

    try {
      Arrays.reduce(array, 3);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }

    int[] reshape = new int[] {3, 2, 1};
    Array<Double> pArray = Arrays.reshape(array, reshape);
    rArray = Arrays.reduce(pArray, 2);
    assertThat(rArray.getShape()).isEqualTo(new int[] {3, 2});
  }

  @Test
  public void testReduce() {
    Array<Double> rArray = Arrays.reduce(array);
    assertThat(rArray.getShape()).isEqualTo(new int[] {2, 3});

    int[] reshape = new int[] {3, 2, 1};
    Array<Double> pArray = Arrays.reshape(array, reshape);
    rArray = Arrays.reduce(pArray);
    assertThat(rArray.getShape()).isEqualTo(new int[] {3, 2});
  }

  @Test
  public void testSection() throws InvalidRangeException {
    ArrayList<Range> ranges = new ArrayList<>();
    ranges.add(null);
    ranges.add(null);
    ranges.add(new Range(2));
    Array<Double> pArray = Arrays.section(array, ranges);
    assertThat(pArray.getShape()).isEqualTo(new int[] {1, 2, 2});

    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(0, 0, 1)).isEqualTo(2);
    assertThat(pArray.get(0, 1, 0)).isEqualTo(4);
    assertThat(pArray.get(0, 1, 1)).isEqualTo(5);

    try {
      assertThat(pArray.get(0, 0, 2)).isEqualTo(3);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }

    double total = 0.0;
    for (double val : pArray) {
      total += val;
    }
    assertThat(total).isEqualTo(12.0);


    Array<Double> rArray = Arrays.reduce(pArray);
    assertThat(rArray.getShape()).isEqualTo(new int[] {2, 2});

    assertThat(rArray.get(0, 0)).isEqualTo(1);
    assertThat(rArray.get(0, 1)).isEqualTo(2);
    assertThat(rArray.get(1, 0)).isEqualTo(4);
    assertThat(rArray.get(1, 1)).isEqualTo(5);

    try {
      assertThat(rArray.get(0, 2)).isEqualTo(3);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void testSectionStrided() throws InvalidRangeException {
    ArrayList<Range> ranges = new ArrayList<>();
    ranges.add(null);
    ranges.add(null);
    ranges.add(new Range(0, 2, 2));
    Array<Double> pArray = Arrays.section(array, ranges);
    assertThat(pArray.getShape()).isEqualTo(new int[] {1, 2, 2});

    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(0, 0, 1)).isEqualTo(3);
    assertThat(pArray.get(0, 1, 0)).isEqualTo(4);
    assertThat(pArray.get(0, 1, 1)).isEqualTo(6);

    try {
      assertThat(pArray.get(0, 0, 2)).isEqualTo(3);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }


    double total = 0.0;
    for (double val : pArray) {
      total += val;
    }
    assertThat(total).isEqualTo(14.0);

    Array<Double> rArray = Arrays.reduce(pArray);
    assertThat(rArray.getShape()).isEqualTo(new int[] {2, 2});

    assertThat(rArray.get(0, 0)).isEqualTo(1);
    assertThat(rArray.get(0, 1)).isEqualTo(3);
    assertThat(rArray.get(1, 0)).isEqualTo(4);
    assertThat(rArray.get(1, 1)).isEqualTo(6);

    try {
      assertThat(rArray.get(0, 2)).isEqualTo(3);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  public void testSlice() throws InvalidRangeException {
    Array<Double> pArray = Arrays.slice(array, 2, 2);
    assertThat(pArray.getShape()).isEqualTo(new int[] {1, 2});

    assertThat(pArray.get(0, 0)).isEqualTo(3);
    assertThat(pArray.get(0, 1)).isEqualTo(6);

    double total = 0.0;
    for (double val : pArray) {
      total += val;
    }
    assertThat(total).isEqualTo(9.0);
  }

  @Test
  public void testTranspose() {
    Array<Double> pArray = Arrays.transpose(array, 0, 2);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(1, 0, 0)).isEqualTo(2);
    assertThat(pArray.get(2, 0, 0)).isEqualTo(3);
    assertThat(pArray.get(0, 1, 0)).isEqualTo(4);
    assertThat(pArray.get(1, 1, 0)).isEqualTo(5);
    assertThat(pArray.get(2, 1, 0)).isEqualTo(6);

    pArray = Arrays.transpose(array, 0, 1);
    assertThat(pArray.get(0, 0, 0)).isEqualTo(1);
    assertThat(pArray.get(0, 0, 1)).isEqualTo(2);
    assertThat(pArray.get(0, 0, 2)).isEqualTo(3);
    assertThat(pArray.get(1, 0, 0)).isEqualTo(4);
    assertThat(pArray.get(1, 0, 1)).isEqualTo(5);
    assertThat(pArray.get(1, 0, 2)).isEqualTo(6);

    double total = 0.0;
    for (double val : pArray) {
      total += val;
    }
    assertThat(total).isEqualTo(21.0);
  }

  @Test
  public void testMinMax() {
    MinMax minmax = Arrays.getMinMaxSkipMissingData(array, null);
    assertThat(minmax.min()).isEqualTo(1.0);
    assertThat(minmax.max()).isEqualTo(6.0);

    MinMax minmax2 = Arrays.getMinMaxSkipMissingData(array, new IsMissingEvaluator() {
      public boolean hasMissing() {
        return false;
      }

      public boolean isMissing(double val) {
        return val == 1.0 || val == 6.0;
      }
    });
    assertThat(minmax2.min()).isEqualTo(1.0);
    assertThat(minmax2.max()).isEqualTo(6.0);

    MinMax minmax3 = Arrays.getMinMaxSkipMissingData(array, new IsMissingEvaluator() {
      public boolean hasMissing() {
        return true;
      }

      public boolean isMissing(double val) {
        return val == 1.0 || val == 6.0;
      }
    });
    assertThat(minmax3.min()).isEqualTo(2.0);
    assertThat(minmax3.max()).isEqualTo(5.0);
  }

}
