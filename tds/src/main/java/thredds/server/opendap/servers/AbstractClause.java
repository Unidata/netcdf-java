/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.opendap.servers;

import java.util.List;

/**
 * Provides default implementations for the Clause interface methods.
 * This eliminates redundant code and provides a starting point
 * for new implementations of Clause.
 * <p>
 * <p/>
 * Note that every Clause object is expected to implement either
 * SubClause or TopLevelClause. No default implementations are provided
 * for the methods of these subinterfaces
 * <p>
 * <p/>
 * Also note that it is not <i>necessary</i> to use this class to
 * create your own implementations, it's just a convenience.
 * <p>
 * <p/>
 * The class has no abstract methods, but is declared abstract because
 * it should not be directly instantiated.
 *
 * @author joew
 */
public abstract class AbstractClause implements Clause {

  public List getChildren() {
    return children;
  }

  public boolean isConstant() {
    return constant;
  }

  public boolean isDefined() {
    return defined;
  }

  /**
   * Value to be returned by isConstant(). Should not change for
   * the lifetime of the object.
   */
  protected boolean constant;

  /**
   * Value to be returned by isDefined(). May change during the
   * lifetime of the object.
   */
  protected boolean defined;

  /**
   * A list of SubClause objects representing
   * the sub-clauses of this clause. Use caution when modifying
   * this list other than at the point of creation, since methods
   * such as evaluate() depend on it.
   */
  protected List children;
}


