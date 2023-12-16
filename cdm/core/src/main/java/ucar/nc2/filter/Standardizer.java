package ucar.nc2.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.dataset.VariableDS;

public class Standardizer {

  private final ScaleOffset scaleOffset;
  private final double mean;
  private final double stdDev;

  public static Standardizer createFromVariable(VariableDS var) {
    try {
      Array arr = var.read();
      DataType type = var.getDataType();
      return createFromArray(arr, type);
    } catch (IOException e) {
      return new Standardizer(0.0, 1.0, var.getDataType());
    }
  }

  public static Standardizer createFromArray(Array arr, DataType type) {
    SummaryStatistics statistics = calculationHelper(arr);
    if (statistics.getStandardDeviation() == 0) {
      return new Standardizer(0.0, 1.0, type);
    }
    return new Standardizer(statistics.getMean(), statistics.getStandardDeviation(), type);
  }

  private Standardizer(double mean, double stdDev, DataType type) {
    this.mean = mean;
    this.stdDev = stdDev;
    Map<String, Object> props = new HashMap<>();
    props.put("offset", mean);
    props.put("scale", 1 / stdDev);
    props.put("dtype", type);
    scaleOffset = new ScaleOffset(props);
  }

  private static SummaryStatistics calculationHelper(Array arr) {
    SummaryStatistics sumStat = new SummaryStatistics();
    IndexIterator iterArr = arr.getIndexIterator();
    while (iterArr.hasNext()) {
      Number value = (Number) iterArr.getObjectNext();
      if (!Double.isNaN(value.doubleValue())) {
        sumStat.addValue(value.doubleValue());
      }
    }
    return sumStat;
  }

  public Array convert(Array arr) {
    return scaleOffset.applyScaleOffset(arr);
  }

  // public double[] convert(double[] in) {
  // Arrays.stream(in).parallel().forEach(this::convertVal);
  // return in;
  // }

  public double[] convert(double[] in) {
    return Arrays.stream(in).parallel().map(num -> convert(new Double(num))).toArray();
  }

  public double convert(Number val) {
    return scaleOffset.applyScaleOffset(val);
  }

  public double getMean() {
    return mean;
  }

  public double getStdDev() {
    return stdDev;
  }
}

