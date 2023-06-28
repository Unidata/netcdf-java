package ucar.nc2.filter;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;

public class StandardScaler extends Filter {

  private final ScaleOffset scaleOffset;
  private static final String name = "Standard Scaler";
  private static final int id = -1;
  private final double mean;
  private final double stdDev;

  public StandardScaler(Array arr){
    mean = calculateMean(arr);
    stdDev = calculateStandardDeviation(arr);
    Map<String, Object> props = new HashMap<>();
    props.put("offset", mean);
    props.put("scale", 1/stdDev);
    props.put("dtype", ">f8");
    scaleOffset = new ScaleOffset(props);
  }

  private double calculateMean(Array arr) {
    SummaryStatistics cur = new SummaryStatistics();
    IndexIterator iterArr = arr.getIndexIterator();
    while (iterArr.hasNext()) {
      Number value = (Number) iterArr.getObjectNext();
      cur.addValue(value.doubleValue());
    }
    return cur.getMean();
  }

  private double calculateStandardDeviation(Array arr) {
    SummaryStatistics cur = new SummaryStatistics();
    IndexIterator iterArr = arr.getIndexIterator();
    while (iterArr.hasNext()) {
      Number value = (Number) iterArr.getObjectNext();
      cur.addValue(value.doubleValue());
    }
    return cur.getStandardDeviation();
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

