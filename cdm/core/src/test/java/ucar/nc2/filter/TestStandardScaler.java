package ucar.nc2.filter;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.junit.Test;

public class TestStandardScaler {

  @Test
  public void testEncodeDecode() {
    double[] data = {1.0, 2.0, 3.0, 4.0, 5.0};
    StandardScaler filter = new StandardScaler(data);
    byte[] array = new byte[10];
    byte[] encoded = filter.encode(array);
    byte[] decoded = filter.decode(encoded);
    assertThat(decoded).isEqualTo(array);
  }

  @Test
  public void testEncode() {
    double[] data = {1.0, 2.0, 3.0, 4.0, 5.0};
    StandardScaler filter = new StandardScaler(data);
    double dataMean = filter.getMean();
    double dataStdDev = filter.getStdDev();
    double[] dataStandardScaler = filter.calculateStandardScaler(data, dataMean, dataStdDev);
    byte[] barray = createByteArray(data);
    byte[] encoded = filter.encode(barray);
    double[] dencoded = createDoubleArray(encoded);
    assertThat(dencoded).isEqualTo(dataStandardScaler);
  }
//  @Test
//  public void testFlattenArray(){
//    double[][] data2D = {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}};
//    double[] data1D = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0};
//    StandardScaler filter = new StandardScaler();
//    double[] flatData = filter.flattenArray(data2D);
//    assertThat(flatData).isEqualTo(data1D);
//  }

  @Test
  public void testCalculateMean(){
    double[] data = {1.0, 2.0, 3.0, 4.0, 5.0};
    StandardScaler filter = new StandardScaler(data);
    double calcMean = filter.getMean();
    assertThat(calcMean).isEqualTo(3);
  }

  @Test
  public void testCalculateStandardDeviation(){
    double[] data = {1.0, 2.0, 3.0, 4.0, 5.0};
    StandardScaler filter = new StandardScaler(data);
    double calcStd = filter.getStdDev();
    assertThat(calcStd).isEqualTo(Math.sqrt(2.5));
  }

  @Test
  public void testCalculateStandardScaler(){
    double[] data = {1.0, 2.0, 3.0, 4.0, 5.0};
    StandardScaler filter = new StandardScaler(data);
    double dataMean = filter.getMean();
    double dataStdDev = filter.getStdDev();
    double[] dataStandardScaler = filter.calculateStandardScaler(data, dataMean, dataStdDev);
    double[] scaledData = normalize(data);
    assertThat(dataStandardScaler).isEqualTo(scaledData);
  }

  public byte[] createByteArray(double[] arr){
    ByteBuffer bb = ByteBuffer.allocate(arr.length * 8);
    for (double d: arr) {
      bb.putDouble(d);
    }
    return bb.array();
  }

  public double[] createDoubleArray(byte[] arr){
    ByteBuffer bb = ByteBuffer.wrap(arr);
    double[] doubles = new double[arr.length / 8];
    for (int i = 0; i < doubles.length; i++) {
      doubles[i] = bb.getDouble();
    }
    return doubles;
  }

  public double[] normalize(double[] sample) {
    DescriptiveStatistics stats = new DescriptiveStatistics();
    for (int i = 0; i < sample.length; i++) {
      stats.addValue(sample[i]);
    }
    double mean = stats.getMean();
    double standardDeviation = stats.getStandardDeviation();
    double[] standardizedSample = new double[sample.length];
    for (int i = 0; i < sample.length; i++) {
      standardizedSample[i] = (sample[i] - mean) / standardDeviation;
    }
    return standardizedSample;
  }

}
