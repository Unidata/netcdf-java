package ucar.nc2.filter;

import java.io.IOException;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.Attribute;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class Classifier implements Enhancement {
  private Classifier classifier = null;
  private static Classifier emptyClassifier;
  private int classifiedVal;
  private List<Attribute> AttCat;
  private List<int[]> rules = new ArrayList<>();

  public static Classifier createFromVariable(VariableDS var) {
    try {
      Array arr = var.read();
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

  public Classifier(List<Attribute> AttCat) {
    this.AttCat = AttCat;
    this.rules = loadClassificationRules();

  }

  public int[] classifyWithAttributes(Array arr) {
    int[] classifiedArray = new int[(int) arr.getSize()];
    IndexIterator iterArr = arr.getIndexIterator();
    int i = 0;
    while (iterArr.hasNext()) {
      Number value = (Number) iterArr.getObjectNext();
      if (!Double.isNaN(value.doubleValue())) {
        classifiedArray[i] = classifyArrayAttribute(value.doubleValue());
      } else {
        classifiedArray[i] = Integer.MIN_VALUE;
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

  public int classifyArrayAttribute(double val) {
    for (int[] rule : rules) {
      if (val > rule[0] && val <= rule[1]) {
        return rule[2]; // Return the matched rule's value
      }
    }
    // Return min possible int if no rule matches
    return Integer.MIN_VALUE;
  }

  // Method to load classification rules from the attributes
  public List<int[]> loadClassificationRules() {
    for (Attribute attribute : this.AttCat) {
      int[] rule = stringToIntArray(attribute.getStringValue());
      this.rules.add(rule);
    }
    return rules;
  }

  @Override
  public double convert(double val) {
    return emptyClassifier.classifyArray(val);
  }

  public static int[] stringToIntArray(String str) {
    String[] stringArray = str.split(" "); // Split the string by spaces
    int[] intArray = new int[stringArray.length]; // Create an array to hold the parsed integers

    for (int i = 0; i < stringArray.length; i++) {

      double value = Double.parseDouble(stringArray[i]); // Parse each string to a double

      if (Double.isNaN(value)) {
        // Check if the entry is NaN and assign Integer.MIN_VALUE or Integer.MAX_VALUE based on the index
        if (i == 0) {
          intArray[i] = Integer.MIN_VALUE;
        } else if (i == 1) {
          intArray[i] = Integer.MAX_VALUE;
        } else {
          intArray[i] = -99999; // Default value for other indices if needed
        }
      } else {
        intArray[i] = (int) value; // Convert the value to int if it is not NaN
      }

    }

    return intArray;
  }

}
