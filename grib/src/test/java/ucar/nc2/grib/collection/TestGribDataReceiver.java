/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.collection;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.RangeComposite;
import ucar.ma2.RangeIterator;
import ucar.ma2.RangeScatter;
import ucar.nc2.grib.collection.GribDataReader.DataReceiver;

public class TestGribDataReceiver {
  @Test
  public void fullFieldReusesDecodedStorageAndPreservesRank() throws InvalidRangeException {
    int[] shape = {1, 1, 2, 3};
    DataReceiver receiver = new DataReceiver(shape, new Range(2), new Range(3));
    float[] decoded = {0, 1, Float.NaN, 3, 4, 5};
    receiver.addData(decoded, 0, 3);
    Array result = receiver.getArray();
    assertThat(result.getShape()).isEqualTo(shape);
    assertThat(result.getStorage()).isSameInstanceAs(decoded);
    assertThat(result.copyTo1DJavaArray()).isEqualTo(decoded);
  }

  @Test
  public void missingRecordsRemainNaN() throws InvalidRangeException {
    DataReceiver empty = new DataReceiver(new int[] {2, 3}, new Range(2), new Range(3));
    assertThat(empty.getArray().copyTo1DJavaArray())
        .isEqualTo(new float[] {Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN});
    DataReceiver partial = new DataReceiver(new int[] {3, 2, 3}, new Range(2), new Range(3));
    float[] decoded = {0, 1, 2, 3, 4, 5};
    partial.addData(decoded, 1, 3);
    assertThat(partial.getArray().getStorage()).isNotSameInstanceAs(decoded);
    assertThat(partial.getArray().copyTo1DJavaArray())
        .isEqualTo(new float[] {Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, 0, 1, 2, 3, 4, 5,
            Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN});
  }

  @Test
  public void recordsCanArriveOutOfOrder() throws InvalidRangeException {
    DataReceiver receiver = new DataReceiver(new int[] {2, 2, 3}, new Range(2), new Range(3));
    receiver.addData(new float[] {6, 7, 8, 9, 10, 11}, 1, 3);
    receiver.addData(new float[] {0, 1, 2, 3, 4, 5}, 0, 3);
    assertThat(receiver.getArray().copyTo1DJavaArray()).isEqualTo(new float[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11});
  }

  @Test
  public void subsetsAndStridesUseTheRequestedSourceIndexes() throws InvalidRangeException {
    float[] decoded = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    DataReceiver subset = new DataReceiver(new int[] {1, 2}, new Range(1, 1), new Range(1, 2));
    subset.addData(decoded, 0, 3);
    assertThat(subset.getArray().copyTo1DJavaArray()).isEqualTo(new float[] {4, 5});
    DataReceiver strided = new DataReceiver(new int[] {2, 2}, new Range(0, 2, 2), new Range(0, 2, 2));
    strided.addData(decoded, 0, 3);
    assertThat(strided.getArray().copyTo1DJavaArray()).isEqualTo(new float[] {0, 2, 6, 8});
  }

  @Test
  public void compositeAndScatteredRangesAreNotMistakenForFullFields() throws InvalidRangeException {
    float[] decoded = {0, 1, 2, 3, 4, 5};
    RangeComposite x = new RangeComposite("x", Arrays.asList(new Range(2, 2), new Range(0, 1)));
    for (RangeIterator y : new RangeIterator[] {new Range(2), new RangeScatter("y", 1, 0)}) {
      DataReceiver receiver = new DataReceiver(new int[] {2, 3}, y, x);
      receiver.addData(decoded, 0, 3);
      assertThat(receiver.getArray().getStorage()).isNotSameInstanceAs(decoded);
      float[] expected = y instanceof Range ? new float[] {2, 0, 1, 5, 3, 4} : new float[] {5, 3, 4, 2, 0, 1};
      assertThat(receiver.getArray().copyTo1DJavaArray()).isEqualTo(expected);
    }
  }

  @Test
  public void oversizedDecoderOutputIsCopiedToTheRequestedShape() throws InvalidRangeException {
    DataReceiver receiver = new DataReceiver(new int[] {2, 3}, new Range(2), new Range(3));
    float[] decoded = {0, 1, 2, 3, 4, 5, 0, 0, 0, 0, 0, 0};
    receiver.addData(decoded, 0, 3);
    assertThat(receiver.getArray().getStorage()).isNotSameInstanceAs(decoded);
    assertThat(receiver.getArray().copyTo1DJavaArray()).isEqualTo(new float[] {0, 1, 2, 3, 4, 5});
  }

  @Test
  public void debugZeroFillAndPreviouslyObtainedArrayStillWork() throws InvalidRangeException {
    DataReceiver receiver = new DataReceiver(new int[] {2, 3}, new Range(2), new Range(3));
    Array result = receiver.getArray();
    receiver.setDataToZero();
    assertThat(result.copyTo1DJavaArray()).isEqualTo(new float[6]);
    receiver.addData(new float[] {0, 1, 2, 3, 4, 5}, 0, 3);
    assertThat(receiver.getArray()).isSameInstanceAs(result);
    assertThat(result.copyTo1DJavaArray()).isEqualTo(new float[] {0, 1, 2, 3, 4, 5});
  }
}
