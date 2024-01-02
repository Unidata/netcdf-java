package ucar.nc2.filter;

import static com.google.common.truth.Truth.assertThat;
import static ucar.ma2.MAMath.nearlyEquals;

import java.nio.ByteOrder;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;

public class TestStandardizer {

  public static final double DATA_MEAN = 10.0;
  public static final double DATA_STDDEV = 3.3166247903554;
  public static double TOLERANCE = 1.0E-9;

  public static final double[] DOUBLES = {Double.NaN, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0};
  public static final Array DATA_DOUBLES = Array.makeFromJavaArray(DOUBLES);
  public static final double[] SAMEDOUBLES = {Double.NaN, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0};
  public static final Array DATA_SAMEDOUBLES = Array.makeFromJavaArray(SAMEDOUBLES);

  public static final float[] FLOATS =
      {Float.NaN, 5.0F, 6.0F, 7.0F, 8.0F, 9.0F, 10.0F, 11.0F, 12.0F, 13.0F, 14.0F, 15.0F};
  public static final Array DATA_FLOATS = Array.makeFromJavaArray(FLOATS);

  public double[] standardize(Array arr) {
    double[] standardizedSample = new double[(int) arr.getSize()];
    IndexIterator iterArr = arr.getIndexIterator();
    for (int i = 0; i < (int) arr.getSize(); i++) {
      Number value = (Number) iterArr.getObjectNext();
      standardizedSample[i] = (value.doubleValue() - DATA_MEAN) / DATA_STDDEV;
    }
    return standardizedSample;
  }

  @Test
  public void testConvertDouble() {
    Standardizer filter = Standardizer.createFromArray(DATA_DOUBLES, DataType.DOUBLE);
    double[] dataStandardized = standardize(DATA_DOUBLES);
    Array dataConverted = filter.convert(DATA_DOUBLES);
    assertThat(nearlyEquals(dataConverted, Array.makeFromJavaArray(dataStandardized))).isTrue();
  }

  @Test
  public void testConvertSameDouble() {
    Standardizer filter = Standardizer.createFromArray(DATA_SAMEDOUBLES, DataType.DOUBLE);
    Array dataConverted = filter.convert(DATA_SAMEDOUBLES);
    assertThat(nearlyEquals(dataConverted, DATA_SAMEDOUBLES)).isTrue();
  }

  @Test
  public void testConvertFloat() {
    Standardizer filter = Standardizer.createFromArray(DATA_FLOATS, DataType.FLOAT);
    double[] dataStandardized = standardize(DATA_FLOATS);
    Array dataConverted = filter.convert(DATA_FLOATS);
    assertThat(nearlyEquals(dataConverted, Array.makeFromJavaArray(dataStandardized))).isTrue();
  }

  @Test
  public void testCalculateMean() {
    Standardizer filter = Standardizer.createFromArray(DATA_DOUBLES, DataType.DOUBLE);
    double calcMean = filter.getMean();
    assertThat(calcMean).isWithin(TOLERANCE).of(DATA_MEAN);
  }

  @Test
  public void testCalculateMeanFloat() {
    Standardizer filter = Standardizer.createFromArray(DATA_FLOATS, DataType.FLOAT);
    double calcMean = filter.getMean();
    assertThat(calcMean).isWithin(TOLERANCE).of(DATA_MEAN);
  }

  @Test
  public void testCalculateStandardDeviation() {
    Standardizer filter = Standardizer.createFromArray(DATA_DOUBLES, DataType.DOUBLE);
    double calcStd = filter.getStdDev();
    assertThat(calcStd).isWithin(TOLERANCE).of(DATA_STDDEV);
  }

  @Test
  public void testCalculateStandardDeviationFloat() {
    Standardizer filter = Standardizer.createFromArray(DATA_FLOATS, DataType.FLOAT);
    double calcStd = filter.getStdDev();
    assertThat(calcStd).isWithin(TOLERANCE).of(DATA_STDDEV);
  }
}
