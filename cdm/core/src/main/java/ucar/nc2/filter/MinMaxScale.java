package ucar.nc2.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.dataset.VariableDS;

public class Standard_Leo implements Enhancement {
  private double min;
  private double max;
  private double newMin;
  private double newMax;

  private final ScaleOffset scaleOffset;
  private final double mean;
  private final double stdDev;
/** VariableDS format is enhanced variable */
  public static Standard_Leo createFromVariable(VariableDS var) {
    try {
      Array arr = var.read();
      DataType type = var.getDataType();
      return createFromArray(arr, type);
    } catch (IOException e) {
      return new Standard_Leo(0.0, 1.0, var.getDataType());
    }
  }

  public static Standard_Leo createFromArray(Array arr, DataType type) {
    SummaryStatistics statistics = calculationHelper(arr);
    if (statistics.getStandardDeviation() == 0) {
      return new Standard_Leo(0.0, 1.0, type);
    }
    return new Standard_Leo(statistics.getMean(), statistics.getStandardDeviation(), type);
  }

  private Standard_Leo(double mean, double stdDev, DataType type) {
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

  public double convert(double val) {
    return scaleOffset.applyScaleOffset(val);
  }

  public double getMean() {
    return mean;
  }

  public double getStdDev() {
    return stdDev;
  }
}

