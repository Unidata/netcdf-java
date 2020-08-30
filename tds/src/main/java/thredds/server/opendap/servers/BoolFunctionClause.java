/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.opendap.servers;


import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

/**
 * Represents a clause which invokes a function that returns a boolean value.
 *
 * @author joew
 * @see ClauseFactory
 */
public class BoolFunctionClause extends AbstractClause implements TopLevelClause {

  /**
   * Creates a new BoolFunctionClause.
   *
   * @param function The function invoked by the clause
   * @param children A list of SubClauses, to be given as arguments
   *        to the function. If all the arguments are constant, the function
   *        clause will be flagged as constant, and evaluated immediatelyx.
   * @throws DAP2ServerSideException Thrown if either 1) the function does not
   *         accept the arguments given, or 2) the
   *         clause is constant, and the attempt to evaluate it fails.
   */
  protected BoolFunctionClause(BoolFunction function, List children) throws DAP2ServerSideException {

    function.checkArgs(children);
    this.function = function;
    this.children = children;
    this.constant = true;
    Iterator it = children.iterator();
    while (it.hasNext()) {
      SubClause current = (SubClause) it.next();
      current.setParent(this);
      if (!current.isConstant()) {
        constant = false;
      }
    }
    if (constant) {
      evaluate();
    }
  }

  public boolean getValue() {
    return value;
  }

  public boolean evaluate() throws DAP2ServerSideException {

    if (!constant || !defined) {
      defined = true;
      value = function.evaluate(children);
    }
    return value;
  }

  /**
   * Returns the server-side function invoked by this clause
   */
  public BoolFunction getFunction() {
    return function;
  }

  /**
   * Prints the original string representation of this clause.
   * For use in debugging.
   */
  public void printConstraint(PrintWriter os) {
    os.print(function.getName() + "(");
    Iterator it = children.iterator();
    boolean first = true;
    while (it.hasNext()) {
      ValueClause vc = (ValueClause) it.next();
      if (!first)
        os.print(",");
      vc.printConstraint(os);
      first = false;
    }
    os.print(")");
  }

  protected BoolFunction function;

  protected boolean value;
}


