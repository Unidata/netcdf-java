package ucar.nc2.filter;

import java.io.IOException;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
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


  // Custom exception for invalid values ?
  public static class InvalidValueException extends Exception {
    public InvalidValueException(String message) {
      super(message);
    }
  }

  // Method to classify int array
  public int classifyArray(double val) {
    int classifiedVal;
    if (val >= 0) {
      classifiedVal = 1;
    } else {
      classifiedVal = 0;
    }

    return classifiedVal;
  }

  /** for a single value? */
  public int classifyArray(int val) {
    int classifiedVal;
    if (val >= 0) {
      classifiedVal = 1;
    } else {
      classifiedVal = 0;
    }

    return classifiedVal;
  }

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

  // Method to classify double array
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

  // Method to classify float array
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


  public static void main(String[] args) {
    try {
      // Example usage
      Classifier classifier = new Classifier();

      int[] intArray = {10, -5, 0, 20, 5};
      double[] doubleArray = {10.5, -5.5, 0.0, 20.1, 5.0};
      float[] floatArray = {10.5f, -5.5f, 0.0f, 20.1f, 5.0f};

      int[] intResult = classifier.classifyArray(intArray);
      int[] doubleResult = classifier.classifyArray(doubleArray); // This will throw an exception
      int[] floatResult = classifier.classifyArray(floatArray); // This will throw an exception

      // Print the classified arrays
      System.out.print("Classified int array: ");
      for (int value : intResult) {
        System.out.print(value + " ");
      }
      System.out.println();

      System.out.print("Classified double array: ");
      for (int value : doubleResult) {
        System.out.print(value + " ");
      }
      System.out.println();

      System.out.print("Classified float array: ");
      for (int value : floatResult) {
        System.out.print(value + " ");
      }
      System.out.println();

    } catch (InvalidValueException e) {
      System.err.println(e.getMessage());
    }
  }
}


