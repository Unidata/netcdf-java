package ucar.nc2.filter;

import static com.google.common.truth.Truth.assertThat;
import static ucar.ma2.MAMath.nearlyEquals;

import java.nio.ByteOrder;
import java.util.Random;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;

public class TestStandardScaler {

  public static final double DATA_MEAN = 100.0;
  public static final double DATA_STDDEV = 10.0;
  public static final int DATA_LEN = 10;
  public static double TOLERANCE = 1.0E-10;
  public static double GAP = 1.0E-2;

  public static double[] createRandData(){
    double[] randData= new double[DATA_LEN];
    Random r = new Random();
    for (int i = 0; i < randData.length; i++) {
      randData[i] = r.nextGaussian() * DATA_STDDEV + DATA_MEAN;
    }
    return randData;
  }

  double[] randData = createRandData();

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

  public double[] createDoubleArray(Array arr) {
    double[] out = new double[arr.getShape()[0]];
    IndexIterator iterArr = arr.getIndexIterator();
    int index = 0;
    while (iterArr.hasNext()) {
      Number value = (Number) iterArr.getObjectNext();
      out[index] = value.doubleValue();
    }
    return out;
  }

  public Array createArray(double[] arr) {
    DataType outType = DataType.DOUBLE;
    Array out = Array.factory(outType, new int[]{arr.length});
    IndexIterator iterOut = out.getIndexIterator();
    for (int i = 0; i < arr.length; i++) {
      iterOut.setObjectNext(arr[i]);
    }
    return out;
  }

  @Test
  public void testEncodeDecode() {
    StandardScaler filter = new StandardScaler(createArray(randData));
    byte[] barray = FilterHelpers.arrayToBytes(Array.makeFromJavaArray(randData), DataType.DOUBLE, ByteOrder.BIG_ENDIAN);
    byte[] encoded = filter.encode(barray);
    byte[] decoded = filter.decode(encoded);
    Array convertedDecoded = FilterHelpers.bytesToArray(decoded, DataType.DOUBLE, ByteOrder.BIG_ENDIAN);
    assertThat(convertedDecoded.getStorage()).isEqualTo(randData);
  }

  @Test
  public void testEncode() {
    StandardScaler filter = new StandardScaler(createArray(randData));
    double[] dataStandardScaler = normalize(randData);
    byte[] barray = FilterHelpers.arrayToBytes(Array.makeFromJavaArray(randData), DataType.DOUBLE, ByteOrder.BIG_ENDIAN);
    byte[] encoded = filter.encode(barray);
    Array dencoded = FilterHelpers.bytesToArray(encoded, DataType.DOUBLE, ByteOrder.BIG_ENDIAN);
//    assertThat(dencoded.getStorage()).isEqualTo(dataStandardScaler);
    assertThat(nearlyEquals(dencoded, createArray(dataStandardScaler))).isTrue();

  }

  @Test
  public void testDecode() {
    StandardScaler filter = new StandardScaler(createArray(randData));
    double DataMean = filter.getMean();
    double DataStdDev = filter.getStdDev();
    double[] dataStandardScaler = denormalize(randData);
    byte[] barray = FilterHelpers.arrayToBytes(Array.makeFromJavaArray(randData), DataType.DOUBLE, ByteOrder.BIG_ENDIAN);
    byte[] decoded = filter.decode(barray);
    Array convertedDecoded = FilterHelpers.bytesToArray(decoded, DataType.DOUBLE, ByteOrder.BIG_ENDIAN);
    assertThat(convertedDecoded.getStorage()).isEqualTo(dataStandardScaler);
  }

  @Test
  public void testCalculateMean(){
//    double[] data = {1.0, 2.0, 3.0, 4.0, 5.0};
    StandardScaler filter = new StandardScaler(createArray(randData));
    double calcMean = filter.getMean();
    assertThat(calcMean).isWithin(GAP).of(DATA_MEAN);
  }

  @Test
  public void testCalculateStandardDeviation(){
//    double[] data = {1.0, 2.0, 3.0, 4.0, 5.0};
    StandardScaler filter = new StandardScaler(createArray(randData));
    double calcStd = filter.getStdDev();
    assertThat(calcStd).isWithin(GAP).of(DATA_STDDEV);
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
