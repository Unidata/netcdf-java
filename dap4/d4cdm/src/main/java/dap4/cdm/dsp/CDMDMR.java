/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.cdm.dsp;

import dap4.core.dmr.*;

public class CDMDMR {
  //////////////////////////////////////////////////

  public static class CDMAttribute extends DapAttribute {
    public CDMAttribute(String name, DapType basetype) {
      super(name, basetype);
    }
  }

  public static class CDMAttributeSet extends DapAttributeSet {
    public CDMAttributeSet(String name) {
      super(name);
    }
  }

  public static class CDMDimension extends DapDimension {
    public CDMDimension(String name, long size) {
      super(name, size);
    }
  }

  public static class CDMMap extends DapMap {
    public CDMMap(DapVariable target) {
      super(target);
    }
  }

  public abstract static class CDMVariable extends DapVariable {
    public CDMVariable(String name, DapType t) {
      super(name, t);
    }
  }

  public static class CDMGroup extends DapGroup {
    public CDMGroup(String name) {
      super(name);
    }
  }

  public static class CDMDataset extends DapDataset {
    public CDMDataset(String name) {
      super(name);
    }
  }

  public static class CDMEnumeration extends DapEnumeration {
    public CDMEnumeration(String name, DapType basetype) {
      super(name, basetype);
    }
  }

  public static class CDMEnumConst extends DapEnumConst {
    public CDMEnumConst(String name, long value) {
      super(name, value);
    }
  }

  public static class CDMStructure extends DapStructure {
    public CDMStructure(String name) {
      super(name);
    }
  }

  public static class CDMSequence extends DapSequence {
    public CDMSequence(String name) {
      super(name);
    }
  }

  public static class CDMOtherXML extends DapOtherXML {
    public CDMOtherXML(String name) {
      super(name);
    }
  }
}
