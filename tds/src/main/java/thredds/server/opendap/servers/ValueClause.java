/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.opendap.servers;

import java.io.PrintWriter;
import java.util.ArrayList;
import opendap.dap.BaseType;

/**
 * Represents a clause containing a simple value. If the value is
 * an constant value such as "2.0", the clause's isConstant() method
 * will return true; if it is a variable of the dataset, isConstant() will
 * return false.
 *
 * @author Joe Wielgosz (joew@cola.iges.org)
 * @see ClauseFactory
 */
public class ValueClause extends AbstractClause implements SubClause {
  protected BaseType value;
  protected Clause parent;

  /**
   * Creates a new ValueClause.
   *
   * @param value The BaseType represented by this clause. This can
   *        be either a BaseType taken from the DDS of a dataset, or a BaseType
   *        object created to hold a constant value.
   * @param constant Should be set to false if the value parameter is
   *        from the DDS of a dataset, and true if the value parameter is a
   *        constant value.
   */
  protected ValueClause(BaseType value, boolean constant) {
    this.value = value;
    this.constant = constant;
    this.defined = constant;
    this.children = new ArrayList<>();
  }

  /**
   * Returns the BaseType represented by this clause.
   */
  public BaseType getValue() {
    return value;
  }

  /**
   * Returns the BaseType represented by this clause. Equivalent to
   * getValue(), except that
   * calling this method flags this clause as "defined".
   */
  public BaseType evaluate() {
    defined = true;
    return value;
  }

  public Clause getParent() {
    return parent;
  }

  public void setParent(Clause parent) {
    this.parent = parent;
  }

  /**
   * Prints the original string representation of this clause.
   * For use in debugging.
   */
  public void printConstraint(PrintWriter os) {
    if (constant) {
      value.printVal(os, "", false);
    } else {
      value.printConstraint(os);
    }
  }

}


