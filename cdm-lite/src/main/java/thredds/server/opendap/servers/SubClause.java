/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.opendap.servers;

import opendap.dap.BaseType;

/**
 * Represents a sub-clause of the selection portion of a constraint
 * expression. A sub-clause is any part of a constraint that
 * can be evaluated to a BaseType value. For instance, the constraint
 * "var1>=function(var2,var3)" would have sub clauses "var1" and
 * "function(var2,var3)". The latter would in turn have the sub-clauses
 * "var2" and "var3".
 * <p>
 * <p/>
 * A given instance of SubClause may change the value it returns when
 * evaluated multiple times, but should not change the class of BaseType
 * it returns. This allows function and operator clauses to do type-checking
 * using the getValue() method.
 * <p/>
 * The parser supports several kinds of sub-clause. These are described
 * in the ClauseFactory interface.
 * <p/>
 * <p>
 * See <code>TopLevelClause</code> for more about the parsing of clauses.
 * <p>
 * See <code>CEEValuator</code> for an explanation of how Clauses
 * are evaluated on data.
 *
 * @author joew
 * @see TopLevelClause
 * @see CEEvaluator
 * @see ClauseFactory
 */
public interface SubClause extends Clause {

  /**
   * Returns the Clause which contains this subclause. The clause returned
   * may be a TopLevelClause or another SubClause.
   */
  public Clause getParent();

  /**
   * Returns a BaseType containing the current value of the sub-clause.
   * Sub-clauses that are not constant have an undefined value until the
   * evaluate() method has been called. However, in such circumstances
   * this method is still useful, as it indicates which class of
   * BaseType the sub-clause will evaluate to. Implementations of this
   * method should never return null.
   */
  public BaseType getValue();

  /**
   * Evaluates the clause, first calling evaluate() on any sub-clauses it
   * contains. Implementations of this method should flag the clause as
   * "defined" if the evaluation is successful.
   *
   * @throws DAP2ServerSideException Thrown if the evaluation fails for any reason.
   */
  public BaseType evaluate() throws DAP2ServerSideException;

  /**
   * Sets the parent of this subclause. Used during parsing.
   */
  public void setParent(Clause parent);
}


