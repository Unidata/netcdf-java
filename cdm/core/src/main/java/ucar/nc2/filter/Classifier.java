package ucar.nc2.filter;

import java.io.IOException;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.dataset.VariableDS;

public class Classifier implements Enhancement {
  private Classifier classifier = null;
  private static Classifier emptyClassifier;
  private int classifiedVal;
  private int[] classifiedArray;

  public static Classifier createFromVariable(VariableDS var) {
    try {
      Array arr = var.read();
      // DataType type = var.getDataType();
      return emptyClassifier();
    } catch (IOException e) {
      return emptyClassifier();
    }
  }

  public static Classifier emptyClassifier() {
    emptyClassifier = new Classifier();
    return emptyClassifier;
  }

  /** Enough of a constructor */
  public Classifier() {}

  /** Classify double array */
  public int[] classifyDoubleArray(Array arr) {
    int[] classifiedArray = new int[(int) arr.getSize()];
    int i = 0;
    IndexIterator iterArr = arr.getIndexIterator();
    while (iterArr.hasNext()) {
      Number value = (Number) iterArr.getObjectNext();
      if (!Double.isNaN(value.doubleValue())) {

        classifiedArray[i] = classifyArray(value.doubleValue());
      }
      i++;
    }
    return classifiedArray;
  }



  /** for a single double */
  public int classifyArray(double val) {
    if (val >= 0) {
      classifiedVal = 1;
    } else {
      classifiedVal = 0;
    }

    return classifiedVal;
  }

  @Override
  public double convert(double val) {
    return emptyClassifier.classifyArray(val);
  }


}


