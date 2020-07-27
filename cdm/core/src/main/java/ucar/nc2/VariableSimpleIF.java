/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import ucar.ma2.DataType;
import java.util.List;

/**
 * A lightweight abstraction of a Variable.
 */
public interface VariableSimpleIF extends Comparable<VariableSimpleIF> {

  /**
   * full, backslash escaped name of the data Variable
   * 
   * @return full, backslash escaped name of the data Variable
   */
  String getFullName();

  /**
   * short name of the data Variable
   * 
   * @return short name of the data Variable
   */
  String getShortName();

  /**
   * description of the Variable
   * 
   * @return description of the Variable, or null if none.
   */
  String getDescription();

  /**
   * Units of the Variable. These should be udunits compatible if possible
   * 
   * @return Units of the Variable, or null if none.
   */
  String getUnitsString();

  /**
   * Variable rank
   * 
   * @return Variable rank
   */
  int getRank();

  /**
   * Variable shape
   * 
   * @return Variable shape
   */
  int[] getShape();

  /**
   * Dimension List. empty for a scalar variable.
   * 
   * @return List of ucar.nc2.Dimension, ImmutableList in ver6
   */
  List<Dimension> getDimensions();

  /**
   * Variable's data type
   * 
   * @return Variable's data type
   */
  DataType getDataType();

  /** Attributes for the variable. */
  AttributeContainer attributes();

}
