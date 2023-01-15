/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.core.interfaces;

import dap4.core.dmr.DapNode;

public interface CEConstraintIF {

  //////////////////////////////////////////////////
  // Reference processing

  /**
   * Reference X match
   *
   * @param node to test
   * @return true if node is referenced by this constraint
   */

  public boolean references(DapNode node);

}
