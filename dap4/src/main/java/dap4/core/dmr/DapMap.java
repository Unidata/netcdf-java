/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.core.dmr;

/**
 * This class holds a reference to a map variable.
 */

public class DapMap extends DapNode {
  DapVariable actualvar = null;

  String target = null;

  //////////////////////////////////////////////////
  // Constructors

  public DapMap() {
    super();
  }

  public DapMap(String target) {
    super();
    this.target = target;
    // Use for toString()
    this.setShortName(this.target);
  }

  //////////////////////////////////////////////////
  // Get/set

  public String getTargetName() {
    if (this.actualvar != null)
      return this.actualvar.getFQN();
    else
      return this.target;
  }

  public DapVariable getVariable() {
    return this.actualvar;
  }

  public void setVariable(DapVariable var) {
    this.actualvar = var;
  }

  //////////////////////////////////////////////////
  // Overrides

  public String getShortName() {
    if (actualvar != null)
      return actualvar.getShortName();
    return null;
  }

  public String getFQN() {
    if (actualvar != null)
      return actualvar.getFQN();
    return null;
  }

} // class DapMap

