/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.opendap.servers;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import thredds.server.opendap.servers.parsers.ExprParserConstants;


/**
 * Represents a clause which compares subclauses, using one of the
 * relative operators supported by the Operator class.
 *
 * @author joew
 * @see Operator
 * @see ClauseFactory
 */
public class RelOpClause extends AbstractClause implements TopLevelClause {
  /**
   * Creates a new RelOpClause. If the lhs and all the elements of the rhs
   * are constant, the RelOpClause will be flagged as constant, and
   * evaluated immediately.
   *
   * @param operator The operator invoked by the clause
   * @param lhs The left-hand side of the comparison.
   * @param rhs A list of SubClauses representing the right-hand side of the
   *        comparison.
   * @throws DAP2ServerSideException Thrown if the clause is constant, but
   *         the attempt to evaluate it fails.
   */
  protected RelOpClause(int operator, SubClause lhs, List rhs) throws DAP2ServerSideException {

    this.operator = operator;
    this.lhs = lhs;
    this.rhs = rhs;
    this.children = new ArrayList();
    children.add(lhs);
    children.addAll(rhs);
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

    if (constant && defined) {
      return value;
    }

    if (rhs.size() == 1) {
      value = Operator.op(operator, lhs.evaluate(), ((SubClause) rhs.get(0)).evaluate());
    } else {
      value = false;
      Iterator it = rhs.iterator();
      while (it.hasNext() && !value) {
        if (Operator.op(operator, lhs.evaluate(), ((SubClause) it.next()).evaluate())) {
          value = true;
        }
      }
    }
    defined = true;
    return value;
  }

  /**
   * Returns a SubClause representing the right-hand side of the
   * comparison.
   */
  public SubClause getLHS() {
    return lhs;
  }

  /**
   * Returns a list of SubClauses representing the right-hand side of the
   * comparison.
   */
  public List getRHS() {
    return rhs;
  }

  /**
   * Returns the type of comparison
   *
   * @see ExprParserConstants
   */
  public int getOperator() {
    return operator;
  }

  /**
   * Prints the original string representation of this clause.
   * For use in debugging.
   */
  public void printConstraint(PrintWriter os) {
    lhs.printConstraint(os);
    String op = ExprParserConstants.tokenImage[operator];
    op = op.substring(1, op.length() - 1);
    os.print(op);
    // os.print(ExprParserConstants.tokenImage[operator].substring(2, 3));
    if (rhs.size() == 1) {
      ((ValueClause) rhs.get(0)).printConstraint(os);
    } else {
      os.print("{");
      Iterator it = rhs.iterator();
      boolean first = true;
      while (it.hasNext()) {
        if (!first)
          os.print(",");
        ((ValueClause) it.next()).printConstraint(os);
        first = false;
      }
      os.print("}");
    }
  }

  protected boolean value;

  protected int operator;

  protected SubClause lhs;

  protected List rhs;
}
