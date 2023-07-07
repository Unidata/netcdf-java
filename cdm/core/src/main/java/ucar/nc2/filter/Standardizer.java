package ucar.nc2.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.dataset.VariableDS;

public class Standardizer {

  private final ScaleOffset scaleOffset;
  private static final String name = "Standardizer";
  private static final int id = -1;
  private final double mean;
  private final double stdDev;

  public static Standardizer createFromVariable(VariableDS var) {
    try {
      Array arr = var.read();
      DataType type = var.getDataType();
      return new Standardizer(arr, type);
    } catch (IOException e) {
      return new Standardizer(0.0, 1.0, var.getDataType());
    }
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

  public Standardizer(Array arr, DataType type) {
    this(calculateMean(arr), calculateStandardDeviation(arr), type);
  }

  private static double calculateMean(Array arr) {
    SummaryStatistics cur = new SummaryStatistics();
    IndexIterator iterArr = arr.getIndexIterator();
    while (iterArr.hasNext()) {
      Number value = (Number) iterArr.getObjectNext();
      if (!Double.isNaN(value.doubleValue())) {
        cur.addValue(value.doubleValue());
      }
    }
    return cur.getMean();
  }

  private static double calculateStandardDeviation(Array arr) {
    SummaryStatistics cur = new SummaryStatistics();
    IndexIterator iterArr = arr.getIndexIterator();
    while (iterArr.hasNext()) {
      Number value = (Number) iterArr.getObjectNext();
      if (!Double.isNaN(value.doubleValue())) {
        cur.addValue(value.doubleValue());
      }
    }
    return cur.getStandardDeviation();
  }

  public Array convert(Array arr) {
    return scaleOffset.applyScaleOffset(arr);
  }

  public double getMean() {
    return mean;
  }

  public double getStdDev() {
    return stdDev;
  }
}

