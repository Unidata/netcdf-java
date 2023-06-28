package ucar.nc2.filter;

import static com.google.common.truth.Truth.assertThat;
import static ucar.ma2.MAMath.nearlyEquals;

import java.nio.ByteOrder;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;

public class TestStandardScaler {

  public static final double DATA_MEAN = 10.0;
  public static final double DATA_STDDEV = 3.3166247903554;
  public static double TOLERANCE = 1.0E-10;

  public static final double[] DOUBLES = {Double.NaN, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0};

  public static final Array DATA = Array.makeFromJavaArray(DOUBLES);
  public static final int[] INTS = {5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
  public static final Array DATA_INTS = Array.makeFromJavaArray(INTS);

  public static final float[] FLOATS = {5.0F, 6.0F, 7.0F, 8.0F, 9.0F, 10.0F, 11.0F, 12.0F, 13.0F, 14.0F, 15.0F};

  public static final Array DATA_FLOATS = Array.makeFromJavaArray(FLOATS);

  public double[] normalize(double[] sample) {
    double[] standardizedSample = new double[sample.length];
    for (int i = 0; i < sample.length; i++) {
      standardizedSample[i] = (sample[i] - DATA_MEAN) / DATA_STDDEV;
    }
    return standardizedSample;
  }

  public double[] denormalize(double[] sample){
    double[] descaledData = new double[sample.length];
    for (int i = 0; i < sample.length; i++) {
      descaledData[i] = sample[i] * DATA_STDDEV + DATA_MEAN;
    }
    return descaledData;
  }

  @Test
  public void testEncodeDecode() {
    StandardScaler filter = new StandardScaler(DATA, DataType.DOUBLE);
    byte[] barray = FilterHelpers.arrayToBytes(DATA, DataType.DOUBLE, ByteOrder.LITTLE_ENDIAN);
    byte[] encoded = filter.encode(barray);
    byte[] decoded = filter.decode(encoded);
    Array convertedDecoded = FilterHelpers.bytesToArray(decoded, DataType.DOUBLE, ByteOrder.LITTLE_ENDIAN);
    assertThat(nearlyEquals(convertedDecoded, DATA)).isTrue();
  }

  @Test
  public void testEncodeDecodeFloat() {
    StandardScaler filter = new StandardScaler(DATA_FLOATS, DataType.FLOAT);
    byte[] barray = FilterHelpers.arrayToBytes(DATA_FLOATS, DataType.FLOAT, ByteOrder.LITTLE_ENDIAN);
    byte[] encoded = filter.encode(barray);
    byte[] decoded = filter.decode(encoded);
    Array convertedDecoded = FilterHelpers.bytesToArray(decoded, DataType.FLOAT, ByteOrder.LITTLE_ENDIAN);
    assertThat(nearlyEquals(convertedDecoded, DATA_FLOATS)).isTrue();
  }

  @Test
  public void testEncode() {
    StandardScaler filter = new StandardScaler(DATA, DataType.DOUBLE);
    double[] dataStandardScaler = normalize(DOUBLES);
    byte[] barray = FilterHelpers.arrayToBytes(DATA, DataType.DOUBLE, ByteOrder.LITTLE_ENDIAN);
    byte[] encoded = filter.encode(barray);
    Array dencoded = FilterHelpers.bytesToArray(encoded, DataType.DOUBLE, ByteOrder.LITTLE_ENDIAN);
    assertThat(nearlyEquals(dencoded, Array.makeFromJavaArray(dataStandardScaler))).isTrue();

  }

  @Test
  public void testDecode() {
    StandardScaler filter = new StandardScaler(DATA, DataType.DOUBLE);
    double[] dataStandardScaler = denormalize(DOUBLES);
    byte[] barray = FilterHelpers.arrayToBytes(DATA, DataType.DOUBLE, ByteOrder.LITTLE_ENDIAN);
    byte[] decoded = filter.decode(barray);
    Array convertedDecoded = FilterHelpers.bytesToArray(decoded, DataType.DOUBLE, ByteOrder.LITTLE_ENDIAN);
    assertThat(nearlyEquals(convertedDecoded, Array.makeFromJavaArray(dataStandardScaler))).isTrue();
  }

  @Test
  public void testCalculateMean(){
    StandardScaler filter = new StandardScaler(DATA, DataType.DOUBLE);
    double calcMean = filter.getMean();
    assertThat(calcMean).isWithin(TOLERANCE).of(DATA_MEAN);
  }
  @Test
  public void testCalculateMeanInt(){
    StandardScaler filter = new StandardScaler(DATA_INTS, DataType.INT);
    double calcMean = filter.getMean();
    assertThat(calcMean).isWithin(TOLERANCE).of(DATA_MEAN);
  }

  @Test
  public void testCalculateStandardDeviation(){
    StandardScaler filter = new StandardScaler(DATA, DataType.DOUBLE);
    double calcStd = filter.getStdDev();
    assertThat(calcStd).isWithin(TOLERANCE).of(DATA_STDDEV);
  }
  @Test
  public void testCalculateStandardDeviationInt(){
    StandardScaler filter = new StandardScaler(DATA_INTS, DataType.INT);
    double calcStd = filter.getStdDev();
    assertThat(calcStd).isWithin(TOLERANCE).of(DATA_STDDEV);
  }
}
