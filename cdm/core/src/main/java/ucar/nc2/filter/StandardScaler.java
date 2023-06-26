package ucar.nc2.filter;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;

public class StandardScaler extends Filter {

  private final ScaleOffset scaleOffset;
  private static final String name = "Standard Scaler";
  private static final int id = -1;
  private final double mean;
  private final double stdDev;

  public StandardScaler(double[] arr){
    mean = calculateMean(arr);
    stdDev = calculateStandardDeviation(arr);
    Map<String, Object> props = new HashMap<>();
    props.put("offset", mean);
    props.put("scale", (int) stdDev);
    props.put("dtype", ">f8");
    scaleOffset = new ScaleOffset(props);
  }

//  public double[] flattenArray(double[][] arr) {
//    double[] result= new double[arr.length * arr[0].length];
//    int index = 0;
//    for (int i = 0; i < arr.length; i++) {
//      for (int j = 0; j < arr[0].length; j++) {
//        result[index] = arr[i][j];
//        index++;
//      }
//    }
//    return result;
//  }
  private double calculateMean(double[] data) {
    SummaryStatistics curr = new SummaryStatistics();
    for (int a = 0; a < data.length; a++) {
      curr.addValue(data[a]);
    }
    return curr.getMean();
  }

  private double calculateStandardDeviation(double[] data) {
    StandardDeviation stdDev = new StandardDeviation();
    return stdDev.evaluate(data);
  }

  public double[] calculateStandardScaler(double[] data, double mean, double stdDev){
    double[] scaledData = new double[data.length];
    for (int i = 0; i < data.length; i++) {
      // remove int conversion later
      scaledData[i] = (data[i] - mean) / (int) stdDev;
    }
    return scaledData;
  }

  public double getMean(){
    return mean;
  }

  public double getStdDev() {
    return stdDev;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getId() {
    return id;
  }

  @Override
  public byte[] encode(byte[] dataIn) {
    return scaleOffset.encode(dataIn);
  }

  @Override
  public byte[] decode(byte[] dataIn) {
    return scaleOffset.decode(dataIn);
  }
}

