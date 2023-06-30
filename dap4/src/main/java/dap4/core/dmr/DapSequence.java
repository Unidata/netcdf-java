/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.core.dmr;

public class DapSequence extends DapStructure {

  //////////////////////////////////////////////////
  // Constructors

  public DapSequence(String name) {
    super(name);
    setTypeSort(TypeSort.Sequence);
  }

} // class DapSequence

