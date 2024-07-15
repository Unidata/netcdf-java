package ucar.nc2.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import ucar.nc2.constants.CDM;
import java.util.Set;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.AttributeContainer;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.VariableDS;

public class Normalizer implements Enhancement {

  private final ScaleOffset scaleOffset;
  private final double minimum;
  private final double range; // maximum - minimum
  private static String name = "Normalizer";
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

  public double convert(double val) {
    return scaleOffset.applyScaleOffset(val);
  }

  public double getMinimum() {
    return minimum;
  }

  public double getRange() {
    return range;
  }
  public static class Provider implements EnhancementProvider {

    @Override
    public void Create(VariableDS var){
      var.normalizer=Normalizer.createFromVariable(var);

    }
    @Override
    public String getName() {
      return name;
    }
    @Override
    public boolean canDo (Set<Enhance> enhancements){
      if (enhancements.contains(Enhance.ApplyNormalizer)) {
        return true;
      }
      return false;
    }

    @Override
    public boolean appliesTo(Enhance enhance, AttributeContainer attributes) {
      return enhance == Enhance.ApplyNormalizer && attributes.findAttribute(CDM.NORMALIZE)!= null;
    }
    @Override
    public boolean appliesTo(Enhance enhance,VariableDS var){
      return enhance == Enhance.ApplyNormalizer && var.normalizer!= null;
    }
@Override
    public Normalizer returnObject( VariableDS var){
      return var.normalizer;
    }




  }

}
