/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.util;

import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * A parameter has a name and a value that is String, a double, or an array of doubles.
 * 
 * @deprecated use Attribute
 */
@Deprecated
@Immutable
public class Parameter {
  private final String name;
  private final String valueS;
  private final double[] valueD;
  private final boolean isString;

  /**
   * Copy constructor, with new name.
   *
   * @param name name of new Parameter.
   * @param from copy values from here.
   */
  public Parameter(String name, Parameter from) {
    this.name = name;
    this.valueS = from.valueS;
    this.valueD = from.valueD;
    this.isString = from.isString;
  }

  /**
   * Create a String-valued param.
   *
   * @param name name of new Parameter.
   * @param val value of Parameter
   */
  public Parameter(String name, String val) {
    this.name = name;
    this.valueS = val;
    this.valueD = null;
    this.isString = true;
  }

  /**
   * Create a scalar double-valued param.
   *
   * @param name name of new Parameter.
   * @param value value of Parameter
   */
  public Parameter(String name, double value) {
    this.name = name;
    valueD = new double[1];
    valueD[0] = value;
    this.isString = false;
    this.valueS = null;
  }

  /**
   * Create an array double-valued param.
   *
   * @param name name of new Parameter.
   * @param value value of Parameter
   */
  public Parameter(String name, double[] value) {
    this.name = name;
    valueD = value.clone();
    this.isString = false;
    this.valueS = null;
  }

  /**
   * Get the name of this Parameter.
   *
   * @return name
   */
  public String getName() {
    return name;
  }

  /**
   * True if value is a String.
   *
   * @return if its String valued
   */
  public boolean isString() {
    return isString;
  }

  /**
   * Retrieve String value.
   *
   * @return String value if this is a String valued attribute, else null.
   */
  @Nullable
  public String getStringValue() {
    return valueS;
  }

  /**
   * Retrieve numeric value, use if isString() is false.
   * Equivalent to <code>getNumericValue(0)</code>
   *
   * @return the first element of the value array, or Exception if its a String.
   */
  public double getNumericValue() {
    return valueD[0];
  }

  /**
   * Get the ith numeric value.
   *
   * @param i index
   * @return ith numeric value, or Exception if its a String.
   */
  public double getNumericValue(int i) {
    return valueD[i];
  }

  /**
   * Get the number of double values.
   *
   * @return the number of values, 0 is its a string.
   */
  public int getLength() {
    return isString ? 0 : valueD.length;
  }

  /**
   * Get array of numeric values as doubles.
   * Do not modify unless you own this object!
   *
   * @return array of numeric values. LOOK: will return copy of array in ver 6.
   */
  @Nullable
  public double[] getNumericValues() {
    return valueD;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Parameter parameter = (Parameter) o;

    if (isString != parameter.isString) {
      return false;
    }
    if (!name.equals(parameter.name)) {
      return false;
    }
    if (!Objects.equals(valueS, parameter.valueS)) {
      return false;
    }
    return Arrays.equals(valueD, parameter.valueD);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + (valueS != null ? valueS.hashCode() : 0);
    result = 31 * result + Arrays.hashCode(valueD);
    result = 31 * result + (isString ? 1 : 0);
    return result;
  }

  @Override
  public String toString() {
    StringBuilder buff = new StringBuilder();
    buff.append(getName());
    if (isString()) {
      buff.append(" = ");
      buff.append(valueS);
    } else {
      buff.append(" = ");
      for (int i = 0; i < getLength(); i++) {
        if (i != 0) {
          buff.append(", ");
        }
        buff.append(getNumericValue(i));
      }
    }
    return buff.toString();
  }
}
