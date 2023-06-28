package ucar.nc2.filter;

import static com.google.common.truth.Truth.assertThat;
import static ucar.ma2.MAMath.nearlyEquals;

import java.nio.ByteOrder;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;

public class TestStandardScaler {

  public static final double DATA_MEAN = 10.0;
  public static final double DATA_STDDEV = 3.3166247903554;
//  public static final int DATA_LEN = 10;
  public static double TOLERANCE = 1.0E-10;

  public static final double[] DOUBLES = {5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0};

  public static final Array DATA = Array.makeFromJavaArray(DOUBLES);


//  public static double[] createRandData(){
//    double[] randData= new double[DATA_LEN];
//    Random r = new Random();
//    for (int i = 0; i < randData.length; i++) {
//      randData[i] = r.nextGaussian() * DATA_STDDEV + DATA_MEAN;
//    }
//    return randData;
//  }
//  double[] randData = createRandData();

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

//  public double[] createDoubleArray(Array arr) {
//    double[] out = new double[arr.getShape()[0]];
//    IndexIterator iterArr = arr.getIndexIterator();
//    int index = 0;
//    while (iterArr.hasNext()) {
//      Number value = (Number) iterArr.getObjectNext();
//      out[index] = value.doubleValue();
//    }
//    return out;
//  }

//  public Array createArray(double[] arr) {
//    DataType outType = DataType.DOUBLE;
//    Array out = Array.factory(outType, new int[]{arr.length});
//    IndexIterator iterOut = out.getIndexIterator();
//    for (int i = 0; i < arr.length; i++) {
//      iterOut.setObjectNext(arr[i]);
//    }
//    return out;
//  }

  @Test
  public void testEncodeDecode() {
    StandardScaler filter = new StandardScaler(DATA);
    byte[] barray = FilterHelpers.arrayToBytes(DATA, DataType.DOUBLE, ByteOrder.BIG_ENDIAN);
    byte[] encoded = filter.encode(barray);
    byte[] decoded = filter.decode(encoded);
    Array convertedDecoded = FilterHelpers.bytesToArray(decoded, DataType.DOUBLE, ByteOrder.BIG_ENDIAN);
    assertThat(nearlyEquals(convertedDecoded, DATA)).isTrue();
  }

  @Test
  public void testEncode() {
    StandardScaler filter = new StandardScaler(DATA);
    double[] dataStandardScaler = normalize(DOUBLES);
    byte[] barray = FilterHelpers.arrayToBytes(DATA, DataType.DOUBLE, ByteOrder.BIG_ENDIAN);
    byte[] encoded = filter.encode(barray);
    Array dencoded = FilterHelpers.bytesToArray(encoded, DataType.DOUBLE, ByteOrder.BIG_ENDIAN);
    assertThat(nearlyEquals(dencoded, Array.makeFromJavaArray(dataStandardScaler))).isTrue();

  }

  @Test
  public void testDecode() {
    StandardScaler filter = new StandardScaler(DATA);
    double[] dataStandardScaler = denormalize(DOUBLES);
    byte[] barray = FilterHelpers.arrayToBytes(DATA, DataType.DOUBLE, ByteOrder.BIG_ENDIAN);
    byte[] decoded = filter.decode(barray);
    Array convertedDecoded = FilterHelpers.bytesToArray(decoded, DataType.DOUBLE, ByteOrder.BIG_ENDIAN);
    assertThat(nearlyEquals(convertedDecoded, Array.makeFromJavaArray(dataStandardScaler))).isTrue();
  }

  @Test
  public void testCalculateMean(){
    StandardScaler filter = new StandardScaler(DATA);
    double calcMean = filter.getMean();
    assertThat(calcMean).isWithin(TOLERANCE).of(DATA_MEAN);
  }

  @Test
  public void testCalculateStandardDeviation(){
    StandardScaler filter = new StandardScaler(DATA);
    double calcStd = filter.getStdDev();
    assertThat(calcStd).isWithin(TOLERANCE).of(DATA_STDDEV);
  }

//  @Test
//  public void testNormalize(){
////    double[] data = {1.0, 2.0, 3.0, 4.0, 5.0};
//    StandardScaler filter = new StandardScaler(randData);
//    double dataMean = filter.getMean();
//    double dataStdDev = filter.getStdDev();
//    double[] dataStandardScaler = filter.normalize(randData, dataMean, dataStdDev);
//    double[] scaledData = normalize(randData);
//    assertThat(dataStandardScaler).usingTolerance(GAP).equals(scaledData);
//  }

}
