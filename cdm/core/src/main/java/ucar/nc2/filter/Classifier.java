package ucar.nc2.filter;


import java.util.Map;
import java.util.Set;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.Attribute;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.util.Misc;
import java.util.ArrayList;
import java.util.List;
import ucar.unidata.io.spi.RandomAccessFileProvider;



import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.filter.*;
import ucar.nc2.internal.dataset.CoordinatesHelper;
import ucar.nc2.util.CancelTask;
import ucar.nc2.util.Misc;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
public class Classifier implements Enhancement {


  private String[] AttCat;
  private List<int[]> rules = new ArrayList<>();

  private static String name = "Classifier";



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

  public static Classifier createFromVariable(VariableDS var) {
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
    return classifyArrayAttribute(val);
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

  public static class Provider implements EnhancementProvider {

    @Override
    public void Create(VariableDS var){
      var.classifier=Classifier.createFromVariable(var);
    }



    @Override
    public String getName() {
      return name;
    }
    @Override
    public boolean canDo (Set<Enhance> enhancements){
      if (enhancements.contains(Enhance.ApplyClassifier)) {
        return true;
      }
      return false;
    }

//    Attribute findAttribute(String attName);

    @Override
    public boolean appliesTo(Enhance enhance,  AttributeContainer attributes) {
      return enhance == Enhance.ApplyClassifier && attributes.findAttribute(CDM.CLASSIFY)!= null;
    }
//    @Override
//    public void applyEnhancement(Object instance) {
//      Attribute classifierAtt = findAttribute(CDM.CLASSIFY);
//      if (classifierAtt != null && instance.enhanceMode.contains(Enhance.ApplyClassifier) && instance.dataType.isNumeric()) {
//        instance.classifier = Classifier.createFromVariable(instance);
//      }

  }




  }




