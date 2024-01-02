package ucar.nc2.filter;

import static com.google.common.truth.Truth.assertThat;
import static ucar.ma2.MAMath.nearlyEquals;

import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;

public class TestNormalizer {

  public static final double DATA_MIN = -5.0;
  public static final double DATA_RANGE = 10.0;
  public static double TOLERANCE = 1.0E-9;

  public static final double[] DOUBLES = {Double.NaN, -5.0, -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0, 5.0};
  public static final Array DATA_DOUBLES = Array.makeFromJavaArray(DOUBLES);

  public static final double[] SAMEDOUBLES = {Double.NaN, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0};
  public static final Array DATA_SAMEDOUBLES = Array.makeFromJavaArray(SAMEDOUBLES);

  public static final float[] FLOATS =
      {Float.NaN, -5.0F, -4.0F, -3.0F, -2.0F, -1.0F, 0.0F, 1.0F, 2.0F, 3.0F, 4.0F, 5.0F};
  public static final Array DATA_FLOATS = Array.makeFromJavaArray(FLOATS);

  public double[] normalize(Array arr) {
    double[] normalizedSample = new double[(int) arr.getSize()];
    IndexIterator iterArr = arr.getIndexIterator();
    for (int i = 0; i < (int) arr.getSize(); i++) {
      Number value = (Number) iterArr.getObjectNext();
      normalizedSample[i] = (value.doubleValue() - DATA_MIN) / DATA_RANGE;
    }
    return normalizedSample;
  }

  @Test
  public void testConvertDouble() {
    Normalizer filter = Normalizer.createFromArray(DATA_DOUBLES, DataType.DOUBLE);
    double[] dataNormalized = normalize(DATA_DOUBLES);
    Array dataConverted = filter.convert(DATA_DOUBLES);
    assertThat(nearlyEquals(dataConverted, Array.makeFromJavaArray(dataNormalized))).isTrue();
  }

  @Test
  public void testConvertSameDouble() {
    Normalizer filter = Normalizer.createFromArray(DATA_SAMEDOUBLES, DataType.DOUBLE);
    Array dataConverted = filter.convert(DATA_SAMEDOUBLES);
    assertThat(nearlyEquals(dataConverted, DATA_SAMEDOUBLES)).isTrue();
  }

  @Test
  public void testConvertFloat() {
    Normalizer filter = Normalizer.createFromArray(DATA_FLOATS, DataType.FLOAT);
    double[] dataNormalized = normalize(DATA_FLOATS);
    Array dataConverted = filter.convert(DATA_FLOATS);
    assertThat(nearlyEquals(dataConverted, Array.makeFromJavaArray(dataNormalized))).isTrue();
  }

  @Test
  public void testCalculateMinimum() {
    Normalizer filter = Normalizer.createFromArray(DATA_DOUBLES, DataType.DOUBLE);
    double calcMinimum = filter.getMinimum();
    assertThat(calcMinimum).isWithin(TOLERANCE).of(DATA_MIN);
  }

  @Test
  public void testCalculateMinimumFloat() {
    Normalizer filter = Normalizer.createFromArray(DATA_FLOATS, DataType.FLOAT);
    double calcMinimum = filter.getMinimum();
    assertThat(calcMinimum).isWithin(TOLERANCE).of(DATA_MIN);
  }

  @Test
  public void testCalculateRange() {
    Normalizer filter = Normalizer.createFromArray(DATA_DOUBLES, DataType.DOUBLE);
    double calcRange = filter.getRange();
    assertThat(calcRange).isWithin(TOLERANCE).of(DATA_RANGE);
  }

  @Test
  public void testCalculateRangeFloat() {
    Normalizer filter = Normalizer.createFromArray(DATA_FLOATS, DataType.FLOAT);
    double calcRange = filter.getRange();
    assertThat(calcRange).isWithin(TOLERANCE).of(DATA_RANGE);
  }

}
