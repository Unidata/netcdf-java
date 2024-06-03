package ucar.nc2.filter;

import java.io.IOException;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.dataset.VariableDS;

public class Classifier implements Enhancement {

  public static Classifier createFromVariable(VariableDS var) {
    try {
      Array arr = var.read();
      DataType type = var.getDataType();
      return createFromArray(arr, type);
    } catch (IOException e) {
      return new Classifier();
    }
  }

  public static Classifier createFromArray(Array arr, DataType type) {
    return new Classifier();
  }

  @Override
  public double convert(double val) {
    return classifyArray(val);
  }

  /** Custom exception for invalid values */
  public static class InvalidValueException extends Exception {
    public InvalidValueException(String message) {
      super(message);
    }
  }

  /** for a single double */
  public int classifyArray(double val) {
    int classifiedVal;
    if (val >= 0) {
      classifiedVal = 1;
    } else {
      classifiedVal = 0;
    }

    return classifiedVal;
  }

  /** for a single float */
  public int classifyArray(float val) {
    int classifiedVal;
    if (val >= 0) {
      classifiedVal = 1;
    } else {
      classifiedVal = 0;
    }

    return classifiedVal;
  }

  /** for a single int ? */
  public int classifyArray(int val) {
    int classifiedVal;
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

  /** Classify integer array */
  public int[] classifyArray(int[] inputArray) throws InvalidValueException {
    int[] classifiedArray = new int[inputArray.length];

    for (int i = 0; i < inputArray.length; i++) {
      if (inputArray[i] >= 0) {
        classifiedArray[i] = 1;
      } else {
        classifiedArray[i] = 0;
      }
    }
    return classifiedArray;
  }

  /** Classify double array */
  public int[] classifyArray(double[] inputArray) throws InvalidValueException {
    int[] classifiedArray = new int[inputArray.length];

    for (int i = 0; i < inputArray.length; i++) {
      if (Double.isNaN(inputArray[i])) {
        throw new InvalidValueException("Array contains NaN values");
      }
      if (inputArray[i] >= 0) {
        classifiedArray[i] = 1;
      } else {
        classifiedArray[i] = 0;
      }
    }

    return classifiedArray;
  }

  /** Classify float array */
  public int[] classifyArray(float[] inputArray) throws InvalidValueException {
    int[] classifiedArray = new int[inputArray.length];

    for (int i = 0; i < inputArray.length; i++) {
      if (Float.isNaN(inputArray[i])) {
        throw new InvalidValueException("Array contains NaN values");
      }
      if (inputArray[i] >= 0) {
        classifiedArray[i] = 1;
      } else {
        classifiedArray[i] = 0;
      }
    }

    return classifiedArray;
  }

}


