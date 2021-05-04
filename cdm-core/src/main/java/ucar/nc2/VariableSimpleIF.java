/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;

import ucar.array.ArrayType;
import ucar.ma2.DataType;

/** A lightweight abstraction of an immutable Variable. */
public interface VariableSimpleIF extends Comparable<VariableSimpleIF> {

  /** full, backslash escaped name of the data Variable */
  String getFullName();

  /** short name of the data Variable */
  String getShortName();

  /** description of the Variable, or null if none. */
  @Nullable
  String getDescription();

  /** Units of the Variable, or null if none. */
  @Nullable
  String getUnitsString();

  /** Variable rank */
  int getRank();

  /** Variable shape */
  int[] getShape();

  /** Dimension List. empty for a scalar variable. */
  ImmutableList<Dimension> getDimensions();

  /**
   * Variable's data type.
   * 
   * @deprecated use getArrayType
   */
  @Deprecated
  DataType getDataType();

  /** Variable's array type */
  ArrayType getArrayType();

  /** Attributes for the variable. */
  AttributeContainer attributes();

}
