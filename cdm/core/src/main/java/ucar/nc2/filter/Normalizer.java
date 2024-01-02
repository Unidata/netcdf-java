package ucar.nc2.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.dataset.VariableDS;

public class Normalizer {

  private final ScaleOffset scaleOffset;
  private final double minimum;
  private final double range; // maximum - minimum

  public static Normalizer createFromVariable(VariableDS var) {
    try {
      Array arr = var.read();
      DataType type = var.getDataType();
      return createFromArray(arr, type);
    } catch (IOException e) {
      return new Normalizer(0.0, 1.0, var.getDataType());
    }
  }

  public static Normalizer createFromArray(Array arr, DataType type) {
    SummaryStatistics statistics = calculationHelper(arr);
    if ((statistics.getMax() - statistics.getMin()) == 0) {
      return new Normalizer(0.0, 1.0, type);
    }
    return new Normalizer(statistics.getMin(), (statistics.getMax() - statistics.getMin()), type);
  }

  private Normalizer(double minimum, double range, DataType type) {
    this.minimum = minimum;
    this.range = range;
    Map<String, Object> props = new HashMap<>();
    props.put("offset", minimum);
    props.put("scale", 1 / range);
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

  public double getMinimum() {
    return minimum;
  }

  public double getRange() {
    return range;
  }

}
