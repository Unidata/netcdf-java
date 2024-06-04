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
      DataType type = var.getDataType();
      return createFromArray(arr, type);
    } catch (IOException e) {
      return emptyClassifier();
    }
  }

  public static Classifier createFromArray(Array arr, DataType type) {
    /** what is the type of array? */
    return new Classifier(arr, type);

  }

  public static Classifier emptyClassifier() {
    emptyClassifier = new Classifier();
    return emptyClassifier;
  }

  /** wEmpty in case data not good */
  public Classifier() {}

  private Classifier(Array arr, DataType type) {
    classifier = new Classifier();
    /** how about doubles? */

  }

  /** Classify integer array */
  public int[] classifyIntArray(Array arr) {
    classifiedArray = new int[(int) arr.getSize()];
    int i = 0;
    IndexIterator iterArr = arr.getIndexIterator();
    while (iterArr.hasNext()) {
      Number value = (Number) iterArr.getObjectNext();
      if (value.intValue() < 0) {
        classifiedArray[i] = 0;
      } else {
        classifiedArray[i] = 1;
      }
      i++;
    }

    return classifiedArray;
  }

  /** Classify double array */
  public int[] classifyDoubleArray(Array arr) {
    int[] classifiedArray = new int[(int) arr.getSize()];
    int i = 0;
    IndexIterator iterArr = arr.getIndexIterator();
    while (iterArr.hasNext()) {
      Number value = (Number) iterArr.getObjectNext();
      if (!Double.isNaN(value.doubleValue())) {
        if (value.doubleValue() < 0) {
          classifiedArray[i] = 0;
        } else {
          classifiedArray[i] = 1;
        }
      }
      i++;
    }
    return classifiedArray;
  }

  /** Classify float array */
  public int[] classifyFloatArray(Array arr) {
    int[] classifiedArray = new int[(int) arr.getSize()];
    int i = 0;
    IndexIterator iterArr = arr.getIndexIterator();
    while (iterArr.hasNext()) {
      Number value = (Number) iterArr.getObjectNext();
      if (!Float.isNaN(value.floatValue())) {
        if (value.floatValue() < 0) {
          classifiedArray[i] = 0;
        } else {
          classifiedArray[i] = 1;
        }
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

  /** for a single float */
  public int classifyArray(float val) {
    if (val >= 0) {
      classifiedVal = 1;
    } else {
      classifiedVal = 0;
    }

    return classifiedVal;
  }

  /** for a single int ? */
  public int classifyArray(int val) {
    if (val >= 0) {
      classifiedVal = 1;
    } else {
      classifiedVal = 0;
    }

    return classifiedVal;
  }

  /**
   * Method to classify int array
   * maybe not needed if enhancement applied only to doubles and floats?
   */



  @Override
  public double convert(double val) {
    return classifier.classifyArray(val);
  }

  public double convert(float val) {
    return classifier.classifyArray(val);
  }

  public double convert(int val) {
    return classifier.classifyArray(val);
  }
}


