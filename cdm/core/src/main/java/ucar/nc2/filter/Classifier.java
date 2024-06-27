package ucar.nc2.filter;


import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.Attribute;
import ucar.nc2.util.Misc;
import java.util.ArrayList;
import java.util.List;


public class Classifier implements Enhancement {
  private int classifiedVal;
  private String[] AttCat;
  private List<int[]> rules = new ArrayList<>();

  public Classifier() {
    this.AttCat = new String[0];
    this.rules = new ArrayList<>();
  }

  // Constructor with attributes
  public Classifier(String[] AttCat) {
    this.AttCat = AttCat;
    this.rules = loadClassificationRules();
  }

  // Factory method to create a Classifier from a Variable
  public static Classifier createFromVariable(Variable var) {
    List<Attribute> attributes = var.attributes().getAttributes();

    for (Attribute attribute : attributes) {
      // check like this, or something else?
      if (attribute == var.attributes().findAttribute(CDM.CLASSIFY)) {
        String[] sets = attribute.getStringValue().split(";");
        for (int i = 0; i < sets.length; i++) {
          // trim and clean so it's ready
          sets[i] = sets[i].trim();
        }
        return new Classifier(sets);
      }
    }

    return new Classifier();

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
      if (val > rule[0] && val <= rule[1] + Misc.defaultMaxRelativeDiffFloat) {
        return rule[2];
      }
    }
    // Return min possible int if no rule matches
    return Integer.MIN_VALUE;
  }

  // Method to load classification rules from the attributes
  private List<int[]> loadClassificationRules() {
    for (String rules : this.AttCat) {
      int[] rule = stringToIntArray(rules);
      this.rules.add(rule);
    }
    return rules;
  }

  @Override
  public double convert(double val) {
    return classifyArray(val);
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
